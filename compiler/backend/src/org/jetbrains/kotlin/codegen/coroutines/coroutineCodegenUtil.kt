/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.coroutines

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.builtins.isBuiltinFunctionalType
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.codegen.topLevelClassAsmType
import org.jetbrains.kotlin.codegen.topLevelClassInternalName
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.calls.tower.NewResolvedCallImpl
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.TypeConstructorSubstitution
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.tree.MethodNode

const val COROUTINE_LABEL_FIELD_NAME = "label"
const val SUSPEND_FUNCTION_CREATE_METHOD_NAME = "create"
const val DO_RESUME_METHOD_NAME = "doResume"
const val DATA_FIELD_NAME = "data"
const val EXCEPTION_FIELD_NAME = "exception"

@JvmField
val COROUTINES_JVM_INTERNAL_PACKAGE_FQ_NAME =
        DescriptorUtils.COROUTINES_PACKAGE_FQ_NAME.child(Name.identifier("jvm")).child(Name.identifier("internal"))

@JvmField
val CONTINUATION_ASM_TYPE = DescriptorUtils.CONTINUATION_INTERFACE_FQ_NAME.topLevelClassAsmType()

@JvmField
val COROUTINE_CONTEXT_ASM_TYPE = DescriptorUtils.COROUTINES_PACKAGE_FQ_NAME.child(Name.identifier("CoroutineContext")).topLevelClassAsmType()

@JvmField
val COROUTINE_IMPL_ASM_TYPE = COROUTINES_JVM_INTERNAL_PACKAGE_FQ_NAME.child(Name.identifier("CoroutineImpl")).topLevelClassAsmType()

private val COROUTINES_INTRINSICS_FILE_FACADE_INTERNAL_NAME =
        COROUTINES_INTRINSICS_PACKAGE_FQ_NAME.child(Name.identifier("IntrinsicsKt")).topLevelClassAsmType()

private val INTERNAL_COROUTINE_INTRINSICS_OWNER_INTERNAL_NAME =
        COROUTINES_JVM_INTERNAL_PACKAGE_FQ_NAME.child(Name.identifier("CoroutineIntrinsics")).topLevelClassInternalName()

private val NORMALIZE_CONTINUATION_METHOD_NAME = "normalizeContinuation"
private val GET_CONTEXT_METHOD_NAME = "getContext"

data class ResolvedCallWithRealDescriptor(val resolvedCall: ResolvedCall<*>, val fakeContinuationExpression: KtExpression)

@JvmField
val INITIAL_DESCRIPTOR_FOR_SUSPEND_FUNCTION = object : FunctionDescriptor.UserDataKey<FunctionDescriptor> {}

@JvmField
val INITIAL_SUSPEND_DESCRIPTOR_FOR_DO_RESUME = object : FunctionDescriptor.UserDataKey<FunctionDescriptor> {}

// Resolved calls to suspension function contain descriptors as they visible within coroutines:
// E.g. `fun <V> await(f: CompletableFuture<V>): V` instead of `fun <V> await(f: CompletableFuture<V>, machine: Continuation<V>): Unit`
// See `createJvmSuspendFunctionView` and it's usages for clarification
// But for call generation it's convenient to have `machine` (continuation) parameter/argument within resolvedCall.
// So this function returns resolved call with descriptor looking like `fun <V> await(f: CompletableFuture<V>, machine: Continuation<V>): Unit`
// and fake `this` expression that used as argument for second parameter
fun ResolvedCall<*>.replaceSuspensionFunctionWithRealDescriptor(
        project: Project,
        bindingContext: BindingContext
): ResolvedCallWithRealDescriptor? {
    if (this is VariableAsFunctionResolvedCall) {
        val replacedFunctionCall =
                functionCall.replaceSuspensionFunctionWithRealDescriptor(project, bindingContext)
                ?: return null

        @Suppress("UNCHECKED_CAST")
        return replacedFunctionCall.copy(
                VariableAsFunctionResolvedCallImpl(
                        replacedFunctionCall.resolvedCall as MutableResolvedCall<FunctionDescriptor>,
                        variableCall.asMutableResolvedCall(bindingContext)
                )
        )
    }
    val function = candidateDescriptor as? FunctionDescriptor ?: return null
    if (!function.isSuspend || function.getUserData(INITIAL_DESCRIPTOR_FOR_SUSPEND_FUNCTION) != null) return null

    val newCandidateDescriptor =
            when (function) {
                is FunctionImportedFromObject ->
                    getOrCreateJvmSuspendFunctionView(function.callableFromObject, bindingContext).asImportedFromObject()
                is SimpleFunctionDescriptor ->
                    getOrCreateJvmSuspendFunctionView(function, bindingContext)
                else ->
                    throw AssertionError("Unexpected suspend function descriptor: $function")
            }

    val newCall = ResolvedCallImpl(
            call,
            newCandidateDescriptor,
            dispatchReceiver, extensionReceiver, explicitReceiverKind,
            null, DelegatingBindingTrace(BindingTraceContext().bindingContext, "Temporary trace for unwrapped suspension function"),
            TracingStrategy.EMPTY, MutableDataFlowInfoForArguments.WithoutArgumentsCheck(DataFlowInfo.EMPTY))

    this.valueArguments.forEach {
        newCall.recordValueArgument(newCandidateDescriptor.valueParameters[it.key.index], it.value)
    }

    val psiFactory = KtPsiFactory(project, markGenerated = false)
    val arguments = psiFactory.createCallArguments("(this)").arguments.single()
    val thisExpression = arguments.getArgumentExpression()!!
    newCall.recordValueArgument(
            newCandidateDescriptor.valueParameters.last(),
            ExpressionValueArgument(arguments))

    val newTypeArguments = newCandidateDescriptor.typeParameters.map {
        Pair(it, typeArguments[candidateDescriptor.typeParameters[it.index]]!!.asTypeProjection())
    }.toMap()

    newCall.setResultingSubstitutor(
            TypeConstructorSubstitution.createByParametersMap(newTypeArguments).buildSubstitutor())

    return ResolvedCallWithRealDescriptor(newCall, thisExpression)
}

