/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.element.builder

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.FirTowerDataContext
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.util.originalDeclaration
import org.jetbrains.kotlin.psi.*

interface FirTowerContextProvider {
    fun getClosestAvailableParentContext(ktElement: KtElement): FirTowerDataContext?
}

internal class FileTowerProvider(
    private val file: KtFile,
    private val context: FirTowerDataContext
) : FirTowerContextProvider {
    override fun getClosestAvailableParentContext(ktElement: KtElement): FirTowerDataContext? =
        if (file == ktElement.containingKtFile) context else null
}

internal class FirTowerDataContextAllElementsCollector : FirTowerDataContextCollector, FirTowerContextProvider {
    private val elementsToContext: MutableMap<KtElement, FirTowerDataContext> = hashMapOf()

    override fun addFileContext(file: FirFile, context: FirTowerDataContext) {
        val ktFile = file.psi as? KtFile ?: return
        elementsToContext[ktFile] = context
    }

    override fun addStatementContext(statement: FirStatement, context: FirTowerDataContext) {
        val closestStatementInBlock = statement.psi?.closestBlockLevelOrInitializerExpression() ?: return
        elementsToContext[closestStatementInBlock] = context
    }

    override fun addDeclarationContext(declaration: FirDeclaration, context: FirTowerDataContext) {
        val psi = declaration.psi as? KtElement ?: return
        elementsToContext[psi] = context
    }

    override fun getClosestAvailableParentContext(ktElement: KtElement): FirTowerDataContext? {
        var current: PsiElement? = ktElement
        while (current != null) {
            if (current is KtElement) {
                elementsToContext[current]?.let { return it }
            }
            if (current is KtDeclaration) {
                val originalDeclaration = current.originalDeclaration
                originalDeclaration?.let { elementsToContext[it] }?.let { return it }
            }
            if (current is KtFile) {
                break
            }
            current = current.parent
        }
        return null
    }
}

private tailrec fun PsiElement.closestBlockLevelOrInitializerExpression(): KtExpression? =
    when {
        this is KtExpression && (parent is KtBlockExpression || parent is KtDeclarationWithInitializer) -> this
        else -> parent?.closestBlockLevelOrInitializerExpression()
    }
