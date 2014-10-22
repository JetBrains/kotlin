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

import org.jetbrains.jet.plugin.completion.ExpectedInfo
import org.jetbrains.jet.lang.psi.JetExpression
import com.intellij.codeInsight.lookup.LookupElement
import org.jetbrains.jet.lang.types.checker.JetTypeChecker
import com.intellij.ui.LayeredIcon
import com.intellij.codeInsight.lookup.LookupElementBuilder
import org.jetbrains.jet.plugin.completion.Tail
import org.jetbrains.jet.plugin.completion.ItemPriority
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.resolve.BindingContext
import java.util.HashSet
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor
import org.jetbrains.jet.plugin.JetDescriptorIconProvider
import org.jetbrains.jet.lang.descriptors.VariableDescriptor
import org.jetbrains.jet.plugin.completion.PositionalArgumentExpectedInfo
import java.util.ArrayList
import org.jetbrains.jet.plugin.util.IdeDescriptorRenderers
import org.jetbrains.jet.plugin.completion.assignPriority
import com.intellij.codeInsight.lookup.Lookup

class MultipleArgumentsItemProvider(val bindingContext: BindingContext,
                                    val typesWithAutoCasts: (DeclarationDescriptor) -> Iterable<JetType>) {

    public fun addToCollection(collection: MutableCollection<LookupElement>,
                               expectedInfos: Collection<ExpectedInfo>,
                               context: JetExpression) {
        val resolutionScope = bindingContext[BindingContext.RESOLUTION_SCOPE, context] ?: return

        val added = HashSet<String>()
        for (expectedInfo in expectedInfos) {
            if (expectedInfo is PositionalArgumentExpectedInfo && expectedInfo.argumentIndex == 0) {
                val parameters = expectedInfo.function.getValueParameters()
                if (parameters.size > 1) {
                    val variables = ArrayList<VariableDescriptor>()
                    for ((i, parameter) in parameters.withIndices()) {
                        val variable = variableInScope(parameter, resolutionScope) ?: break
                        variables.add(variable) // TODO: cannot inline variable because of KT-5890

                        if (i > 0 && parameters.stream().drop(i + 1).all { it.hasDefaultValue() }) { // this is the last parameter or all others have default values
                            val lookupElement = createParametersLookupElement(variables)
                            if (added.add(lookupElement.getLookupString())) { // check that we don't already have item with the same text
                                collection.add(lookupElement)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createParametersLookupElement(variables: List<VariableDescriptor>): LookupElement {
        val compoundIcon = LayeredIcon(2)
        val firstIcon = JetDescriptorIconProvider.getIcon(variables.first(), null, 0)
        val lastIcon = JetDescriptorIconProvider.getIcon(variables.last(), null, 0)
        compoundIcon.setIcon(lastIcon, 0, 2 * firstIcon.getIconWidth() / 5, 0)
        compoundIcon.setIcon(firstIcon, 1, 0, 0)

        return LookupElementBuilder
                .create(variables.map { IdeDescriptorRenderers.SOURCE_CODE.renderName(it.getName()) }.joinToString(", "))
                .withInsertHandler { (context, lookupElement) ->
                    if (context.getCompletionChar() == Lookup.REPLACE_SELECT_CHAR) {
                        val offset = context.getOffsetMap().getOffset(SmartCompletion.MULTIPLE_ARGUMENTS_REPLACEMENT_OFFSET)
                        if (offset != -1) {
                            context.getDocument().deleteString(context.getTailOffset(), offset)
                        }
                    }

                }
                .withIcon(compoundIcon)
                .addTail(Tail.RPARENTH) //TODO: support square brackets
                .assignPriority(ItemPriority.MULTIPLE_ARGUMENTS_ITEM)
    }

    private fun variableInScope(parameter: ValueParameterDescriptor, scope: JetScope): VariableDescriptor? {
        val name = parameter.getName()
        //TODO: there can be more than one property with such name in scope and we should be able to select one (but we need API for this)
        val variable = scope.getLocalVariable(name) ?: scope.getProperties(name).singleOrNull() ?: return null
        return if (typesWithAutoCasts(variable).any { JetTypeChecker.DEFAULT.isSubtypeOf(it, parameter.getType()) })
            variable
        else
            null
    }
}