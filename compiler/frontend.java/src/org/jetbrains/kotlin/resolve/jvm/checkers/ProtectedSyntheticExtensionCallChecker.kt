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

package org.jetbrains.kotlin.resolve.jvm.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory3
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.context.CallPosition
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.smartcasts.getReceiverValueWithSmartCast
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor

object ProtectedSyntheticExtensionCallChecker : CallChecker {
    fun computeSuitableDescriptorAndError(
        descriptor: SyntheticJavaPropertyDescriptor,
        reportOn: PsiElement,
        context: CallCheckerContext
    ): Pair<FunctionDescriptor, DiagnosticFactory3<PsiElement, DeclarationDescriptor, DescriptorVisibility, DeclarationDescriptor>> {
        val callPosition = context.resolutionContext.callPosition
        val isLeftSide = callPosition is CallPosition.PropertyAssignment
                && (callPosition.leftPart as? KtDotQualifiedExpression)?.selectorExpression == reportOn
        val getMethod = descriptor.getMethod
        val setMethod = descriptor.setMethod
        val isImprovingDiagnosticsEnabled =
            context.languageVersionSettings.supportsFeature(LanguageFeature.ImproveReportingDiagnosticsOnProtectedMembersOfBaseClass)
        val needToTakeSetter = isImprovingDiagnosticsEnabled && isLeftSide
        val suitableDescriptor = if (needToTakeSetter && setMethod != null) setMethod else getMethod

        return suitableDescriptor to if (needToTakeSetter && setMethod != null) Errors.INVISIBLE_SETTER else Errors.INVISIBLE_MEMBER
    }

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val descriptor = resolvedCall.resultingDescriptor

        if (descriptor !is SyntheticJavaPropertyDescriptor) return

        val (sourceFunction, error) = computeSuitableDescriptorAndError(descriptor, reportOn, context)

        val from = context.scope.ownerDescriptor

        // Already reported
        if (!DescriptorVisibilities.isVisibleIgnoringReceiver(descriptor, from)) return

        if (resolvedCall.dispatchReceiver != null && resolvedCall.extensionReceiver !is ReceiverValue) return

        val receiverValue = resolvedCall.extensionReceiver as ReceiverValue
        val receiverTypes = listOf(receiverValue.type) + context.dataFlowInfo.getStableTypes(
            context.dataFlowValueFactory.createDataFlowValue(
                receiverValue, context.trace.bindingContext, context.scope.ownerDescriptor
            ),
            context.languageVersionSettings
        )

        if (receiverTypes.none { DescriptorVisibilities.isVisible(getReceiverValueWithSmartCast(null, it), sourceFunction, from) }) {
            context.trace.report(error.on(reportOn, descriptor, descriptor.visibility, from))
        }
    }
}
