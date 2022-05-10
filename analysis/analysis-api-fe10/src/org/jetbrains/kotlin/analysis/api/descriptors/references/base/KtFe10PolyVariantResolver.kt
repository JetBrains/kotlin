/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.references.base

import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.ResolveCache
import org.jetbrains.kotlin.analysis.api.KtAllowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.lifetime.allowAnalysisOnEdt
import org.jetbrains.kotlin.idea.references.AbstractKtReference
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.load.kotlin.toSourceElement
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.source.getPsi

internal interface CliKtFe10Reference : KtReference {
    override val resolver: ResolveCache.PolyVariantResolver<KtReference>
        get() = KtFe10PolyVariantResolver
}

object KtFe10PolyVariantResolver : ResolveCache.PolyVariantResolver<KtReference> {
    @OptIn(KtAllowAnalysisOnEdt::class)
    override fun resolve(reference: KtReference, incompleteCode: Boolean): Array<ResolveResult> {
        require(reference is AbstractKtReference<*>) { "reference should be AbstractKtReference, but was ${reference::class}" }
        return allowAnalysisOnEdt {
            val expression = reference.expression
            analyze(reference.expression) {
                val analysisSession = this as KtFe10AnalysisSession
                val bindingContext = analysisSession.analysisContext.analyze(expression, AnalysisMode.PARTIAL)
                val descriptor = when (expression) {
                    is KtReferenceExpression -> bindingContext[BindingContext.REFERENCE_TARGET, expression]
                    else -> expression.getResolvedCall(bindingContext)?.resultingDescriptor
                }
                val source = descriptor?.toSourceElement?.getPsi()
                if (source != null) arrayOf(PsiElementResolveResult(source)) else emptyArray()
            }
        }
    }
}