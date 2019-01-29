/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.builtins.getFunctionalClassKind
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.resolve.calls.components.TypeArgumentsToParametersMapper.TypeArgumentsMapping.NoExplicitArguments
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemOperation
import org.jetbrains.kotlin.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.resolve.calls.inference.substitute
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.getReceiverValueWithSmartCast
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind.*
import org.jetbrains.kotlin.resolve.calls.tower.InfixCallNoInfixModifier
import org.jetbrains.kotlin.resolve.calls.tower.InvokeConventionCallNoOperatorModifier
import org.jetbrains.kotlin.resolve.calls.tower.VisibilityError
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.checker.anySuperTypeConstructor
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal object CheckInstantiationOfAbstractClass : ResolutionPart() {
    override fun KotlinResolutionCandidate.process(workIndex: Int) {
        val candidateDescriptor = resolvedCall.candidateDescriptor

        if (candidateDescriptor is ConstructorDescriptor &&
            !callComponents.statelessCallbacks.isSuperOrDelegatingConstructorCall(resolvedCall.atom)
        ) {
            if (candidateDescriptor.constructedClass.modality == Modality.ABSTRACT) {
                addDiagnostic(InstantiationOfAbstractClass)
            }
        }
    }
}

internal object CheckVisibility : ResolutionPart() {
    override fun KotlinResolutionCandidate.process(workIndex: Int) {
        val containingDescriptor = scopeTower.lexicalScope.ownerDescriptor
        val dispatchReceiverArgument = resolvedCall.dispatchReceiverArgument

        if (scopeTower.isDebuggerContext) return

        val receiverValue = dispatchReceiverArgument?.receiver?.receiverValue ?: Visibilities.ALWAYS_SUITABLE_RECEIVER
        val invisibleMember =
            Visibilities.findInvisibleMember(receiverValue, resolvedCall.candidateDescriptor, containingDescriptor) ?: return

        if (dispatchReceiverArgument is ExpressionKotlinCallArgument) {
            val smartCastReceiver = getReceiverValueWithSmartCast(receiverValue, dispatchReceiverArgument.receiver.stableType)
            if (Visibilities.findInvisibleMember(smartCastReceiver, candidateDescriptor, containingDescriptor) == null) {
                addDiagnostic(
                    SmartCastDiagnostic(
                        dispatchReceiverArgument,
                        dispatchReceiverArgument.receiver.stableType,
                        resolvedCall.atom
                    )
                )
                return
            }
        }

        addDiagnostic(VisibilityError(invisibleMember))
    }
}

internal object MapTypeArguments : ResolutionPart() {
    override fun KotlinResolutionCandidate.process(workIndex: Int) {
        resolvedCall.typeArgumentMappingByOriginal =
                callComponents.typeArgumentsToParametersMapper.mapTypeArguments(kotlinCall, candidateDescriptor.original).also {
                    it.diagnostics.forEach(this@process::addDiagnostic)
                }
    }
}

internal object NoTypeArguments : ResolutionPart() {
    override fun KotlinResolutionCandidate.process(workIndex: Int) {
        assert(kotlinCall.typeArguments.isEmpty()) {
            "Variable call cannot has explicit type arguments: ${kotlinCall.typeArguments}. Call: $kotlinCall"
        }
        resolvedCall.typeArgumentMappingByOriginal = NoExplicitArguments
    }
}

internal object MapArguments : ResolutionPart() {
    override fun KotlinResolutionCandidate.process(workIndex: Int) {
        val mapping = callComponents.argumentsToParametersMapper.mapArguments(kotlinCall, candidateDescriptor)
        mapping.diagnostics.forEach(this::addDiagnostic)

        resolvedCall.argumentMappingByOriginal = mapping.parameterToCallArgumentMap
    }
}

