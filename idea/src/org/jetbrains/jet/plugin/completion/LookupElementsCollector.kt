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

package org.jetbrains.jet.plugin.completion

import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.plugin.completion.handlers.*
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies
import com.intellij.codeInsight.completion.PrefixMatcher
import java.util.ArrayList
import com.intellij.codeInsight.completion.CompletionResultSet
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor

class LookupElementsCollector(private val prefixMatcher: PrefixMatcher, private val resolveSession: ResolveSessionForBodies) {
    private val elements = ArrayList<LookupElement>()

    public fun flushToResultSet(resultSet: CompletionResultSet) {
        if (!elements.isEmpty()) {
            resultSet.addAllElements(elements)
            elements.clear()
            isResultEmpty = false
        }
    }

    public var isResultEmpty: Boolean = true
        private set

    public fun addDescriptorElements(descriptors: Iterable<DeclarationDescriptor>,
                                     suppressAutoInsertion: Boolean // auto-insertion suppression is used for elements that require adding an import
    ) {
        for (descriptor in descriptors) {
            addDescriptorElements(descriptor, suppressAutoInsertion)
        }
    }

    public fun addDescriptorElements(descriptor: DeclarationDescriptor, suppressAutoInsertion: Boolean) {
        run {
            val lookupElement = KotlinLookupElementFactory.createLookupElement(resolveSession, descriptor)
            if (suppressAutoInsertion) {
                addElementWithAutoInsertionSuppressed(lookupElement)
            }
            else {
                addElement(lookupElement)
            }
        }

        // add special item for function with one argument of function type with more than one parameter
        if (descriptor is FunctionDescriptor) {
            val parameters = descriptor.getValueParameters()
            if (parameters.size() == 1) {
                val parameterType = parameters.get(0).getType()
                if (KotlinBuiltIns.getInstance().isFunctionOrExtensionFunctionType(parameterType)) {
                    val parameterCount = KotlinBuiltIns.getInstance().getParameterTypeProjectionsFromFunctionType(parameterType).size()
                    if (parameterCount > 1) {
                        val lookupElement = KotlinLookupElementFactory.createLookupElement(resolveSession, descriptor)
                        addElement(object : LookupElementDecorator<LookupElement>(lookupElement) {
                            override fun renderElement(presentation: LookupElementPresentation) {
                                super.renderElement(presentation)
                                presentation.setItemText(getLookupString() + " " + buildLambdaPresentation(parameterType))
                            }

                            override fun handleInsert(context: InsertionContext) {
                                JetFunctionInsertHandler(CaretPosition.IN_BRACKETS, GenerateLambdaInfo(parameterType, true)).handleInsert(context, this)
                            }
                        })
                    }
                }
            }
        }
    }

    public fun addElement(element: LookupElement) {
        if (prefixMatcher.prefixMatches(element)) {
            elements.add(element)
        }
    }

    public fun addElementWithAutoInsertionSuppressed(element: LookupElement) {
        if (isResultEmpty && elements.isEmpty()) { /* without these checks we may get duplicated items */
            addElement(element.suppressAutoInsertion())
        }
        else {
            addElement(element)
        }
    }

    public fun addElements(elements: Iterable<LookupElement>) {
        elements.forEach { addElement(it) }
    }
}
