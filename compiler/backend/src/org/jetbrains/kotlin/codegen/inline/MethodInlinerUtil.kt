/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.codegen.InsnSequence
import org.jetbrains.kotlin.codegen.optimization.common.FastMethodAnalyzer
import org.jetbrains.kotlin.codegen.optimization.common.isMeaningful
import org.jetbrains.kotlin.codegen.optimization.nullCheck.isCheckParameterIsNotNull
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.FieldInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.tree.TypeInsnNode
import org.jetbrains.org.objectweb.asm.tree.VarInsnNode
import org.jetbrains.org.objectweb.asm.tree.analysis.*

fun MethodNode.remove(instructions: Sequence<AbstractInsnNode>) =
    instructions.forEach {
        this@remove.instructions.remove(it)
    }

fun MethodNode.remove(instructions: Collection<AbstractInsnNode>) {
    instructions.forEach {
        this@remove.instructions.remove(it)
    }
}

fun MethodNode.findCapturedFieldAssignmentInstructions(): Sequence<FieldInsnNode> {
    return InsnSequence(instructions).filterIsInstance<FieldInsnNode>().filter { fieldNode ->
        //filter captured field assignment
        //  aload 0
        //  aload x
        //  PUTFIELD $fieldName

        val prevPrev = fieldNode.previous?.previous as? VarInsnNode

        fieldNode.opcode == Opcodes.PUTFIELD &&
                isCapturedFieldName(fieldNode.name) &&
                fieldNode.previous is VarInsnNode && prevPrev != null && prevPrev.`var` == 0
    }
}

fun getAnonymousObjectCapturedFieldsByConstructorArgumentFromBytecode(
    state: GenerationState,
    ownerInternalName: String
): List<String?> {
    val constructors = arrayListOf<MethodNode>()

    val classBytes = loadClassBytesByInternalName(state, ownerInternalName)
    ClassReader(classBytes).accept(object : ClassVisitor(Opcodes.API_VERSION) {
        override fun visitMethod(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            exceptions: Array<String>?
        ) : MethodVisitor? {
            if (name != "<init>") return null
            return MethodNode(Opcodes.API_VERSION, access, name, descriptor, signature, exceptions).also(constructors::add)
        }
    }, ClassReader.SKIP_FRAMES)

    require(constructors.size == 1) {
        "Expected exactly one constructor for anonymous object $ownerInternalName, but found ${constructors.size}"
    }

    val constructor = constructors.single()
    val constructorSlotToArgumentIndex = mutableMapOf<Int, Int>()
    val argumentTypes = Type.getArgumentTypes(constructor.desc)
    var constructorSlot = 1
    for ((argumentIndex, type) in argumentTypes.withIndex()) {
        constructorSlotToArgumentIndex[constructorSlot] = argumentIndex
        constructorSlot += type.size
    }

    val capturedFieldsByConstructorArgument = MutableList<String?>(argumentTypes.size) { null }
    constructor.findCapturedFieldAssignmentInstructions().forEach { fieldNode ->
        val constructorParameterIndex = (fieldNode.previous as VarInsnNode).`var`
        val argumentIndex = constructorSlotToArgumentIndex[constructorParameterIndex]
            ?: error("Couldn't map constructor slot $constructorParameterIndex to argument index for $ownerInternalName")
        capturedFieldsByConstructorArgument[argumentIndex] = fieldNode.name
    }
    return capturedFieldsByConstructorArgument
}

fun AbstractInsnNode.getNextMeaningful(): AbstractInsnNode? {
    var result: AbstractInsnNode? = next
    while (result != null && !result.isMeaningful) {
        result = result.next
    }
    return result
}

// Interpreter, that analyzes functional arguments only, to replace SourceInterpreter, since SourceInterpreter's merge has O(N²) complexity

internal class FunctionalArgumentValue(
    val functionalArgument: FunctionalArgument, basicValue: BasicValue?
) : BasicValue(basicValue?.type) {
    override fun toString(): String = "$functionalArgument"
}

