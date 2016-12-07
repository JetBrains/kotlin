/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.smartcasts.getReceiverValueWithSmartCast
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.synthetic.SamAdapterExtensionFunctionDescriptor
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor

object ProtectedSyntheticExtensionCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val descriptor = resolvedCall.resultingDescriptor

        val sourceFunction = when (descriptor) {
            is SyntheticJavaPropertyDescriptor -> descriptor.getMethod
            // TODO: this branch becomes unnecessary, because common checks are applied to SAM adapters being resolved as common members
            // But this part may be still useful when we enable backward compatibility mode and SAM adapters become extensions again
            is SamAdapterExtensionFunctionDescriptor -> descriptor.baseDescriptorForSynthetic
            else -> return
        }

        val from = context.scope.ownerDescriptor

        // Already reported
        if (!Visibilities.isVisibleIgnoringReceiver(descriptor, from)) return

        if (resolvedCall.dispatchReceiver != null && resolvedCall.extensionReceiver !is ReceiverValue) return

        val receiverValue = resolvedCall.extensionReceiver as ReceiverValue
        val receiverTypes = listOf(receiverValue.type) + context.dataFlowInfo.getStableTypes(
                DataFlowValueFactory.createDataFlowValue(receiverValue, context.trace.bindingContext, context.scope.ownerDescriptor)
        )

        if (receiverTypes.none { Visibilities.isVisible(getReceiverValueWithSmartCast(null, it), sourceFunction, from) }) {
            context.trace.report(Errors.INVISIBLE_MEMBER.on(reportOn, descriptor, descriptor.visibility, from))
        }
    }
}
