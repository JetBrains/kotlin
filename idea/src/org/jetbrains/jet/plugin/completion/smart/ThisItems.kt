/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.completion.smart

import com.intellij.codeInsight.lookup.LookupElement
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor
import com.intellij.codeInsight.lookup.LookupElementBuilder
import org.jetbrains.jet.renderer.DescriptorRenderer
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.BindingContextUtils
import org.jetbrains.jet.lang.psi.JetFunctionLiteral
import org.jetbrains.jet.lang.psi.JetFunctionLiteralExpression
import org.jetbrains.jet.lang.psi.JetObjectDeclaration
import org.jetbrains.jet.lang.psi.JetObjectLiteralExpression
import org.jetbrains.jet.lang.psi.JetValueArgument
import org.jetbrains.jet.lang.psi.JetValueArgumentList
import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.plugin.completion.ExpectedInfo
import org.jetbrains.jet.plugin.util.makeNotNullable
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils

class ThisItems(val bindingContext: BindingContext) {
    public fun addToCollection(collection: MutableCollection<LookupElement>, context: JetExpression, expectedInfos: Collection<ExpectedInfo>) {
        val scope = bindingContext[BindingContext.RESOLUTION_SCOPE, context]
        if (scope == null) return

        val receivers: List<ReceiverParameterDescriptor> = scope.getImplicitReceiversHierarchy()
        for (i in 0..receivers.size - 1) {
            val receiver = receivers[i]
            val thisType = receiver.getType()
            val classifier = { (expectedInfo: ExpectedInfo) ->
                when {
                    thisType.isSubtypeOf(expectedInfo.`type`) -> ExpectedInfoClassification.MATCHES
                    thisType.isNullable() && thisType.makeNotNullable().isSubtypeOf(expectedInfo.`type`) -> ExpectedInfoClassification.MAKE_NOT_NULLABLE
                    else -> ExpectedInfoClassification.NOT_MATCHES
                }
            }
            fun lookupElementFactory(): LookupElement? {
                //TODO: use this code when KT-4258 fixed
                //val expressionText = if (i == 0) "this" else "this@" + (thisQualifierName(receiver, bindingContext) ?: return null)
                val qualifier = if (i == 0) null else (thisQualifierName(receiver) ?: return null)
                val expressionText = if (qualifier == null) "this" else "this@" + qualifier
                return LookupElementBuilder.create(expressionText).withTypeText(DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(thisType))
            }
            collection.addLookupElements(expectedInfos, classifier, ::lookupElementFactory)
        }
    }

    private fun thisQualifierName(receiver: ReceiverParameterDescriptor): String? {
        val descriptor: DeclarationDescriptor = receiver.getContainingDeclaration()
        val name: Name = descriptor.getName()
        if (!name.isSpecial()) return name.asString()

        val psiElement = DescriptorToSourceUtils.descriptorToDeclaration(descriptor)
        val expression: JetExpression? = when (psiElement) {
            is JetFunctionLiteral -> psiElement.getParent() as? JetFunctionLiteralExpression
            is JetObjectDeclaration -> psiElement.getParent() as? JetObjectLiteralExpression
            else -> null
        }
        return ((((expression?.getParent() as? JetValueArgument)
                       ?.getParent() as? JetValueArgumentList)
                           ?.getParent() as? JetCallExpression)
                               ?.getCalleeExpression() as? JetSimpleNameExpression)
                                   ?.getReferencedName()
    }

}