/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.element.builder

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.FirTowerDataContext
import org.jetbrains.kotlin.psi.KtElement

class FirTowerDataContextCollector {
    private val state: MutableMap<KtElement, FirTowerDataContext> = mutableMapOf()

    fun addStatementContext(statement: FirStatement, context: FirTowerDataContext) {
        (statement.psi as? KtElement)?.let { state[it] = context }
    }

    fun addDeclarationContext(declaration: FirDeclaration, context: FirTowerDataContext) {
        (declaration.psi as? KtElement)?.let { state[it] = context }
    }

    fun getContext(psi: KtElement): FirTowerDataContext? = state[psi]
}

fun FirTowerDataContextCollector.getClosestAvailableParentContext(element: KtElement): FirTowerDataContext? {
    var current: KtElement = element
    while (true) {
        getContext(current)?.let { return it }
        current = current.parent as? KtElement ?: return null
    }
}
