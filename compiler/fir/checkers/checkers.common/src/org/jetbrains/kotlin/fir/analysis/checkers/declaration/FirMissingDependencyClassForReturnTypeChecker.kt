/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.checkMissingDependencySuperTypes
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirMissingDependencyClassProxy
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.types.coneType

object FirMissingDependencyClassForReturnTypeChecker : FirCallableDeclarationChecker(Common), FirMissingDependencyClassProxy {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirCallableDeclaration) {
        if (declaration is FirValueParameter) return // Handled by FirMissingDependencyClassForParameterChecker
        if (!isNonMetadataKmpSeparateCompilation()) return
        checkMissingDependencySuperTypes(declaration.returnTypeRef.coneType, declaration.returnTypeRef.source)
    }
}
