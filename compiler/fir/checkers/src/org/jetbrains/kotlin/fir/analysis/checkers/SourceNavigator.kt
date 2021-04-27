/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirLightSourceElement
import org.jetbrains.kotlin.fir.FirPsiSourceElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.diagnostics.getAncestors
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.psi.KtConstructorCalleeExpression
import org.jetbrains.kotlin.psi.KtTypeReference

/**
 * Service to answer source-related questions in generic fashion.
 * Shouldn't expose (receive or return) any specific source tree types
 */
interface SourceNavigator {

    fun FirTypeRef.isInConstructorCallee(): Boolean

    fun FirTypeRef.isInTypeConstraint(): Boolean

    companion object {

        private val lightTreeInstance = LightTreeSourceNavigator()

        fun forElement(e: FirElement): SourceNavigator = when (e.source) {
            is FirLightSourceElement -> lightTreeInstance
            is FirPsiSourceElement<*> -> PsiSourceNavigator
            null -> lightTreeInstance //shouldn't matter
        }

        inline fun <R> FirElement.withNavigator(block: SourceNavigator.() -> R): R = with(forElement(this), block)
    }
}

open class LightTreeSourceNavigator : SourceNavigator {

    private fun <T> FirElement.withSource(f: (FirSourceElement) -> T): T? =
        source?.let { f(it) }

    override fun FirTypeRef.isInConstructorCallee(): Boolean = withSource { source ->
        source.treeStructure.getParent(source.lighterASTNode)?.tokenType == KtNodeTypes.CONSTRUCTOR_CALLEE
    } ?: false

    override fun FirTypeRef.isInTypeConstraint(): Boolean {
        val source = source ?: return false
        return source.treeStructure.getAncestors(source.lighterASTNode)
            .find { it.tokenType == KtNodeTypes.TYPE_CONSTRAINT || it.tokenType == KtNodeTypes.TYPE_PARAMETER }
            ?.tokenType == KtNodeTypes.TYPE_CONSTRAINT
    }
}

//by default psi tree can reuse light tree manipulations
object PsiSourceNavigator : LightTreeSourceNavigator() {

    //Swallows incorrect casts!!!
    private inline fun <reified P : PsiElement> FirElement.psi(): P? {
        val psi = (source as? FirPsiSourceElement<*>)?.psi
        return psi as? P
    }

    override fun FirTypeRef.isInConstructorCallee(): Boolean = psi<KtTypeReference>()?.parent is KtConstructorCalleeExpression
}
