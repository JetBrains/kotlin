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

import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.core.setVisibility
import org.jetbrains.kotlin.idea.inspections.RedundantSamConstructorInspection
import org.jetbrains.kotlin.idea.intentions.*
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions.IfThenToElvisIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions.IfThenToSafeAccessIntention
import org.jetbrains.kotlin.idea.intentions.conventionNameCalls.ExplicitGetInspection
import org.jetbrains.kotlin.idea.intentions.conventionNameCalls.ReplaceGetIntention
import org.jetbrains.kotlin.idea.quickfix.RemoveModifierFix
import org.jetbrains.kotlin.idea.quickfix.RemoveRightPartOfBinaryExpressionFix
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierType
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import java.util.*

interface J2kPostProcessing {
    fun createAction(element: KtElement, diagnostics: Diagnostics): (() -> Unit)?
}

object J2KPostProcessingRegistrar {
    private val _processings = ArrayList<J2kPostProcessing>()

    val processings: Collection<J2kPostProcessing>
        get() = _processings

    init {
        _processings.add(RemoveExplicitTypeArgumentsProcessing())
        _processings.add(RemoveRedundantOverrideVisibilityProcessing())
        _processings.add(MoveLambdaOutsideParenthesesProcessing())
        _processings.add(ConvertToStringTemplateProcessing())
        _processings.add(UsePropertyAccessSyntaxProcessing())
        _processings.add(RemoveRedundantSamAdaptersProcessing())
        _processings.add(AccessorBodyToExpression())

        registerIntentionBasedProcessing(IfThenToSafeAccessIntention()) { applyTo(it) }
        registerIntentionBasedProcessing(IfThenToElvisIntention()) { applyTo(it) }
        registerIntentionBasedProcessing(IfNullToElvisIntention()) { applyTo(it) }
        registerIntentionBasedProcessing(SimplifyNegatedBinaryExpressionIntention()) { applyTo(it) }
        registerIntentionBasedProcessing(ReplaceGetIntention(), additionalChecker = ExplicitGetInspection.additionalChecker) { applyTo(it) }
        registerIntentionBasedProcessing(AddOperatorModifierIntention()) { applyTo(it) }

        registerDiagnosticBasedProcessing<KtBinaryExpressionWithTypeRHS>(Errors.USELESS_CAST) { element, diagnostic ->
            val expression = RemoveRightPartOfBinaryExpressionFix(element, "").invoke()

            val variable = expression.parent as? KtProperty
            if (variable != null && expression == variable.initializer && variable.isLocal) {
                val refs = ReferencesSearch.search(variable, LocalSearchScope(variable.containingFile)).findAll()
                for (ref in refs) {
                    val usage = ref.element as? KtSimpleNameExpression ?: continue
                    usage.replace(expression)
                }
                variable.delete()
            }
        }

        registerDiagnosticBasedProcessing<KtTypeProjection>(Errors.REDUNDANT_PROJECTION) { element, diagnostic ->
            val fix = RemoveModifierFix.createRemoveProjectionFactory(true).createActions(diagnostic).single() as RemoveModifierFix
            fix.invoke()
        }

        registerDiagnosticBasedProcessing<KtSimpleNameExpression>(Errors.UNNECESSARY_NOT_NULL_ASSERTION) { element, diagnostic ->
            val exclExclExpr = element.parent as KtUnaryExpression
            exclExclExpr.replace(exclExclExpr.baseExpression!!)
        }

        registerDiagnosticBasedProcessingFactory(
                Errors.VAL_REASSIGNMENT,
                fun (element: KtSimpleNameExpression, diagnostic: Diagnostic): (() -> Unit)? {
                    val property = element.mainReference.resolve() as? KtProperty ?: return null
                    return {
                        if (!property.isVar) {
                            property.valOrVarKeyword.replace(KtPsiFactory(element.project).createVarKeyword())
                        }
                    }
                }
        )
    }

    private inline fun <reified TElement : KtElement, TIntention: JetSelfTargetingRangeIntention<TElement>> registerIntentionBasedProcessing(
            intention: TIntention,
            crossinline apply: TIntention.(TElement) -> Unit
    ) {
        //TODO: replace with optional argument when supported for inline functions
        return registerIntentionBasedProcessing<TElement, TIntention>(intention, { true }, apply)
    }

