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

package org.jetbrains.kotlin.resolve.calls.resolvedCallUtil

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.JetPsiUtil
import org.jetbrains.kotlin.psi.JetSuperExpression
import org.jetbrains.kotlin.psi.JetThisExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.getOwnerForEffectiveDispatchReceiverParameter
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisReceiver

// it returns true if call has no dispatch receiver (e.g. resulting descriptor is top-level function or local variable)
// or call receiver is effectively `this` instance (explicitly or implicitly) of resulting descriptor
// class A(other: A) {
//   val x
//   val y = other.x // return false for `other.x` as it's receiver is not `this`
// }
public fun ResolvedCall<*>.hasThisOrNoDispatchReceiver(context: BindingContext): Boolean =
        hasThisOrNoDispatchReceiver(context, true, true)

public fun ResolvedCall<*>.hasImplicitThisOrSuperDispatchReceiver(context: BindingContext): Boolean =
        hasThisOrNoDispatchReceiver(context, false, false)

private fun ResolvedCall<*>.hasThisOrNoDispatchReceiver(
        context: BindingContext,
        returnForNoReceiver: Boolean,
        considerExplicitReceivers: Boolean
): Boolean {
    val dispatchReceiverValue = getDispatchReceiver()
    if (getResultingDescriptor().getDispatchReceiverParameter() == null || !dispatchReceiverValue.exists()) return returnForNoReceiver

    var dispatchReceiverDescriptor: DeclarationDescriptor? = null
    if (dispatchReceiverValue is ThisReceiver) {
        // foo() -- implicit receiver
        dispatchReceiverDescriptor = dispatchReceiverValue.getDeclarationDescriptor()
    }
    else if (dispatchReceiverValue is ExpressionReceiver && considerExplicitReceivers) {
        val expression = JetPsiUtil.deparenthesize(dispatchReceiverValue.getExpression())
        if (expression is JetThisExpression) {
            // this.foo() -- explicit receiver
            dispatchReceiverDescriptor = context.get(BindingContext.REFERENCE_TARGET, expression.getInstanceReference())
        }
    }

    return dispatchReceiverDescriptor == getResultingDescriptor().getOwnerForEffectiveDispatchReceiverParameter()
}

