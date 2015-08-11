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

package org.jetbrains.kotlin.idea.j2k

import org.jetbrains.kotlin.idea.intentions.*
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions.IfThenToElvisIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions.IfThenToSafeAccessIntention
import org.jetbrains.kotlin.psi.*
import java.util.ArrayList

interface J2kPostProcessing {
    fun createAction(element: JetElement): (() -> Unit)?
}

object J2KPostProcessingRegistrar {
    private val _processings = ArrayList<J2kPostProcessing>()

    val processings: Collection<J2kPostProcessing>
        get() = _processings

    init {
        _processings.add(RemoveExplicitTypeArgumentsProcessing())
        _processings.add(MoveLambdaOutsideParenthesesProcessing())
        _processings.add(ConvertToStringTemplateProcessing())

        registerTypicalIntentionBasedProcessing(UsePropertyAccessSyntaxIntention()) { applyTo(it) }
        registerTypicalIntentionBasedProcessing(IfThenToSafeAccessIntention()) { applyTo(it) }
        registerTypicalIntentionBasedProcessing(IfThenToElvisIntention()) { applyTo(it) }
        registerTypicalIntentionBasedProcessing(IfNullToElvisIntention()) { applyTo(it) }
        registerTypicalIntentionBasedProcessing(SimplifyNegatedBinaryExpressionIntention()) { applyTo(it) }
    }

    private inline fun <reified TElement : JetElement, TIntention: JetSelfTargetingRangeIntention<TElement>> registerTypicalIntentionBasedProcessing(
            intention: TIntention,
            inlineOptions(InlineOption.ONLY_LOCAL_RETURN) apply: TIntention.(TElement) -> Unit
    ) {
        _processings.add(object : J2kPostProcessing {
            override fun createAction(element: JetElement): (() -> Unit)? {
                if (!javaClass<TElement>().isInstance(element)) return null
                @suppress("UNCHECKED_CAST")
                if (intention.applicabilityRange(element as TElement) == null) return null
                return { intention.apply(element) }
            }
        })
    }

    private class RemoveExplicitTypeArgumentsProcessing : J2kPostProcessing {
        override fun createAction(element: JetElement): (() -> Unit)? {
            if (element !is JetTypeArgumentList || !RemoveExplicitTypeArgumentsIntention.isApplicableTo(element, approximateFlexible = true)) return null

            return { element.delete() }
        }
    }

    private class MoveLambdaOutsideParenthesesProcessing : J2kPostProcessing {
        private val intention = MoveLambdaOutsideParenthesesIntention()

        override fun createAction(element: JetElement): (() -> Unit)? {
            if (element !is JetCallExpression) return null
            val literalArgument = element.valueArguments.lastOrNull()?.getArgumentExpression()?.unpackFunctionLiteral() ?: return null
            if (!intention.isApplicableTo(element, literalArgument.textOffset)) return null
            return { intention.applyTo(element) }
        }
    }

    private class ConvertToStringTemplateProcessing : J2kPostProcessing {
        private val intention = ConvertToStringTemplateIntention()

        override fun createAction(element: JetElement): (() -> Unit)? {
            if (element is JetBinaryExpression && intention.isApplicableTo(element) && intention.isConversionResultSimple(element)) {
                return { intention.applyTo(element) }
            }
            else {
                return null
            }
        }
    }

}