internal object ArgumentsToCandidateParameterDescriptor : ResolutionPart() {
    override fun KotlinResolutionCandidate.process(workIndex: Int) {
        val map = hashMapOf<KotlinCallArgument, ValueParameterDescriptor>()
        for ((originalValueParameter, resolvedCallArgument) in resolvedCall.argumentMappingByOriginal) {
            val valueParameter = candidateDescriptor.valueParameters.getOrNull(originalValueParameter.index) ?: continue
            for (argument in resolvedCallArgument.arguments) {
                map[argument] = valueParameter
            }
        }
        resolvedCall.argumentToCandidateParameter = map
    }
}

internal object NoArguments : ResolutionPart() {
    override fun KotlinResolutionCandidate.process(workIndex: Int) {
        assert(kotlinCall.argumentsInParenthesis.isEmpty()) {
            "Variable call cannot has arguments: ${kotlinCall.argumentsInParenthesis}. Call: $kotlinCall"
        }
        assert(kotlinCall.externalArgument == null) {
            "Variable call cannot has external argument: ${kotlinCall.externalArgument}. Call: $kotlinCall"
        }
        resolvedCall.argumentMappingByOriginal = emptyMap()
        resolvedCall.argumentToCandidateParameter = emptyMap()
    }
}


internal object CreateFreshVariablesSubstitutor : ResolutionPart() {
    override fun KotlinResolutionCandidate.process(workIndex: Int) {
        if (candidateDescriptor.typeParameters.isEmpty()) {
            resolvedCall.substitutor = FreshVariableNewTypeSubstitutor.Empty
            return
        }
        val toFreshVariables = createToFreshVariableSubstitutorAndAddInitialConstraints(candidateDescriptor, csBuilder)
        resolvedCall.substitutor = toFreshVariables

        // bad function -- error on declaration side
        if (csBuilder.hasContradiction) return

        // optimization
        if (resolvedCall.typeArgumentMappingByOriginal == NoExplicitArguments && knownTypeParametersResultingSubstitutor == null) {
            return
        }

        val typeParameters = candidateDescriptor.original.typeParameters
        for (index in typeParameters.indices) {
            val typeParameter = typeParameters[index]
            val freshVariable = toFreshVariables.freshVariables[index]

            val knownTypeArgument = knownTypeParametersResultingSubstitutor?.substitute(typeParameter.defaultType)
            if (knownTypeArgument != null) {
                csBuilder.addEqualityConstraint(
                    freshVariable.defaultType,
                    knownTypeArgument.unwrap(),
                    KnownTypeParameterConstraintPosition(knownTypeArgument)
                )
                continue
            }

            val typeArgument = resolvedCall.typeArgumentMappingByOriginal.getTypeArgument(typeParameter)

            if (typeArgument is SimpleTypeArgument) {
                csBuilder.addEqualityConstraint(
                    freshVariable.defaultType,
                    typeArgument.type,
                    ExplicitTypeParameterConstraintPosition(typeArgument)
                )
            } else {
                assert(typeArgument == TypeArgumentPlaceholder) {
                    "Unexpected typeArgument: $typeArgument, ${typeArgument.javaClass.canonicalName}"
                }
            }
        }
    }

