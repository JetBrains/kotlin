/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.checkMissingDependencySuperTypes
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.collectUpperBounds
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRefsOwner
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.types.typeContext

/**
 * @see org.jetbrains.kotlin.resolve.checkers.MissingDependencySupertypeChecker
 */
object FirMissingDependencySupertypeInDeclarationsChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        if (declaration is FirClass) {
            checkMissingDependencySuperTypes(declaration.symbol, declaration.source, isEagerCheck = false)
        }

        if (declaration is FirTypeParameterRefsOwner) {
            for (typeParameter in declaration.typeParameters) {
                for (upperBound in typeParameter.toConeType().collectUpperBounds(context.session.typeContext)) {
                    checkMissingDependencySuperTypes(upperBound, typeParameter.source)
                }
            }
        }
    }
}
