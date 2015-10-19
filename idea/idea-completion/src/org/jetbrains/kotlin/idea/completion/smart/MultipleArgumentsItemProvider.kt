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

package org.jetbrains.kotlin.idea.completion.smart

import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.ui.LayeredIcon
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.JetDescriptorIconProvider
import org.jetbrains.kotlin.idea.completion.ArgumentPositionData
import org.jetbrains.kotlin.idea.completion.ExpectedInfo
import org.jetbrains.kotlin.idea.completion.SmartCastCalculator
import org.jetbrains.kotlin.idea.completion.Tail
import org.jetbrains.kotlin.idea.util.getVariableFromImplicitReceivers
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue
import org.jetbrains.kotlin.resolve.scopes.KtScope
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import java.util.*

class MultipleArgumentsItemProvider(val bindingContext: BindingContext,
                                    val smartCastCalculator: SmartCastCalculator) {

    public fun addToCollection(collection: MutableCollection<LookupElement>,
                               expectedInfos: Collection<ExpectedInfo>,
                               context: KtExpression) {
        val resolutionScope = bindingContext[BindingContext.RESOLUTION_SCOPE, context] ?: return

        val added = HashSet<String>()
        for (expectedInfo in expectedInfos) {
            val additionalData = expectedInfo.additionalData
            if (additionalData is ArgumentPositionData.Positional && additionalData.argumentIndex == 0) {
                val parameters = additionalData.function.valueParameters
                if (parameters.size() > 1) {
                    val tail = when (additionalData.callType) {
                        Call.CallType.ARRAY_GET_METHOD, Call.CallType.ARRAY_SET_METHOD -> Tail.RBRACKET
                        else -> Tail.RPARENTH
                    }
                    val variables = ArrayList<VariableDescriptor>()
                    for ((i, parameter) in parameters.withIndex()) {
                        val variable = variableInScope(parameter, resolutionScope) ?: break
                        variables.add(variable) // TODO: cannot inline variable because of KT-5890

                        if (i > 0 && parameters.asSequence().drop(i + 1).all { it.hasDefaultValue() }) { // this is the last parameter or all others have default values
                            val lookupElement = createParametersLookupElement(variables, tail)
                            if (added.add(lookupElement.getLookupString())) { // check that we don't already have item with the same text
                                collection.add(lookupElement)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createParametersLookupElement(variables: List<VariableDescriptor>, tail: Tail): LookupElement {
        val compoundIcon = LayeredIcon(2)
        val firstIcon = JetDescriptorIconProvider.getIcon(variables.first(), null, 0)
        val lastIcon = JetDescriptorIconProvider.getIcon(variables.last(), null, 0)
        compoundIcon.setIcon(lastIcon, 0, 2 * firstIcon.getIconWidth() / 5, 0)
        compoundIcon.setIcon(firstIcon, 1, 0, 0)

        return LookupElementBuilder
                .create(variables.map { it.getName().render() }.joinToString(", "))
                .withInsertHandler { context, lookupElement ->
                    if (context.getCompletionChar() == Lookup.REPLACE_SELECT_CHAR) {
                        val offset = context.getOffsetMap().getOffset(SmartCompletion.MULTIPLE_ARGUMENTS_REPLACEMENT_OFFSET)
                        if (offset != -1) {
                            context.getDocument().deleteString(context.getTailOffset(), offset)
                        }
                    }

                }
                .withIcon(compoundIcon)
                .addTail(tail)
                .assignSmartCompletionPriority(SmartCompletionItemPriority.MULTIPLE_ARGUMENTS_ITEM)
    }

    private fun variableInScope(parameter: ValueParameterDescriptor, scope: KtScope): VariableDescriptor? {
        val name = parameter.getName()
        //TODO: there can be more than one property with such name in scope and we should be able to select one (but we need API for this)
        val variable = scope.getLocalVariable(name) ?: scope.getProperties(name, NoLookupLocation.FROM_IDE).singleOrNull() ?:
                       scope.getVariableFromImplicitReceivers(name) ?: return null
        return if (smartCastCalculator.types(variable).any { KotlinTypeChecker.DEFAULT.isSubtypeOf(it, parameter.getType()) })
            variable
        else
            null
    }
}
