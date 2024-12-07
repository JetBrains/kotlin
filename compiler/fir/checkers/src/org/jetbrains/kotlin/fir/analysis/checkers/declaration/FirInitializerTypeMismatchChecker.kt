/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.checkTypeMismatch
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirComponentCall
import org.jetbrains.kotlin.fir.expressions.FirErrorExpression
import org.jetbrains.kotlin.fir.types.coneType

object FirInitializerTypeMismatchChecker : FirPropertyChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val initializer = declaration.initializer?.takeIf { it !is FirErrorExpression } ?: return
        val source = declaration.source ?: return
        if (source.elementType == KtNodeTypes.DESTRUCTURING_DECLARATION) return
        if (initializer is FirComponentCall) return
        if (declaration.returnTypeRef.source?.kind != KtRealSourceElementKind) return
        val propertyType = declaration.returnTypeRef.coneType

        checkTypeMismatch(propertyType, null, initializer, context, source, reporter, true)
    }
}
