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

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.calls.components.TypeArgumentsToParametersMapper.TypeArgumentsMapping.NoExplicitArguments
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemOperation
import org.jetbrains.kotlin.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.model.DeclaredUpperBoundConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.ExplicitTypeParameterConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.KnownTypeParameterConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.TypeVariableFromCallableDescriptor
import org.jetbrains.kotlin.resolve.calls.inference.substitute
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.getReceiverValueWithSmartCast
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind.*
import org.jetbrains.kotlin.resolve.calls.tower.InfixCallNoInfixModifier
import org.jetbrains.kotlin.resolve.calls.tower.InvokeConventionCallNoOperatorModifier
import org.jetbrains.kotlin.resolve.calls.tower.VisibilityError
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal object CheckInstantiationOfAbstractClass : ResolutionPart() {
    override fun KotlinResolutionCandidate.process(workIndex: Int) {
        val candidateDescriptor = resolvedCall.candidateDescriptor

        if (candidateDescriptor is ConstructorDescriptor &&
            !callComponents.statelessCallbacks.isSuperOrDelegatingConstructorCall(resolvedCall.atom)) {
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
        val invisibleMember = Visibilities.findInvisibleMember(receiverValue, resolvedCall.candidateDescriptor, containingDescriptor) ?: return

        if (dispatchReceiverArgument is ExpressionKotlinCallArgument) {
            val smartCastReceiver = getReceiverValueWithSmartCast(receiverValue, dispatchReceiverArgument.receiver.stableType)
            if (Visibilities.findInvisibleMember(smartCastReceiver, candidateDescriptor, containingDescriptor) == null) {
                addDiagnostic(SmartCastDiagnostic(dispatchReceiverArgument, dispatchReceiverArgument.receiver.stableType, resolvedCall.atom))
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

        val typeParameters = candidateDescriptor.typeParameters
        for (index in typeParameters.indices) {
            val typeParameter = typeParameters[index]
            val freshVariable = toFreshVariables.freshVariables[index]

            val knownTypeArgument = knownTypeParametersResultingSubstitutor?.substitute(typeParameter.defaultType)
            if (knownTypeArgument != null) {
                csBuilder.addEqualityConstraint(freshVariable.defaultType, knownTypeArgument.unwrap(), KnownTypeParameterConstraintPosition(knownTypeArgument))
                continue
            }

            val typeArgument = resolvedCall.typeArgumentMappingByOriginal.getTypeArgument(typeParameter)

            if (typeArgument is SimpleTypeArgument) {
                csBuilder.addEqualityConstraint(freshVariable.defaultType, typeArgument.type, ExplicitTypeParameterConstraintPosition(typeArgument))
            }
            else {
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

        for (index in typeParameters.indices) {
            val typeParameter = typeParameters[index]
            val freshVariable = freshTypeVariables[index]
            val position = DeclaredUpperBoundConstraintPosition(typeParameter)

            for (upperBound in typeParameter.upperBounds) {
                csBuilder.addSubtypeConstraint(freshVariable.defaultType, toFreshVariables.safeSubstitute(upperBound.unwrap()), position)
            }
        }
        return toFreshVariables
    }
}

internal object CheckExplicitReceiverKindConsistency : ResolutionPart() {
    private fun KotlinResolutionCandidate.hasError(): Nothing =
            error("Inconsistent call: $kotlinCall. \n" +
                  "Candidate: $candidateDescriptor, explicitReceiverKind: ${resolvedCall.explicitReceiverKind}.\n" +
                  "Explicit receiver: ${kotlinCall.explicitReceiver}, dispatchReceiverForInvokeExtension: ${kotlinCall.dispatchReceiverForInvokeExtension}")

    override fun KotlinResolutionCandidate.process(workIndex: Int) {
        when (resolvedCall.explicitReceiverKind) {
            NO_EXPLICIT_RECEIVER -> if (kotlinCall.explicitReceiver is SimpleKotlinCallArgument || kotlinCall.dispatchReceiverForInvokeExtension != null) hasError()
            DISPATCH_RECEIVER, EXTENSION_RECEIVER -> if (kotlinCall.explicitReceiver == null || kotlinCall.dispatchReceiverForInvokeExtension != null) hasError()
            BOTH_RECEIVERS -> if (kotlinCall.explicitReceiver == null || kotlinCall.dispatchReceiverForInvokeExtension == null) hasError()
        }
    }
}

private fun KotlinResolutionCandidate.resolveKotlinArgument(
        argument: KotlinCallArgument,
        candidateParameter: ParameterDescriptor?,
        isReceiver: Boolean
) {
    val expectedType = candidateParameter?.let {
        resolvedCall.substitutor.substituteKeepAnnotations(argument.getExpectedType(candidateParameter))
    }
    addResolvedKtPrimitive(resolveKtPrimitive(csBuilder, argument, expectedType, this, isReceiver))
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

internal object CheckArguments : ResolutionPart() {
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
            (candidateDescriptor !is FunctionDescriptor || !candidateDescriptor.isInfix)) {
            addDiagnostic(InfixCallNoInfixModifier)
        }
    }
}

internal object CheckOperatorResolutionPart : ResolutionPart() {
    override fun KotlinResolutionCandidate.process(workIndex: Int) {
        val candidateDescriptor = resolvedCall.candidateDescriptor
        if (callComponents.statelessCallbacks.isOperatorCall(kotlinCall) &&
            (candidateDescriptor !is FunctionDescriptor || !candidateDescriptor.isOperator)) {
            addDiagnostic(InvokeConventionCallNoOperatorModifier)
        }
    }
}

internal object CheckAbstractSuperCallPart : ResolutionPart() {
    override fun KotlinResolutionCandidate.process(workIndex: Int) {
        val candidateDescriptor = resolvedCall.candidateDescriptor

        if (callComponents.statelessCallbacks.isSuperExpression(resolvedCall.dispatchReceiverArgument)) {
            if (candidateDescriptor is MemberDescriptor && candidateDescriptor.modality == Modality.ABSTRACT) {
                addDiagnostic(AbstractSuperCall)
            }
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