/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.ASSERTIONS_DISABLED_FIELD_NAME
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.BaseExpressionCodegen
import org.jetbrains.kotlin.codegen.MemberCodegen
import org.jetbrains.kotlin.codegen.SamWrapperCodegen.SAM_WRAPPER_SUFFIX
import org.jetbrains.kotlin.codegen.`when`.WhenByEnumsMapping
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.context.CodegenContext
import org.jetbrains.kotlin.codegen.context.CodegenContextUtil
import org.jetbrains.kotlin.codegen.context.InlineLambdaContext
import org.jetbrains.kotlin.codegen.context.MethodContext
import org.jetbrains.kotlin.codegen.coroutines.unwrapInitialDescriptorForSuspendFunction
import org.jetbrains.kotlin.codegen.optimization.common.InsnSequence
import org.jetbrains.kotlin.codegen.optimization.common.asSequence
import org.jetbrains.kotlin.codegen.optimization.common.intConstant
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.util.Printer
import org.jetbrains.org.objectweb.asm.util.Textifier
import org.jetbrains.org.objectweb.asm.util.TraceMethodVisitor
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.math.max
import kotlin.math.min

const val GENERATE_SMAP = true
const val NUMBERED_FUNCTION_PREFIX = "kotlin/jvm/functions/Function"
const val INLINE_FUN_VAR_SUFFIX = "\$iv"

internal const val FIRST_FUN_LABEL = "$$$$\$ROOT$$$$$"
internal const val SPECIAL_TRANSFORMATION_NAME = "\$special"
const val INLINE_TRANSFORMATION_SUFFIX = "\$inlined"
internal const val INLINE_CALL_TRANSFORMATION_SUFFIX = "$" + INLINE_TRANSFORMATION_SUFFIX
internal const val INLINE_FUN_THIS_0_SUFFIX = "\$inline_fun"
internal const val DEFAULT_LAMBDA_FAKE_CALL = "$$\$DEFAULT_LAMBDA_FAKE_CALL$$$"
internal const val CAPTURED_FIELD_FOLD_PREFIX = "$$$"

private const val NON_LOCAL_RETURN = "$$$$\$NON_LOCAL_RETURN$$$$$"
const val CAPTURED_FIELD_PREFIX = "$"
private const val NON_CAPTURED_FIELD_PREFIX = "$$"
private const val INLINE_MARKER_CLASS_NAME = "kotlin/jvm/internal/InlineMarker"
private const val INLINE_MARKER_BEFORE_METHOD_NAME = "beforeInlineCall"
private const val INLINE_MARKER_AFTER_METHOD_NAME = "afterInlineCall"
private const val INLINE_MARKER_FINALLY_START = "finallyStart"

private const val INLINE_MARKER_FINALLY_END = "finallyEnd"
private const val INLINE_MARKER_BEFORE_SUSPEND_ID = 0
private const val INLINE_MARKER_AFTER_SUSPEND_ID = 1
private const val INLINE_MARKER_RETURNS_UNIT = 2
private const val INLINE_MARKER_FAKE_CONTINUATION = 3
private const val INLINE_MARKER_BEFORE_FAKE_CONTINUATION_CONSTRUCTOR_CALL = 4
private const val INLINE_MARKER_AFTER_FAKE_CONTINUATION_CONSTRUCTOR_CALL = 5
private const val INLINE_MARKER_BEFORE_INLINE_SUSPEND_ID = 6
private const val INLINE_MARKER_AFTER_INLINE_SUSPEND_ID = 7

internal fun getMethodNode(
    classData: ByteArray,
    methodName: String,
    methodDescriptor: String,
    classType: Type,
    signatureAmbiguity: Boolean = false
): SMAPAndMethodNode? {
    val cr = ClassReader(classData)
    var node: MethodNode? = null
    val debugInfo = arrayOfNulls<String>(2)

    cr.accept(object : ClassVisitor(Opcodes.API_VERSION) {
        override fun visitSource(source: String?, debug: String?) {
            super.visitSource(source, debug)
            debugInfo[0] = source
            debugInfo[1] = debug
        }

        override fun visitMethod(
            access: Int,
            name: String,
            desc: String,
            signature: String?,
            exceptions: Array<String>?
        ): MethodVisitor? {
            if (methodName != name || (signatureAmbiguity && access.and(Opcodes.ACC_SYNTHETIC) != 0)) return null

            if (methodDescriptor != desc) {
                val sameNumberOfParameters = Type.getArgumentTypes(methodDescriptor).size == Type.getArgumentTypes(desc).size
                if (!signatureAmbiguity || !sameNumberOfParameters) {
                    return null
                }
            }

            node?.let { existing ->
                throw AssertionError("Can't find proper '$name' method for inline: ambiguity between '${existing.name + existing.desc}' and '${name + desc}'")
            }
            node = MethodNode(Opcodes.API_VERSION, access, name, desc, signature, exceptions)
            return node!!
        }
    }, ClassReader.SKIP_FRAMES or if (GENERATE_SMAP) 0 else ClassReader.SKIP_DEBUG)

    if (node == null) {
        return null
    }

    val (first, last) = listOfNotNull(node).lineNumberRange()
    val smap = SMAPParser.parseOrCreateDefault(debugInfo[1], debugInfo[0], classType.internalName, first, last)
    return SMAPAndMethodNode(node!!, smap)
}

