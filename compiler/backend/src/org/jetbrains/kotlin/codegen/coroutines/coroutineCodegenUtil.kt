/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.coroutines

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.COROUTINE_SUSPENDED_NAME
import org.jetbrains.kotlin.backend.common.isBuiltInSuspendCoroutineUninterceptedOrReturn
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.isBuiltinFunctionalClassDescriptor
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.inline.addFakeContinuationMarker
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.config.*
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
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.TypeConstructorSubstitution
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
const val DO_RESUME_METHOD_NAME = "doResume"
const val INVOKE_SUSPEND_METHOD_NAME = "invokeSuspend"
const val EXCEPTION_FIELD_NAME = "exception"

val RELEASE_COROUTINES_VERSION_SETTINGS = LanguageVersionSettingsImpl(LanguageVersion.KOTLIN_1_3, ApiVersion.KOTLIN_1_3)

fun LanguageVersionSettings.isResumeImplMethodName(name: String) =
    if (isReleaseCoroutines())
        name == INVOKE_SUSPEND_METHOD_NAME
    else
        name == DO_RESUME_METHOD_NAME

fun LanguageVersionSettings.dataFieldName(): String = if (isReleaseCoroutines()) "result" else "data"

fun isResumeImplMethodNameFromAnyLanguageSettings(name: String) = name == INVOKE_SUSPEND_METHOD_NAME || name == DO_RESUME_METHOD_NAME

fun LanguageVersionSettings.coroutinesJvmInternalPackageFqName() =
    coroutinesPackageFqName().child(Name.identifier("jvm")).child(Name.identifier("internal"))

val DEBUG_METADATA_ANNOTATION_ASM_TYPE = RELEASE_COROUTINES_VERSION_SETTINGS.coroutinesJvmInternalPackageFqName()
    .child(Name.identifier("DebugMetadata")).topLevelClassAsmType()

fun LanguageVersionSettings.continuationAsmType() =
    continuationInterfaceFqName().topLevelClassAsmType()

fun continuationAsmTypes() = listOf(
    LanguageVersionSettingsImpl(LanguageVersion.KOTLIN_1_3, ApiVersion.KOTLIN_1_3).continuationAsmType(),
    LanguageVersionSettingsImpl(LanguageVersion.KOTLIN_1_2, ApiVersion.KOTLIN_1_2).continuationAsmType()
)

fun LanguageVersionSettings.coroutineContextAsmType() =
    coroutinesPackageFqName().child(Name.identifier("CoroutineContext")).topLevelClassAsmType()

fun LanguageVersionSettings.isCoroutineSuperClass(internalName: String): Boolean {
    val coroutinesJvmInternalPackage = coroutinesJvmInternalPackageFqName()

    return if (isReleaseCoroutines())
        coroutinesJvmInternalPackage.identifiedChild("ContinuationImpl") == internalName ||
                coroutinesJvmInternalPackage.identifiedChild("RestrictedContinuationImpl") == internalName ||
                coroutinesJvmInternalPackage.identifiedChild("SuspendLambda") == internalName ||
                coroutinesJvmInternalPackage.identifiedChild("RestrictedSuspendLambda") == internalName
    else
        coroutinesJvmInternalPackage.identifiedChild("CoroutineImpl") == internalName
}

private fun FqName.identifiedChild(name: String) = child(Name.identifier(name)).topLevelClassInternalName()

private fun LanguageVersionSettings.coroutinesIntrinsicsFileFacadeInternalName() =
    coroutinesIntrinsicsPackageFqName().child(Name.identifier("IntrinsicsKt")).topLevelClassAsmType()

private fun LanguageVersionSettings.internalCoroutineIntrinsicsOwnerInternalName() =
    coroutinesJvmInternalPackageFqName().child(Name.identifier("CoroutineIntrinsics")).topLevelClassInternalName()

