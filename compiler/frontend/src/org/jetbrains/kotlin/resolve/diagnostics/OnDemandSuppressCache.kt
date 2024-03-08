/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.resolve.BindingContext

class OnDemandSuppressCache(private val context: BindingContext) : KotlinSuppressCache(context.project) {
    private val processedRoots = mutableSetOf<KtFile>()

    private val storage = mutableMapOf<PsiElement, List<AnnotationDescriptor>>()

    @Synchronized
    private fun ensureRootProcessed(rootElement: PsiElement) {
        require(rootElement is KtFile)
        if (!processedRoots.contains(rootElement)) {
            val visitor = PrecomputingVisitor(storage, BindingContextSuppressCache(context))
            rootElement.accept(visitor, null)
            processedRoots.add(rootElement)
        }
    }

    private class PrecomputingVisitor(
        val storage: MutableMap<PsiElement, List<AnnotationDescriptor>>,
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

    override fun getSuppressionAnnotations(annotated: PsiElement): List<AnnotationDescriptor> =
        storage[annotated].orEmpty()

    override fun getClosestAnnotatedAncestorElement(element: PsiElement, rootElement: PsiElement, excludeSelf: Boolean): PsiElement? {
        ensureRootProcessed(rootElement)
        return super.getClosestAnnotatedAncestorElement(element, rootElement, excludeSelf)
    }
}
