/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.SamWrapperCodegen.SAM_WRAPPER_SUFFIX
import org.jetbrains.kotlin.codegen.optimization.common.intConstant
import org.jetbrains.kotlin.codegen.optimization.common.nodeType
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapperBase
import org.jetbrains.kotlin.codegen.`when`.WhenByEnumsMapping
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.util.Printer
import org.jetbrains.org.objectweb.asm.util.Textifier
import org.jetbrains.org.objectweb.asm.util.TraceMethodVisitor
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.math.max

const val GENERATE_SMAP = true
const val NUMBERED_FUNCTION_PREFIX = "kotlin/jvm/functions/Function"
const val INLINE_FUN_VAR_SUFFIX = "\$iv"

internal const val FIRST_FUN_LABEL = "$$$$\$ROOT$$$$$"
const val SPECIAL_TRANSFORMATION_NAME = "\$special"
const val INLINE_TRANSFORMATION_SUFFIX = "\$inlined"
const val INLINE_CALL_TRANSFORMATION_SUFFIX = "$$INLINE_TRANSFORMATION_SUFFIX"
internal const val INLINE_FUN_THIS_0_SUFFIX = "\$inline_fun"
internal const val DEFAULT_LAMBDA_FAKE_CALL = "$$\$DEFAULT_LAMBDA_FAKE_CALL$$$"
internal const val CAPTURED_FIELD_FOLD_PREFIX = "$$$"

private const val NON_LOCAL_RETURN = "$$$$\$NON_LOCAL_RETURN$$$$$"
const val CAPTURED_FIELD_PREFIX = "$"
private const val NON_CAPTURED_FIELD_PREFIX = "$$"
internal const val INLINE_MARKER_CLASS_NAME = "kotlin/jvm/internal/InlineMarker"
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
private const val INLINE_MARKER_BEFORE_UNBOX_INLINE_CLASS = 8
private const val INLINE_MARKER_AFTER_UNBOX_INLINE_CLASS = 9
private const val INLINE_MARKER_SUSPEND_LAMBDA_PARAMETER = 10

internal inline fun getMethodNode(classData: ByteArray, classType: Type, crossinline match: (Method) -> Boolean): SMAPAndMethodNode? {
    var node: MethodNode? = null
    var sourceFile: String? = null
    var sourceMap: String? = null
    ClassReader(classData).accept(object : ClassVisitor(Opcodes.API_VERSION) {
        override fun visitSource(source: String?, debug: String?) {
            sourceFile = source
            sourceMap = debug
        }

        override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<String>?): MethodVisitor? {
            if (!match(Method(name, desc))) return null
            node?.let { existing ->
                throw AssertionError("Can't find proper '$name' method for inline: ambiguity between '${existing.name + existing.desc}' and '${name + desc}'")
            }
            node = MethodNode(Opcodes.API_VERSION, access, name, desc, signature, exceptions)
            return node!!
        }
    }, ClassReader.SKIP_FRAMES or if (GENERATE_SMAP) 0 else ClassReader.SKIP_DEBUG)

    return node?.let {
        val parsedSourceMap = sourceMap?.let(SMAPParser::parseOrNull)
            ?: SMAP.identityMapping(sourceFile, classType.internalName, listOfNotNull(it))
        SMAPAndMethodNode(it, parsedSourceMap)
    }
}

internal fun getMethodNode(classData: ByteArray, classType: Type, method: Method): SMAPAndMethodNode? =
    getMethodNode(classData, classType) { it == method }

fun argumentsSize(descriptor: String, isStatic: Boolean): Int =
    (Type.getArgumentsAndReturnSizes(descriptor) shr 2) - (if (isStatic) 1 else 0)

internal fun findVirtualFile(state: GenerationState, classId: ClassId): VirtualFile? {
    val moduleInfo = state.module.getCapability(ModuleInfo.Capability)
    return VirtualFileFinder.getInstance(state.project, moduleInfo).findVirtualFileWithHeader(classId)
}

