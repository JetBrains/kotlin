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

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import org.jetbrains.kotlin.builtins.isExtensionFunctionType
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.util.CallType
import org.jetbrains.kotlin.idea.util.ReceiverType
import org.jetbrains.kotlin.idea.util.substituteExtensionIfCallable
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.resolve.calls.tasks.createSynthesizedInvokes
import org.jetbrains.kotlin.util.OperatorNameConventions
import java.util.*

class ExtensionFunctionTypeValueCompletion(
        private val receiverTypes: Collection<ReceiverType>,
        private val callType: CallType<*>,
        private val lookupElementFactory: LookupElementFactory
) {
    data class Result(val invokeDescriptor: FunctionDescriptor, val factory: AbstractLookupElementFactory)

    fun processVariables(variablesProvider: RealContextVariablesProvider): Collection<Result> {
        if (callType != CallType.DOT && callType != CallType.SAFE) return emptyList()

        val results = ArrayList<Result>()

        for (variable in variablesProvider.allFunctionTypeVariables) {
            val variableType = variable.type
            if (!variableType.isExtensionFunctionType) continue

            val invokes = variableType.memberScope.getContributedFunctions(OperatorNameConventions.INVOKE, NoLookupLocation.FROM_IDE)
            for (invoke in createSynthesizedInvokes(invokes)) {
                for (substituted in invoke.substituteExtensionIfCallable(receiverTypes.map { it.type }, callType)) {
                    val factory = object : AbstractLookupElementFactory {
                        override fun createStandardLookupElementsForDescriptor(descriptor: DeclarationDescriptor, useReceiverTypes: Boolean): Collection<LookupElement> {
                            if (!useReceiverTypes) return emptyList()
                            descriptor as FunctionDescriptor // should be descriptor for "invoke"

                            val invokeLookupElement = lookupElementFactory.createLookupElement(substituted, useReceiverTypes = true)
                            val variableLookupElement = lookupElementFactory.createLookupElement(variable, useReceiverTypes = false)
                            val insertHandler = lookupElementFactory.insertHandlerProvider.insertHandler(invoke)

                            val lookupElement = object : LookupElementDecorator<LookupElement>(variableLookupElement) {
                                override fun renderElement(presentation: LookupElementPresentation) {
                                    invokeLookupElement.renderElement(presentation)

                                    presentation.itemText = variable.name.asString()

                                    val parameterTail = presentation.tailFragments.first()
                                    presentation.clearTail()
                                    presentation.appendTailText(parameterTail.text, false)

                                    lookupElementFactory.basicFactory.appendContainerAndReceiverInformation(variable) {
                                        presentation.appendTailText(it, true)
                                    }
                                }

                                override fun handleInsert(context: InsertionContext) {
                                    insertHandler.handleInsert(context, this)
                                }
                            }
                            return listOf(lookupElement)
                        }

                        override fun createLookupElement(
                                descriptor: DeclarationDescriptor,
                                useReceiverTypes: Boolean,
                                qualifyNestedClasses: Boolean,
                                includeClassTypeArguments: Boolean,
                                parametersAndTypeGrayed: Boolean): LookupElement? = null
                    }

                    results.add(Result(substituted, factory))
                }
            }
        }

        return results
    }
}
