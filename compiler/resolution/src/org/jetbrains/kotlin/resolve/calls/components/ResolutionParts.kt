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
import org.jetbrains.kotlin.resolve.calls.tower.ResolutionCandidateApplicability
import org.jetbrains.kotlin.resolve.calls.tower.ResolutionCandidateApplicability.INAPPLICABLE
import org.jetbrains.kotlin.resolve.calls.tower.ResolutionCandidateApplicability.RUNTIME_ERROR
import org.jetbrains.kotlin.resolve.calls.tower.VisibilityError
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.Variance

internal object CheckInstantiationOfAbstractClass : ResolutionPart {
    override fun SimpleKotlinResolutionCandidate.process(): List<KotlinCallDiagnostic> {
        if (candidateDescriptor is ConstructorDescriptor && !kotlinCall.isSuperOrDelegatingConstructorCall) {
            if (candidateDescriptor.constructedClass.modality == Modality.ABSTRACT) {
                return listOf(InstantiationOfAbstractClass)
            }
        }

        return emptyList()
    }
}

internal object CheckVisibility : ResolutionPart {
    override fun SimpleKotlinResolutionCandidate.process(): List<KotlinCallDiagnostic> {
        val receiverValue = dispatchReceiverArgument?.receiver?.receiverValue ?: Visibilities.ALWAYS_SUITABLE_RECEIVER
        val invisibleMember = Visibilities.findInvisibleMember(receiverValue, candidateDescriptor, containingDescriptor) ?: return emptyList()

        if (dispatchReceiverArgument is ExpressionKotlinCallArgument) {
            val smartCastReceiver = getReceiverValueWithSmartCast(receiverValue, dispatchReceiverArgument.stableType)
            if (Visibilities.findInvisibleMember(smartCastReceiver, candidateDescriptor, containingDescriptor) == null) {
                return listOf(SmartCastDiagnostic(dispatchReceiverArgument, dispatchReceiverArgument.stableType))
            }
        }

        return listOf(VisibilityError(invisibleMember))
    }

    private val SimpleKotlinResolutionCandidate.containingDescriptor: DeclarationDescriptor get() = callContext.scopeTower.lexicalScope.ownerDescriptor
}

internal object MapTypeArguments : ResolutionPart {
    override fun SimpleKotlinResolutionCandidate.process(): List<KotlinCallDiagnostic> {
        typeArgumentMappingByOriginal = callContext.typeArgumentsToParametersMapper.mapTypeArguments(kotlinCall, candidateDescriptor.original)
        return typeArgumentMappingByOriginal.diagnostics
    }
}

internal object NoTypeArguments : ResolutionPart {
    override fun SimpleKotlinResolutionCandidate.process(): List<KotlinCallDiagnostic> {
        assert(kotlinCall.typeArguments.isEmpty()) {
            "Variable call cannot has explicit type arguments: ${kotlinCall.typeArguments}. Call: $kotlinCall"
        }
        typeArgumentMappingByOriginal = NoExplicitArguments
        return typeArgumentMappingByOriginal.diagnostics
    }
}

internal object MapArguments : ResolutionPart {
    override fun SimpleKotlinResolutionCandidate.process(): List<KotlinCallDiagnostic> {
        val mapping = callContext.argumentsToParametersMapper.mapArguments(kotlinCall, candidateDescriptor.original)
        argumentMappingByOriginal = mapping.parameterToCallArgumentMap
        return mapping.diagnostics
    }
}

internal object NoArguments : ResolutionPart {
    override fun SimpleKotlinResolutionCandidate.process(): List<KotlinCallDiagnostic> {
        assert(kotlinCall.argumentsInParenthesis.isEmpty()) {
            "Variable call cannot has arguments: ${kotlinCall.argumentsInParenthesis}. Call: $kotlinCall"
        }
        assert(kotlinCall.externalArgument == null) {
            "Variable call cannot has external argument: ${kotlinCall.externalArgument}. Call: $kotlinCall"
        }
        argumentMappingByOriginal = emptyMap()
        return emptyList()
    }
}

