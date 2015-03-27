/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.tasks

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Errors.UNRESOLVED_REFERENCE
import org.jetbrains.kotlin.diagnostics.Errors.UNRESOLVED_REFERENCE_WRONG_RECEIVER
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.JetConstructorDelegationCall
import org.jetbrains.kotlin.psi.JetFunctionLiteralArgument
import org.jetbrains.kotlin.psi.JetSecondaryConstructor
import org.jetbrains.kotlin.resolve.BindingContext.CALL
import org.jetbrains.kotlin.resolve.BindingContext.REFERENCE_TARGET
import org.jetbrains.kotlin.resolve.BindingContext.RESOLVED_CALL
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.inference.InferenceErrorData
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.JetType


public class TracingStrategyForImplicitConstructorDelegationCall(
        val delegationCall: JetConstructorDelegationCall, call: Call
) : AbstractTracingStrategy(delegationCall.getCalleeExpression()!!, call) {

    val calleeExpression = delegationCall.getCalleeExpression()

    override fun bindCall(trace: BindingTrace, call: Call) {
        trace.record(CALL, call.getCalleeExpression(), call)
    }

    override fun <D : CallableDescriptor> bindReference(trace: BindingTrace, resolvedCall: ResolvedCall<D>) {
        val descriptor = resolvedCall.getCandidateDescriptor()
        val storedReference = trace.get(REFERENCE_TARGET, calleeExpression)
        if (storedReference == null || !ErrorUtils.isError(descriptor)) {
            trace.record(REFERENCE_TARGET, calleeExpression, descriptor)
        }
    }

    override fun <D : CallableDescriptor> bindResolvedCall(trace: BindingTrace, resolvedCall: ResolvedCall<D>) {
        trace.record(RESOLVED_CALL, call, resolvedCall)
    }

    override fun unresolvedReference(trace: BindingTrace) {
        trace.report(UNRESOLVED_REFERENCE.on(calleeExpression, calleeExpression))
    }

    override fun <D : CallableDescriptor> unresolvedReferenceWrongReceiver(trace: BindingTrace, candidates: Collection<ResolvedCall<D>>) {
        trace.report(UNRESOLVED_REFERENCE_WRONG_RECEIVER.on(reference, candidates))
    }

    override fun noValueForParameter(trace: BindingTrace, valueParameter: ValueParameterDescriptor) {
        reportError(trace)
    }

    override fun <D : CallableDescriptor?> ambiguity(trace: BindingTrace, descriptors: MutableCollection<out ResolvedCall<D>>) {
        reportError(trace)
    }

    override fun <D : CallableDescriptor?> noneApplicable(trace: BindingTrace, descriptors: MutableCollection<out ResolvedCall<D>>) {
        reportError(trace)
    }

    override fun invisibleMember(trace: BindingTrace, descriptor: DeclarationDescriptorWithVisibility) {
        reportError(trace)
    }

    private fun reportError(trace: BindingTrace) {
        trace.report(Errors.EXPLICIT_DELEGATION_CALL_REQUIRED.on(delegationCall))
    }

    // Underlying methods should not be called because such errors are impossible
    // when resolving delegation call
    override fun <D : CallableDescriptor?> cannotCompleteResolve(trace: BindingTrace, descriptors: MutableCollection<out ResolvedCall<D>>) {
        unexpectedError("cannotCompleteResolve")
    }

    override fun instantiationOfAbstractClass(trace: BindingTrace) {
        unexpectedError("instantiationOfAbstractClass")
    }

    override fun abstractSuperCall(trace: BindingTrace) {
        unexpectedError("abstractSuperCall")
    }

    override fun nestedClassAccessViaInstanceReference(
            trace: BindingTrace, classDescriptor: ClassDescriptor, explicitReceiverKind: ExplicitReceiverKind
    ) {
        unexpectedError("nestedClassAccessViaInstanceReference")
    }

    override fun unsafeCall(trace: BindingTrace, type: JetType, isCallForImplicitInvoke: Boolean) {
        unexpectedError("unsafeCall")
    }

    override fun unnecessarySafeCall(trace: BindingTrace, type: JetType) {
        unexpectedError("unnecessarySafeCall")
    }

    override fun danglingFunctionLiteralArgumentSuspected(
            trace: BindingTrace, functionLiteralArguments: MutableList<JetFunctionLiteralArgument>
    ) {
        unexpectedError("danglingFunctionLiteralArgumentSuspected")
    }

    override fun missingReceiver(trace: BindingTrace, expectedReceiver: ReceiverParameterDescriptor) {
        unexpectedError("missingReceiver")
    }

    override fun wrongReceiverType(trace: BindingTrace, receiverParameter: ReceiverParameterDescriptor, receiverArgument: ReceiverValue) {
        unexpectedError("wrongReceiverType")
    }

    override fun noReceiverAllowed(trace: BindingTrace) {
        unexpectedError("noReceiverAllowed")
    }

    override fun wrongNumberOfTypeArguments(trace: BindingTrace, expectedTypeArgumentCount: Int) {
        unexpectedError("wrongNumberOfTypeArguments")
    }

    override fun typeInferenceFailed(trace: BindingTrace, data: InferenceErrorData) {
        unexpectedError("typeInferenceFailed")
    }

    private fun unexpectedError(type: String) {
        throw AssertionError("Unexpected error type: $type")
    }
}
