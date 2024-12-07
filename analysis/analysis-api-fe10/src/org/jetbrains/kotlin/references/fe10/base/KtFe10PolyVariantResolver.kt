/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.references.fe10.base

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.ResolveCache
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.idea.references.AbstractKtReference
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.references.fe10.util.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.resolve.BindingContext
import java.util.*

internal object KtFe10PolyVariantResolver : ResolveCache.PolyVariantResolver<KtReference> {
    class KotlinResolveResult(element: PsiElement) : PsiElementResolveResult(element)

    private fun resolveToPsiElements(ref: KtFe10Reference): Collection<PsiElement> {
        require(ref is AbstractKtReference<*>) { "reference should be AbstractKtReference, but was ${ref::class}" }
        val bindingContext = KtFe10ReferenceResolutionHelper.getInstance().partialAnalyze(ref.expression)
        if (bindingContext == BindingContext.EMPTY) return emptySet()
        return resolveToPsiElements(ref, bindingContext, ref.getTargetDescriptors(bindingContext))
    }

    private fun resolveToPsiElements(
        ref: KtFe10Reference,
        context: BindingContext,
        targetDescriptors: Collection<DeclarationDescriptor>
    ): Collection<PsiElement> {
        if (targetDescriptors.isNotEmpty()) {
            return targetDescriptors.flatMap { target -> resolveToPsiElements(ref, target) }.toSet()
        }

        val labelTargets = getLabelTargets(ref, context)
        if (labelTargets != null) {
            return labelTargets
        }

        return Collections.emptySet()
    }

    private fun resolveToPsiElements(
        ref: KtFe10Reference,
        targetDescriptor: DeclarationDescriptor
    ): Collection<PsiElement> = if (targetDescriptor is PackageViewDescriptor) {
        val psiFacade = JavaPsiFacade.getInstance(ref.element.project)
        val fqName = targetDescriptor.fqName.asString()
        listOfNotNull(psiFacade.findPackage(fqName))
    } else {
        DescriptorToSourceUtilsIde.getAllDeclarations(ref.element.project, targetDescriptor, ref.element.resolveScope)
    }

    private fun getLabelTargets(ref: KtFe10Reference, context: BindingContext): Collection<PsiElement>? {
        val reference = ref.element as? KtReferenceExpression ?: return null
        val labelTarget = context[BindingContext.LABEL_TARGET, reference]
        if (labelTarget != null) {
            return listOf(labelTarget)
        }

        return context[BindingContext.AMBIGUOUS_LABEL_TARGET, reference]
    }

    // TODO: Old impl from IDE side has this logic wrapped inside runWithCancellationCheck. Figure out what we should do here.
    override fun resolve(ref: KtReference, incompleteCode: Boolean): Array<ResolveResult> {
        require(ref is KtFe10Reference) { "reference should be KtFe10Reference, but was ${ref::class}" }
        val resolveToPsiElements = resolveToPsiElements(ref)
        return resolveToPsiElements.map { KotlinResolveResult(it) }.toTypedArray()
    }
}