/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.checkUpperBoundViolated
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol

object FirUpperBoundViolatedDeclarationChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        if (declaration is FirClass) {
            for (typeParameter in declaration.typeParameters) {
                if (typeParameter is FirTypeParameter) {
                    for (bound in typeParameter.bounds) {
                        checkUpperBoundViolated(bound)
                    }
                }
            }

            for (superTypeRef in declaration.superTypeRefs) {
                checkUpperBoundViolated(superTypeRef)
            }
        } else if (declaration is FirTypeAlias) {
            checkUpperBoundViolated(declaration.expandedTypeRef, isIgnoreTypeParameters = true)
        } else if (declaration is FirCallableDeclaration) {
            if (declaration.returnTypeRef.source?.kind !is KtFakeSourceElementKind) {
                checkUpperBoundViolated(
                    declaration.returnTypeRef,
                    isIgnoreTypeParameters = context.containingDeclarations.lastOrNull() is FirTypeAliasSymbol
                )
            }

            declaration.receiverParameter?.typeRef?.let { receiverTypeRef ->
                checkUpperBoundViolated(receiverTypeRef)
            }
        }
    }
}
