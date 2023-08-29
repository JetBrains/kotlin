/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.utils

import org.jetbrains.kotlin.analysis.api.KtAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext

@OptIn(KtAnalysisNonPublicApi::class) // used in IDEA K1 evaluator
@Suppress("unused")
fun KtAnalysisSession.getInlineFunctionAnalyzer(analyzeOnlyReifiedInlineFunctions: Boolean): InlineFunctionAnalyzer {
    require(this is KtFe10AnalysisSession) {
        "K2 implementation shouldn't call this code"
    }
    return InlineFunctionAnalyzer(analysisContext, analyzeOnlyReifiedInlineFunctions)
}

@OptIn(KtAnalysisNonPublicApi::class)
class InlineFunctionAnalyzer(
    private val analysisContext: Fe10AnalysisContext,
    private val analyzeOnlyReifiedInlineFunctions: Boolean,
) {
    private val analyzedElements: MutableSet<KtElement> = mutableSetOf()
    private val inlineFunctionsWithBody: MutableSet<KtDeclarationWithBody> = mutableSetOf()

    /**
     * Collects all inline function calls in an [element] (usually a file) and follows each transitively.
     */
    fun analyze(element: KtElement) {
        val project = element.project
        val nextInlineFunctions = HashSet<KtDeclarationWithBody>()
        val collector = InlineFunctionsCollector(project, analyzeOnlyReifiedInlineFunctions) { declaration ->
            if (!analyzedElements.contains(declaration)) {
                nextInlineFunctions.add(declaration)
            }
        }
        val propertyAccessor = InlineDelegatedPropertyAccessorsAnalyzer(analysisContext, collector)

        element.accept(object : KtTreeVisitorVoid() {
            override fun visitExpression(expression: KtExpression) {
                super.visitExpression(expression)

                val bindingContext = analysisContext.analyze(expression)
                val call = bindingContext.get(BindingContext.CALL, expression) ?: return
                val resolvedCall = bindingContext.get(BindingContext.RESOLVED_CALL, call)
                collector.checkResolveCall(resolvedCall)
            }

            override fun visitDestructuringDeclaration(destructuringDeclaration: KtDestructuringDeclaration) {
                super.visitDestructuringDeclaration(destructuringDeclaration)

                val bindingContext = analysisContext.analyze(destructuringDeclaration)

                for (entry in destructuringDeclaration.entries) {
                    val resolvedCall = bindingContext.get(BindingContext.COMPONENT_RESOLVED_CALL, entry)
                    collector.checkResolveCall(resolvedCall)
                }
            }

            override fun visitForExpression(expression: KtForExpression) {
                super.visitForExpression(expression)

                val bindingContext = analysisContext.analyze(expression)

                collector.checkResolveCall(bindingContext.get(BindingContext.LOOP_RANGE_ITERATOR_RESOLVED_CALL, expression.loopRange))
                collector.checkResolveCall(bindingContext.get(BindingContext.LOOP_RANGE_HAS_NEXT_RESOLVED_CALL, expression.loopRange))
                collector.checkResolveCall(bindingContext.get(BindingContext.LOOP_RANGE_NEXT_RESOLVED_CALL, expression.loopRange))
            }

            override fun visitProperty(property: KtProperty) {
                super.visitProperty(property)
                propertyAccessor.visitProperty(property)
            }
        })

        analyzedElements.add(element)

        if (nextInlineFunctions.isNotEmpty()) {
            for (inlineFunction in nextInlineFunctions) {
                if (inlineFunction.bodyExpression != null) {
                    inlineFunctionsWithBody.add(inlineFunction)
                    analyze(inlineFunction)
                }
            }
            analyzedElements.addAll(nextInlineFunctions)
        }
    }

    /**
     * Returns the list of files that contain all reached inline functions.
     */
    fun allFiles(): List<KtFile> = analyzedElements.mapTo(mutableSetOf()) { it.containingKtFile }.toList()

    /**
     * Returns the set of [KtObjectDeclaration]s which are defined as an object literal in one of the reached inline functions.
     */
    fun inlineObjectDeclarations(): Set<KtObjectDeclaration> {
        val results = mutableSetOf<KtObjectDeclaration>()

        inlineFunctionsWithBody.forEach { inlineFunction ->
            val body = inlineFunction.bodyExpression ?: return@forEach
            body.accept(object : KtTreeVisitorVoid() {
                override fun visitObjectLiteralExpression(expression: KtObjectLiteralExpression) {
                    super.visitObjectLiteralExpression(expression)
                    results.add(expression.objectDeclaration)
                }
            })
        }

        return results
    }
}