private fun ResolvedCall<VariableDescriptor>.asMutableResolvedCall(bindingContext: BindingContext): MutableResolvedCall<VariableDescriptor> {
    return when (this) {
        is ResolvedCallImpl<*> -> this as MutableResolvedCall<VariableDescriptor>
        is NewResolvedCallImpl<*> -> (this as NewResolvedCallImpl<VariableDescriptor>).asDummyOldResolvedCall(bindingContext)
        else -> throw IllegalStateException("No mutable resolved call for $this")
    }
}

private fun NewResolvedCallImpl<VariableDescriptor>.asDummyOldResolvedCall(bindingContext: BindingContext): ResolvedCallImpl<VariableDescriptor> {
    return ResolvedCallImpl(
            call,
            candidateDescriptor,
            dispatchReceiver, extensionReceiver, explicitReceiverKind,
            null, DelegatingBindingTrace(bindingContext, "Trace for old call"),
            TracingStrategy.EMPTY, MutableDataFlowInfoForArguments.WithoutArgumentsCheck(DataFlowInfo.EMPTY)
    )
}

fun ResolvedCall<*>.isSuspendNoInlineCall() =
        resultingDescriptor.safeAs<FunctionDescriptor>()
                ?.let {
                    it.isSuspend && (!it.isInline || it.isBuiltInSuspendCoroutineOrReturnInJvm())
                } == true

fun CallableDescriptor.isSuspendFunctionNotSuspensionView(): Boolean {
    if (this !is FunctionDescriptor) return false
    return this.isSuspend && this.getUserData(INITIAL_DESCRIPTOR_FOR_SUSPEND_FUNCTION) == null
}

// Suspend functions have irregular signatures on JVM, containing an additional last parameter with type `Continuation<return-type>`,
// and return type Any?
// This function returns a function descriptor reflecting how the suspend function looks from point of view of JVM
@JvmOverloads
fun <D : FunctionDescriptor> getOrCreateJvmSuspendFunctionView(function: D, bindingContext: BindingContext? = null): D {
    assert(function.isSuspend) {
        "Suspended function is expected, but $function was found"
    }

    @Suppress("UNCHECKED_CAST")
    bindingContext?.get(CodegenBinding.SUSPEND_FUNCTION_TO_JVM_VIEW, function)?.let { return it as D }

    val continuationParameter = ValueParameterDescriptorImpl(
            function, null, function.valueParameters.size, Annotations.EMPTY, Name.identifier("continuation"),
            // Add j.l.Object to invoke(), because that is the type of parameters we have in FunctionN+1
            if (function.containingDeclaration.safeAs<ClassDescriptor>()?.defaultType?.isBuiltinFunctionalType == true)
                function.builtIns.nullableAnyType
            else
                function.getContinuationParameterTypeOfSuspendFunction(),
            /* declaresDefaultValue = */ false, /* isCrossinline = */ false,
            /* isNoinline = */ false, /* varargElementType = */ null, SourceElement.NO_SOURCE
    )

    return function.createCustomCopy {
        setDropOriginalInContainingParts()
        setPreserveSourceElement()
        setReturnType(function.builtIns.nullableAnyType)
        setValueParameters(it.valueParameters + continuationParameter)
        putUserData(INITIAL_DESCRIPTOR_FOR_SUSPEND_FUNCTION, it)
    }
}

typealias FunctionDescriptorCopyBuilderToFunctionDescriptorCopyBuilder =
    FunctionDescriptor.CopyBuilder<out FunctionDescriptor>.(FunctionDescriptor)
        -> FunctionDescriptor.CopyBuilder<out FunctionDescriptor>

fun <D : FunctionDescriptor> D.createCustomCopy(
        copySettings: FunctionDescriptorCopyBuilderToFunctionDescriptorCopyBuilder
): D {

    val newOriginal =
            if (original !== this)
                original.createCustomCopy(copySettings)
            else
                null

    val result = newCopyBuilder().copySettings(this).setOriginal(newOriginal).build()!!

    result.overriddenDescriptors = this.overriddenDescriptors.map { it.createCustomCopy(copySettings) }

    @Suppress("UNCHECKED_CAST")
    return result as D
}