internal fun Collection<MethodNode>.lineNumberRange(): Pair<Int, Int> {
    var minLine = Int.MAX_VALUE
    var maxLine = Int.MIN_VALUE
    for (node in this) {
        for (insn in node.instructions.asSequence()) {
            if (insn is LineNumberNode) {
                minLine = min(minLine, insn.line)
                maxLine = max(maxLine, insn.line)
            }
        }
    }
    return minLine to maxLine
}

internal fun findVirtualFile(state: GenerationState, classId: ClassId): VirtualFile? {
    return VirtualFileFinder.getInstance(state.project, state.module).findVirtualFileWithHeader(classId)
}

internal fun findVirtualFileImprecise(state: GenerationState, internalClassName: String): VirtualFile? {
    val packageFqName = JvmClassName.byInternalName(internalClassName).packageFqName
    val classNameWithDollars = internalClassName.substringAfterLast("/", internalClassName)
    //TODO: we cannot construct proper classId at this point, we need to read InnerClasses info from class file
    // we construct valid.package.name/RelativeClassNameAsSingleName that should work in compiler, but fails for inner classes in IDE
    return findVirtualFile(state, ClassId(packageFqName, Name.identifier(classNameWithDollars)))
}

internal fun getInlineName(codegenContext: CodegenContext<*>, typeMapper: KotlinTypeMapper): String =
    getInlineName(codegenContext, codegenContext.contextDescriptor, typeMapper)

private fun getInlineName(
    codegenContext: CodegenContext<*>,
    currentDescriptor: DeclarationDescriptor,
    typeMapper: KotlinTypeMapper
): String {
    when (currentDescriptor) {
        is PackageFragmentDescriptor -> {
            val file = DescriptorToSourceUtils.getContainingFile(codegenContext.contextDescriptor)

            val implementationOwnerInternalName: String? =
                if (file == null) {
                    CodegenContextUtil.getImplementationOwnerClassType(codegenContext)?.internalName
                } else JvmFileClassUtil.getFileClassInternalName(file)

            if (implementationOwnerInternalName == null) {
                val contextDescriptor = codegenContext.contextDescriptor
                throw RuntimeException(
                    "Couldn't find declaration for " +
                            contextDescriptor.containingDeclaration!!.name + "." + contextDescriptor.name +
                            "; context: " + codegenContext
                )
            }

            return implementationOwnerInternalName
        }
        is ClassifierDescriptor -> {
            return typeMapper.mapClass(currentDescriptor).internalName
        }
        is FunctionDescriptor -> {
            val descriptor = typeMapper.bindingContext.get(CodegenBinding.CLASS_FOR_CALLABLE, currentDescriptor)
            if (descriptor != null) {
                return typeMapper.mapClass(descriptor).internalName
            }
        }
    }

    //TODO: add suffix for special case
    val suffix = if (currentDescriptor.name.isSpecial) "" else currentDescriptor.name.asString()

    return getInlineName(codegenContext, currentDescriptor.containingDeclaration!!, typeMapper) + "$" + suffix
}

internal fun isInvokeOnLambda(owner: String, name: String): Boolean {
    return OperatorNameConventions.INVOKE.asString() == name &&
            owner.startsWith(NUMBERED_FUNCTION_PREFIX) &&
            owner.substring(NUMBERED_FUNCTION_PREFIX.length).isInteger()
}

internal fun isAnonymousConstructorCall(internalName: String, methodName: String): Boolean =
    isConstructor(methodName) && isAnonymousClass(internalName)

private fun isConstructor(methodName: String) = "<init>" == methodName