internal fun findVirtualFileImprecise(state: GenerationState, internalClassName: String): VirtualFile? {
    val packageFqName = JvmClassName.byInternalName(internalClassName).packageFqName
    val classNameWithDollars = internalClassName.substringAfterLast("/", internalClassName)
    //TODO: we cannot construct proper classId at this point, we need to read InnerClasses info from class file
    // we construct valid.package.name/RelativeClassNameAsSingleName that should work in compiler, but fails for inner classes in IDE
    return findVirtualFile(state, ClassId(packageFqName, Name.identifier(classNameWithDollars)))
}

internal fun isInvokeOnLambda(owner: String, name: String): Boolean {
    return OperatorNameConventions.INVOKE.asString() == name && owner.isNumberedFunctionInternalName()
}

internal fun String.isNumberedFunctionInternalName(): Boolean =
    startsWith(NUMBERED_FUNCTION_PREFIX) && substring(NUMBERED_FUNCTION_PREFIX.length).isInteger()

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
    !internalName.contains("\$sam\$") &&
            internalName.substringAfterLast('/').substringAfterLast("$", "").isInteger()

fun wrapWithMaxLocalCalc(methodNode: MethodNode) =
    MaxStackFrameSizeAndLocalsCalculator(Opcodes.API_VERSION, methodNode.access, methodNode.desc, methodNode)

inline fun newMethodNodeWithCorrectStackSize(block: (InstructionAdapter) -> Unit): MethodNode {
    val newMethodNode = MethodNode(Opcodes.API_VERSION, "fake", "()V", null, null)
    val mv = wrapWithMaxLocalCalc(newMethodNode)
    block(InstructionAdapter(mv))

    // Adding a fake return (and removing it below) to trigger maxStack calculation
    mv.visitInsn(Opcodes.RETURN)
    mv.visitMaxs(-1, -1)

    newMethodNode.instructions.apply { remove(last) }
    return newMethodNode
}

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

internal fun firstLabelInChain(node: LabelNode): LabelNode {
    var curNode = node
    while (curNode.previous is LabelNode) {
        curNode = curNode.previous as LabelNode
    }
    return curNode
}

internal fun areLabelsBeforeSameInsn(first: LabelNode, second: LabelNode): Boolean =
    firstLabelInChain(first) == firstLabelInChain(second)

val MethodNode?.nodeText: String
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

fun AbstractInsnNode?.insnText(insnList: InsnList): String {
    if (this == null) return "<null>"

    fun AbstractInsnNode.indexOf() =
        insnList.indexOf(this)

    fun LabelNode.labelText() =
        "L#${this.indexOf()}"

    return when (this) {
        is LabelNode ->
            labelText()
        is JumpInsnNode ->
            "$insnOpcodeText ${label.labelText()}"
        is LookupSwitchInsnNode ->
            "$insnOpcodeText " +
                    this.keys.zip(this.labels).joinToString(prefix = "[", postfix = "]") { (key, label) -> "$key:${label.labelText()}" }
        is TableSwitchInsnNode ->
            "$insnOpcodeText " +
                    (min..max).zip(this.labels).joinToString(prefix = "[", postfix = "]") { (key, label) -> "$key:${label.labelText()}" }
        else ->
            insnText
    }
}

