/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.platform

import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.type.TypeCheckers
import org.jetbrains.kotlin.fir.analysis.js.checkers.JsDeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.js.checkers.JsExpressionCheckers

internal object LLJsCheckersConfiguration : LLPlatformCheckersConfiguration {
    override val declarationCheckers: List<DeclarationCheckers> = listOf(JsDeclarationCheckers)
    override val expressionCheckers: List<ExpressionCheckers> = listOf(JsExpressionCheckers)
    override val typeCheckers: List<TypeCheckers> = emptyList()
}
