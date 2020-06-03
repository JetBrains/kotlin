/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.builtins.UnsignedTypes
import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.resolve.calls.components.TypeArgumentsToParametersMapper.TypeArgumentsMapping.NoExplicitArguments
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemOperation
import org.jetbrains.kotlin.resolve.calls.inference.NewConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.resolve.calls.inference.substitute
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.getReceiverValueWithSmartCast
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind.*
import org.jetbrains.kotlin.resolve.calls.tower.InfixCallNoInfixModifier
import org.jetbrains.kotlin.resolve.calls.tower.InvokeConventionCallNoOperatorModifier
import org.jetbrains.kotlin.resolve.calls.tower.VisibilityError
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.typeConstructor
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal object CheckVisibility : ResolutionPart() {
    override fun KotlinResolutionCandidate.process(workIndex: Int) {
        val containingDescriptor = scopeTower.lexicalScope.ownerDescriptor
        val dispatchReceiverArgument = resolvedCall.dispatchReceiverArgument

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
        resolvedCall.knownParametersSubstitutor = knownTypeParametersResultingSubstitutor ?: TypeSubstitutor.EMPTY

        if (candidateDescriptor.typeParameters.isEmpty()) {
            resolvedCall.freshVariablesSubstitutor = FreshVariableNewTypeSubstitutor.Empty
            return
        }
        val toFreshVariables = createToFreshVariableSubstitutorAndAddInitialConstraints(candidateDescriptor, csBuilder)
        resolvedCall.freshVariablesSubstitutor = toFreshVariables

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
                    getTypePreservingFlexibilityWrtTypeVariable(knownTypeArgument.unwrap(), freshVariable),
                    KnownTypeParameterConstraintPosition(knownTypeArgument)
                )
                continue
            }

            val typeArgument = resolvedCall.typeArgumentMappingByOriginal.getTypeArgument(typeParameter)

            if (typeArgument is SimpleTypeArgument) {
                csBuilder.addEqualityConstraint(
                    freshVariable.defaultType,
                    getTypePreservingFlexibilityWrtTypeVariable(typeArgument.type, freshVariable),
                    ExplicitTypeParameterConstraintPosition(typeArgument)
                )
            } else {
                assert(typeArgument == TypeArgumentPlaceholder) {
                    "Unexpected typeArgument: $typeArgument, ${typeArgument.javaClass.canonicalName}"
                }
            }
        }
    }

    fun TypeParameterDescriptor.shouldBeFlexible(): Boolean {
        return upperBounds.any {
            it.isFlexible() || ((it.constructor.declarationDescriptor as? TypeParameterDescriptor)?.run { shouldBeFlexible() } ?: false)
        }
    }

    private fun getTypePreservingFlexibilityWrtTypeVariable(
        type: KotlinType,
        typeVariable: TypeVariableFromCallableDescriptor
    ) = if (typeVariable.originalTypeParameter.shouldBeFlexible()) {
        KotlinTypeFactory.flexibleType(type.makeNotNullable().lowerIfFlexible(), type.makeNullable().upperIfFlexible())
    } else type

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
            val position = DeclaredUpperBoundConstraintPositionImpl(typeParameter)

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
                    val position = DeclaredUpperBoundConstraintPositionImpl(originalTypeParameter)
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

            for (freshVariable in resolvedCall.freshVariablesSubstitutor.freshVariables) {
                if (resolvedCall.typeArgumentMappingByOriginal.getTypeArgument(freshVariable.originalTypeParameter) is SimpleTypeArgument)
                    continue

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

internal object CollectionTypeVariableUsagesInfo : ResolutionPart() {
    private val KotlinType.isComputed get() = this !is WrappedType || isComputed()

    private fun NewConstraintSystem.isContainedInInvariantOrContravariantPositions(
        variableTypeConstructor: TypeConstructorMarker,
        baseType: KotlinTypeMarker,
        wasOutVariance: Boolean = true
    ): Boolean {
        if (baseType !is KotlinType) return false

        val dependentTypeParameter = getTypeParameterByVariable(variableTypeConstructor) ?: return false
        val declaredTypeParameters = baseType.constructor.parameters

        if (declaredTypeParameters.size < baseType.arguments.size) return false

        for ((argumentsIndex, argument) in baseType.arguments.withIndex()) {
            if (argument.isStarProjection || argument.type.isMarkedNullable) continue

            val currentEffectiveVariance =
                declaredTypeParameters[argumentsIndex].variance == Variance.OUT_VARIANCE || argument.projectionKind == Variance.OUT_VARIANCE
            val effectiveVarianceFromTopLevel = wasOutVariance && currentEffectiveVariance

            if ((argument.type.constructor == dependentTypeParameter || argument.type.constructor == variableTypeConstructor) && !effectiveVarianceFromTopLevel)
                return true

            if (isContainedInInvariantOrContravariantPositions(variableTypeConstructor, argument.type, effectiveVarianceFromTopLevel))
                return true
        }

        return false
    }

    private fun isContainedInInvariantOrContravariantPositionsAmongTypeParameters(
        checkingType: TypeVariableFromCallableDescriptor,
        typeParameters: List<TypeParameterDescriptor>
    ) = typeParameters.any {
        it.variance != Variance.OUT_VARIANCE && it.typeConstructor == checkingType.originalTypeParameter.typeConstructor
    }

    private fun NewConstraintSystem.getDependentTypeParameters(
        variable: TypeConstructorMarker,
        dependentTypeParametersSeen: List<Pair<TypeConstructorMarker, KotlinTypeMarker?>> = listOf()
    ): List<Pair<TypeConstructorMarker, KotlinTypeMarker?>> {
        val context = asConstraintSystemCompleterContext()
        val dependentTypeParameters = getBuilder().currentStorage().notFixedTypeVariables.mapNotNull { (typeConstructor, constraints) ->
            val upperBounds = constraints.constraints.filter {
                it.position.from is DeclaredUpperBoundConstraintPosition && it.kind == ConstraintKind.UPPER
            }

            upperBounds.mapNotNull { constraint ->
                if (constraint.type.typeConstructor(context) != variable) {
                    val suitableUpperBound = upperBounds.find { upperBound ->
                        with(context) { upperBound.type.contains { it.typeConstructor() == variable } }
                    }?.type

                    if (suitableUpperBound != null) typeConstructor to suitableUpperBound else null
                } else typeConstructor to null
            }
        }.flatten().filter { it !in dependentTypeParametersSeen && it.first != variable }

        return dependentTypeParameters + dependentTypeParameters.mapNotNull { (typeConstructor, _) ->
            if (typeConstructor != variable) {
                getDependentTypeParameters(typeConstructor, dependentTypeParameters + dependentTypeParametersSeen)
            } else null
        }.flatten()
    }

    private fun NewConstraintSystem.isContainedInInvariantOrContravariantPositionsAmongUpperBound(
        checkingType: TypeConstructorMarker,
        dependentTypeParameters: List<Pair<TypeConstructorMarker, KotlinTypeMarker?>>
    ): Boolean {
        var currentTypeParameterConstructor = checkingType

        return dependentTypeParameters.any { (typeConstructor, upperBound) ->
            val isContainedOrNoUpperBound =
                upperBound == null || isContainedInInvariantOrContravariantPositions(currentTypeParameterConstructor, upperBound)
            currentTypeParameterConstructor = typeConstructor
            isContainedOrNoUpperBound
        }
    }

    private fun NewConstraintSystem.getTypeParameterByVariable(typeConstructor: TypeConstructorMarker) =
        (getBuilder().currentStorage().allTypeVariables[typeConstructor] as? TypeVariableFromCallableDescriptor)?.originalTypeParameter?.typeConstructor

    private fun NewConstraintSystem.getDependingOnTypeParameter(variable: TypeConstructor) =
        getBuilder().currentStorage().notFixedTypeVariables[variable]?.constraints?.mapNotNull {
            if (it.position.from is DeclaredUpperBoundConstraintPosition && it.kind == ConstraintKind.UPPER) {
                it.type.typeConstructor(asConstraintSystemCompleterContext())
            } else null
        } ?: emptyList()

    private fun NewConstraintSystem.isContainedInInvariantOrContravariantPositionsWithDependencies(
        variable: TypeVariableFromCallableDescriptor,
        declarationDescriptor: DeclarationDescriptor?
    ): Boolean {
        if (declarationDescriptor !is CallableDescriptor) return false

        val returnType = declarationDescriptor.returnType ?: return false

        if (!returnType.isComputed) return false

        val typeVariableConstructor = variable.freshTypeConstructor
        val dependentTypeParameters = getDependentTypeParameters(typeVariableConstructor)
        val dependingOnTypeParameter = getDependingOnTypeParameter(typeVariableConstructor)

        val isContainedInUpperBounds =
            isContainedInInvariantOrContravariantPositionsAmongUpperBound(typeVariableConstructor, dependentTypeParameters)
        val isContainedAnyDependentTypeInReturnType = dependentTypeParameters.any { (typeParameter, _) ->
            returnType.contains {
                it.typeConstructor(asConstraintSystemCompleterContext()) == getTypeParameterByVariable(typeParameter) && !it.isMarkedNullable
            }
        }

        return isContainedInInvariantOrContravariantPositions(typeVariableConstructor, returnType)
                || dependingOnTypeParameter.any { isContainedInInvariantOrContravariantPositions(it, returnType) }
                || dependentTypeParameters.any { isContainedInInvariantOrContravariantPositions(it.first, returnType) }
                || (isContainedAnyDependentTypeInReturnType && isContainedInUpperBounds)
    }

    private fun TypeVariableFromCallableDescriptor.recordInfoAboutTypeVariableUsagesAsInvariantOrContravariantParameter() {
        freshTypeConstructor.isContainedInInvariantOrContravariantPositions = true
    }

    override fun KotlinResolutionCandidate.process(workIndex: Int) {
        for (variable in resolvedCall.freshVariablesSubstitutor.freshVariables) {
            if (resolvedCall.candidateDescriptor is ClassConstructorDescriptor) {
                val typeParameters = resolvedCall.candidateDescriptor.containingDeclaration.declaredTypeParameters

                if (isContainedInInvariantOrContravariantPositionsAmongTypeParameters(variable, typeParameters)) {
                    variable.recordInfoAboutTypeVariableUsagesAsInvariantOrContravariantParameter()
                }
            } else if (getSystem().isContainedInInvariantOrContravariantPositionsWithDependencies(variable, candidateDescriptor)) {
                variable.recordInfoAboutTypeVariableUsagesAsInvariantOrContravariantParameter()
            }
        }
    }
}

private fun KotlinResolutionCandidate.resolveKotlinArgument(
    argument: KotlinCallArgument,
    candidateParameter: ParameterDescriptor?,
    receiverInfo: ReceiverInfo
) {
    val candidateExpectedType = candidateParameter?.let { argument.getExpectedType(it, callComponents.languageVersionSettings) }

    val isReceiver = receiverInfo.isReceiver
    val conversionDataBeforeSubtyping =
        if (isReceiver || candidateParameter == null || candidateExpectedType == null) {
            null
        } else {
            TypeConversions.performCompositeConversionBeforeSubtyping(
                this, argument, candidateParameter, candidateExpectedType
            )
        }

    val convertedExpectedType = conversionDataBeforeSubtyping?.convertedType
    val unsubstitutedExpectedType = conversionDataBeforeSubtyping?.convertedType ?: candidateExpectedType
    val expectedType = unsubstitutedExpectedType?.let { prepareExpectedType(it) }

    val convertedArgument = if (expectedType != null && !isReceiver && shouldRunConversionForConstants(expectedType)) {
        val convertedConstant = resolutionCallbacks.convertSignedConstantToUnsigned(argument)
        if (convertedConstant != null) {
            resolvedCall.registerArgumentWithConstantConversion(argument, convertedConstant)
        }

        convertedConstant
    } else null


    val inferenceSession = resolutionCallbacks.inferenceSession
    if (candidateExpectedType == null || // Nothing to convert
        convertedExpectedType != null || // Type is already converted
        isReceiver || // Receivers don't participate in conversions
        conversionDataBeforeSubtyping?.wasConversion == true || // We tried to convert type but failed
        conversionDataBeforeSubtyping?.conversionDefinitelyNotNeeded == true ||
        csBuilder.hasContradiction
    ) {
        val resolvedAtom = resolveKtPrimitive(
            csBuilder,
            argument,
            expectedType,
            this,
            receiverInfo,
            convertedArgument?.unknownIntegerType?.unwrap(),
            inferenceSession
        )

        addResolvedKtPrimitive(resolvedAtom)
    } else {
        var convertedTypeAfterSubtyping: UnwrappedType? = null
        csBuilder.runTransaction {
            val resolvedAtom = resolveKtPrimitive(
                csBuilder,
                argument,
                expectedType,
                this@resolveKotlinArgument,
                receiverInfo,
                convertedArgument?.unknownIntegerType?.unwrap(),
                inferenceSession
            )

            if (!hasContradiction) {
                addResolvedKtPrimitive(resolvedAtom)
                return@runTransaction true
            }

            convertedTypeAfterSubtyping =
                TypeConversions.performCompositeConversionAfterSubtyping(
                    this@resolveKotlinArgument,
                    argument,
                    candidateParameter,
                    candidateExpectedType
                )?.let { prepareExpectedType(it) }

            if (convertedTypeAfterSubtyping == null) {
                addResolvedKtPrimitive(resolvedAtom)
                return@runTransaction true
            }

            false
        }

        if (convertedTypeAfterSubtyping != null) {
            val resolvedAtom = resolveKtPrimitive(
                csBuilder,
                argument,
                convertedTypeAfterSubtyping,
                this@resolveKotlinArgument,
                receiverInfo,
                convertedArgument?.unknownIntegerType?.unwrap(),
                inferenceSession
            )
            addResolvedKtPrimitive(resolvedAtom)
        }

    }
}

private fun KotlinResolutionCandidate.shouldRunConversionForConstants(expectedType: UnwrappedType): Boolean {
    if (UnsignedTypes.isUnsignedType(expectedType)) return true
    if (csBuilder.isTypeVariable(expectedType)) {
        val variableWithConstraints = csBuilder.currentStorage().notFixedTypeVariables[expectedType.constructor] ?: return false
        return variableWithConstraints.constraints.any {
            it.kind == ConstraintKind.EQUALITY &&
                    it.position.from is ExplicitTypeParameterConstraintPosition &&
                    UnsignedTypes.isUnsignedType(it.type as UnwrappedType)

        }
    }

    return false
}

internal enum class ImplicitInvokeCheckStatus {
    NO_INVOKE, INVOKE_ON_NOT_NULL_VARIABLE, UNSAFE_INVOKE_REPORTED
}

private fun KotlinResolutionCandidate.checkUnsafeImplicitInvokeAfterSafeCall(argument: SimpleKotlinCallArgument): ImplicitInvokeCheckStatus {
    val variableForInvoke = variableCandidateIfInvoke ?: return ImplicitInvokeCheckStatus.NO_INVOKE

    val receiverArgument = with(variableForInvoke.resolvedCall) {
        when (explicitReceiverKind) {
            DISPATCH_RECEIVER -> dispatchReceiverArgument
            EXTENSION_RECEIVER,
            BOTH_RECEIVERS -> extensionReceiverArgument
            NO_EXPLICIT_RECEIVER -> return ImplicitInvokeCheckStatus.INVOKE_ON_NOT_NULL_VARIABLE
        }
    } ?: error("Receiver kind does not match receiver argument")

    if (receiverArgument.isSafeCall && receiverArgument.receiver.stableType.isNullable() && resolvedCall.candidateDescriptor.typeParameters.isEmpty()) {
        addDiagnostic(UnsafeCallError(argument, isForImplicitInvoke = true))
        return ImplicitInvokeCheckStatus.UNSAFE_INVOKE_REPORTED
    }

    return ImplicitInvokeCheckStatus.INVOKE_ON_NOT_NULL_VARIABLE
}

private fun KotlinResolutionCandidate.prepareExpectedType(expectedType: UnwrappedType): UnwrappedType {
    val resultType = knownTypeParametersResultingSubstitutor?.substitute(expectedType) ?: expectedType
    return resolvedCall.freshVariablesSubstitutor.safeSubstitute(resultType)
}

internal object CheckReceivers : ResolutionPart() {
    private fun KotlinResolutionCandidate.checkReceiver(
        receiverArgument: SimpleKotlinCallArgument?,
        receiverParameter: ReceiverParameterDescriptor?,
        shouldCheckImplicitInvoke: Boolean,
    ) {
        if ((receiverArgument == null) != (receiverParameter == null)) {
            error("Inconsistency receiver state for call $kotlinCall and candidate descriptor: $candidateDescriptor")
        }
        if (receiverArgument == null || receiverParameter == null) return

        val implicitInvokeState = if (shouldCheckImplicitInvoke) {
            checkUnsafeImplicitInvokeAfterSafeCall(receiverArgument)
        } else ImplicitInvokeCheckStatus.NO_INVOKE

        val receiverInfo = ReceiverInfo(
            isReceiver = true,
            shouldReportUnsafeCall = implicitInvokeState != ImplicitInvokeCheckStatus.UNSAFE_INVOKE_REPORTED,
            reportUnsafeCallAsUnsafeImplicitInvoke = implicitInvokeState == ImplicitInvokeCheckStatus.INVOKE_ON_NOT_NULL_VARIABLE
        )

        resolveKotlinArgument(receiverArgument, receiverParameter, receiverInfo)
    }

    override fun KotlinResolutionCandidate.process(workIndex: Int) {
        if (workIndex == 0) {
            checkReceiver(
                resolvedCall.dispatchReceiverArgument,
                candidateDescriptor.dispatchReceiverParameter,
                shouldCheckImplicitInvoke = true,
            )
        } else {
            checkReceiver(
                resolvedCall.extensionReceiverArgument,
                candidateDescriptor.extensionReceiverParameter,
                shouldCheckImplicitInvoke = false, // reproduce old inference behaviour
            )
        }
    }

    override fun KotlinResolutionCandidate.workCount() = 2
}

internal object CheckArgumentsInParenthesis : ResolutionPart() {
    override fun KotlinResolutionCandidate.process(workIndex: Int) {
        val argument = kotlinCall.argumentsInParenthesis[workIndex]
        resolveKotlinArgument(argument, resolvedCall.argumentToCandidateParameter[argument], ReceiverInfo.notReceiver)
    }

    override fun KotlinResolutionCandidate.workCount() = kotlinCall.argumentsInParenthesis.size
}

internal object CheckExternalArgument : ResolutionPart() {
    override fun KotlinResolutionCandidate.process(workIndex: Int) {
        val argument = kotlinCall.externalArgument ?: return

        resolveKotlinArgument(argument, resolvedCall.argumentToCandidateParameter[argument], ReceiverInfo.notReceiver)
    }
}

internal object EagerResolveOfCallableReferences : ResolutionPart() {
    override fun KotlinResolutionCandidate.process(workIndex: Int) {
        getSubResolvedAtoms()
            .filterIsInstance<EagerCallableReferenceAtom>()
            .forEach {
                callableReferenceResolver.processCallableReferenceArgument(csBuilder, it, this)
            }
    }
}

internal object CheckInfixResolutionPart : ResolutionPart() {
    override fun KotlinResolutionCandidate.process(workIndex: Int) {
        val candidateDescriptor = resolvedCall.candidateDescriptor
        if (candidateDescriptor !is FunctionDescriptor) return
        if (!candidateDescriptor.isInfix && callComponents.statelessCallbacks.isInfixCall(kotlinCall)) {
            addDiagnostic(InfixCallNoInfixModifier)
        }
    }
}

internal object CheckOperatorResolutionPart : ResolutionPart() {
    override fun KotlinResolutionCandidate.process(workIndex: Int) {
        val candidateDescriptor = resolvedCall.candidateDescriptor
        if (candidateDescriptor !is FunctionDescriptor) return
        if (!candidateDescriptor.isOperator && callComponents.statelessCallbacks.isOperatorCall(kotlinCall)) {
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
        resolvedCall.freshVariablesSubstitutor = FreshVariableNewTypeSubstitutor.Empty
        resolvedCall.knownParametersSubstitutor = TypeSubstitutor.EMPTY
        resolvedCall.argumentToCandidateParameter = emptyMap()

        kotlinCall.explicitReceiver?.safeAs<SimpleKotlinCallArgument>()?.let {
            resolveKotlinArgument(it, null, ReceiverInfo.notReceiver)
        }
        for (argument in kotlinCall.argumentsInParenthesis) {
            resolveKotlinArgument(argument, null, ReceiverInfo.notReceiver)
        }

        kotlinCall.externalArgument?.let {
            resolveKotlinArgument(it, null, ReceiverInfo.notReceiver)
        }
    }
}