internal class AnonymousObjectValue(
    val ownerInternalName: String,
) : BasicValue(Type.getObjectType(ownerInternalName)) {
    lateinit var capturedFieldValues: Map<String, BasicValue>

    val areCapturedFieldsInitialized: Boolean
        get() = this::capturedFieldValues.isInitialized

    override fun toString(): String =
        if (!areCapturedFieldsInitialized) {
            "AnonymousObject($ownerInternalName, uninitialized)"
        } else {
            "AnonymousObject($ownerInternalName, captured=${capturedFieldValues.keys.sorted()})"
        }
}

val BasicValue?.functionalArgument
    get() = (this as? FunctionalArgumentValue)?.functionalArgument

private object ThisValue : BasicValue(Type.getObjectType("java/lang/Object")) {
    override fun toString(): String = "this"
}

private class ThisAliasInterpreter : BasicInterpreter(API_VERSION) {
    override fun newParameterValue(isInstanceMethod: Boolean, local: Int, type: Type): BasicValue =
        if (isInstanceMethod && local == 0) ThisValue else super.newParameterValue(isInstanceMethod, local, type)

    override fun merge(v: BasicValue?, w: BasicValue?): BasicValue? =
        if (v === ThisValue && w === ThisValue) ThisValue else super.merge(v, w)
}

internal class FunctionalArgumentInterpreter(private val inliner: MethodInliner) : BasicInterpreter(API_VERSION) {
    override fun newParameterValue(isInstanceMethod: Boolean, local: Int, type: Type): BasicValue =
        inliner.getFunctionalArgumentIfExists(local)?.let { FunctionalArgumentValue(it, newValue(type)) } ?: newValue(type)

    override fun unaryOperation(insn: AbstractInsnNode, value: BasicValue): BasicValue? =
        if (insn.opcode == GETFIELD && value is AnonymousObjectValue && value.areCapturedFieldsInitialized && isCapturedFieldName((insn as FieldInsnNode).name))
            value.capturedFieldValues[insn.name]
        else
            wrapArgumentInValueIfNeeded(insn, super.unaryOperation(insn, value))

    override fun newOperation(insn: AbstractInsnNode): BasicValue? =
        if (insn.opcode == NEW) {
            val ownerInternalName = (insn as TypeInsnNode).desc
            if (isAnonymousClass(ownerInternalName)) {
                AnonymousObjectValue(ownerInternalName)
            } else {
                wrapArgumentInValueIfNeeded(insn, super.newOperation(insn))
            }
        } else {
            wrapArgumentInValueIfNeeded(insn, super.newOperation(insn))
        }

    private fun isInitAnonymousObjectWithCapturedLambda(methodInsn: MethodInsnNode, values: MutableList<out BasicValue>): Boolean {
        if (methodInsn.opcode != INVOKESPECIAL || methodInsn.name != "<init>") {
            return false
        }
        if (values.size < 2 || values.first() !is AnonymousObjectValue || values.none() { it is FunctionalArgumentValue}) {
            // We care only about anonymous object initialization with lambdas capturing.
            // Also, it excludes other synthetic classes (like $annotationImpl) that are not generated to bytecode yet and with IrClass
            // stored not within current or callee IrFile
            return false
        }
        if ((values.first() as AnonymousObjectValue).ownerInternalName != methodInsn.owner)
            return false

        return true
    }

