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

package org.jetbrains.kotlin.idea.util

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetFunctionLiteral
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.JetScope
import java.util.LinkedHashMap
import java.util.LinkedHashSet

public fun JetScope.getImplicitReceiversWithInstance(): Collection<ReceiverParameterDescriptor>
        = getImplicitReceiversWithInstanceToExpression().keySet()

public interface ReceiverExpressionFactory {
    public fun createExpression(psiFactory: JetPsiFactory, shortThis: Boolean = true): JetExpression
}

public fun JetScope.getImplicitReceiversWithInstanceToExpression(): Map<ReceiverParameterDescriptor, ReceiverExpressionFactory?> {
    // we use a set to workaround a bug with receiver for companion object present twice in the result of getImplicitReceiversHierarchy()
    val receivers = LinkedHashSet(getImplicitReceiversHierarchy())

    val outerDeclarationsWithInstance = LinkedHashSet<DeclarationDescriptor>()
    var current: DeclarationDescriptor? = getContainingDeclaration()
    while (current != null) {
        if (current is PropertyAccessorDescriptor) {
            current =  current.getCorrespondingProperty()
        }
        outerDeclarationsWithInstance.add(current)

        val classDescriptor = current as? ClassDescriptor
        if (classDescriptor != null && !classDescriptor.isInner() && !DescriptorUtils.isLocal(classDescriptor)) break

        current = current!!.getContainingDeclaration()
    }

    val result = LinkedHashMap<ReceiverParameterDescriptor, ReceiverExpressionFactory?>()
    for ((index, receiver) in receivers.withIndex()) {
        val owner = receiver.getContainingDeclaration()
        val (expressionText, isImmediateThis) = if (owner in outerDeclarationsWithInstance) {
            val thisWithLabel = thisQualifierName(receiver)?.let { "this@${it.render()}" }
            if (index == 0)
                (thisWithLabel ?: "this") to true
            else
                thisWithLabel to false
        }
        else if (owner is ClassDescriptor && owner.getKind().isSingleton()) {
            IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(owner) to false
        }
        else {
            continue
        }
        val factory = if (expressionText != null)
            object : ReceiverExpressionFactory {
                override fun createExpression(psiFactory: JetPsiFactory, shortThis: Boolean): JetExpression {
                    return psiFactory.createExpression(if (shortThis && isImmediateThis) "this" else expressionText)
                }
            }
        else
            null
        result.put(receiver, factory)
    }
    return result
}

private fun thisQualifierName(receiver: ReceiverParameterDescriptor): Name? {
    val descriptor = receiver.getContainingDeclaration()
    val name = descriptor.getName()
    if (!name.isSpecial()) return name

    val functionLiteral = DescriptorToSourceUtils.descriptorToDeclaration(descriptor) as? JetFunctionLiteral
    return functionLiteral?.findLabelAndCall()?.first
}
