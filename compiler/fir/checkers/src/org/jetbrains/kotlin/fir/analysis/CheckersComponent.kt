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

/**
 * [CheckersComponent] stores various kinds of checkers in the CLI compiler mode. In the Analysis API mode, this component is not
 * registered, as `LLCheckersFactory` is used instead.
 */
@NoMutableState
class CheckersComponent : FirSessionComponent {
    val commonDeclarationCheckers: DeclarationCheckers
        field = ComposedDeclarationCheckers(MppCheckerKind.Common)
    val platformDeclarationCheckers: DeclarationCheckers
        field = ComposedDeclarationCheckers(MppCheckerKind.Platform)

    val commonExpressionCheckers: ExpressionCheckers
        field = ComposedExpressionCheckers(MppCheckerKind.Common)
    val platformExpressionCheckers: ExpressionCheckers
        field = ComposedExpressionCheckers(MppCheckerKind.Platform)

    val commonTypeCheckers: TypeCheckers
        field = ComposedTypeCheckers(MppCheckerKind.Common)
    val platformTypeCheckers: TypeCheckers
        field = ComposedTypeCheckers(MppCheckerKind.Platform)

    val languageVersionSettingsCheckers: LanguageVersionSettingsCheckers
        field = ComposedLanguageVersionSettingsCheckers()

    @SessionConfiguration
    @OptIn(CheckersComponentInternal::class)
    fun register(checkers: DeclarationCheckers) {
        commonDeclarationCheckers.register(checkers)
        platformDeclarationCheckers.register(checkers)
    }

    @SessionConfiguration
    @OptIn(CheckersComponentInternal::class)
    fun register(checkers: ExpressionCheckers) {
        commonExpressionCheckers.register(checkers)
        platformExpressionCheckers.register(checkers)
    }

    @SessionConfiguration
    @OptIn(CheckersComponentInternal::class)
    fun register(checkers: TypeCheckers) {
        commonTypeCheckers.register(checkers)
        platformTypeCheckers.register(checkers)
    }

    @SessionConfiguration
    @OptIn(CheckersComponentInternal::class)
    fun register(checkers: LanguageVersionSettingsCheckers) {
        languageVersionSettingsCheckers.register(checkers)
    }

    @SessionConfiguration
    fun register(checkers: FirAdditionalCheckersExtension) {
        register(checkers.declarationCheckers)
        register(checkers.expressionCheckers)
        register(checkers.typeCheckers)
        register(checkers.languageVersionSettingsCheckers)
    }
}

val FirSession.nullableCheckersComponent: CheckersComponent? by FirSession.nullableSessionComponentAccessor()

val FirSession.checkersComponent: CheckersComponent
    get() = nullableCheckersComponent ?: error("Expected `${CheckersComponent::class}` to be registered in CLI compiler mode.")