fun MethodNode.dumpBody(): String {
    val sw = StringWriter()
    val pw = PrintWriter(sw)

    fun LabelNode.labelRef() =
        "L#${this@dumpBody.instructions.indexOf(this)}"

    pw.println("${this.name} ${this.desc}")

    for (tcb in this.tryCatchBlocks) {
        pw.println("  TRYCATCHBLOCK start:${tcb.start.labelRef()} end:${tcb.end.labelRef()} handler:${tcb.handler.labelRef()}")
    }

    for ((i, insn) in this.instructions.toArray().withIndex()) {
        when (insn.nodeType) {
            AbstractInsnNode.INSN ->
                pw.println("$i\t${Printer.OPCODES[insn.opcode]}")
            AbstractInsnNode.INT_INSN ->
                pw.println("$i\t${Printer.OPCODES[insn.opcode]} ${(insn as IntInsnNode).operand}")
            AbstractInsnNode.VAR_INSN ->
                pw.println("$i\t${Printer.OPCODES[insn.opcode]} ${(insn as VarInsnNode).`var`}")
            AbstractInsnNode.TYPE_INSN ->
                pw.println("$i\t${Printer.OPCODES[insn.opcode]} ${(insn as TypeInsnNode).desc}")
            AbstractInsnNode.FIELD_INSN -> {
                val fieldInsn = insn as FieldInsnNode
                pw.println("$i\t${Printer.OPCODES[insn.opcode]} ${fieldInsn.owner}#${fieldInsn.name} ${fieldInsn.desc}")
            }
            AbstractInsnNode.METHOD_INSN -> {
                val methodInsn = insn as MethodInsnNode
                pw.println("$i\t${Printer.OPCODES[insn.opcode]} ${methodInsn.owner}#${methodInsn.name} ${methodInsn.desc}")
            }
            AbstractInsnNode.INVOKE_DYNAMIC_INSN -> {
                val indyInsn = insn as InvokeDynamicInsnNode
                pw.println("$i\t${Printer.OPCODES[insn.opcode]} ${indyInsn.name} ${indyInsn.desc}")
                val bsmTag = when (val tag = indyInsn.bsm.tag) {
                    Opcodes.H_GETFIELD -> "H_GETFIELD"
                    Opcodes.H_GETSTATIC -> "H_GETSTATIC"
                    Opcodes.H_PUTFIELD -> "H_PUTFIELD"
                    Opcodes.H_PUTSTATIC -> "H_PUTSTATIC"
                    Opcodes.H_INVOKEVIRTUAL -> "H_INVOKEVIRTUAL"
                    Opcodes.H_INVOKESTATIC -> "H_INVOKESTATIC"
                    Opcodes.H_INVOKESPECIAL -> "H_INVOKESPECIAL"
                    Opcodes.H_NEWINVOKESPECIAL -> "H_NEWINVOKESPECIAL"
                    Opcodes.H_INVOKEINTERFACE -> "H_INVOKEINTERFACE"
                    else -> "<$tag>"
                }
                pw.println("\t$bsmTag ${indyInsn.bsm.owner}#${indyInsn.bsm.name} ${indyInsn.bsm.desc} [")
                for (bsmArg in indyInsn.bsmArgs) {
                    pw.println("\t$bsmArg")
                }
                pw.println("\t]")
            }
            AbstractInsnNode.JUMP_INSN ->
                pw.println("$i\t${Printer.OPCODES[insn.opcode]} ${(insn as JumpInsnNode).label.labelRef()}")
            AbstractInsnNode.LABEL ->
                pw.println("$i\tL#$i")
            AbstractInsnNode.LDC_INSN ->
                pw.println("$i\tLDC ${(insn as LdcInsnNode).cst}")
            AbstractInsnNode.IINC_INSN -> {
                val iincInsn = insn as IincInsnNode
                pw.println("$i\tIINC ${iincInsn.`var`} incr:${iincInsn.incr}")
            }
            AbstractInsnNode.TABLESWITCH_INSN -> {
                val switchInsn = insn as TableSwitchInsnNode
                pw.println("$i\tTABLESWITCH min:${switchInsn.min} max:${switchInsn.max}{")
                for (k in switchInsn.labels.indices) {
                    pw.println("\t${k + switchInsn.min}: ${switchInsn.labels[k].labelRef()}")
                }
                pw.println("\tdefault: ${switchInsn.dflt.labelRef()}")
                pw.println("\t}")
            }
            AbstractInsnNode.LOOKUPSWITCH_INSN -> {
                val switchInsn = insn as LookupSwitchInsnNode
                pw.println("$i\tLOOKUPSWITCH {")
                for (k in switchInsn.labels.indices) {
                    val key = switchInsn.keys[k]
                    val label = switchInsn.labels[k]
                    pw.println("\t$key: ${label.labelRef()}")
                }
                pw.println("\t}")
            }
            AbstractInsnNode.MULTIANEWARRAY_INSN -> {
                val manInsn = insn as MultiANewArrayInsnNode
                pw.println("$i\t${Printer.OPCODES[insn.opcode]} ${manInsn.desc} dims:${manInsn.dims} ")
            }
            AbstractInsnNode.FRAME -> {
                // TODO dump frame if needed
                pw.println("$i\tFRAME {...}")
            }
            AbstractInsnNode.LINE -> {
                val lineInsn = insn as LineNumberNode
                pw.println("$i\tLINENUMBER ${lineInsn.line} ${lineInsn.start.labelRef()}")
            }
            else -> {
                pw.println("$i\t??? $insn")
            }
        }
    }

    for (lv in this.localVariables) {
        pw.println("  LOCALVARIABLE ${lv.index} ${lv.name} ${lv.desc} ${lv.start.labelRef()} ${lv.end.labelRef()}")
        if (lv.signature != null) {
            pw.println("    // signature: ${lv.signature}")
        }
    }

    pw.flush()
    return sw.toString()
}

