/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.components.KtVisibilityChecker
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getSymbolDescriptor
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.getResolutionScope
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.DescriptorVisibilityUtils.isVisible
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyPublicApi
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.descriptorUtil.isPublishedApi
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.utils.getImplicitReceiversHierarchy

internal class KtFe10VisibilityChecker(
    override val analysisSession: KtFe10AnalysisSession
) : KtVisibilityChecker(), Fe10KtAnalysisSessionComponent {
    override val token: KtLifetimeToken
        get() = analysisSession.token

    override fun isVisible(
        candidateSymbol: KtSymbolWithVisibility,
        useSiteFile: KtFileSymbol,
        position: PsiElement,
        receiverExpression: KtExpression?
    ): Boolean {
        if (candidateSymbol.visibility == Visibilities.Public) {
            return true
        }

        val targetDescriptor = getSymbolDescriptor(candidateSymbol) as? DeclarationDescriptorWithVisibility ?: return false

        val useSiteDeclaration = findContainingNonLocalDeclaration(position) ?: return false
        val bindingContextForUseSite = analysisContext.analyze(useSiteDeclaration)
        val useSiteDescriptor = bindingContextForUseSite[BindingContext.DECLARATION_TO_DESCRIPTOR, useSiteDeclaration] ?: return false

        if (receiverExpression != null && !targetDescriptor.isExtension) {
            val bindingContext = analysisContext.analyze(receiverExpression, AnalysisMode.PARTIAL)
            val receiverType = bindingContext.getType(receiverExpression) ?: return false
            val explicitReceiver = ExpressionReceiver.create(receiverExpression, receiverType, bindingContext)
            return isVisible(
                explicitReceiver,
                targetDescriptor,
                useSiteDescriptor,
                analysisContext.languageVersionSettings
            )
        } else {
            val bindingContext = analysisContext.analyze(useSiteDeclaration, AnalysisMode.FULL)

            val lexicalScope = position.getResolutionScope(bindingContext)
            if (lexicalScope != null) {
                return lexicalScope.getImplicitReceiversHierarchy().any {
                    isVisible(it.value, targetDescriptor, useSiteDescriptor, analysisContext.languageVersionSettings)
                }
            }
        }

        return false
    }

    override fun isPublicApi(symbol: KtSymbolWithVisibility): Boolean {
        val descriptor = getSymbolDescriptor(symbol) as? DeclarationDescriptorWithVisibility ?: return false
        return descriptor.isEffectivelyPublicApi || descriptor.isPublishedApi()
    }
}

private fun findContainingNonLocalDeclaration(element: PsiElement): KtCallableDeclaration? {
    for (parent in element.parentsWithSelf) {
        if (parent is KtCallableDeclaration && !KtPsiUtil.isLocal(parent)) {
            return parent
        }
    }

    return null
}