internal object CreateDescriptorWithFreshTypeVariables : ResolutionPart {
    override fun SimpleKotlinResolutionCandidate.process(): List<KotlinCallDiagnostic> {
        if (candidateDescriptor.typeParameters.isEmpty()) {
            descriptorWithFreshTypes = candidateDescriptor
            return emptyList()
        }
        val toFreshVariables = createToFreshVariableSubstitutorAndAddInitialConstraints(candidateDescriptor, csBuilder, kotlinCall)
        typeVariablesForFreshTypeParameters = toFreshVariables.freshVariables

        // bad function -- error on declaration side
        if (csBuilder.hasContradiction) {
            descriptorWithFreshTypes = candidateDescriptor
            return emptyList()
        }

        // optimization
        if (typeArgumentMappingByOriginal == NoExplicitArguments && knownTypeParametersResultingSubstitutor == null) {
            descriptorWithFreshTypes = candidateDescriptor.substitute(toFreshVariables)
            csBuilder.simplify().let { assert(it.isEmpty) { "Substitutor should be empty: $it, call: $kotlinCall" } }
            return emptyList()
        }

        val typeParameters = candidateDescriptor.typeParameters
        for (index in typeParameters.indices) {
            val typeParameter = typeParameters[index]
            val knownTypeArgument = knownTypeParametersResultingSubstitutor?.substitute(typeParameter.defaultType)
            if (knownTypeArgument != null) {
                val freshVariable = toFreshVariables.freshVariables[index]
                csBuilder.addEqualityConstraint(freshVariable.defaultType, knownTypeArgument.unwrap(), KnownTypeParameterConstraintPosition(knownTypeArgument))
                continue
            }

            val typeArgument = typeArgumentMappingByOriginal.getTypeArgument(typeParameter)

            if (typeArgument is SimpleTypeArgument) {
                val freshVariable = toFreshVariables.freshVariables[index]
                csBuilder.addEqualityConstraint(freshVariable.defaultType, typeArgument.type, ExplicitTypeParameterConstraintPosition(typeArgument))
            }
            else {
                assert(typeArgument == TypeArgumentPlaceholder) {
                    "Unexpected typeArgument: $typeArgument, ${typeArgument.javaClass.canonicalName}"
                }
            }
        }

        /**
         * Note: here we can fix also placeholders arguments.
         * Example:
         *  fun <X : Array<Y>, Y> foo()
         *
         *  foo<Array<String>, *>()
         */
        val toFixedTypeParameters = csBuilder.simplify()
        // todo optimize -- composite substitutions before run safeSubstitute
        descriptorWithFreshTypes = candidateDescriptor.substitute(toFreshVariables).substitute(toFixedTypeParameters)

        return emptyList()
    }

