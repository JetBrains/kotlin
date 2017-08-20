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

import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemOperation
import org.jetbrains.kotlin.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.model.ArgumentConstraintPosition
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind.DISPATCH_RECEIVER
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind.EXTENSION_RECEIVER
import org.jetbrains.kotlin.resolve.calls.tower.*
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.receivers.DetailedReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.QualifierReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.expressions.CoercionStrategy
import org.jetbrains.kotlin.types.typeUtil.immediateSupertypes
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.types.upperIfFlexible
import org.jetbrains.kotlin.utils.SmartList

sealed class CallableReceiver(val receiver: ReceiverValueWithSmartCastInfo) {
    class UnboundReference(val qualifier: QualifierReceiver, receiver: ReceiverValueWithSmartCastInfo) : CallableReceiver(receiver)
    class BoundValueReference(val qualifier: QualifierReceiver, receiver: ReceiverValueWithSmartCastInfo) : CallableReceiver(receiver)
    class ScopeReceiver(receiver: ReceiverValueWithSmartCastInfo) : CallableReceiver(receiver)
    class ExplicitValueReceiver(val lhsArgument: SimpleKotlinCallArgument, receiver: ReceiverValueWithSmartCastInfo) : CallableReceiver(receiver)
}

private val CallableReceiver.asReceiverValueForVisibilityChecks: ReceiverValue
    get() = receiver.receiverValue

/**
 * Suppose we have class A with staticM, memberM, memberExtM.
 * For A::staticM both receivers will be null
 * For A::memberM dispatchReceiver = UnboundReceiver, extensionReceiver = null
 * For a::memberExtM dispatchReceiver = ExplicitValueReceiver, extensionReceiver = ExplicitValueReceiver
 *
 * For class B with companion object B::companionM dispatchReceiver = BoundValueReference
 */
class CallableReferenceCandidate(
        val candidate: CallableDescriptor,
        val dispatchReceiver: CallableReceiver?,
        val extensionReceiver: CallableReceiver?,
        val explicitReceiverKind: ExplicitReceiverKind,
        val reflectionCandidateType: UnwrappedType,
        val numDefaults: Int,
        override val status: ResolutionCandidateStatus
) : Candidate {
    override val isSuccessful get() = status.resultingApplicability.isSuccess
}

/**
 * cases: class A {}, class B { companion object }, object C, enum class D { E }
 * A::foo <-> Type
 * a::foo <-> Expression
 * B::foo <-> Type
 * C::foo <-> Object
 * D.E::foo <-> Expression
 */
fun createCallableReferenceProcessor(factory: CallableReferencesCandidateFactory): ScopeTowerProcessor<CallableReferenceCandidate> {
    val lhsResult = factory.argument.lhsResult
    when (lhsResult) {
        LHSResult.Empty, is LHSResult.Expression -> {
            val explicitReceiver = (lhsResult as? LHSResult.Expression)?.lshCallArgument?.receiver
            return factory.createCallableProcessor(explicitReceiver)
        }
        is LHSResult.Type -> {
            val static = factory.createCallableProcessor(lhsResult.qualifier)
            val unbound = factory.createCallableProcessor(lhsResult.unboundDetailedReceiver)

            // note that if we use CompositeScopeTowerProcessor then static will win over unbound members
            val staticOrUnbound = CompositeSimpleScopeTowerProcessor(static, unbound)

            val asValue = lhsResult.qualifier.classValueReceiverWithSmartCastInfo ?: return staticOrUnbound
            return CompositeScopeTowerProcessor(staticOrUnbound, factory.createCallableProcessor(asValue))
        }
        is LHSResult.Object -> {
            // callable reference to nested class constructor
            val static = factory.createCallableProcessor(lhsResult.qualifier)
            val boundObjectReference = factory.createCallableProcessor(lhsResult.objectValueReceiver)

            return CompositeSimpleScopeTowerProcessor(static, boundObjectReference)
        }
    }
}