    override fun naryOperation(insn: AbstractInsnNode, values: MutableList<out BasicValue>): BasicValue? {
        val methodInsn = insn as? MethodInsnNode ?: return super.naryOperation(insn, values)
        when {
            isInitAnonymousObjectWithCapturedLambda(methodInsn, values) -> {
                val receiver = values.first() as AnonymousObjectValue
                // found initialization of an anonymous object, initialize captured field values map
                val anonymousObjectCapturedFieldsByConstructorArgument =
                    inliner.resolveAnonymousObjectCapturedFieldsByConstructorArgument(methodInsn.owner)
                receiver.capturedFieldValues = buildMap {
                    for ((argumentIndex, fieldName) in anonymousObjectCapturedFieldsByConstructorArgument.withIndex()) {
                        if (fieldName == null) continue
                        val argumentValue = values.getOrNull(argumentIndex + 1) ?: continue
                        put(fieldName, argumentValue)
                    }
                }
            }
            methodInsn.opcode == INVOKESTATIC &&
                    values.size == 1 && values.first() is AnonymousObjectValue &&
                    methodInsn.name.startsWith("access\$get") && methodInsn.name.endsWith("\$p") -> {
                // accessor of a probably captured field in an anonymous object
                val fieldName = methodInsn.name.removePrefix("access\$get").removeSuffix("\$p")
                val objectValue = values.first() as AnonymousObjectValue
                if (!objectValue.areCapturedFieldsInitialized) {
                    return super.naryOperation(insn, values)
                }
                objectValue.capturedFieldValues[fieldName]?.functionalArgument?.let {
                    return FunctionalArgumentValue(it, super.naryOperation(insn, values))
                }
            }
        }
        return super.naryOperation(insn, values)
    }

    private fun wrapArgumentInValueIfNeeded(insn: AbstractInsnNode, basicValue: BasicValue?): BasicValue? =
        if (insn is FieldInsnNode) // GETFIELD or GETSTATIC
            inliner.getFunctionalArgumentIfExists(insn)?.let { FunctionalArgumentValue(it, basicValue) } ?: basicValue
        else
            basicValue

    override fun merge(v: BasicValue?, w: BasicValue?): BasicValue? =
        when {
            v is FunctionalArgumentValue && w is FunctionalArgumentValue && v.functionalArgument == w.functionalArgument -> v
            v is AnonymousObjectValue && v === w -> v
            else -> super.merge(v, w)
        }
}

internal fun AbstractInsnNode.isAloadBeforeCheckParameterIsNotNull(): Boolean =
    opcode == Opcodes.ALOAD && next?.opcode == Opcodes.LDC && next?.next?.isCheckParameterIsNotNull() == true

internal fun analyzeMethodNodeWithInterpreter(
    node: MethodNode,
    interpreter: BasicInterpreter
): Array<out Frame<BasicValue>?> {
    class BasicValueFrame(nLocals: Int, nStack: Int) : Frame<BasicValue>(nLocals, nStack) {
        @Throws(AnalyzerException::class)
        override fun execute(insn: AbstractInsnNode, interpreter: Interpreter<BasicValue>) {
            // This can be a void non-local return from a non-void method; Frame#execute would throw and do nothing else.
            if (insn.opcode == Opcodes.RETURN) return
            super.execute(insn, interpreter)
        }
    }

    val analyzer = FastMethodAnalyzer<BasicValue>(
        "fake", node, interpreter, pruneExceptionEdges = true
    ) { nLocals, nStack -> BasicValueFrame(nLocals, nStack) }

    try {
        return analyzer.analyze()
    } catch (e: AnalyzerException) {
        throw RuntimeException(e)
    }
}

internal fun analyzeThisAliases(node: MethodNode): Array<out Frame<BasicValue>?> =
    analyzeMethodNodeWithInterpreter(node, ThisAliasInterpreter())

internal fun Frame<BasicValue>.isThisValue(local: Int): Boolean =
    getLocal(local) === ThisValue

internal fun String.isGetAccessorToCapturedField(): Boolean =
    startsWith("access\$get\$") && endsWith("\$p")

internal fun String.extractAccessedCapturedField(): String =
    removePrefix("access\$get").removeSuffix("\$p")

internal fun AbstractInsnNode.isCallGetAccessorToCapturedField(): Boolean =
    opcode == Opcodes.INVOKESTATIC && this is MethodInsnNode && name.isGetAccessorToCapturedField() &&
            Type.getArgumentTypes(desc).firstOrNull()?.internalName == owner
