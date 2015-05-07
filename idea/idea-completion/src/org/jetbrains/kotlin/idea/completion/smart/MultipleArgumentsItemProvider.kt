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
import org.jetbrains.kotlin.idea.completion.*
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.checker.JetTypeChecker
import java.util.ArrayList
import java.util.HashSet

class MultipleArgumentsItemProvider(val bindingContext: BindingContext,
                                    val smartCastTypes: (VariableDescriptor) -> Collection<JetType>) {

    public fun addToCollection(collection: MutableCollection<LookupElement>,
                               expectedInfos: Collection<ExpectedInfo>,
                               context: JetExpression) {
        val resolutionScope = bindingContext[BindingContext.RESOLUTION_SCOPE, context] ?: return

        val added = HashSet<String>()
        for (expectedInfo in expectedInfos) {
            if (expectedInfo is ArgumentExpectedInfo && expectedInfo.position == ArgumentPosition(0)) {
                val parameters = expectedInfo.function.getValueParameters()
                if (parameters.size() > 1) {
                    val variables = ArrayList<VariableDescriptor>()
                    for ((i, parameter) in parameters.withIndex()) {
                        val variable = variableInScope(parameter, resolutionScope) ?: break
                        variables.add(variable) // TODO: cannot inline variable because of KT-5890

                        if (i > 0 && parameters.asSequence().drop(i + 1).all { it.hasDefaultValue() }) { // this is the last parameter or all others have default values
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
                .withInsertHandler { context, lookupElement ->
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
        return if (smartCastTypes(variable).any { JetTypeChecker.DEFAULT.isSubtypeOf(it, parameter.getType()) })
            variable
        else
            null
    }
}