private fun FunctionDescriptor.getContinuationParameterTypeOfSuspendFunction() =
        module.getContinuationOfTypeOrAny(returnType!!)

fun ModuleDescriptor.getContinuationOfTypeOrAny(kotlinType: KotlinType) =
        module.findContinuationClassDescriptorOrNull(NoLookupLocation.FROM_BACKEND)?.defaultType?.let {
            KotlinTypeFactory.simpleType(
                    it,
                    arguments = listOf(kotlinType.asTypeProjection())
            )
        } ?: module.builtIns.nullableAnyType


fun FunctionDescriptor.isBuiltInSuspendCoroutineOrReturnInJvm() =
        getUserData(INITIAL_DESCRIPTOR_FOR_SUSPEND_FUNCTION)?.isBuiltInSuspendCoroutineOrReturn() == true

fun createMethodNodeForSuspendCoroutineOrReturn(
        functionDescriptor: FunctionDescriptor,
        typeMapper: KotlinTypeMapper
): MethodNode {
    assert(functionDescriptor.isBuiltInSuspendCoroutineOrReturnInJvm()) {
        "functionDescriptor must be kotlin.coroutines.intrinsics.suspendOrReturn"
    }

    val node =
            MethodNode(
                    Opcodes.ASM5,
                    Opcodes.ACC_STATIC,
                    "fake",
                    typeMapper.mapAsmMethod(functionDescriptor).descriptor, null, null
            )

    node.visitVarInsn(Opcodes.ALOAD, 0)
    node.visitVarInsn(Opcodes.ALOAD, 1)

    node.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            INTERNAL_COROUTINE_INTRINSICS_OWNER_INTERNAL_NAME,
            NORMALIZE_CONTINUATION_METHOD_NAME,
            Type.getMethodDescriptor(CONTINUATION_ASM_TYPE, CONTINUATION_ASM_TYPE),
            false
    )

    node.visitMethodInsn(
            Opcodes.INVOKEINTERFACE,
            typeMapper.mapType(functionDescriptor.valueParameters[0]).internalName,
            OperatorNameConventions.INVOKE.identifier,
            "(${AsmTypes.OBJECT_TYPE})${AsmTypes.OBJECT_TYPE}",
            true
    )
    node.visitInsn(Opcodes.ARETURN)
    node.visitMaxs(2, 2)

    return node
}

fun createMethodNodeForCoroutineContext(functionDescriptor: FunctionDescriptor): MethodNode {
    assert(functionDescriptor.isBuiltInCoroutineContext()) {
        "functionDescriptor must be kotlin.coroutines.intrinsics.coroutineContext property getter"
    }

    val node =
            MethodNode(
                    Opcodes.ASM5,
                    Opcodes.ACC_STATIC,
                    "fake",
                    Type.getMethodDescriptor(COROUTINE_CONTEXT_ASM_TYPE, CONTINUATION_ASM_TYPE),
                    null, null
            )

    node.visitVarInsn(Opcodes.ALOAD, 0)

    node.visitMethodInsn(
            Opcodes.INVOKEINTERFACE,
            CONTINUATION_ASM_TYPE.internalName,
            GET_CONTEXT_METHOD_NAME,
            Type.getMethodDescriptor(COROUTINE_CONTEXT_ASM_TYPE),
            true
    )
    node.visitInsn(Opcodes.ARETURN)
    node.visitMaxs(1, 1)

    return node
}

@Suppress("UNCHECKED_CAST")
fun <D : CallableDescriptor?> D.unwrapInitialDescriptorForSuspendFunction(): D =
        this.safeAs<SimpleFunctionDescriptor>()?.getUserData(INITIAL_DESCRIPTOR_FOR_SUSPEND_FUNCTION) as D ?: this


fun FunctionDescriptor.getOriginalSuspendFunctionView(bindingContext: BindingContext): FunctionDescriptor =
        if (isSuspend)
            getOrCreateJvmSuspendFunctionView(unwrapInitialDescriptorForSuspendFunction().original, bindingContext)
        else
            this

fun InstructionAdapter.loadCoroutineSuspendedMarker() {
    invokestatic(
            COROUTINES_INTRINSICS_FILE_FACADE_INTERNAL_NAME.internalName,
            "get$COROUTINE_SUSPENDED_NAME",
            Type.getMethodDescriptor(AsmTypes.OBJECT_TYPE),
            false
    )
}

fun InstructionAdapter.invokeDoResumeWithUnit(thisName: String) {
    // .doResume(Unit, null)
    StackValue.putUnitInstance(this)

    aconst(null)

    invokevirtual(
            thisName,
            DO_RESUME_METHOD_NAME,
            Type.getMethodDescriptor(AsmTypes.OBJECT_TYPE, AsmTypes.OBJECT_TYPE, AsmTypes.JAVA_THROWABLE_TYPE),
            false
    )
}

fun Method.getImplForOpenMethod(ownerInternalName: String) =
        Method("$name\$suspendImpl", returnType, arrayOf(Type.getObjectType(ownerInternalName)) + argumentTypes)