    fun createToFreshVariableSubstitutorAndAddInitialConstraints(
        candidateDescriptor: CallableDescriptor,
        csBuilder: ConstraintSystemOperation
    ): FreshVariableNewTypeSubstitutor {
        val typeParameters = candidateDescriptor.typeParameters

        val freshTypeVariables = typeParameters.map { TypeVariableFromCallableDescriptor(it) }

        val toFreshVariables = FreshVariableNewTypeSubstitutor(freshTypeVariables)

        for (freshVariable in freshTypeVariables) {
            csBuilder.registerVariable(freshVariable)
        }

        fun TypeVariableFromCallableDescriptor.addSubtypeConstraint(
            upperBound: KotlinType,
            position: DeclaredUpperBoundConstraintPosition
        ) {
            csBuilder.addSubtypeConstraint(defaultType, toFreshVariables.safeSubstitute(upperBound.unwrap()), position)
        }

        for (index in typeParameters.indices) {
            val typeParameter = typeParameters[index]
            val freshVariable = freshTypeVariables[index]
            val position = DeclaredUpperBoundConstraintPosition(typeParameter)

            for (upperBound in typeParameter.upperBounds) {
                freshVariable.addSubtypeConstraint(upperBound, position)
            }
        }

        if (candidateDescriptor is TypeAliasConstructorDescriptor) {
            val typeAliasDescriptor = candidateDescriptor.typeAliasDescriptor
            val originalTypes = typeAliasDescriptor.underlyingType.arguments.map { it.type }
            val originalTypeParameters = candidateDescriptor.underlyingConstructorDescriptor.typeParameters
            for (index in typeParameters.indices) {
                val typeParameter = typeParameters[index]
                val freshVariable = freshTypeVariables[index]
                val typeMapping = originalTypes.mapIndexedNotNull { i: Int, kotlinType: KotlinType ->
                    if (kotlinType == typeParameter.defaultType) i else null
                }
                for (originalIndex in typeMapping) {
                    // there can be null in case we already captured type parameter in outer class (in case of inner classes)
                    // see test innerClassTypeAliasConstructor.kt
                    val originalTypeParameter = originalTypeParameters.getOrNull(originalIndex) ?: continue
                    val position = DeclaredUpperBoundConstraintPosition(originalTypeParameter)
                    for (upperBound in originalTypeParameter.upperBounds) {
                        freshVariable.addSubtypeConstraint(upperBound, position)
                    }
                }
            }
        }
        return toFreshVariables
    }
}

internal object PostponedVariablesInitializerResolutionPart : ResolutionPart() {
    override fun KotlinResolutionCandidate.process(workIndex: Int) {
        for ((argument, parameter) in resolvedCall.argumentToCandidateParameter) {
            if (!callComponents.statelessCallbacks.isCoroutineCall(argument, parameter)) continue
            val receiverType = parameter.type.getReceiverTypeFromFunctionType() ?: continue

            for (freshVariable in resolvedCall.substitutor.freshVariables) {
                if (csBuilder.isPostponedTypeVariable(freshVariable)) continue
                if (receiverType.contains { it.constructor == freshVariable.originalTypeParameter.typeConstructor }) {
                    csBuilder.markPostponedVariable(freshVariable)
                }
            }
        }
    }
}

internal object CheckExplicitReceiverKindConsistency : ResolutionPart() {
    private fun KotlinResolutionCandidate.hasError(): Nothing =
        error(
            "Inconsistent call: $kotlinCall. \n" +
                    "Candidate: $candidateDescriptor, explicitReceiverKind: ${resolvedCall.explicitReceiverKind}.\n" +
                    "Explicit receiver: ${kotlinCall.explicitReceiver}, dispatchReceiverForInvokeExtension: ${kotlinCall.dispatchReceiverForInvokeExtension}"
        )

    override fun KotlinResolutionCandidate.process(workIndex: Int) {
        when (resolvedCall.explicitReceiverKind) {
            NO_EXPLICIT_RECEIVER -> if (kotlinCall.explicitReceiver is SimpleKotlinCallArgument || kotlinCall.dispatchReceiverForInvokeExtension != null) hasError()
            DISPATCH_RECEIVER, EXTENSION_RECEIVER ->
                if (kotlinCall.callKind == KotlinCallKind.INVOKE && kotlinCall.dispatchReceiverForInvokeExtension == null ||
                    kotlinCall.callKind != KotlinCallKind.INVOKE &&
                    (kotlinCall.explicitReceiver == null || kotlinCall.dispatchReceiverForInvokeExtension != null)
                ) hasError()
            BOTH_RECEIVERS -> if (kotlinCall.explicitReceiver == null || kotlinCall.dispatchReceiverForInvokeExtension == null) hasError()
        }
    }
}

