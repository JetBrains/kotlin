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

package org.jetbrains.kotlin.idea.patterns

import com.intellij.patterns.*
import com.intellij.psi.PsiElement
import com.intellij.util.PairProcessor
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.renderer.DescriptorRenderer

// Methods in this class are used through reflection
@Suppress("unused")
object KotlinPatterns: StandardPatterns() {
    @JvmStatic fun kotlinParameter() = KtParameterPattern()
    @JvmStatic fun kotlinFunction() = KotlinFunctionPattern()
    @JvmStatic fun receiver() = KotlinReceiverPattern()
}

// Methods in this class are used through reflection during pattern construction
@Suppress("unused")
open class KotlinFunctionPattern : PsiElementPattern<KtFunction, KotlinFunctionPattern>(KtFunction::class.java) {
    fun withParameters(vararg parameterTypes: String): KotlinFunctionPattern {
        return withPatternCondition("kotlinFunctionPattern-withParameters") { function, _ ->
            if (function.valueParameters.size != parameterTypes.size) return@withPatternCondition false

            val descriptor = function.resolveToDescriptorIfAny() as? FunctionDescriptor ?: return@withPatternCondition false
            val valueParameters = descriptor.valueParameters

            if (valueParameters.size != parameterTypes.size) return@withPatternCondition false
            for (i in 0..valueParameters.size - 1) {
                val expectedTypeString = parameterTypes[i]
                val actualParameterDescriptor = valueParameters[i]

                if (DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(actualParameterDescriptor.type) != expectedTypeString) {
                    return@withPatternCondition false
                }
            }

            true
        }
    }

    fun withReceiver(receiverFqName: String): KotlinFunctionPattern {
        return withPatternCondition("kotlinFunctionPattern-withReceiver") { function, _ ->
            if (function.receiverTypeReference == null) return@withPatternCondition false
            if (receiverFqName == "?") return@withPatternCondition true

            val descriptor = function.resolveToDescriptorIfAny() as? FunctionDescriptor ?: return@withPatternCondition false
            val receiver = descriptor.extensionReceiverParameter ?: return@withPatternCondition false

            DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(receiver.type) == receiverFqName
        }
    }

    fun definedInClass(fqName: String): KotlinFunctionPattern {
        return withPatternCondition("kotlinFunctionPattern-definedInClass") { function, _ ->
            if (function.parent is KtFile) return@withPatternCondition false

            function.containingClassOrObject?.fqName?.asString() == fqName
        }
    }

    fun definedInPackage(packageFqName: String): KotlinFunctionPattern {
        return withPatternCondition("kotlinFunctionPattern-definedInPackage") { function, _ ->
            if (function.parent !is KtFile) return@withPatternCondition false

            function.containingKtFile.packageFqName.asString() == packageFqName
        }
    }
}

// Methods in this class are used through reflection during pattern construction
@Suppress("unused")
class KtParameterPattern : PsiElementPattern<KtParameter, KtParameterPattern>(KtParameter::class.java) {
    fun ofFunction(index: Int, pattern: ElementPattern<Any>): KtParameterPattern {
        return with(object : PatternConditionPlus<KtParameter, KtFunction>("KtParameterPattern-ofMethod", pattern) {
            override fun processValues(ktParameter: KtParameter,
                                       context: ProcessingContext,
                                       processor: PairProcessor<KtFunction, ProcessingContext>): Boolean {
                val function = ktParameter.ownerFunction as? KtFunction ?: return true
                return processor.process(function, context)
            }

            override fun accepts(ktParameter: KtParameter, context: ProcessingContext): Boolean {
                val ktFunction = ktParameter.ownerFunction ?: return false

                val parameters = ktFunction.valueParameters
                if (index < 0 || index >= parameters.size || ktParameter != parameters[index]) return false

                return super.accepts(ktParameter, context)
            }
        })
    }

    fun withAnnotation(fqName: String): KtParameterPattern {
        return withPatternCondition("KtParameterPattern-withAnnotation") { ktParameter, _ ->
            if (ktParameter.annotationEntries.isEmpty()) return@withPatternCondition false

            val parameterDescriptor = ktParameter.resolveToDescriptorIfAny()
            parameterDescriptor is ValueParameterDescriptor && parameterDescriptor.annotations.any { annotation ->
                annotation.fqName?.asString() == fqName
            }
        }
    }
}

@Suppress("unused")
class KotlinReceiverPattern : PsiElementPattern<KtTypeReference, KotlinReceiverPattern>(KtTypeReference::class.java) {
    fun ofFunction(pattern: ElementPattern<Any>): KotlinReceiverPattern {
        return with(object : PatternConditionPlus<KtTypeReference, KtFunction>("KtReceiverPattern-ofMethod", pattern) {
            override fun processValues(typeReference: KtTypeReference, context: ProcessingContext?, processor: PairProcessor<KtFunction, ProcessingContext>): Boolean {
                return processor.process(typeReference.parent as? KtFunction, context)
            }

            override fun accepts(typeReference: KtTypeReference, context: ProcessingContext?): Boolean {
                val ktFunction = typeReference.parent as? KtFunction ?: return false
                if (ktFunction.receiverTypeReference != typeReference) return false

                return super.accepts(typeReference, context)
            }
        })
    }
}

private fun <T: PsiElement, Self: PsiElementPattern<T, Self>> PsiElementPattern<T, Self>.withPatternCondition(
        debugName: String, condition: (T, ProcessingContext?) -> Boolean): Self {
    return with(object: PatternCondition<T>(debugName) {
        override fun accepts(element: T, context: ProcessingContext?): Boolean {
            return condition(element, context)
        }
    })
}

