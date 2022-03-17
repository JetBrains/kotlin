/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.coroutines

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.COROUTINE_SUSPENDED_NAME
import org.jetbrains.kotlin.backend.common.isBuiltInSuspendCoroutineUninterceptedOrReturn
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.StandardNames.COROUTINES_INTRINSICS_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.builtins.StandardNames.COROUTINES_JVM_INTERNAL_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.builtins.isBuiltinFunctionalClassDescriptor
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.inline.addFakeContinuationMarker
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.coroutines.isSuspendLambda
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.checkers.isBuiltInCoroutineContext
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.calls.tower.NewResolvedCallImpl
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.descriptorUtil.resolveTopLevelClass
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.commons.Method
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

data class ResolvedCallWithRealDescriptor(val resolvedCall: ResolvedCall<*>, val fakeContinuationExpression: KtExpression)

@JvmField
val INITIAL_DESCRIPTOR_FOR_SUSPEND_FUNCTION = object : CallableDescriptor.UserDataKey<FunctionDescriptor> {}

@JvmField
val INITIAL_SUSPEND_DESCRIPTOR_FOR_INVOKE_SUSPEND = object : CallableDescriptor.UserDataKey<FunctionDescriptor> {}

val CONTINUATION_PARAMETER_NAME = Name.identifier("continuation")

const val CONTINUATION_VARIABLE_NAME = "\$continuation"

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
            resolvedCall = VariableAsFunctionResolvedCallImpl(
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
        TracingStrategy.EMPTY, MutableDataFlowInfoForArguments.WithoutArgumentsCheck(DataFlowInfo.EMPTY)
    )

    this.valueArguments.forEach {
        newCall.recordValueArgument(newCandidateDescriptor.valueParameters[it.key.index], it.value)
    }

    val psiFactory = KtPsiFactory(project, markGenerated = false)
    val arguments = psiFactory.createCallArguments("(this)").arguments.single()
    val thisExpression = arguments.getArgumentExpression()!!
    newCall.recordValueArgument(
        newCandidateDescriptor.valueParameters.last(),
        ExpressionValueArgument(arguments)
    )

    val newTypeArguments = newCandidateDescriptor.typeParameters.associateWith {
        typeArguments[candidateDescriptor.typeParameters[it.index]]!!.asTypeProjection()
    }

    newCall.setSubstitutor(
        TypeConstructorSubstitution.createByParametersMap(newTypeArguments).buildSubstitutor()
    )

    return ResolvedCallWithRealDescriptor(newCall, thisExpression)
}

fun ResolvedCall<*>.replaceSuspensionFunctionWithRealDescriptor(state: GenerationState): ResolvedCallWithRealDescriptor? =
    replaceSuspensionFunctionWithRealDescriptor(
        state.project,
        state.bindingContext
    )

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

enum class SuspensionPointKind { NEVER, NOT_INLINE, ALWAYS }

fun ResolvedCall<*>.isSuspensionPoint(codegen: ExpressionCodegen): SuspensionPointKind {
    val functionDescriptor = resultingDescriptor as? FunctionDescriptor ?: return SuspensionPointKind.NEVER
    if (!functionDescriptor.unwrapInitialDescriptorForSuspendFunction().isSuspend) return SuspensionPointKind.NEVER
    if (functionDescriptor.isBuiltInSuspendCoroutineUninterceptedOrReturnInJvm()) return SuspensionPointKind.ALWAYS
    if (functionDescriptor.isInline) return SuspensionPointKind.NEVER

    val isInlineLambda = this.safeAs<VariableAsFunctionResolvedCall>()
        ?.variableCall?.resultingDescriptor?.safeAs<ValueParameterDescriptor>()
        ?.let { it.isCrossinline || (!it.isNoinline && codegen.context.functionDescriptor.isInline) } == true
    return if (isInlineLambda) SuspensionPointKind.NOT_INLINE else SuspensionPointKind.ALWAYS
}

fun CallableDescriptor.isSuspendFunctionNotSuspensionView(): Boolean {
    if (this !is FunctionDescriptor) return false
    return this.isSuspend && this.getUserData(INITIAL_DESCRIPTOR_FOR_SUSPEND_FUNCTION) == null
}

fun <D : FunctionDescriptor> getOrCreateJvmSuspendFunctionView(function: D, state: GenerationState): D = getOrCreateJvmSuspendFunctionView(
    function,
    state.bindingContext
)