internal fun isWhenMappingAccess(internalName: String, fieldName: String): Boolean =
    fieldName.startsWith(WhenByEnumsMapping.MAPPING_ARRAY_FIELD_PREFIX) && internalName.endsWith(WhenByEnumsMapping.MAPPINGS_CLASS_NAME_POSTFIX)

internal fun isAnonymousSingletonLoad(internalName: String, fieldName: String): Boolean =
    JvmAbi.INSTANCE_FIELD == fieldName && isAnonymousClass(internalName)

/*
 * Note that sam wrapper prior to 1.2.30 was generated with next template name (that was included suffix hash):
 * int hash = PackagePartClassUtils.getPathHashCode(containingFile.getVirtualFile()) * 31 + DescriptorUtils.getFqNameSafe(descriptor).hashCode();
 *  String shortName = String.format(
 *       "%s$sam$%s%s$%08x",
 *       outermostOwner.shortName().asString(),
 *       descriptor.getName().asString(),
 *       (isInsideInline ? "$i" : ""),
 *       hash
 *  );
 */
private fun isOldSamWrapper(internalName: String) =
    internalName.contains("\$sam$") && internalName.substringAfter("\$i$", "").run { length == 8 && toLongOrNull(16) != null }

internal fun isSamWrapper(internalName: String) =
    (internalName.endsWith(SAM_WRAPPER_SUFFIX) && internalName.contains("\$sam\$i\$")) || isOldSamWrapper(internalName)


internal fun isSamWrapperConstructorCall(internalName: String, methodName: String) =
    isConstructor(methodName) && isSamWrapper(internalName)

internal fun isAnonymousClass(internalName: String) =
    !isSamWrapper(internalName) &&
            internalName.substringAfterLast('/').substringAfterLast("$", "").isInteger()

fun wrapWithMaxLocalCalc(methodNode: MethodNode) =
    MaxStackFrameSizeAndLocalsCalculator(Opcodes.API_VERSION, methodNode.access, methodNode.desc, methodNode)

private fun String.isInteger(radix: Int = 10) = toIntOrNull(radix) != null

internal fun isCapturedFieldName(fieldName: String): Boolean {
    // TODO: improve this heuristic
    return fieldName.startsWith(CAPTURED_FIELD_PREFIX) && !fieldName.startsWith(NON_CAPTURED_FIELD_PREFIX)
            && fieldName != ASSERTIONS_DISABLED_FIELD_NAME
            || AsmUtil.CAPTURED_THIS_FIELD == fieldName
            || AsmUtil.CAPTURED_RECEIVER_FIELD == fieldName
}

internal fun isReturnOpcode(opcode: Int) = opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN

//marked return could be either non-local or local in case of labeled lambda self-returns
internal fun isMarkedReturn(returnIns: AbstractInsnNode) = getMarkedReturnLabelOrNull(returnIns) != null

internal fun getMarkedReturnLabelOrNull(returnInsn: AbstractInsnNode): String? {
    if (!isReturnOpcode(returnInsn.opcode)) {
        return null
    }
    val previous = returnInsn.previous
    if (previous is MethodInsnNode) {
        if (NON_LOCAL_RETURN == previous.owner) {
            return previous.name
        }
    }
    return null
}

fun generateGlobalReturnFlag(iv: InstructionAdapter, labelName: String) {
    iv.invokestatic(NON_LOCAL_RETURN, labelName, "()V", false)
}

internal fun getReturnType(opcode: Int): Type {
    return when (opcode) {
        Opcodes.RETURN -> Type.VOID_TYPE
        Opcodes.IRETURN -> Type.INT_TYPE
        Opcodes.DRETURN -> Type.DOUBLE_TYPE
        Opcodes.FRETURN -> Type.FLOAT_TYPE
        Opcodes.LRETURN -> Type.LONG_TYPE
        else -> AsmTypes.OBJECT_TYPE
    }
}

fun insertNodeBefore(from: MethodNode, to: MethodNode, beforeNode: AbstractInsnNode) {
    val iterator = from.instructions.iterator()
    while (iterator.hasNext()) {
        val next = iterator.next()
        to.instructions.insertBefore(beforeNode, next)
    }
}

fun createEmptyMethodNode() = MethodNode(Opcodes.API_VERSION, 0, "fake", "()V", null, null)

internal fun createFakeContinuationMethodNodeForInline(): MethodNode {
    val methodNode = createEmptyMethodNode()
    val v = InstructionAdapter(methodNode)
    addFakeContinuationMarker(v)
    return methodNode
}