    private inline fun <reified TElement : KtElement, TIntention: JetSelfTargetingRangeIntention<TElement>> registerIntentionBasedProcessing(
            intention: TIntention,
            noinline additionalChecker: (TElement) -> Boolean,
            crossinline apply: TIntention.(TElement) -> Unit
    ) {
        _processings.add(object : J2kPostProcessing {
            override fun createAction(element: KtElement, diagnostics: Diagnostics): (() -> Unit)? {
                if (!javaClass<TElement>().isInstance(element)) return null
                val tElement = element as TElement
                if (intention.applicabilityRange(tElement) == null) return null
                if (!additionalChecker(tElement)) return null
                return { intention.apply(element) }
            }
        })
    }

    private inline fun <reified TElement : KtElement> registerDiagnosticBasedProcessing(
            diagnosticFactory: DiagnosticFactory<*>,
            crossinline fix: (TElement, Diagnostic) -> Unit
    ) {
        registerDiagnosticBasedProcessingFactory(diagnosticFactory) { element: TElement, diagnostic: Diagnostic -> { fix(element, diagnostic) } }
    }

    private inline fun <reified TElement : KtElement> registerDiagnosticBasedProcessingFactory(
            diagnosticFactory: DiagnosticFactory<*>,
            crossinline fixFactory: (TElement, Diagnostic) -> (() -> Unit)?
    ) {
        _processings.add(object : J2kPostProcessing {
            override fun createAction(element: KtElement, diagnostics: Diagnostics): (() -> Unit)? {
                if (!javaClass<TElement>().isInstance(element)) return null
                val diagnostic = diagnostics.forElement(element).firstOrNull { it.factory == diagnosticFactory } ?: return null
                return fixFactory(element as TElement, diagnostic)
            }
        })
    }

    private class RemoveExplicitTypeArgumentsProcessing : J2kPostProcessing {
        override fun createAction(element: KtElement, diagnostics: Diagnostics): (() -> Unit)? {
            if (element !is KtTypeArgumentList || !RemoveExplicitTypeArgumentsIntention.isApplicableTo(element, approximateFlexible = true)) return null

            return { element.delete() }
        }
    }

    private class RemoveRedundantOverrideVisibilityProcessing : J2kPostProcessing {
        override fun createAction(element: KtElement, diagnostics: Diagnostics): (() -> Unit)? {
            if (element !is KtCallableDeclaration || !element.hasModifier(KtTokens.OVERRIDE_KEYWORD)) return null
            val modifier = element.visibilityModifierType() ?: return null
            return { element.setVisibility(modifier) }
        }
    }

    private class MoveLambdaOutsideParenthesesProcessing : J2kPostProcessing {
        private val intention = MoveLambdaOutsideParenthesesIntention()

        override fun createAction(element: KtElement, diagnostics: Diagnostics): (() -> Unit)? {
            if (element !is KtCallExpression) return null
            val literalArgument = element.valueArguments.lastOrNull()?.getArgumentExpression()?.unpackFunctionLiteral() ?: return null
            if (!intention.isApplicableTo(element, literalArgument.textOffset)) return null
            return { intention.applyTo(element) }
        }
    }

    private class ConvertToStringTemplateProcessing : J2kPostProcessing {
        private val intention = ConvertToStringTemplateIntention()

        override fun createAction(element: KtElement, diagnostics: Diagnostics): (() -> Unit)? {
            if (element is KtBinaryExpression && intention.isApplicableTo(element) && intention.shouldSuggestToConvert(element)) {
                return { intention.applyTo(element) }
            }
            else {
                return null
            }
        }
    }

    private class UsePropertyAccessSyntaxProcessing : J2kPostProcessing {
        private val intention = UsePropertyAccessSyntaxIntention()

        override fun createAction(element: KtElement, diagnostics: Diagnostics): (() -> Unit)? {
            if (element !is KtCallExpression) return null
            val propertyName = intention.detectPropertyNameToUse(element) ?: return null
            return { intention.applyTo(element, propertyName) }
        }
    }

    private class RemoveRedundantSamAdaptersProcessing : J2kPostProcessing {
        override fun createAction(element: KtElement, diagnostics: Diagnostics): (() -> Unit)? {
            if (element !is KtCallExpression) return null

            val expressions = RedundantSamConstructorInspection.samConstructorCallsToBeConverted(element)
            if (expressions.isEmpty()) return null

            return {
                RedundantSamConstructorInspection.samConstructorCallsToBeConverted(element)
                        .forEach { RedundantSamConstructorInspection.replaceSamConstructorCall(it) }
            }
        }
    }

    private class AccessorBodyToExpression : J2kPostProcessing {
        private val intention = ConvertToExpressionBodyIntention()

        override fun createAction(element: KtElement, diagnostics: Diagnostics): (() -> Unit)? {
            if (element !is KtPropertyAccessor) return null
            if (!intention.isApplicableTo(element)) return null
            return { intention.applyTo(element, true) }
        }
    }
}