internal val AbstractInsnNode?.insnOpcodeText: String
    get() = when (this) {
        null -> "null"
        is LabelNode -> "LABEL"
        is LineNumberNode -> "LINENUMBER"
        is FrameNode -> "FRAME"
        else -> Printer.OPCODES[opcode]
    }

internal fun TryCatchBlockNode.text(insns: InsnList): String =
    "[${insns.indexOf(start)} .. ${insns.indexOf(end)} -> ${insns.indexOf(handler)}]"

internal fun loadClassBytesByInternalName(state: GenerationState, internalName: String): ByteArray {
    //try to find just compiled classes then in dependencies
    state.factory.get("$internalName.class")?.let { return it.asByteArray() }

    state.inlineCache.getClassBytes(internalName)?.let { return it }

    val file = findVirtualFileImprecise(state, internalName) ?: throw RuntimeException("Couldn't find virtual file for $internalName")

    return file.contentsToByteArray()
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
fun removeFinallyMarkers(intoNode: MethodNode) {
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

fun generateResumePathUnboxing(v: InstructionAdapter, inlineClass: KotlinTypeMarker, typeMapper: KotlinTypeMapperBase) {
    addBeforeUnboxInlineClassMarker(v)
    StackValue.unboxInlineClass(AsmTypes.OBJECT_TYPE, inlineClass, v, typeMapper)
    // Suspend functions always returns Any?, but the unboxing disrupts type analysis of the bytecode.
    // For example, if the underlying type is String, CHECKCAST String is removed.
    // However, the unboxing is moved to the resume path, the direct path still has Any?, but now, without the CHECKCAST.
    // Thus, we add CHECKCAST Object, which we remove, after we copy the unboxing to the resume path.
    v.checkcast(AsmTypes.OBJECT_TYPE)
    addAfterUnboxInlineClassMarker(v)
}

private fun addBeforeUnboxInlineClassMarker(v: InstructionAdapter) {
    v.emitInlineMarker(INLINE_MARKER_BEFORE_UNBOX_INLINE_CLASS)
}

private fun addAfterUnboxInlineClassMarker(v: InstructionAdapter) {
    v.emitInlineMarker(INLINE_MARKER_AFTER_UNBOX_INLINE_CLASS)
}

fun addSuspendLambdaParameterMarker(v: InstructionAdapter) {
    v.emitInlineMarker(INLINE_MARKER_SUSPEND_LAMBDA_PARAMETER)
}

fun isSuspendLambdaParameterMarker(insn: AbstractInsnNode): Boolean = isSuspendMarker(insn, INLINE_MARKER_SUSPEND_LAMBDA_PARAMETER)

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

fun isBeforeSuspendMarker(insn: AbstractInsnNode) = isSuspendMarker(insn, INLINE_MARKER_BEFORE_SUSPEND_ID)
internal fun isAfterSuspendMarker(insn: AbstractInsnNode) = isSuspendMarker(insn, INLINE_MARKER_AFTER_SUSPEND_ID)
fun isBeforeInlineSuspendMarker(insn: AbstractInsnNode) = isSuspendMarker(insn, INLINE_MARKER_BEFORE_INLINE_SUSPEND_ID)
internal fun isAfterInlineSuspendMarker(insn: AbstractInsnNode) = isSuspendMarker(insn, INLINE_MARKER_AFTER_INLINE_SUSPEND_ID)
internal fun isReturnsUnitMarker(insn: AbstractInsnNode) = isSuspendMarker(insn, INLINE_MARKER_RETURNS_UNIT)
internal fun isFakeContinuationMarker(insn: AbstractInsnNode) =
    insn.previous != null && isSuspendMarker(insn.previous, INLINE_MARKER_FAKE_CONTINUATION) && insn.opcode == Opcodes.ACONST_NULL

internal fun isBeforeUnboxInlineClassMarker(insn: AbstractInsnNode) = isSuspendMarker(insn, INLINE_MARKER_BEFORE_UNBOX_INLINE_CLASS)
internal fun isAfterUnboxInlineClassMarker(insn: AbstractInsnNode) = isSuspendMarker(insn, INLINE_MARKER_AFTER_UNBOX_INLINE_CLASS)

internal fun isBeforeFakeContinuationConstructorCallMarker(insn: AbstractInsnNode) =
    isSuspendMarker(insn, INLINE_MARKER_BEFORE_FAKE_CONTINUATION_CONSTRUCTOR_CALL)

internal fun isAfterFakeContinuationConstructorCallMarker(insn: AbstractInsnNode) =
    isSuspendMarker(insn, INLINE_MARKER_AFTER_FAKE_CONTINUATION_CONSTRUCTOR_CALL)

internal fun isSuspendInlineMarker(insn: AbstractInsnNode) =
    isInlineMarker(insn, "mark")

private fun isSuspendMarker(insn: AbstractInsnNode, id: Int) =
    isInlineMarker(insn, "mark") && insn.previous.intConstant == id

internal fun isInlineMarker(insn: AbstractInsnNode): Boolean {
    return isInlineMarker(insn, null)
}

internal fun isInlineMarker(insn: AbstractInsnNode, name: String?): Boolean {
    if (insn.opcode != Opcodes.INVOKESTATIC) return false

    val methodInsn = insn as MethodInsnNode
    return methodInsn.owner == INLINE_MARKER_CLASS_NAME &&
            if (name != null)
                methodInsn.name == name
            else
                methodInsn.name == INLINE_MARKER_BEFORE_METHOD_NAME || methodInsn.name == INLINE_MARKER_AFTER_METHOD_NAME
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
        if (JvmAbi.isFakeLocalVariableForInline(variable.name)) {
            result = max(result, variable.index + 1)
        }
    }
    return result
}

internal fun isThis0(name: String): Boolean = AsmUtil.CAPTURED_THIS_FIELD == name

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

fun cloneMethodNode(methodNode: MethodNode): MethodNode {
    synchronized(methodNode) {
        methodNode.instructions.resetLabels()
        return MethodNode(
            Opcodes.API_VERSION, methodNode.access, methodNode.name, methodNode.desc, methodNode.signature,
            methodNode.exceptions.toTypedArray()
        ).also(methodNode::accept)
    }
}

fun isCatchStoreInstruction(insn: AbstractInsnNode): Boolean = resolveCatchStoreInstruction(insn) != null

fun resolveCatchStoreInstruction(insn: AbstractInsnNode): AbstractInsnNode? {
    if (insn.opcode == Opcodes.ASTORE) return insn

    val marker = insn.next?.next ?: return null
    if (!ReifiedTypeInliner.isOperationReifiedMarker(marker)) return null

    val afterMarker = marker.next
    if (afterMarker.opcode == Opcodes.ASTORE) return afterMarker

    return null
}
