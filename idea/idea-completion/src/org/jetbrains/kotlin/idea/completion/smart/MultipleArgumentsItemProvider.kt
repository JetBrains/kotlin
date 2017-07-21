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
import org.jetbrains.kotlin.idea.KotlinDescriptorIconProvider
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.completion.tryGetOffset
import org.jetbrains.kotlin.idea.core.ArgumentPositionData
import org.jetbrains.kotlin.idea.core.ExpectedInfo
import org.jetbrains.kotlin.idea.core.SmartCastCalculator
import org.jetbrains.kotlin.idea.core.Tail
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.idea.util.getVariableFromImplicitReceivers
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.findVariable
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import java.util.*

class MultipleArgumentsItemProvider(
        private val bindingContext: BindingContext,
        private val smartCastCalculator: SmartCastCalculator,
        private val resolutionFacade: ResolutionFacade
) {

    fun addToCollection(collection: MutableCollection<LookupElement>,
                               expectedInfos: Collection<ExpectedInfo>,
                               context: KtExpression) {
        val resolutionScope = context.getResolutionScope(bindingContext, resolutionFacade)

        val added = HashSet<String>()
        for (expectedInfo in expectedInfos) {
            val additionalData = expectedInfo.additionalData
            if (additionalData is ArgumentPositionData.Positional) {
                val parameters = additionalData.function.valueParameters.drop(additionalData.argumentIndex)
                if (parameters.size > 1) {
                    val tail = when (additionalData.callType) {
                        Call.CallType.ARRAY_GET_METHOD, Call.CallType.ARRAY_SET_METHOD -> Tail.RBRACKET
                        else -> Tail.RPARENTH
                    }
                    val variables = ArrayList<VariableDescriptor>()
                    for ((i, parameter) in parameters.withIndex()) {
                        variables.add(variableInScope(parameter, resolutionScope) ?: break)

                        if (i > 0 && parameters.asSequence().drop(i + 1).all { it.hasDefaultValue() }) { // this is the last parameter or all others have default values
                            val lookupElement = createParametersLookupElement(variables, tail)
                            if (added.add(lookupElement.lookupString)) { // check that we don't already have item with the same text
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
        val firstIcon = KotlinDescriptorIconProvider.getIcon(variables.first(), null, 0) ?: KotlinIcons.PARAMETER
        val lastIcon = KotlinDescriptorIconProvider.getIcon(variables.last(), null, 0) ?: KotlinIcons.PARAMETER
        compoundIcon.setIcon(lastIcon, 0, 2 * firstIcon.iconWidth / 5, 0)
        compoundIcon.setIcon(firstIcon, 1, 0, 0)

        return LookupElementBuilder
                .create(variables.joinToString(", ") { it.name.render() }) //TODO: use code formatting settings
                .withInsertHandler { context, _ ->
                    if (context.completionChar == Lookup.REPLACE_SELECT_CHAR) {
                        val offset = context.offsetMap.tryGetOffset(SmartCompletion.MULTIPLE_ARGUMENTS_REPLACEMENT_OFFSET)
                        if (offset != null) {
                            context.document.deleteString(context.tailOffset, offset)
                        }
                    }

                }
                .withIcon(compoundIcon)
                .addTail(tail)
                .assignSmartCompletionPriority(SmartCompletionItemPriority.MULTIPLE_ARGUMENTS_ITEM)
    }

    private fun variableInScope(parameter: ValueParameterDescriptor, scope: LexicalScope): VariableDescriptor? {
        val name = parameter.name
        //TODO: there can be more than one property with such name in scope and we should be able to select one (but we need API for this)
        val variable = scope.findVariable(name, NoLookupLocation.FROM_IDE) { !it.isExtension }
                ?: scope.getVariableFromImplicitReceivers(name) ?: return null
        return if (smartCastCalculator.types(variable).any { KotlinTypeChecker.DEFAULT.isSubtypeOf(it, parameter.type) })
            variable
        else
            null
    }
}
