/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.codegen.inline

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.BaseExpressionCodegen
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import org.jetbrains.kotlin.codegen.`when`.WhenByEnumsMapping
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.context.CodegenContext
import org.jetbrains.kotlin.codegen.context.CodegenContextUtil
import org.jetbrains.kotlin.codegen.context.InlineLambdaContext
import org.jetbrains.kotlin.codegen.context.MethodContext
import org.jetbrains.kotlin.codegen.intrinsics.classId
import org.jetbrains.kotlin.codegen.optimization.common.intConstant
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryPackageSourceElement
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.load.kotlin.VirtualFileKotlinClass
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.ENUM_TYPE
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.JAVA_CLASS_TYPE
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.util.Printer
import org.jetbrains.org.objectweb.asm.util.Textifier
import org.jetbrains.org.objectweb.asm.util.TraceMethodVisitor
import java.io.PrintWriter
import java.io.StringWriter

const val GENERATE_SMAP = true
const val API = Opcodes.ASM5
const val NUMBERED_FUNCTION_PREFIX = "kotlin/jvm/functions/Function"
const val INLINE_FUN_VAR_SUFFIX = "\$iv"

internal const val THIS = "this"
internal const val THIS_0 = "this$0"
internal const val FIRST_FUN_LABEL = "$$$$\$ROOT$$$$$"
internal const val SPECIAL_TRANSFORMATION_NAME = "\$special"
internal const val INLINE_TRANSFORMATION_SUFFIX = "\$inlined"
internal const val INLINE_CALL_TRANSFORMATION_SUFFIX = "$" + INLINE_TRANSFORMATION_SUFFIX
internal const val INLINE_FUN_THIS_0_SUFFIX = "\$inline_fun"
internal const val DEFAULT_LAMBDA_FAKE_CALL = "$$\$DEFAULT_LAMBDA_FAKE_CALL$$$"
internal const val CAPTURED_FIELD_FOLD_PREFIX = "$$$"

private const val RECEIVER_0 = "receiver$0"
private const val NON_LOCAL_RETURN = "$$$$\$NON_LOCAL_RETURN$$$$$"
private const val CAPTURED_FIELD_PREFIX = "$"
private const val NON_CAPTURED_FIELD_PREFIX = "$$"
private const val INLINE_MARKER_CLASS_NAME = "kotlin/jvm/internal/InlineMarker"
private const val INLINE_MARKER_BEFORE_METHOD_NAME = "beforeInlineCall"
private const val INLINE_MARKER_AFTER_METHOD_NAME = "afterInlineCall"
private const val INLINE_MARKER_FINALLY_START = "finallyStart"

private const val INLINE_MARKER_FINALLY_END = "finallyEnd"
private const val INLINE_MARKER_BEFORE_SUSPEND_ID = 0
private const val INLINE_MARKER_AFTER_SUSPEND_ID = 1
private val INTRINSIC_ARRAY_CONSTRUCTOR_TYPE = AsmUtil.asmTypeByClassId(classId)

internal fun getMethodNode(
        classData: ByteArray,
        methodName: String,
        methodDescriptor: String,
        classType: Type
): SMAPAndMethodNode? {
    val cr = ClassReader(classData)
    var node: MethodNode? = null
    val debugInfo = arrayOfNulls<String>(2)
    val lines = IntArray(2)
    lines[0] = Integer.MAX_VALUE
    lines[1] = Integer.MIN_VALUE

    cr.accept(object : ClassVisitor(API) {

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
            if (methodName == name && methodDescriptor == desc) {
                node = object : MethodNode(API, access, name, desc, signature, exceptions) {
                    override fun visitLineNumber(line: Int, start: Label) {
                        super.visitLineNumber(line, start)
                        lines[0] = Math.min(lines[0], line)
                        lines[1] = Math.max(lines[1], line)
                    }
                }
                return node
            }
            return null
        }
    }, ClassReader.SKIP_FRAMES or if (GENERATE_SMAP) 0 else ClassReader.SKIP_DEBUG)

    if (node == null) {
        return null
    }

    if (INTRINSIC_ARRAY_CONSTRUCTOR_TYPE == classType) {
        // Don't load source map for intrinsic array constructors
        debugInfo[0] = null
    }

    val smap = SMAPParser.parseOrCreateDefault(debugInfo[1], debugInfo[0], classType.internalName, lines[0], lines[1])
    return SMAPAndMethodNode(node!!, smap)
}