class CallableReferencesCandidateFactory(
        val argument: CallableReferenceKotlinCallArgument,
        val outerCallContext: KotlinCallContext,
        val compatibilityChecker: ((ConstraintSystemOperation) -> Unit) -> Unit,
        val expectedType: UnwrappedType?
) : CandidateFactory<CallableReferenceCandidate> {
    private val position = ArgumentConstraintPosition(argument)

    fun createCallableProcessor(explicitReceiver: DetailedReceiver?) =
            createCallableReferenceProcessor(outerCallContext.scopeTower, argument.rhsName, this, explicitReceiver)

    override fun createCandidate(
            towerCandidate: CandidateWithBoundDispatchReceiver,
            explicitReceiverKind: ExplicitReceiverKind,
            extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): CallableReferenceCandidate {

        val dispatchCallableReceiver = towerCandidate.dispatchReceiver?.let { toCallableReceiver(it, explicitReceiverKind == DISPATCH_RECEIVER) }
        val extensionCallableReceiver = extensionReceiver?.let { toCallableReceiver(it, explicitReceiverKind == EXTENSION_RECEIVER) }
        val candidateDescriptor = towerCandidate.descriptor

        val (reflectionCandidateType, defaults) = buildReflectionType(candidateDescriptor,
                                                          dispatchCallableReceiver,
                                                          extensionCallableReceiver,
                                                          expectedType)

        if (candidateDescriptor !is CallableMemberDescriptor) {
            val status = ResolutionCandidateStatus(listOf(NotCallableMemberReference(argument, candidateDescriptor)))
            return CallableReferenceCandidate(candidateDescriptor, dispatchCallableReceiver, extensionCallableReceiver,
                                              explicitReceiverKind, reflectionCandidateType, defaults, status)
        }

        val diagnostics = SmartList<KotlinCallDiagnostic>()
        diagnostics.addAll(towerCandidate.diagnostics)
        val invisibleMember = Visibilities.findInvisibleMember(dispatchCallableReceiver?.asReceiverValueForVisibilityChecks,
                                                               candidateDescriptor, outerCallContext.scopeTower.lexicalScope.ownerDescriptor)
        if (invisibleMember != null) {
            diagnostics.add(VisibilityError(invisibleMember))
        }
        // todo smartcast on receiver diagnostic and CheckInstantiationOfAbstractClass

        compatibilityChecker {
            if (it.hasContradiction || expectedType == null) return@compatibilityChecker

            val toFreshSubstitutor = CreateDescriptorWithFreshTypeVariables.createToFreshVariableSubstitutorAndAddInitialConstraints(candidateDescriptor, it, kotlinCall = null)
            val reflectionType = toFreshSubstitutor.safeSubstitute(reflectionCandidateType)
            it.addSubtypeConstraint(reflectionType, expectedType, position)

            it.addUnboundConstraint(toFreshSubstitutor, dispatchCallableReceiver, candidateDescriptor.dispatchReceiverParameter)
            it.addUnboundConstraint(toFreshSubstitutor, extensionCallableReceiver, candidateDescriptor.extensionReceiverParameter)

            if (it.hasContradiction) diagnostics.add(CallableReferenceNotCompatible(argument, candidateDescriptor, expectedType, reflectionType))
        }

        return CallableReferenceCandidate(candidateDescriptor, dispatchCallableReceiver, extensionCallableReceiver,
                                          explicitReceiverKind, reflectionCandidateType, defaults, ResolutionCandidateStatus(diagnostics))
    }

    private fun ConstraintSystemOperation.addUnboundConstraint(
            toFreshSubstitutor: FreshVariableNewTypeSubstitutor,
            receiver: CallableReceiver?,
            candidateReceiver: ReceiverParameterDescriptor?
    ) {
        val expectedType = toFreshSubstitutor.safeSubstitute(candidateReceiver?.value?.type?.unwrap() ?: return)
        val receiverType = receiver?.receiver?.stableType ?: return
        addSubtypeConstraint(receiverType, expectedType, position)
    }

    private fun getArgumentAndReturnTypeUseMappingByExpectedType(
            descriptor: FunctionDescriptor,
            expectedType: UnwrappedType?,
            unboundReceiverCount: Int
    ): Triple<Array<KotlinType>, CoercionStrategy, Int>? {
        if (expectedType == null) return null

        val functionType =
                if (expectedType.isFunctionType) {
                    expectedType
                }
                else if (ReflectionTypes.isNumberedKFunction(expectedType)) {
                    expectedType.immediateSupertypes().first { it.isFunctionType }
                }
                else {
                    return null
                }


        val expectedArgumentCount = functionType.arguments.size - unboundReceiverCount - 1 // 1 -- return type
        if (expectedArgumentCount < 0) return null

        val fakeArguments = (0..(expectedArgumentCount - 1)).map { FakeKotlinCallArgumentForCallableReference(it) }
        val argumentMapping = outerCallContext.argumentsToParametersMapper.mapArguments(fakeArguments, externalArgument = null, descriptor = descriptor)
        if (argumentMapping.diagnostics.any { !it.candidateApplicability.isSuccess }) return null

        /**
         * (A, B, C) -> Unit
         * fun foo(a: A, b: B = B(), vararg c: C)
         */
        var defaults = 0
        val mappedArguments = arrayOfNulls<KotlinType?>(fakeArguments.size)
        for ((valueParameter, resolvedArgument) in argumentMapping.parameterToCallArgumentMap) {
            for (fakeArgument in resolvedArgument.arguments) {
                val index = (fakeArgument as FakeKotlinCallArgumentForCallableReference).index
                val substitutedParameter = descriptor.valueParameters.getOrNull(valueParameter.index) ?: continue

                mappedArguments[index] = substitutedParameter.varargElementType ?: substitutedParameter.type
            }
            if (resolvedArgument == ResolvedCallArgument.DefaultArgument) defaults++
        }
        if (mappedArguments.any { it == null }) return null

        val unitExpectedType = functionType.let(KotlinType::getReturnTypeFromFunctionType).takeIf { it.upperIfFlexible().isUnit() }
        val coercion = if (unitExpectedType != null) CoercionStrategy.COERCION_TO_UNIT else CoercionStrategy.NO_COERCION

        @Suppress("UNCHECKED_CAST")
        return Triple(mappedArguments as Array<KotlinType>, coercion, defaults)
    }

    private fun buildReflectionType(
            descriptor: CallableDescriptor,
            dispatchReceiver: CallableReceiver?,
            extensionReceiver: CallableReceiver?,
            expectedType: UnwrappedType?
    ): Pair<UnwrappedType, /*defaults*/ Int> {
        val argumentsAndReceivers = ArrayList<KotlinType>(descriptor.valueParameters.size + 2)

        if (dispatchReceiver is CallableReceiver.UnboundReference) {
            argumentsAndReceivers.add(descriptor.dispatchReceiverParameter?.type
                                      ?: error("Dispatch receiver should be not null for descriptor: $descriptor"))
        }
        if (extensionReceiver is CallableReceiver.UnboundReference) {
            argumentsAndReceivers.add(descriptor.extensionReceiverParameter?.type
                                      ?: error("Extension receiver should be not null for descriptor: $descriptor"))
        }

        val descriptorReturnType = descriptor.returnType
                                   ?: ErrorUtils.createErrorType("Error return type for descriptor: $descriptor")

        when (descriptor) {
            is PropertyDescriptor -> {
                val mutable = descriptor.isVar && run {
                    val setter = descriptor.setter
                    setter == null || Visibilities.isVisible(dispatchReceiver?.asReceiverValueForVisibilityChecks, setter,
                                                             outerCallContext.scopeTower.lexicalScope.ownerDescriptor)
                }

                return outerCallContext.reflectionTypes.getKPropertyType(Annotations.EMPTY, argumentsAndReceivers, descriptorReturnType, mutable) to 0
            }
            is FunctionDescriptor -> {
                val returnType: KotlinType
                val defaults: Int
                val argumentsAndExpectedTypeCoercion = getArgumentAndReturnTypeUseMappingByExpectedType(descriptor, expectedType,
                                                                                                        unboundReceiverCount = argumentsAndReceivers.size)

                if (argumentsAndExpectedTypeCoercion == null) {
                    descriptor.valueParameters.mapTo(argumentsAndReceivers) { it.type }
                    returnType = descriptorReturnType
                    defaults = 0
                }
                else {
                    val (arguments, coercion) = argumentsAndExpectedTypeCoercion
                    defaults = argumentsAndExpectedTypeCoercion.third
                    argumentsAndReceivers.addAll(arguments)

                    returnType = if (coercion == CoercionStrategy.COERCION_TO_UNIT) descriptor.builtIns.unitType else descriptorReturnType
                }

                return outerCallContext.reflectionTypes.getKFunctionType(Annotations.EMPTY, null, argumentsAndReceivers, null,
                                                                         returnType, descriptor.builtIns) to defaults
            }
            else -> error("Unsupported descriptor type: $descriptor")
        }
    }

    private fun toCallableReceiver(receiver: ReceiverValueWithSmartCastInfo, isExplicit: Boolean): CallableReceiver {
        if (!isExplicit) return CallableReceiver.ScopeReceiver(receiver)

        val lhsResult = argument.lhsResult
        return when (lhsResult) {
            is LHSResult.Expression -> CallableReceiver.ExplicitValueReceiver(lhsResult.lshCallArgument, receiver)
            is LHSResult.Type -> {
                if (lhsResult.qualifier.classValueReceiver?.type == receiver.receiverValue.type) {
                    CallableReceiver.BoundValueReference(lhsResult.qualifier, receiver)
                }
                else {
                    CallableReceiver.UnboundReference(lhsResult.qualifier, receiver)
                }
            }
            is LHSResult.Object -> CallableReceiver.BoundValueReference(lhsResult.qualifier, receiver)
            else -> throw IllegalStateException("Unsupported kind of lhsResult: $lhsResult")
        }
    }
}

class CallableReferenceNotCompatible(
        val argument: CallableReferenceKotlinCallArgument,
        val candidate: CallableMemberDescriptor,
        val expectedType: UnwrappedType,
        val callableReverenceType: UnwrappedType
) : KotlinCallDiagnostic(ResolutionCandidateApplicability.INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(argument, this)
}

class NotCallableMemberReference(
        val argument: CallableReferenceKotlinCallArgument,
        val candidate: CallableDescriptor
) : KotlinCallDiagnostic(ResolutionCandidateApplicability.INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(argument, this)
}