private fun KotlinResolutionCandidate.resolveKotlinArgument(
    argument: KotlinCallArgument,
    candidateParameter: ParameterDescriptor?,
    isReceiver: Boolean
) {
    val expectedType = candidateParameter?.let { prepareExpectedType(argument, candidateParameter) }
    addResolvedKtPrimitive(resolveKtPrimitive(csBuilder, argument, expectedType, this, isReceiver))
}

private fun KotlinResolutionCandidate.prepareExpectedType(
    argument: KotlinCallArgument,
    candidateParameter: ParameterDescriptor
): UnwrappedType {
    val argumentType = getExpectedTypeWithSAMConversion(argument, candidateParameter) ?: argument.getExpectedType(
        candidateParameter,
        callComponents.languageVersionSettings
    )
    val resultType = knownTypeParametersResultingSubstitutor?.substitute(argumentType) ?: argumentType
    return resolvedCall.substitutor.substituteKeepAnnotations(resultType)
}

private fun KotlinResolutionCandidate.getExpectedTypeWithSAMConversion(
    argument: KotlinCallArgument,
    candidateParameter: ParameterDescriptor
): UnwrappedType? {
    if (!callComponents.samConversionTransformer.shouldRunSamConversionForFunction(resolvedCall.candidateDescriptor)) return null

    val argumentIsFunctional = when (argument) {
        is SimpleKotlinCallArgument -> argument.receiver.stableType.isSubtypeOfFunctionType()
        is LambdaKotlinCallArgument, is CallableReferenceKotlinCallArgument -> true
        else -> false
    }
    if (!argumentIsFunctional) return null

    val originalExpectedType = argument.getExpectedType(candidateParameter.original, callComponents.languageVersionSettings)
    val convertedTypeByOriginal =
        callComponents.samConversionTransformer.getFunctionTypeForPossibleSamType(originalExpectedType) ?: return null

    val candidateExpectedType = argument.getExpectedType(candidateParameter, callComponents.languageVersionSettings)
    val convertedTypeByCandidate = callComponents.samConversionTransformer.getFunctionTypeForPossibleSamType(candidateExpectedType)

    assert(candidateExpectedType.constructor == originalExpectedType.constructor && convertedTypeByCandidate != null) {
        "If original type is SAM type, then candidate should have same type constructor and corresponding function type\n" +
                "originalExpectType: $originalExpectedType, candidateExpectType: $candidateExpectedType\n" +
                "functionTypeByOriginal: $convertedTypeByOriginal, functionTypeByCandidate: $convertedTypeByCandidate"
    }

    resolvedCall.registerArgumentWithSamConversion(argument, SamConversionDescription(convertedTypeByOriginal, convertedTypeByCandidate!!))

    return convertedTypeByCandidate
}

private fun UnwrappedType.isSubtypeOfFunctionType() = anySuperTypeConstructor {
    it.declarationDescriptor?.getFunctionalClassKind() == FunctionClassDescriptor.Kind.Function
}

internal object CheckReceivers : ResolutionPart() {
    private fun KotlinResolutionCandidate.checkReceiver(
        receiverArgument: SimpleKotlinCallArgument?,
        receiverParameter: ReceiverParameterDescriptor?
    ) {
        if ((receiverArgument == null) != (receiverParameter == null)) {
            error("Inconsistency receiver state for call $kotlinCall and candidate descriptor: $candidateDescriptor")
        }
        if (receiverArgument == null || receiverParameter == null) return

        resolveKotlinArgument(receiverArgument, receiverParameter, isReceiver = true)
    }

    override fun KotlinResolutionCandidate.process(workIndex: Int) {
        if (workIndex == 0) {
            checkReceiver(resolvedCall.dispatchReceiverArgument, candidateDescriptor.dispatchReceiverParameter)
        } else {
            checkReceiver(resolvedCall.extensionReceiverArgument, candidateDescriptor.extensionReceiverParameter)
        }
    }

