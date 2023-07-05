/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.utils

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.references.fe10.util.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor

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

        element.accept(object : KtTreeVisitorVoid() {
            override fun visitExpression(expression: KtExpression) {
                super.visitExpression(expression)

                val bindingContext = analysisContext.analyze(expression)
                val call = bindingContext.get(BindingContext.CALL, expression) ?: return
                val resolvedCall = bindingContext.get(BindingContext.RESOLVED_CALL, call)
                checkResolveCall(resolvedCall)
            }

            override fun visitDestructuringDeclaration(destructuringDeclaration: KtDestructuringDeclaration) {
                super.visitDestructuringDeclaration(destructuringDeclaration)

                val bindingContext = analysisContext.analyze(destructuringDeclaration)

                for (entry in destructuringDeclaration.entries) {
                    val resolvedCall = bindingContext.get(BindingContext.COMPONENT_RESOLVED_CALL, entry)
                    checkResolveCall(resolvedCall)
                }
            }

            override fun visitForExpression(expression: KtForExpression) {
                super.visitForExpression(expression)

                val bindingContext = analysisContext.analyze(expression)

                checkResolveCall(bindingContext.get(BindingContext.LOOP_RANGE_ITERATOR_RESOLVED_CALL, expression.loopRange))
                checkResolveCall(bindingContext.get(BindingContext.LOOP_RANGE_HAS_NEXT_RESOLVED_CALL, expression.loopRange))
                checkResolveCall(bindingContext.get(BindingContext.LOOP_RANGE_NEXT_RESOLVED_CALL, expression.loopRange))
            }

            private fun checkResolveCall(resolvedCall: ResolvedCall<*>?) {
                if (resolvedCall == null) return

                val descriptor = resolvedCall.resultingDescriptor
                if (descriptor is DeserializedSimpleFunctionDescriptor) return

                analyzeNextIfInline(descriptor)

                if (descriptor is PropertyDescriptor) {
                    for (accessor in descriptor.accessors) {
                        analyzeNextIfInline(accessor)
                    }
                }
            }

            private fun analyzeNextIfInline(descriptor: CallableDescriptor) {
                if (!InlineUtil.isInline(descriptor) || analyzeOnlyReifiedInlineFunctions && !hasReifiedTypeParameters(descriptor)) {
                    return
                }

                val declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor)
                if (declaration != null && declaration is KtDeclarationWithBody && !analyzedElements.contains(declaration)) {
                    nextInlineFunctions.add(declaration)
                    return
                }
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

    private fun hasReifiedTypeParameters(descriptor: CallableDescriptor): Boolean {
        return descriptor.typeParameters.any { it.isReified }
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