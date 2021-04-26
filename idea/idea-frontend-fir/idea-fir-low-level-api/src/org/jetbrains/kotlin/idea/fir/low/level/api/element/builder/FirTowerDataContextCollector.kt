/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.element.builder

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.FirTowerDataContext
import org.jetbrains.kotlin.idea.fir.low.level.api.util.originalDeclaration
import org.jetbrains.kotlin.psi.*

@ThreadSafeMutableState
class FirTowerDataContextCollector {
    private val state: MutableMap<KtElement, FirTowerDataContext> = hashMapOf()

    fun addFileContext(file: FirFile, context: FirTowerDataContext) {
        val ktFile = file.psi as? KtFile ?: return
        state[ktFile] = context
    }

    fun addStatementContext(statement: FirStatement, context: FirTowerDataContext) {
        val closestStatementInBlock = statement.psi?.closestBlockLevelOrInitializerExpression() ?: return
        state[closestStatementInBlock] = context
    }

    fun addDeclarationContext(declaration: FirDeclaration, context: FirTowerDataContext) {
        (declaration.psi as? KtElement)?.let { state[it] = context }
    }

    fun getContext(psi: KtElement): FirTowerDataContext? = state[psi]
}

fun FirTowerDataContextCollector.getClosestAvailableParentContext(element: KtElement): FirTowerDataContext? {
    var current: PsiElement? = element
    while (current != null) {
        if (current is KtElement) {
            getContext(current)?.let { return it }
        }
        if (current is KtDeclaration) {
            val originalDeclaration = current.originalDeclaration
            originalDeclaration?.let { getContext(it) }?.let { return it }
        }
        if (current is KtFile) {
            break
        }
        current = current.parent
    }
    return null
}

private tailrec fun PsiElement.closestBlockLevelOrInitializerExpression(): KtExpression? =
    when {
        this is KtExpression && (parent is KtBlockExpression || parent is KtDeclarationWithInitializer) -> this
        else -> parent?.closestBlockLevelOrInitializerExpression()
    }
