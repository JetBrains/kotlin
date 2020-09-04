/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.*
import org.jetbrains.kotlin.fir.analysis.checkers.expression.*
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

@RequiresOptIn
annotation class CheckersComponentInternal

class CheckersComponent : FirSessionComponent {
    val declarationCheckers: DeclarationCheckers get() = _declarationCheckers
    private val _declarationCheckers = ComposedDeclarationCheckers()

    val expressionCheckers: ExpressionCheckers get() = _expressionCheckers
    private val _expressionCheckers = ComposedExpressionCheckers()

    @SessionConfiguration
    @OptIn(CheckersComponentInternal::class)
    fun register(checkers: DeclarationCheckers) {
        _declarationCheckers.register(checkers)
    }

    @SessionConfiguration
    @OptIn(CheckersComponentInternal::class)
    fun register(checkers: ExpressionCheckers) {
        _expressionCheckers.register(checkers)
    }

    @SessionConfiguration
    fun register(checkers: FirAdditionalCheckersExtension) {
        register(checkers.declarationCheckers)
        register(checkers.expressionCheckers)
    }
}

val FirSession.checkersComponent: CheckersComponent by FirSession.sessionComponentAccessor()