    override fun KotlinResolutionCandidate.workCount() = 2
}

internal object CheckArgumentsInParenthesis : ResolutionPart() {
    override fun KotlinResolutionCandidate.process(workIndex: Int) {
        val argument = kotlinCall.argumentsInParenthesis[workIndex]
        resolveKotlinArgument(argument, resolvedCall.argumentToCandidateParameter[argument], isReceiver = false)
    }

    override fun KotlinResolutionCandidate.workCount() = kotlinCall.argumentsInParenthesis.size
}

internal object CheckExternalArgument : ResolutionPart() {
    override fun KotlinResolutionCandidate.process(workIndex: Int) {
        val argument = kotlinCall.externalArgument ?: return

        resolveKotlinArgument(argument, resolvedCall.argumentToCandidateParameter[argument], isReceiver = false)
    }
}

internal object CheckInfixResolutionPart : ResolutionPart() {
    override fun KotlinResolutionCandidate.process(workIndex: Int) {
        val candidateDescriptor = resolvedCall.candidateDescriptor
        if (callComponents.statelessCallbacks.isInfixCall(kotlinCall) &&
            (candidateDescriptor !is FunctionDescriptor || !candidateDescriptor.isInfix)
        ) {
            addDiagnostic(InfixCallNoInfixModifier)
        }
    }
}

internal object CheckOperatorResolutionPart : ResolutionPart() {
    override fun KotlinResolutionCandidate.process(workIndex: Int) {
        val candidateDescriptor = resolvedCall.candidateDescriptor
        if (callComponents.statelessCallbacks.isOperatorCall(kotlinCall) &&
            (candidateDescriptor !is FunctionDescriptor || !candidateDescriptor.isOperator)
        ) {
            addDiagnostic(InvokeConventionCallNoOperatorModifier)
        }
    }
}

internal object CheckSuperExpressionCallPart : ResolutionPart() {
    override fun KotlinResolutionCandidate.process(workIndex: Int) {
        val candidateDescriptor = resolvedCall.candidateDescriptor

        if (callComponents.statelessCallbacks.isSuperExpression(resolvedCall.dispatchReceiverArgument)) {
            if (candidateDescriptor is MemberDescriptor && candidateDescriptor.modality == Modality.ABSTRACT) {
                addDiagnostic(AbstractSuperCall)
            }
        }

        val extensionReceiver = resolvedCall.extensionReceiverArgument
        if (extensionReceiver != null && callComponents.statelessCallbacks.isSuperExpression(extensionReceiver)) {
            addDiagnostic(SuperAsExtensionReceiver(extensionReceiver))
        }
    }
}

internal object ErrorDescriptorResolutionPart : ResolutionPart() {
    override fun KotlinResolutionCandidate.process(workIndex: Int) {
        assert(ErrorUtils.isError(candidateDescriptor)) {
            "Should be error descriptor: $candidateDescriptor"
        }
        resolvedCall.typeArgumentMappingByOriginal = TypeArgumentsToParametersMapper.TypeArgumentsMapping.NoExplicitArguments
        resolvedCall.argumentMappingByOriginal = emptyMap()
        resolvedCall.substitutor = FreshVariableNewTypeSubstitutor.Empty
        resolvedCall.argumentToCandidateParameter = emptyMap()

        kotlinCall.explicitReceiver?.safeAs<SimpleKotlinCallArgument>()?.let {
            resolveKotlinArgument(it, null, isReceiver = true)
        }
        for (argument in kotlinCall.argumentsInParenthesis) {
            resolveKotlinArgument(argument, null, isReceiver = true)
        }

        kotlinCall.externalArgument?.let {
            resolveKotlinArgument(it, null, isReceiver = true)
        }
    }
}