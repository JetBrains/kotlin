/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.analysis.checkers.LanguageVersionSettingsCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
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
    val platformDeclarationCheckers: DeclarationCheckers get() = _platformDeclarationCheckers

    private val _commonDeclarationCheckers = ComposedDeclarationCheckers(MppCheckerKind.Common)
    private val _platformDeclarationCheckers = ComposedDeclarationCheckers(MppCheckerKind.Platform)

    val commonExpressionCheckers: ExpressionCheckers get() = _commonExpressionCheckers
    val platformExpressionCheckers: ExpressionCheckers get() = _platformExpressionCheckers

    private val _commonExpressionCheckers = ComposedExpressionCheckers(MppCheckerKind.Common)
    private val _platformExpressionCheckers = ComposedExpressionCheckers(MppCheckerKind.Platform)

    val commonTypeCheckers: TypeCheckers get() = _commonTypeCheckers
    val platformTypeCheckers: TypeCheckers get() = _platformTypeCheckers

    private val _commonTypeCheckers = ComposedTypeCheckers(MppCheckerKind.Common)
    private val _platformTypeCheckers = ComposedTypeCheckers(MppCheckerKind.Platform)

    val languageVersionSettingsCheckers: LanguageVersionSettingsCheckers get() = _languageVersionSettingsCheckers
    private val _languageVersionSettingsCheckers = ComposedLanguageVersionSettingsCheckers()

    @SessionConfiguration
    @OptIn(CheckersComponentInternal::class)
    fun register(checkers: DeclarationCheckers) {
        _commonDeclarationCheckers.register(checkers)
        _platformDeclarationCheckers.register(checkers)
    }

    @SessionConfiguration
    @OptIn(CheckersComponentInternal::class)
    fun register(checkers: ExpressionCheckers) {
        _commonExpressionCheckers.register(checkers)
        _platformExpressionCheckers.register(checkers)
    }

    @SessionConfiguration
    @OptIn(CheckersComponentInternal::class)
    fun register(checkers: TypeCheckers) {
        _commonTypeCheckers.register(checkers)
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