// Suspend functions have irregular signatures on JVM, containing an additional last parameter with type `Continuation<return-type>`,
// and return type Any?
// This function returns a function descriptor reflecting how the suspend function looks from point of view of JVM
@JvmOverloads
fun <D : FunctionDescriptor> getOrCreateJvmSuspendFunctionView(
    function: D,
    bindingContext: BindingContext? = null
): D {
    assert(function.isSuspend) {
        "Suspended function is expected, but $function was found"
    }

    @Suppress("UNCHECKED_CAST")
    bindingContext?.get(CodegenBinding.SUSPEND_FUNCTION_TO_JVM_VIEW, function)?.let { return it as D }

    val continuationParameter = ValueParameterDescriptorImpl(
        containingDeclaration = function,
        original = null,
        index = function.valueParameters.size,
        annotations = Annotations.EMPTY,
        name = CONTINUATION_PARAMETER_NAME,
        // Add j.l.Object to invoke(), because that is the type of parameters we have in FunctionN+1
        outType = if (function.containingDeclaration.safeAs<ClassDescriptor>()?.isBuiltinFunctionalClassDescriptor == true)
            function.builtIns.nullableAnyType
        else
            function.getContinuationParameterTypeOfSuspendFunction(),
        declaresDefaultValue = false, isCrossinline = false,
        isNoinline = false, varargElementType = null,
        source = SourceElement.NO_SOURCE
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

private fun FunctionDescriptor.getContinuationParameterTypeOfSuspendFunction() = module.getContinuationOfTypeOrAny(returnType!!)

fun ModuleDescriptor.getResult(kotlinType: KotlinType) =
    module.resolveTopLevelClass(
        StandardNames.RESULT_FQ_NAME,
        NoLookupLocation.FROM_BACKEND
    )?.defaultType?.let {
        KotlinTypeFactory.simpleType(
            it,
            arguments = listOf(kotlinType.asTypeProjection())
        )
    } ?: ErrorUtils.createErrorType(ErrorTypeKind.TYPE_FOR_RESULT)

fun FunctionDescriptor.isBuiltInSuspendCoroutineUninterceptedOrReturnInJvm() =
    getUserData(INITIAL_DESCRIPTOR_FOR_SUSPEND_FUNCTION)?.isBuiltInSuspendCoroutineUninterceptedOrReturn() == true

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
            COROUTINES_JVM_INTERNAL_PACKAGE_FQ_NAME.child(Name.identifier("DebugProbesKt")).topLevelClassAsmType().internalName,
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

@Suppress("UNCHECKED_CAST")
fun <D : CallableDescriptor?> D.unwrapInitialDescriptorForSuspendFunction(): D =
    this.safeAs<SimpleFunctionDescriptor>()?.getUserData(INITIAL_DESCRIPTOR_FOR_SUSPEND_FUNCTION) as D ?: this


fun FunctionDescriptor.getOriginalSuspendFunctionView(bindingContext: BindingContext): FunctionDescriptor =
    if (isSuspend)
        getOrCreateJvmSuspendFunctionView(unwrapInitialDescriptorForSuspendFunction().original, bindingContext)
    else
        this

// For each suspend function, we have a corresponding JVM view function that has an extra continuation parameter,
// and, more importantly, returns 'kotlin.Any' (so that it can return as a reference value or a special COROUTINE_SUSPENDED object).
// This also causes boxing of primitives and inline class values.
// If we have a function returning an inline class value that is mapped to a reference type, we want to avoid boxing.
// However, we have to do that consistently both on declaration site and on call site.
fun FunctionDescriptor.originalReturnTypeOfSuspendFunctionReturningUnboxedInlineClass(typeMapper: KotlinTypeMapper): KotlinType? {
    if (!isSuspend) return null
    // Suspend lambdas cannot return unboxed inline class
    if (this is AnonymousFunctionDescriptor) return null
    val originalDescriptor = unwrapInitialDescriptorForSuspendFunction().original
    val originalReturnType = originalDescriptor.returnType ?: return null
    if (!originalReturnType.isInlineClassType()) return null
    // Force boxing for primitives
    if (AsmUtil.isPrimitive(typeMapper.mapType(originalReturnType.makeNotNullable()))) return null
    // Force boxing for nullable inline class types with nullable underlying type
    if (originalReturnType.isMarkedNullable && originalReturnType.isNullableUnderlyingType()) return null
    // Force boxing if the function overrides function with different type modulo nullability
    if (originalDescriptor.overriddenDescriptors.any {
            (it.original.returnType?.isMarkedNullable == true && it.original.returnType?.isNullableUnderlyingType() == true) ||
                    // We do not care about type parameters, just main class type
                    it.original.returnType?.constructor?.declarationDescriptor != originalReturnType.constructor.declarationDescriptor
        }) return null
    // Don't box other inline classes
    return originalReturnType
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

fun InstructionAdapter.invokeInvokeSuspendWithUnit(thisName: String) {
    StackValue.putUnitInstance(this)

    invokevirtual(
        thisName,
        INVOKE_SUSPEND_METHOD_NAME,
        Type.getMethodDescriptor(OBJECT_TYPE, OBJECT_TYPE),
        false
    )
}

const val SUSPEND_IMPL_NAME_SUFFIX = "\$suspendImpl"

fun Method.getImplForOpenMethod(ownerInternalName: String) =
    Method(name + SUSPEND_IMPL_NAME_SUFFIX, returnType, arrayOf(Type.getObjectType(ownerInternalName)) + argumentTypes)

fun FunctionDescriptor.isSuspendLambdaOrLocalFunction() = this.isSuspend && when (this) {
    is AnonymousFunctionDescriptor -> this.isSuspendLambda
    is SimpleFunctionDescriptor -> this.visibility == DescriptorVisibilities.LOCAL
    else -> false
}

fun FunctionDescriptor.isLocalSuspendFunctionNotSuspendLambda(): Boolean =
    isSuspendLambdaOrLocalFunction() && this !is AnonymousFunctionDescriptor

@JvmField
val CONTINUATION_ASM_TYPE = StandardNames.CONTINUATION_INTERFACE_FQ_NAME.topLevelClassAsmType()

fun FunctionDescriptor.isInvokeSuspendOfLambda(): Boolean {
    if (this !is SimpleFunctionDescriptor) return false
    if (valueParameters.size != 1 ||
        valueParameters[0].name.asString() != SUSPEND_CALL_RESULT_NAME ||
        name.asString() != INVOKE_SUSPEND_METHOD_NAME
    ) return false
    return containingDeclaration is SyntheticClassDescriptorForLambda
}