internal fun firstLabelInChain(node: LabelNode): LabelNode {
    var curNode = node
    while (curNode.previous is LabelNode) {
        curNode = curNode.previous as LabelNode
    }
    return curNode
}

internal val MethodNode?.nodeText: String
    get() {
        if (this == null) {
            return "Not generated"
        }
        val textifier = Textifier()
        accept(TraceMethodVisitor(textifier))
        val sw = StringWriter()
        textifier.print(PrintWriter(sw))
        sw.flush()
        return name + " " + desc + ":\n" + sw.buffer.toString()
    }

internal val AbstractInsnNode?.insnText: String
    get() {
        if (this == null) return "<null>"
        val textifier = Textifier()
        accept(TraceMethodVisitor(textifier))
        val sw = StringWriter()
        textifier.print(PrintWriter(sw))
        sw.flush()
        return sw.toString().trim()
    }

internal val AbstractInsnNode?.insnOpcodeText: String
    get() = if (this == null) "null" else Printer.OPCODES[opcode]

internal fun buildClassReaderByInternalName(state: GenerationState, internalName: String): ClassReader {
    //try to find just compiled classes then in dependencies
    val outputFile = state.factory.get(internalName + ".class")
    if (outputFile != null) {
        return ClassReader(outputFile.asByteArray())
    }

    val file = findVirtualFileImprecise(state, internalName) ?: throw RuntimeException("Couldn't find virtual file for " + internalName)

    return ClassReader(file.contentsToByteArray())
}

fun generateFinallyMarker(v: InstructionAdapter, depth: Int, start: Boolean) {
    v.iconst(depth)
    v.invokestatic(INLINE_MARKER_CLASS_NAME, if (start) INLINE_MARKER_FINALLY_START else INLINE_MARKER_FINALLY_END, "(I)V", false)
}

fun isFinallyEnd(node: AbstractInsnNode) = isFinallyMarker(node, INLINE_MARKER_FINALLY_END)

fun isFinallyStart(node: AbstractInsnNode) = isFinallyMarker(node, INLINE_MARKER_FINALLY_START)

fun isFinallyMarker(node: AbstractInsnNode?): Boolean = node != null && (isFinallyStart(node) || isFinallyEnd(node))

private fun isFinallyMarker(node: AbstractInsnNode, name: String): Boolean {
    if (node !is MethodInsnNode) return false
    return INLINE_MARKER_CLASS_NAME == node.owner && name == node.name
}

internal fun isFinallyMarkerRequired(context: MethodContext) = context.isInlineMethodContext || context is InlineLambdaContext

fun getConstant(ins: AbstractInsnNode): Int {
    val opcode = ins.opcode
    return when (opcode) {
        in Opcodes.ICONST_0..Opcodes.ICONST_5 -> opcode - Opcodes.ICONST_0
        Opcodes.BIPUSH, Opcodes.SIPUSH -> (ins as IntInsnNode).operand
        else -> {
            (ins as LdcInsnNode).cst as Int
        }
    }
}

internal fun removeFinallyMarkers(intoNode: MethodNode) {
    val instructions = intoNode.instructions
    var curInstr: AbstractInsnNode? = instructions.first
    while (curInstr != null) {
        if (isFinallyMarker(curInstr)) {
            val marker = curInstr
            //just to assert
            getConstant(marker.previous)
            curInstr = curInstr.next
            instructions.remove(marker.previous)
            instructions.remove(marker)
            continue
        }
        curInstr = curInstr.next
    }
}

fun addInlineMarker(v: InstructionAdapter, isStartNotEnd: Boolean) {
    v.visitMethodInsn(
        Opcodes.INVOKESTATIC, INLINE_MARKER_CLASS_NAME,
        if (isStartNotEnd) INLINE_MARKER_BEFORE_METHOD_NAME else INLINE_MARKER_AFTER_METHOD_NAME,
        "()V", false
    )
}

internal fun addReturnsUnitMarkerIfNecessary(v: InstructionAdapter, resolvedCall: ResolvedCall<*>) {
    val wrapperDescriptor = resolvedCall.candidateDescriptor.safeAs<FunctionDescriptor>() ?: return
    val unsubstitutedDescriptor = wrapperDescriptor.unwrapInitialDescriptorForSuspendFunction()

    val typeSubstitutor = TypeSubstitutor.create(
        unsubstitutedDescriptor.typeParameters
            .withIndex()
            .associateBy({ it.value.typeConstructor }) {
                TypeProjectionImpl(resolvedCall.typeArguments[wrapperDescriptor.typeParameters[it.index]] ?: return)
            }
    )

    val substitutedDescriptor = unsubstitutedDescriptor.substitute(typeSubstitutor) ?: return
    val returnType = substitutedDescriptor.returnType ?: return

    if (KotlinBuiltIns.isUnit(returnType)) {
        addReturnsUnitMarker(v)
    }
}