internal fun findVirtualFile(state: GenerationState, classId: ClassId): VirtualFile? {
    return VirtualFileFinder.getInstance(state.project).findVirtualFileWithHeader(classId)
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
                    }
                    else JvmFileClassUtil.getFileClassInternalName(file)

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
            return typeMapper.mapType(currentDescriptor).internalName
        }
        is FunctionDescriptor -> {
            val descriptor = typeMapper.bindingContext.get(CodegenBinding.CLASS_FOR_CALLABLE, currentDescriptor)
            if (descriptor != null) {
                return typeMapper.mapType(descriptor).internalName
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
           isInteger(owner.substring(NUMBERED_FUNCTION_PREFIX.length))
}

internal fun isAnonymousConstructorCall(internalName: String, methodName: String): Boolean {
    return "<init>" == methodName && isAnonymousClass(internalName)
}

internal fun isWhenMappingAccess(internalName: String, fieldName: String): Boolean {
    return fieldName.startsWith(WhenByEnumsMapping.MAPPING_ARRAY_FIELD_PREFIX) && internalName.endsWith(WhenByEnumsMapping.MAPPINGS_CLASS_NAME_POSTFIX)
}

internal fun isAnonymousSingletonLoad(internalName: String, fieldName: String): Boolean {
    return JvmAbi.INSTANCE_FIELD == fieldName && isAnonymousClass(internalName)
}

internal fun isAnonymousClass(internalName: String): Boolean {
    val shortName = getLastNamePart(internalName)
    val index = shortName.lastIndexOf("$")

    if (index < 0) {
        return false
    }

    val suffix = shortName.substring(index + 1)
    return isInteger(suffix)
}

private fun getLastNamePart(internalName: String): String {
    val index = internalName.lastIndexOf("/")
    return if (index < 0) internalName else internalName.substring(index + 1)
}

fun wrapWithMaxLocalCalc(methodNode: MethodNode): MethodVisitor {
    return MaxStackFrameSizeAndLocalsCalculator(API, methodNode.access, methodNode.desc, methodNode)
}

private fun isInteger(string: String): Boolean {
    string.toIntOrNull() != null
    if (string.isEmpty()) {
        return false
    }

    for (i in 0..string.length - 1) {
        if (!Character.isDigit(string[i])) {
            return false
        }
    }

    return true
}

internal fun isCapturedFieldName(fieldName: String): Boolean {
    // TODO: improve this heuristic
    return fieldName.startsWith(CAPTURED_FIELD_PREFIX) && !fieldName.startsWith(NON_CAPTURED_FIELD_PREFIX) ||
           THIS_0 == fieldName ||
           RECEIVER_0 == fieldName
}

internal fun isReturnOpcode(opcode: Int): Boolean {
    return opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN
}

//marked return could be either non-local or local in case of labeled lambda self-returns
internal fun isMarkedReturn(returnIns: AbstractInsnNode): Boolean {
    return getMarkedReturnLabelOrNull(returnIns) != null
}

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

internal fun generateGlobalReturnFlag(iv: InstructionAdapter, labelName: String) {
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

internal fun insertNodeBefore(from: MethodNode, to: MethodNode, beforeNode: AbstractInsnNode) {
    val iterator = from.instructions.iterator()
    while (iterator.hasNext()) {
        val next = iterator.next()
        to.instructions.insertBefore(beforeNode, next)
    }
}

internal fun createEmptyMethodNode(): MethodNode {
    return MethodNode(API, 0, "fake", "()V", null, null)
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
    get() {
        return if (this == null) "null" else Printer.OPCODES[opcode]
    }

internal fun buildClassReaderByInternalName(state: GenerationState, internalName: String): ClassReader {
    //try to find just compiled classes then in dependencies
    val outputFile = state.factory.get(internalName + ".class")
    if (outputFile != null) {
        return ClassReader(outputFile.asByteArray())
    }

    val file = findVirtualFileImprecise(state, internalName) ?:
               throw RuntimeException("Couldn't find virtual file for " + internalName)

    return ClassReader(file.contentsToByteArray())
}

internal fun generateFinallyMarker(v: InstructionAdapter, depth: Int, start: Boolean) {
    v.iconst(depth)
    v.invokestatic(INLINE_MARKER_CLASS_NAME, if (start) INLINE_MARKER_FINALLY_START else INLINE_MARKER_FINALLY_END, "(I)V", false)
}

internal fun isFinallyEnd(node: AbstractInsnNode): Boolean {
    return isFinallyMarker(node, INLINE_MARKER_FINALLY_END)
}

internal fun isFinallyStart(node: AbstractInsnNode): Boolean {
    return isFinallyMarker(node, INLINE_MARKER_FINALLY_START)
}

internal fun isFinallyMarker(node: AbstractInsnNode?): Boolean {
    return node != null && (isFinallyStart(node) || isFinallyEnd(node))
}

private fun isFinallyMarker(node: AbstractInsnNode, name: String): Boolean {
    if (node !is MethodInsnNode) return false
    return INLINE_MARKER_CLASS_NAME == node.owner && name == node.name
}

internal fun isFinallyMarkerRequired(context: MethodContext): Boolean {
    return context.isInlineMethodContext || context is InlineLambdaContext
}

internal fun getConstant(ins: AbstractInsnNode): Int {
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

internal fun addInlineMarker(v: InstructionAdapter, isStartNotEnd: Boolean) {
    v.visitMethodInsn(
            Opcodes.INVOKESTATIC, INLINE_MARKER_CLASS_NAME,
            if (isStartNotEnd) INLINE_MARKER_BEFORE_METHOD_NAME else INLINE_MARKER_AFTER_METHOD_NAME,
            "()V", false
    )
}

internal fun addSuspendMarker(v: InstructionAdapter, isStartNotEnd: Boolean) {
    v.iconst(if (isStartNotEnd) INLINE_MARKER_BEFORE_SUSPEND_ID else INLINE_MARKER_AFTER_SUSPEND_ID)
    v.visitMethodInsn(
            Opcodes.INVOKESTATIC, INLINE_MARKER_CLASS_NAME,
            "mark",
            "(I)V", false
    )
}

internal fun isBeforeSuspendMarker(insn: AbstractInsnNode) = isSuspendMarker(insn, INLINE_MARKER_BEFORE_SUSPEND_ID)
internal fun isAfterSuspendMarker(insn: AbstractInsnNode) = isSuspendMarker(insn, INLINE_MARKER_AFTER_SUSPEND_ID)

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
            result = Math.max(result, variable.index + 1)
        }
    }
    return result
}

fun isFakeLocalVariableForInline(name: String): Boolean {
    return name.startsWith(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_FUNCTION) || name.startsWith(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_ARGUMENT)
}

internal fun isThis0(name: String): Boolean {
    return THIS_0 == name
}

internal fun isSpecialEnumMethod(functionDescriptor: FunctionDescriptor): Boolean {
    val containingDeclaration = functionDescriptor.containingDeclaration as? PackageFragmentDescriptor ?: return false
    if (containingDeclaration.fqName != KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME) {
        return false
    }
    if (functionDescriptor.typeParameters.size != 1) {
        return false
    }
    val name = functionDescriptor.name.asString()
    val parameters = functionDescriptor.valueParameters
    return "enumValues" == name && parameters.size == 0 ||
           ("enumValueOf" == name && parameters.size == 1 &&
            KotlinBuiltIns.isString(parameters[0].type))
}

internal fun createSpecialEnumMethodBody(
        codegen: BaseExpressionCodegen,
        name: String,
        type: KotlinType,
        typeMapper: KotlinTypeMapper
): MethodNode {
    val isValueOf = "enumValueOf" == name
    val invokeType = typeMapper.mapType(type)
    val desc = getSpecialEnumFunDescriptor(invokeType, isValueOf)
    val node = MethodNode(API, Opcodes.ACC_STATIC, "fake", desc, null, null)
    ExpressionCodegen.putReifiedOperationMarkerIfTypeIsReifiedParameter(type, ReifiedTypeInliner.OperationKind.ENUM_REIFIED, InstructionAdapter(node), codegen)
    if (isValueOf) {
        node.visitInsn(Opcodes.ACONST_NULL)
        node.visitVarInsn(Opcodes.ALOAD, 0)

        node.visitMethodInsn(Opcodes.INVOKESTATIC, ENUM_TYPE.internalName, "valueOf",
                             Type.getMethodDescriptor(ENUM_TYPE, JAVA_CLASS_TYPE, AsmTypes.JAVA_STRING_TYPE), false)
    }
    else {
        node.visitInsn(Opcodes.ICONST_0)
        node.visitTypeInsn(Opcodes.ANEWARRAY, ENUM_TYPE.internalName)
    }
    node.visitInsn(Opcodes.ARETURN)
    node.visitMaxs(if (isValueOf) 3 else 2, if (isValueOf) 1 else 0)
    return node
}

internal fun getSpecialEnumFunDescriptor(type: Type, isValueOf: Boolean): String {
    return if (isValueOf) Type.getMethodDescriptor(type, AsmTypes.JAVA_STRING_TYPE) else Type.getMethodDescriptor(AsmUtil.getArrayType(type))
}


val FunctionDescriptor.sourceFilePath: String
    get() {
        val source = source as PsiSourceElement
        val containingFile = source.psi?.containingFile
        return containingFile?.virtualFile?.canonicalPath!!
    }

fun FunctionDescriptor.getClassFilePath(typeMapper: KotlinTypeMapper, cache: IncrementalCache): String {
    val container = containingDeclaration as? DeclarationDescriptorWithSource
    val source = container?.source

    return when (source) {
        is KotlinJvmBinaryPackageSourceElement -> {
            val directMember = JvmCodegenUtil.getDirectMember(this) as? DeserializedCallableMemberDescriptor ?:
                               throw AssertionError("Expected DeserializedCallableMemberDescriptor, got: $this")
            val kotlinClass = source.getContainingBinaryClass(directMember) ?:
                              throw AssertionError("Descriptor $this is not found, in: $source")
            if (kotlinClass !is VirtualFileKotlinClass) {
                throw AssertionError("Expected VirtualFileKotlinClass, got $kotlinClass")
            }
            kotlinClass.file.canonicalPath!!
        }
        is KotlinJvmBinarySourceElement -> {
            val directMember = JvmCodegenUtil.getDirectMember(this)
            assert(directMember is DeserializedCallableMemberDescriptor) { "Expected DeserializedSimpleFunctionDescriptor, got: $this" }
            val kotlinClass = source.binaryClass as VirtualFileKotlinClass
            kotlinClass.file.canonicalPath!!
        }
        else -> {
            val implementationOwnerType = typeMapper.mapImplementationOwner(this)
            val className = implementationOwnerType.internalName
            cache.getClassFilePath(className)
        }
    }
}

class InlineOnlySmapSkipper(codegen: BaseExpressionCodegen) {

    private val callLineNumber = codegen.lastLineNumber

    fun markCallSiteLineNumber(mv: MethodVisitor) {
        if (callLineNumber >= 0) {
            val label = Label()
            mv.visitLabel(label)
            mv.visitLineNumber(callLineNumber, label)
        }
    }
}
