/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.coroutines

import org.jetbrains.kotlin.backend.common.COROUTINE_SUSPENDED_NAME
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.StandardNames.COROUTINES_INTRINSICS_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.builtins.StandardNames.COROUTINES_JVM_INTERNAL_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.codegen.inline.addFakeContinuationMarker
import org.jetbrains.kotlin.codegen.topLevelClassAsmType
import org.jetbrains.kotlin.codegen.topLevelClassInternalName
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.checkers.isBuiltInCoroutineContext
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.MethodNode

const val COROUTINE_LABEL_FIELD_NAME = "label"
const val SUSPEND_FUNCTION_CREATE_METHOD_NAME = "create"
const val INVOKE_SUSPEND_METHOD_NAME = "invokeSuspend"
const val CONTINUATION_RESULT_FIELD_NAME = "result"

private const val GET_CONTEXT_METHOD_NAME = "getContext"

val DEBUG_METADATA_ANNOTATION_ASM_TYPE: Type =
    COROUTINES_JVM_INTERNAL_PACKAGE_FQ_NAME.child(Name.identifier("DebugMetadata")).topLevelClassAsmType()

fun coroutineContextAsmType(): Type =
    StandardNames.COROUTINES_PACKAGE_FQ_NAME.child(Name.identifier("CoroutineContext")).topLevelClassAsmType()

fun String.isCoroutineSuperClass(): Boolean =
    COROUTINES_JVM_INTERNAL_PACKAGE_FQ_NAME.identifiedChild("ContinuationImpl") == this ||
            COROUTINES_JVM_INTERNAL_PACKAGE_FQ_NAME.identifiedChild("RestrictedContinuationImpl") == this ||
            COROUTINES_JVM_INTERNAL_PACKAGE_FQ_NAME.identifiedChild("SuspendLambda") == this ||
            COROUTINES_JVM_INTERNAL_PACKAGE_FQ_NAME.identifiedChild("RestrictedSuspendLambda") == this

private fun FqName.identifiedChild(name: String) = child(Name.identifier(name)).topLevelClassInternalName()

private val coroutinesIntrinsicsFileFacadeInternalName: Type =
    COROUTINES_INTRINSICS_PACKAGE_FQ_NAME.child(Name.identifier("IntrinsicsKt")).topLevelClassAsmType()

val CONTINUATION_PARAMETER_NAME = Name.identifier("continuation")

const val CONTINUATION_VARIABLE_NAME = "\$continuation"

private val DEBUG_PROBES_INTERNAL_NAME =
    COROUTINES_JVM_INTERNAL_PACKAGE_FQ_NAME.child(Name.identifier("DebugProbesKt")).topLevelClassInternalName()

private val SPILLING_INTERNAL_NAME =
    COROUTINES_JVM_INTERNAL_PACKAGE_FQ_NAME.child(Name.identifier("SpillingKt")).topLevelClassInternalName()

enum class SuspensionPointKind { NEVER, NOT_INLINE, ALWAYS }

fun createMethodNodeForCoroutineContext(functionDescriptor: FunctionDescriptor): MethodNode {
    assert(functionDescriptor.isBuiltInCoroutineContext()) {
        "functionDescriptor must be kotlin.coroutines.intrinsics.coroutineContext property getter"
    }

    val node =
        MethodNode(
            Opcodes.API_VERSION,
            Opcodes.ACC_STATIC,
            "fake",
            Type.getMethodDescriptor(coroutineContextAsmType()),
            null, null
        )

    val v = InstructionAdapter(node)

    addFakeContinuationMarker(v)

    v.invokeGetContext()

    node.visitMaxs(1, 1)

    return node
}

fun createMethodNodeForSuspendCoroutineUninterceptedOrReturn(): MethodNode {
    val node =
        MethodNode(
            Opcodes.API_VERSION,
            Opcodes.ACC_STATIC,
            "fake",
            Type.getMethodDescriptor(OBJECT_TYPE, AsmTypes.FUNCTION1, CONTINUATION_ASM_TYPE),
            null, null
        )

    with(InstructionAdapter(node)) {
        load(0, OBJECT_TYPE) // block
        load(1, OBJECT_TYPE) // continuation

        // block.invoke(continuation)
        invokeinterface(
            AsmTypes.FUNCTION1.internalName,
            OperatorNameConventions.INVOKE.identifier,
            "($OBJECT_TYPE)$OBJECT_TYPE"
        )

        val elseLabel = Label()
        // if (result === COROUTINE_SUSPENDED) {
        dup()
        loadCoroutineSuspendedMarker()
        ifacmpne(elseLabel)
        //   DebugProbesKt.probeCoroutineSuspended(continuation)
        load(1, OBJECT_TYPE) // continuation
        checkcast(CONTINUATION_ASM_TYPE)
        invokestatic(
            DEBUG_PROBES_INTERNAL_NAME,
            "probeCoroutineSuspended",
            "($CONTINUATION_ASM_TYPE)V",
            false
        )
        // }
        mark(elseLabel)
    }

    node.visitInsn(Opcodes.ARETURN)
    node.visitMaxs(3, 2)

    return node
}


private fun InstructionAdapter.invokeGetContext() {
    invokeinterface(
        CONTINUATION_ASM_TYPE.internalName,
        GET_CONTEXT_METHOD_NAME,
        Type.getMethodDescriptor(coroutineContextAsmType())
    )
    areturn(coroutineContextAsmType())
}

fun InstructionAdapter.loadCoroutineSuspendedMarker() {
    invokestatic(
        coroutinesIntrinsicsFileFacadeInternalName.internalName,
        "get$COROUTINE_SUSPENDED_NAME",
        Type.getMethodDescriptor(OBJECT_TYPE),
        false
    )
}

fun InstructionAdapter.generateCoroutineSuspendedCheck() {
    dup()
    loadCoroutineSuspendedMarker()
    val elseLabel = Label()
    ifacmpne(elseLabel)
    areturn(OBJECT_TYPE)
    mark(elseLabel)
}

const val SUSPEND_IMPL_NAME_SUFFIX = "\$suspendImpl"

@JvmField
val CONTINUATION_ASM_TYPE = StandardNames.CONTINUATION_INTERFACE_FQ_NAME.topLevelClassAsmType()

fun InstructionAdapter.invokeNullOutSpilledVariable() {
    invokestatic(
        SPILLING_INTERNAL_NAME,
        "nullOutSpilledVariable",
        "($OBJECT_TYPE)$OBJECT_TYPE",
        false
    )
}