fun addReturnsUnitMarker(v: InstructionAdapter) {
    v.emitInlineMarker(INLINE_MARKER_RETURNS_UNIT)
}

fun addSuspendMarker(v: InstructionAdapter, isStartNotEnd: Boolean, inlinable: Boolean = false) {
    val marker = when {
        inlinable && isStartNotEnd -> INLINE_MARKER_BEFORE_INLINE_SUSPEND_ID
        inlinable -> INLINE_MARKER_AFTER_INLINE_SUSPEND_ID
        isStartNotEnd -> INLINE_MARKER_BEFORE_SUSPEND_ID
        else -> INLINE_MARKER_AFTER_SUSPEND_ID
    }
    v.emitInlineMarker(marker)
}

fun addFakeContinuationConstructorCallMarker(v: InstructionAdapter, isStartNotEnd: Boolean) {
    v.emitInlineMarker(if (isStartNotEnd) INLINE_MARKER_BEFORE_FAKE_CONTINUATION_CONSTRUCTOR_CALL else INLINE_MARKER_AFTER_FAKE_CONTINUATION_CONSTRUCTOR_CALL)
}

/* There are contexts when the continuation does not yet exist, for example, in inline lambdas, which are going to
 * be inlined into suspendable functions.
 * In such cases we just generate the marker which is going to be replaced with real continuation on generating state machine.
 * See [CoroutineTransformerMethodVisitor] for more info.
 */
fun addFakeContinuationMarker(v: InstructionAdapter) {
    v.emitInlineMarker(INLINE_MARKER_FAKE_CONTINUATION)
    v.aconst(null)
}

private fun InstructionAdapter.emitInlineMarker(id: Int) {
    iconst(id)
    invokestatic(
        INLINE_MARKER_CLASS_NAME,
        "mark",
        "(I)V", false
    )
}

internal fun isBeforeSuspendMarker(insn: AbstractInsnNode) = isSuspendMarker(insn, INLINE_MARKER_BEFORE_SUSPEND_ID)
internal fun isAfterSuspendMarker(insn: AbstractInsnNode) = isSuspendMarker(insn, INLINE_MARKER_AFTER_SUSPEND_ID)
internal fun isBeforeInlineSuspendMarker(insn: AbstractInsnNode) = isSuspendMarker(insn, INLINE_MARKER_BEFORE_INLINE_SUSPEND_ID)
internal fun isAfterInlineSuspendMarker(insn: AbstractInsnNode) = isSuspendMarker(insn, INLINE_MARKER_AFTER_INLINE_SUSPEND_ID)
internal fun isReturnsUnitMarker(insn: AbstractInsnNode) = isSuspendMarker(insn, INLINE_MARKER_RETURNS_UNIT)
internal fun isFakeContinuationMarker(insn: AbstractInsnNode) =
    insn.previous != null && isSuspendMarker(insn.previous, INLINE_MARKER_FAKE_CONTINUATION) && insn.opcode == Opcodes.ACONST_NULL

internal fun isBeforeFakeContinuationConstructorCallMarker(insn: AbstractInsnNode) =
    isSuspendMarker(insn, INLINE_MARKER_BEFORE_FAKE_CONTINUATION_CONSTRUCTOR_CALL)

internal fun isAfterFakeContinuationConstructorCallMarker(insn: AbstractInsnNode) =
    isSuspendMarker(insn, INLINE_MARKER_AFTER_FAKE_CONTINUATION_CONSTRUCTOR_CALL)

private fun isSuspendMarker(insn: AbstractInsnNode, id: Int) =
    isInlineMarker(insn, "mark") && insn.previous.intConstant == id

internal fun isInlineMarker(insn: AbstractInsnNode): Boolean {
    return isInlineMarker(insn, null)
}

private fun isInlineMarker(insn: AbstractInsnNode, name: String?): Boolean {
    if (insn !is MethodInsnNode) {
        return false
    }

    return insn.getOpcode() == Opcodes.INVOKESTATIC &&
            insn.owner == INLINE_MARKER_CLASS_NAME &&
            if (name != null)
                insn.name == name
            else
                insn.name == INLINE_MARKER_BEFORE_METHOD_NAME || insn.name == INLINE_MARKER_AFTER_METHOD_NAME
}

