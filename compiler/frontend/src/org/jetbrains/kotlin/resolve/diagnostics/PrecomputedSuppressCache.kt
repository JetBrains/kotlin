/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.diagnostics

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.resolve.BindingContext

/**
 * A [KotlinSuppressCache] implementation that computes all suppressions at the moment of instantiation.
 * This is useful in the IR backend, where we clear the main binding context after psi2ir to avoid taking extra memory.
 * To make suppression of errors reported from backend possible though, we need to precompute all resolved `@Suppress` annotations,
 * and store this information outside of the binding context, which is going to be cleared.
 *
 * @param context the binding context where the data should be loaded from. Note that it shouldn't be stored as a property because the
 *   primary use case of this class is when that binding context is cleared and thus is useless after a certain point.
 */
class PrecomputedSuppressCache(context: BindingContext, files: List<KtFile>) : KotlinSuppressCache() {
    val storage: Map<KtAnnotated, List<AnnotationDescriptor>> =
        mutableMapOf<KtAnnotated, List<AnnotationDescriptor>>().also { storage ->
            val visitor = PrecomputingVisitor(storage, BindingContextSuppressCache(context))
            for (file in files) {
                file.accept(visitor, null)
            }
        }

    private class PrecomputingVisitor(
        val storage: MutableMap<KtAnnotated, List<AnnotationDescriptor>>,
        val computer: KotlinSuppressCache,
    ) : KtTreeVisitorVoid() {
        override fun visitKtElement(element: KtElement) {
            super.visitKtElement(element)
            if (element is KtAnnotated) {
                computeAnnotations(element)
            }
        }

        override fun visitKtFile(file: KtFile) {
            super.visitKtFile(file)
            computeAnnotations(file)
        }

        private fun computeAnnotations(element: KtAnnotated) {
            val suppressions = computer.getSuppressionAnnotations(element).filter { it.fqName == StandardNames.FqNames.suppress }
            if (suppressions.isNotEmpty()) {
                storage[element] = suppressions
            }
        }
    }

    override fun getSuppressionAnnotations(annotated: KtAnnotated): List<AnnotationDescriptor> =
        storage[annotated].orEmpty()
}
