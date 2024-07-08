/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(CheckersCornerCase::class)

package org.jetbrains.kotlin.fir.analysis

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.analysis.checkers.LanguageVersionSettingsCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.CheckerSessionKind
import org.jetbrains.kotlin.fir.analysis.checkers.CheckersCornerCase
import org.jetbrains.kotlin.fir.analysis.checkers.config.ComposedLanguageVersionSettingsCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.ComposedDeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ComposedExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.type.ComposedTypeCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.type.TypeCheckers
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

@RequiresOptIn
annotation class CheckersComponentInternal

@NoMutableState
class CheckersComponent : FirSessionComponent {
    val commonDeclarationCheckers: DeclarationCheckers get() = _commonDeclarationCheckers
    val expectDeclarationCheckers: DeclarationCheckers get() = _expectDeclarationCheckers
    val platformDeclarationCheckers: DeclarationCheckers get() = _platformDeclarationCheckers

    private val _commonDeclarationCheckers = ComposedDeclarationCheckers(CheckerSessionKind.DeclarationSite)
    private val _expectDeclarationCheckers = ComposedDeclarationCheckers(CheckerSessionKind.DeclarationSiteForExpectsPlatformForOthers)
    private val _platformDeclarationCheckers = ComposedDeclarationCheckers(CheckerSessionKind.Platform)

    val commonExpressionCheckers: ExpressionCheckers get() = _commonExpressionCheckers
    val expectExpressionCheckers: ExpressionCheckers get() = _expectExpressionCheckers
    val platformExpressionCheckers: ExpressionCheckers get() = _platformExpressionCheckers

    private val _commonExpressionCheckers = ComposedExpressionCheckers(CheckerSessionKind.DeclarationSite)
    private val _expectExpressionCheckers = ComposedExpressionCheckers(CheckerSessionKind.DeclarationSiteForExpectsPlatformForOthers)
    private val _platformExpressionCheckers = ComposedExpressionCheckers(CheckerSessionKind.Platform)

    val commonTypeCheckers: TypeCheckers get() = _commonTypeCheckers
    val expectTypeCheckers: TypeCheckers get() = _expectTypeCheckers
    val platformTypeCheckers: TypeCheckers get() = _platformTypeCheckers

    private val _commonTypeCheckers = ComposedTypeCheckers(CheckerSessionKind.DeclarationSite)
    private val _expectTypeCheckers = ComposedTypeCheckers(CheckerSessionKind.DeclarationSiteForExpectsPlatformForOthers)
    private val _platformTypeCheckers = ComposedTypeCheckers(CheckerSessionKind.Platform)

    val languageVersionSettingsCheckers: LanguageVersionSettingsCheckers get() = _languageVersionSettingsCheckers
    private val _languageVersionSettingsCheckers = ComposedLanguageVersionSettingsCheckers()

    @SessionConfiguration
    @OptIn(CheckersComponentInternal::class)
    fun register(checkers: DeclarationCheckers) {
        _commonDeclarationCheckers.register(checkers)
        _expectDeclarationCheckers.register(checkers)
        _platformDeclarationCheckers.register(checkers)
    }

    @SessionConfiguration
    @OptIn(CheckersComponentInternal::class)
    fun register(checkers: ExpressionCheckers) {
        _commonExpressionCheckers.register(checkers)
        _expectExpressionCheckers.register(checkers)
        _platformExpressionCheckers.register(checkers)
    }

    @SessionConfiguration
    @OptIn(CheckersComponentInternal::class)
    fun register(checkers: TypeCheckers) {
        _commonTypeCheckers.register(checkers)
        _expectTypeCheckers.register(checkers)
        _platformTypeCheckers.register(checkers)
    }

    @SessionConfiguration
    @OptIn(CheckersComponentInternal::class)
    fun register(checkers: LanguageVersionSettingsCheckers) {
        _languageVersionSettingsCheckers.register(checkers)
    }

    @SessionConfiguration
    fun register(checkers: FirAdditionalCheckersExtension) {
        register(checkers.declarationCheckers)
        register(checkers.expressionCheckers)
        register(checkers.typeCheckers)
        register(checkers.languageVersionSettingsCheckers)
    }
}

val FirSession.checkersComponent: CheckersComponent by FirSession.sessionComponentAccessor()