internal fun isBeforeInlineMarker(insn: AbstractInsnNode): Boolean {
    return isInlineMarker(insn, INLINE_MARKER_BEFORE_METHOD_NAME)
}

internal fun isAfterInlineMarker(insn: AbstractInsnNode): Boolean {
    return isInlineMarker(insn, INLINE_MARKER_AFTER_METHOD_NAME)
}

internal fun getLoadStoreArgSize(opcode: Int): Int {
    return if (opcode == Opcodes.DSTORE || opcode == Opcodes.LSTORE || opcode == Opcodes.DLOAD || opcode == Opcodes.LLOAD) 2 else 1
}

internal fun isStoreInstruction(opcode: Int): Boolean {
    return opcode >= Opcodes.ISTORE && opcode <= Opcodes.ASTORE
}

internal fun calcMarkerShift(parameters: Parameters, node: MethodNode): Int {
    val markerShiftTemp = getIndexAfterLastMarker(node)
    return markerShiftTemp - parameters.realParametersSizeOnStack + parameters.argsSizeOnStack
}

private fun getIndexAfterLastMarker(node: MethodNode): Int {
    var result = -1
    for (variable in node.localVariables) {
        if (isFakeLocalVariableForInline(variable.name)) {
            result = max(result, variable.index + 1)
        }
    }
    return result
}

fun isFakeLocalVariableForInline(name: String): Boolean {
    return name.startsWith(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_FUNCTION) || name.startsWith(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_ARGUMENT)
}

internal fun isThis0(name: String): Boolean = AsmUtil.CAPTURED_THIS_FIELD == name

class InlineOnlySmapSkipper(codegen: BaseExpressionCodegen) {
    private val callLineNumber = codegen.lastLineNumber

    fun onInlineLambdaStart(mv: MethodVisitor, info: LambdaInfo) {
        val firstLine = info.node.node.instructions.asSequence().mapNotNull { it as? LineNumberNode }.firstOrNull()?.line ?: -1
        if (callLineNumber >= 0 && firstLine == callLineNumber) {
            // We want the debugger to be able to break both on the inline call itself, plus on each
            // invocation of the inline lambda passed to it. For that to happen there needs to be at least
            // one different line number in between those breakpoints for the VM to emit a locatable event.
            // @InlineOnly functions, however, contain no line numbers, so if the lambda is single-line,
            // the entire call will "meld" into a single region. To break it up, we insert a different line
            // number that is remapped by the SMAP to a line that does not exist.
            val label = Label()
            mv.visitLabel(label)
            mv.visitLineNumber(JvmAbi.LOCAL_VARIABLE_INLINE_ARGUMENT_SYNTHETIC_LINE_NUMBER, label)
        }
    }

    fun onInlineLambdaEnd(mv: MethodVisitor) {
        if (callLineNumber >= 0) {
            val label = Label()
            mv.visitLabel(label)
            mv.visitLineNumber(callLineNumber, label)
        }
    }
}

fun MethodNode.preprocessSuspendMarkers(forInline: Boolean, keepFakeContinuation: Boolean = true) {
    if (instructions.first == null) return
    if (!keepFakeContinuation) {
        val sequence = instructions.asSequence()
        val start = sequence.find { isBeforeFakeContinuationConstructorCallMarker(it) }
        val end = sequence.find { isAfterFakeContinuationConstructorCallMarker(it) }
        if (start != null) {
            // Include one instruction before the start marker (that's the id) and one after the end marker (that's a pop).
            InsnSequence(start.previous, end?.next?.next).forEach(instructions::remove)
        }
    }
    for (insn in instructions.asSequence().filter { isBeforeInlineSuspendMarker(it) || isAfterInlineSuspendMarker(it) }) {
        if (forInline || keepFakeContinuation) {
            val beforeMarker = insn.previous.previous
            if (isReturnsUnitMarker(beforeMarker)) {
                instructions.remove(beforeMarker.previous)
                instructions.remove(beforeMarker)
            }
            instructions.remove(insn.previous)
            instructions.remove(insn)
        } else {
            val newId = if (isBeforeInlineSuspendMarker(insn)) INLINE_MARKER_BEFORE_SUSPEND_ID else INLINE_MARKER_AFTER_SUSPEND_ID
            instructions.set(insn.previous, InsnNode(Opcodes.ICONST_0 + newId))
        }
    }
}
