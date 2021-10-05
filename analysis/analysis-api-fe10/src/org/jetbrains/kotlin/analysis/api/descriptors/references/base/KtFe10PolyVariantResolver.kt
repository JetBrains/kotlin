/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.references.base

import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.ResolveCache
import org.jetbrains.kotlin.analysis.api.analyse
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.impl.base.util.runInPossiblyEdtThread
import org.jetbrains.kotlin.analysis.api.tokens.HackToForceAllowRunningAnalyzeOnEDT
import org.jetbrains.kotlin.idea.references.AbstractKtReference
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.load.kotlin.toSourceElement
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.source.getPsi

object KtFe10PolyVariantResolver : ResolveCache.PolyVariantResolver<KtReference> {
    @OptIn(HackToForceAllowRunningAnalyzeOnEDT::class)
    override fun resolve(reference: KtReference, incompleteCode: Boolean): Array<ResolveResult> {
        require(reference is AbstractKtReference<*>) { "reference should be AbstractKtReference, but was ${reference::class}" }
        return runInPossiblyEdtThread {
            val expression = reference.expression
            analyse(reference.expression) {
                val session = this as KtFe10AnalysisSession
                val bindingContext = session.analyze(expression, KtFe10AnalysisSession.AnalysisMode.PARTIAL)
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