fun computeLabelOwner(languageVersionSettings: LanguageVersionSettings, thisName: String): Type =
    if (languageVersionSettings.isReleaseCoroutines())
        Type.getObjectType(thisName)
    else
        languageVersionSettings.coroutinesJvmInternalPackageFqName().child(Name.identifier("CoroutineImpl")).topLevelClassAsmType()

private const val NORMALIZE_CONTINUATION_METHOD_NAME = "normalizeContinuation"
private const val GET_CONTEXT_METHOD_NAME = "getContext"

data class ResolvedCallWithRealDescriptor(val resolvedCall: ResolvedCall<*>, val fakeContinuationExpression: KtExpression)

@JvmField
val INITIAL_DESCRIPTOR_FOR_SUSPEND_FUNCTION = object : CallableDescriptor.UserDataKey<FunctionDescriptor> {}

@JvmField
val INITIAL_SUSPEND_DESCRIPTOR_FOR_DO_RESUME = object : CallableDescriptor.UserDataKey<FunctionDescriptor> {}

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
    bindingContext: BindingContext,
    isReleaseCoroutines: Boolean
): ResolvedCallWithRealDescriptor? {
    if (this is VariableAsFunctionResolvedCall) {
        val replacedFunctionCall =
            functionCall.replaceSuspensionFunctionWithRealDescriptor(project, bindingContext, isReleaseCoroutines)
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
                getOrCreateJvmSuspendFunctionView(function.callableFromObject, isReleaseCoroutines, bindingContext).asImportedFromObject()
            is SimpleFunctionDescriptor ->
                getOrCreateJvmSuspendFunctionView(function, isReleaseCoroutines, bindingContext)
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

    val newTypeArguments = newCandidateDescriptor.typeParameters.map {
        Pair(it, typeArguments[candidateDescriptor.typeParameters[it.index]]!!.asTypeProjection())
    }.toMap()

    newCall.setResultingSubstitutor(
        TypeConstructorSubstitution.createByParametersMap(newTypeArguments).buildSubstitutor()
    )

    return ResolvedCallWithRealDescriptor(newCall, thisExpression)
}

fun ResolvedCall<*>.replaceSuspensionFunctionWithRealDescriptor(state: GenerationState): ResolvedCallWithRealDescriptor? =
    replaceSuspensionFunctionWithRealDescriptor(
        state.project,
        state.bindingContext,
        state.languageVersionSettings.supportsFeature(LanguageFeature.ReleaseCoroutines)
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

fun ResolvedCall<*>.isSuspensionPoint(codegen: ExpressionCodegen, languageVersionSettings: LanguageVersionSettings): SuspensionPointKind {
    val functionDescriptor = resultingDescriptor as? FunctionDescriptor ?: return SuspensionPointKind.NEVER
    if (!functionDescriptor.unwrapInitialDescriptorForSuspendFunction().isSuspend) return SuspensionPointKind.NEVER
    if (functionDescriptor.isBuiltInSuspendCoroutineUninterceptedOrReturnInJvm(languageVersionSettings)) return SuspensionPointKind.ALWAYS
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
    state.languageVersionSettings.supportsFeature(LanguageFeature.ReleaseCoroutines),
    state.bindingContext
)

// Suspend functions have irregular signatures on JVM, containing an additional last parameter with type `Continuation<return-type>`,
// and return type Any?
// This function returns a function descriptor reflecting how the suspend function looks from point of view of JVM
@JvmOverloads
fun <D : FunctionDescriptor> getOrCreateJvmSuspendFunctionView(
    function: D,
    isReleaseCoroutines: Boolean,
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
            function.getContinuationParameterTypeOfSuspendFunction(isReleaseCoroutines),
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

private fun FunctionDescriptor.getContinuationParameterTypeOfSuspendFunction(isReleaseCoroutines: Boolean) =
    module.getContinuationOfTypeOrAny(returnType!!, if (this.needsExperimentalCoroutinesWrapper()) false else isReleaseCoroutines)

fun ModuleDescriptor.getResult(kotlinType: KotlinType) =
    module.resolveTopLevelClass(
        StandardNames.RESULT_FQ_NAME,
        NoLookupLocation.FROM_BACKEND
    )?.defaultType?.let {
        KotlinTypeFactory.simpleType(
            it,
            arguments = listOf(kotlinType.asTypeProjection())
        )
    } ?: ErrorUtils.createErrorType("For Result")

private fun MethodNode.invokeNormalizeContinuation(languageVersionSettings: LanguageVersionSettings) {
    visitMethodInsn(
        Opcodes.INVOKESTATIC,
        languageVersionSettings.internalCoroutineIntrinsicsOwnerInternalName(),
        NORMALIZE_CONTINUATION_METHOD_NAME,
        Type.getMethodDescriptor(languageVersionSettings.continuationAsmType(), languageVersionSettings.continuationAsmType()),
        false
    )
}

fun FunctionDescriptor.isBuiltInSuspendCoroutineUninterceptedOrReturnInJvm(languageVersionSettings: LanguageVersionSettings) =
    getUserData(INITIAL_DESCRIPTOR_FOR_SUSPEND_FUNCTION)?.isBuiltInSuspendCoroutineUninterceptedOrReturn(languageVersionSettings) == true

fun createMethodNodeForIntercepted(languageVersionSettings: LanguageVersionSettings): MethodNode {
    val node =
        MethodNode(
            Opcodes.API_VERSION,
            Opcodes.ACC_STATIC,
            "fake",
            Type.getMethodDescriptor(languageVersionSettings.continuationAsmType(), languageVersionSettings.continuationAsmType()),
            null, null
        )

    node.visitVarInsn(Opcodes.ALOAD, 0)

    node.invokeNormalizeContinuation(languageVersionSettings)

    node.visitInsn(Opcodes.ARETURN)
    node.visitMaxs(1, 1)

    return node
}

fun createMethodNodeForCoroutineContext(
    functionDescriptor: FunctionDescriptor,
    languageVersionSettings: LanguageVersionSettings
): MethodNode {
    assert(functionDescriptor.isBuiltInCoroutineContext(languageVersionSettings)) {
        "functionDescriptor must be kotlin.coroutines.intrinsics.coroutineContext property getter"
    }

    val node =
        MethodNode(
            Opcodes.API_VERSION,
            Opcodes.ACC_STATIC,
            "fake",
            Type.getMethodDescriptor(languageVersionSettings.coroutineContextAsmType()),
            null, null
        )

    val v = InstructionAdapter(node)

    addFakeContinuationMarker(v)

    v.invokeGetContext(languageVersionSettings)

    node.visitMaxs(1, 1)

    return node
}

fun createMethodNodeForSuspendCoroutineUninterceptedOrReturn(languageVersionSettings: LanguageVersionSettings): MethodNode {
    val node =
        MethodNode(
            Opcodes.API_VERSION,
            Opcodes.ACC_STATIC,
            "fake",
            Type.getMethodDescriptor(OBJECT_TYPE, AsmTypes.FUNCTION1, languageVersionSettings.continuationAsmType()),
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

        if (languageVersionSettings.supportsFeature(LanguageFeature.ReleaseCoroutines)) {
            val elseLabel = Label()
            // if (result === COROUTINE_SUSPENDED) {
            dup()
            loadCoroutineSuspendedMarker(languageVersionSettings)
            ifacmpne(elseLabel)
            //   DebugProbesKt.probeCoroutineSuspended(continuation)
            load(1, OBJECT_TYPE) // continuation
            checkcast(languageVersionSettings.continuationAsmType())
            invokestatic(
                languageVersionSettings.coroutinesJvmInternalPackageFqName().child(Name.identifier("DebugProbesKt"))
                    .topLevelClassAsmType().internalName,
                "probeCoroutineSuspended",
                "(${languageVersionSettings.continuationAsmType()})V",
                false
            )
            // }
            mark(elseLabel)
        }
    }

    node.visitInsn(Opcodes.ARETURN)
    node.visitMaxs(3, 2)

    return node
}


private fun InstructionAdapter.invokeGetContext(languageVersionSettings: LanguageVersionSettings) {
    invokeinterface(
        languageVersionSettings.continuationAsmType().internalName,
        GET_CONTEXT_METHOD_NAME,
        Type.getMethodDescriptor(languageVersionSettings.coroutineContextAsmType())
    )
    areturn(languageVersionSettings.coroutineContextAsmType())
}

@Suppress("UNCHECKED_CAST")
fun <D : CallableDescriptor?> D.unwrapInitialDescriptorForSuspendFunction(): D =
    this.safeAs<SimpleFunctionDescriptor>()?.getUserData(INITIAL_DESCRIPTOR_FOR_SUSPEND_FUNCTION) as D ?: this


fun FunctionDescriptor.getOriginalSuspendFunctionView(bindingContext: BindingContext, isReleaseCoroutines: Boolean): FunctionDescriptor =
    if (isSuspend)
        getOrCreateJvmSuspendFunctionView(unwrapInitialDescriptorForSuspendFunction().original, isReleaseCoroutines, bindingContext)
    else
        this

fun FunctionDescriptor.getOriginalSuspendFunctionView(bindingContext: BindingContext, state: GenerationState) =
    getOriginalSuspendFunctionView(bindingContext, state.languageVersionSettings.supportsFeature(LanguageFeature.ReleaseCoroutines))

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

fun InstructionAdapter.loadCoroutineSuspendedMarker(languageVersionSettings: LanguageVersionSettings) {
    invokestatic(
        languageVersionSettings.coroutinesIntrinsicsFileFacadeInternalName().internalName,
        "get$COROUTINE_SUSPENDED_NAME",
        Type.getMethodDescriptor(OBJECT_TYPE),
        false
    )
}

fun InstructionAdapter.generateCoroutineSuspendedCheck(languageVersionSettings: LanguageVersionSettings) {
    dup()
    loadCoroutineSuspendedMarker(languageVersionSettings)
    val elseLabel = Label()
    ifacmpne(elseLabel)
    areturn(OBJECT_TYPE)
    mark(elseLabel)
}

fun InstructionAdapter.invokeDoResumeWithUnit(thisName: String) {
    // .doResume(Unit, null)
    StackValue.putUnitInstance(this)

    aconst(null)

    invokevirtual(
        thisName,
        DO_RESUME_METHOD_NAME,
        Type.getMethodDescriptor(OBJECT_TYPE, OBJECT_TYPE, AsmTypes.JAVA_THROWABLE_TYPE),
        false
    )
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

fun FunctionDescriptor.isLocalSuspendFunctionNotSuspendLambda() = isSuspendLambdaOrLocalFunction() && this !is AnonymousFunctionDescriptor

@JvmField
val EXPERIMENTAL_CONTINUATION_ASM_TYPE = StandardNames.CONTINUATION_INTERFACE_FQ_NAME_EXPERIMENTAL.topLevelClassAsmType()

@JvmField
val RELEASE_CONTINUATION_ASM_TYPE = StandardNames.CONTINUATION_INTERFACE_FQ_NAME_RELEASE.topLevelClassAsmType()

fun FunctionDescriptor.isInvokeSuspendOfLambda(): Boolean {
    if (this !is SimpleFunctionDescriptor) return false
    if (valueParameters.size != 1 ||
        valueParameters[0].name.asString() != SUSPEND_CALL_RESULT_NAME ||
        name.asString() != "invokeSuspend"
    ) return false
    return containingDeclaration is SyntheticClassDescriptorForLambda
}