    fun createToFreshVariableSubstitutorAndAddInitialConstraints(
            candidateDescriptor: CallableDescriptor,
            csBuilder: ConstraintSystemOperation,
            kotlinCall: KotlinCall?
    ): FreshVariableNewTypeSubstitutor {
        val typeParameters = candidateDescriptor.typeParameters

        val freshTypeVariables = typeParameters.map { TypeVariableFromCallableDescriptor(it, kotlinCall) }

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

internal object CheckExplicitReceiverKindConsistency : ResolutionPart {
    private fun SimpleKotlinResolutionCandidate.hasError(): Nothing =
            error("Inconsistent call: $kotlinCall. \n" +
                  "Candidate: $candidateDescriptor, explicitReceiverKind: $explicitReceiverKind.\n" +
                  "Explicit receiver: ${kotlinCall.explicitReceiver}, dispatchReceiverForInvokeExtension: ${kotlinCall.dispatchReceiverForInvokeExtension}")

    override fun SimpleKotlinResolutionCandidate.process(): List<KotlinCallDiagnostic> {
        when (explicitReceiverKind) {
            NO_EXPLICIT_RECEIVER -> if (kotlinCall.explicitReceiver is SimpleKotlinCallArgument || kotlinCall.dispatchReceiverForInvokeExtension != null) hasError()
            DISPATCH_RECEIVER, EXTENSION_RECEIVER -> if (kotlinCall.explicitReceiver == null || kotlinCall.dispatchReceiverForInvokeExtension != null) hasError()
            BOTH_RECEIVERS -> if (kotlinCall.explicitReceiver == null || kotlinCall.dispatchReceiverForInvokeExtension == null) hasError()
        }
        return emptyList()
    }
}

internal object CheckReceivers : ResolutionPart {
    private fun SimpleKotlinResolutionCandidate.checkReceiver(
            receiverArgument: SimpleKotlinCallArgument?,
            receiverParameter: ReceiverParameterDescriptor?
    ): KotlinCallDiagnostic? {
        if ((receiverArgument == null) != (receiverParameter == null)) {
            error("Inconsistency receiver state for call $kotlinCall and candidate descriptor: $candidateDescriptor")
        }
        if (receiverArgument == null || receiverParameter == null) return null

        val expectedType = receiverParameter.type.unwrap()

        return when (receiverArgument) {
            is ExpressionKotlinCallArgument -> checkExpressionArgument(csBuilder, receiverArgument, expectedType, isReceiver = true)
            is SubKotlinCallArgument -> checkSubCallArgument(csBuilder, receiverArgument, expectedType, isReceiver = true)
            else -> incorrectReceiver(receiverArgument)
        }
    }

    private fun incorrectReceiver(callReceiver: SimpleKotlinCallArgument): Nothing =
            error("Incorrect receiver type: $callReceiver. Class name: ${callReceiver.javaClass.canonicalName}")

    override fun SimpleKotlinResolutionCandidate.process() =
            listOfNotNull(checkReceiver(dispatchReceiverArgument, descriptorWithFreshTypes.dispatchReceiverParameter),
                          checkReceiver(extensionReceiver, descriptorWithFreshTypes.extensionReceiverParameter))
}


fun <D : CallableDescriptor> D.safeSubstitute(substitutor: TypeSubstitutor): D =
        @Suppress("UNCHECKED_CAST") (substitute(substitutor) as D)

fun UnwrappedType.substitute(substitutor: TypeSubstitutor): UnwrappedType = substitutor.substitute(this, Variance.INVARIANT)!!.unwrap()


object InstantiationOfAbstractClass : KotlinCallDiagnostic(RUNTIME_ERROR) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCall(this)
}

class UnstableSmartCast(val expressionArgument: ExpressionKotlinCallArgument, val targetType: UnwrappedType) :
        KotlinCallDiagnostic(ResolutionCandidateApplicability.MAY_THROW_RUNTIME_ERROR) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(expressionArgument, this)
}

class UnsafeCallError(val receiver: SimpleKotlinCallArgument) : KotlinCallDiagnostic(ResolutionCandidateApplicability.MAY_THROW_RUNTIME_ERROR) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallReceiver(receiver, this)
}

class ExpectedLambdaParametersCountMismatch(
        val lambdaArgument: LambdaKotlinCallArgument,
        val expected: Int,
        val actual: Int
) : KotlinCallDiagnostic(INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(lambdaArgument, this)
}

class UnexpectedReceiver(val functionExpression: FunctionExpression) : KotlinCallDiagnostic(INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(functionExpression, this)
}

class MissingReceiver(val functionExpression: FunctionExpression) : KotlinCallDiagnostic(INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(functionExpression, this)
}

class NoneCallableReferenceCandidates(val argument: CallableReferenceKotlinCallArgument) : KotlinCallDiagnostic(INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(argument, this)
}

class CallableReferenceCandidatesAmbiguity(
        val argument: CallableReferenceKotlinCallArgument,
        val candidates: Collection<CallableReferenceCandidate>
) : KotlinCallDiagnostic(INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(argument, this)
}

class NotCallableExpectedType(
        val argument: CallableReferenceKotlinCallArgument,
        val expectedType: UnwrappedType,
        val notCallableTypeConstructor: TypeConstructor
) : KotlinCallDiagnostic(INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(argument, this)
}