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

@file:JvmName("ExtensionUtils")

package org.jetbrains.kotlin.idea.util

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.TypeNullability
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.nullability

fun <TCallable : CallableDescriptor> TCallable.substituteExtensionIfCallable(
        receiverTypes: Collection<KotlinType>,
        callType: CallType<*>
): Collection<TCallable> {
    if (!callType.descriptorKindFilter.accepts(this)) return listOf()

    var types = receiverTypes.asSequence()
    if (callType == CallType.SAFE) {
        types = types.map { it.makeNotNullable() }
    }

    val extensionReceiverType = fuzzyExtensionReceiverType()!!
    val substitutors = types
            .mapNotNull {
                var substitutor = extensionReceiverType.checkIsSuperTypeOf(it)
                // check if we may fail due to receiver expression being nullable
                if (substitutor == null && it.nullability() == TypeNullability.NULLABLE && extensionReceiverType.nullability() == TypeNullability.NOT_NULL) {
                    substitutor = extensionReceiverType.checkIsSuperTypeOf(it.makeNotNullable())
                }
                substitutor
            }
    return if (typeParameters.isEmpty()) { // optimization for non-generic callables
        if (substitutors.any()) listOf(this) else listOf()
    }
    else {
        substitutors
                .mapNotNull { @Suppress("UNCHECKED_CAST") (substitute(it) as TCallable?) }
                .toList()
    }
}

fun ReceiverValue?.getThisReceiverOwner(bindingContext: BindingContext): DeclarationDescriptor? {
    return when (this) {
        is ExpressionReceiver -> {
            val thisRef = (KtPsiUtil.deparenthesize(this.expression) as? KtThisExpression)?.instanceReference ?: return null
            bindingContext[BindingContext.REFERENCE_TARGET, thisRef]
        }

        is ImplicitReceiver -> this.declarationDescriptor

        else -> null
    }
}

fun ReceiverValue?.getReceiverTargetDescriptor(bindingContext: BindingContext): DeclarationDescriptor? {
    return when (this) {
        is ExpressionReceiver -> {
            val expression = KtPsiUtil.deparenthesize(this.expression)
            val refExpression = when (expression) {
                                    is KtThisExpression -> expression.instanceReference
                                    is KtReferenceExpression -> expression
                                    else -> null
                                }  ?: return null
            bindingContext[BindingContext.REFERENCE_TARGET, refExpression]
        }

        is ImplicitReceiver -> this.declarationDescriptor

        else -> null
    }
}
