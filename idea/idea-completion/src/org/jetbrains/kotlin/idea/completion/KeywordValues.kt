/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.completion.smart.ExpectedInfoMatch
import org.jetbrains.kotlin.idea.completion.smart.SmartCompletionItemPriority
import org.jetbrains.kotlin.idea.completion.smart.matchExpectedInfo
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.idea.util.toFuzzyType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.typeUtil.isBooleanOrNullableBoolean

object KeywordValues {
    interface Consumer {
        fun consume(
                lookupString: String,
                expectedInfoMatcher: (ExpectedInfo) -> ExpectedInfoMatch,
                priority: SmartCompletionItemPriority,
                factory: () -> LookupElement)
    }

    fun process(
            consumer: Consumer,
            callTypeAndReceiver: CallTypeAndReceiver<*, *>,
            bindingContext: BindingContext,
            resolutionFacade: ResolutionFacade,
            moduleDescriptor: ModuleDescriptor,
            isJvmModule: Boolean
    ) {
        if (callTypeAndReceiver is CallTypeAndReceiver.DEFAULT) {
            val booleanInfoMatcher = matcher@ { info: ExpectedInfo ->
                // no sense in true or false as if-condition or when entry for when with no subject
                val additionalData = info.additionalData
                val skipTrueFalse = when (additionalData) {
                    is IfConditionAdditionalData -> true
                    is WhenEntryAdditionalData -> !additionalData.whenWithSubject
                    else -> false
                }
                if (skipTrueFalse) {
                    return@matcher ExpectedInfoMatch.noMatch
                }

                if (info.fuzzyType?.type?.isBooleanOrNullableBoolean() ?: false)
                    ExpectedInfoMatch.match(TypeSubstitutor.EMPTY)
                else
                    ExpectedInfoMatch.noMatch
            }
            consumer.consume("true",  booleanInfoMatcher, SmartCompletionItemPriority.TRUE) {
                LookupElementBuilder.create(KeywordLookupObject(), "true").bold()
            }
            consumer.consume("false",  booleanInfoMatcher, SmartCompletionItemPriority.FALSE) {
                LookupElementBuilder.create(KeywordLookupObject(), "false").bold()
            }

            val nullMatcher = { info: ExpectedInfo ->
                when {
                    (info.additionalData as? ComparisonOperandAdditionalData)?.suppressNullLiteral == true -> ExpectedInfoMatch.noMatch

                    info.fuzzyType?.type?.isMarkedNullable == true -> ExpectedInfoMatch.match(TypeSubstitutor.EMPTY)

                    else -> ExpectedInfoMatch.noMatch
                }
            }
            consumer.consume("null", nullMatcher, SmartCompletionItemPriority.NULL) {
                LookupElementBuilder.create(KeywordLookupObject(), "null").bold()
            }
        }

        if (callTypeAndReceiver is CallTypeAndReceiver.CALLABLE_REFERENCE && callTypeAndReceiver.receiver != null) {
            val qualifierType = bindingContext.get(BindingContext.DOUBLE_COLON_LHS, callTypeAndReceiver.receiver!!)?.type
            if (qualifierType != null) {
                val kClassDescriptor = resolutionFacade.getFrontendService(ReflectionTypes::class.java).kClass
                val classLiteralType = KotlinTypeFactory.simpleNotNullType(Annotations.EMPTY, kClassDescriptor, listOf(TypeProjectionImpl(qualifierType)))
                val kClassTypes = listOf(classLiteralType.toFuzzyType(emptyList()))
                val kClassMatcher = { info: ExpectedInfo -> kClassTypes.matchExpectedInfo(info) }
                consumer.consume("class", kClassMatcher, SmartCompletionItemPriority.CLASS_LITERAL) {
                    LookupElementBuilder.create(KeywordLookupObject(), "class").bold()
                }

                if (isJvmModule) {
                    val javaLangClassDescriptor = resolutionFacade.resolveImportReference(moduleDescriptor, FqName("java.lang.Class"))
                            .singleOrNull() as? ClassDescriptor

                    if (javaLangClassDescriptor != null) {
                        val javaLangClassType = KotlinTypeFactory.simpleNotNullType(Annotations.EMPTY, javaLangClassDescriptor, listOf(TypeProjectionImpl(qualifierType)))
                        val javaClassTypes = listOf(javaLangClassType.toFuzzyType(emptyList()))
                        val javaClassMatcher = { info: ExpectedInfo -> javaClassTypes.matchExpectedInfo(info) }
                        consumer.consume("class", javaClassMatcher, SmartCompletionItemPriority.CLASS_LITERAL) {
                            LookupElementBuilder.create(KeywordLookupObject(), "class.java")
                                    .withPresentableText("class")
                                    .withTailText(".java")
                                    .bold()
                        }
                    }
                }
            }
        }
    }
}
