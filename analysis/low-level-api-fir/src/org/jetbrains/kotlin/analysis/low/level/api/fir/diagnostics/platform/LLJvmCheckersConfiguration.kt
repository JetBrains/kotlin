/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.platform

import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.type.TypeCheckers
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.JvmDeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.JvmExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.JvmTypeCheckers

internal object LLJvmCheckersConfiguration : LLPlatformCheckersConfiguration {
    override val declarationCheckers: List<DeclarationCheckers> = listOf(JvmDeclarationCheckers)
    override val expressionCheckers: List<ExpressionCheckers> = listOf(JvmExpressionCheckers)
    override val typeCheckers: List<TypeCheckers> = listOf(JvmTypeCheckers)
}
