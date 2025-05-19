/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.components.KaUseSiteVisibilityChecker
import org.jetbrains.kotlin.analysis.api.components.KaVisibilityChecker
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getSymbolDescriptor
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.getResolutionScope
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.components.withPsiValidityAssertion
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.DescriptorVisibilityUtils.isVisible
import org.jetbrains.kotlin.descriptors.DescriptorVisibilityUtils.isVisibleWithAnyReceiver
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

internal class KaFe10VisibilityChecker(
    override val analysisSessionProvider: () -> KaFe10Session,
) : KaBaseSessionComponent<KaFe10Session>(), KaVisibilityChecker, KaFe10SessionComponent {
    override fun createUseSiteVisibilityChecker(
        useSiteFile: KaFileSymbol,
        receiverExpression: KtExpression?,
        position: PsiElement,
    ): KaUseSiteVisibilityChecker = withPsiValidityAssertion(receiverExpression, position) {
        KaFe10UseSiteVisibilityChecker(receiverExpression, position, analysisContext, token)
    }

    override fun KaCallableSymbol.isVisibleInClass(classSymbol: KaClassSymbol): Boolean = withValidityAssertion {
        val memberDescriptor = getSymbolDescriptor(this) as? DeclarationDescriptorWithVisibility ?: return false
        val classDescriptor = getSymbolDescriptor(classSymbol) ?: return false
        return isVisibleWithAnyReceiver(memberDescriptor, classDescriptor, analysisSession.analysisContext.languageVersionSettings)
    }

    override fun isPublicApi(symbol: KaDeclarationSymbol): Boolean = withValidityAssertion {
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


// This implementation is not optimized for multiple invocations at the same use-site.
private class KaFe10UseSiteVisibilityChecker(
    private val receiverExpression: KtExpression?,
    private val position: PsiElement,
    private val analysisContext: Fe10AnalysisContext,
    override val token: KaLifetimeToken,
) : KaUseSiteVisibilityChecker {
    override fun isVisible(candidateSymbol: KaDeclarationSymbol): Boolean = withValidityAssertion {
        if (candidateSymbol.visibility == KaSymbolVisibility.PUBLIC) {
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
}