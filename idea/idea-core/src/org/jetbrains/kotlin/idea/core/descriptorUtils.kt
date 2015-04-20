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

package org.jetbrains.kotlin.idea.core

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils

fun DeclarationDescriptorWithVisibility.isVisible(
        from: DeclarationDescriptor,
        bindingContext: BindingContext? = null,
        element: JetSimpleNameExpression? = null
): Boolean {
    if (Visibilities.isVisible(ReceiverValue.IRRELEVANT_RECEIVER, this, from)) return true
    if (bindingContext == null || element == null) return false

    val receiver = element.getReceiverExpression()
    val type = receiver?.let { bindingContext.getType(it) }
    val explicitReceiver = type?.let { ExpressionReceiver(receiver, it) }

    if (explicitReceiver != null) {
        val normalizeReceiver = ExpressionTypingUtils.normalizeReceiverValueForVisibility(explicitReceiver, bindingContext)
        return Visibilities.isVisible(normalizeReceiver, this, from)
    }

    val jetScope = bindingContext[BindingContext.RESOLUTION_SCOPE, element]
    val implicitReceivers = jetScope?.getImplicitReceiversHierarchy()
    if (implicitReceivers != null) {
        for (implicitReceiver in implicitReceivers) {
            val normalizeReceiver = ExpressionTypingUtils.normalizeReceiverValueForVisibility(implicitReceiver.getValue(), bindingContext)
            if (Visibilities.isVisible(normalizeReceiver, this, from)) return true
        }
    }
    return false
}
