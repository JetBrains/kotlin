/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility

object FirActualDeclarationChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirMemberDeclaration || !declaration.isActual) return

        if (declaration is FirCallableDeclaration) {
            val actualFunctionSymbol = declaration.symbol
            val expectFunctionSymbol = actualFunctionSymbol.getSingleCompatibleExpectForActualOrNull() as? FirCallableSymbol ?: return

            val expectTypeParameters = expectFunctionSymbol.getContainingClassSymbol(expectFunctionSymbol.moduleData.session)
                ?.typeParameterSymbols.orEmpty()
            val actualClassTypeParameters = actualFunctionSymbol.getContainingClassSymbol(context.session)?.typeParameterSymbols.orEmpty()
            val parentSubstitutor =
                createExpectActualTypeParameterSubstitutor(expectTypeParameters, actualClassTypeParameters, context.session)

            val substitutor = createExpectActualTypeParameterSubstitutor(
                expectFunctionSymbol.typeParameterSymbols,
                actualFunctionSymbol.typeParameterSymbols,
                context.session,
                parentSubstitutor
            )

            if (!areCompatibleExpectActualTypes(
                    substitutor.substituteOrSelf(expectFunctionSymbol.resolvedReturnType.type),
                    actualFunctionSymbol.resolvedReturnType.type,
                    expectFunctionSymbol.moduleData.session,
                    context.session
                )
            ) {
                @Suppress("UNCHECKED_CAST")
                reporter.reportOn(
                    declaration.source,
                    FirErrors.ACTUAL_WITHOUT_EXPECT,
                    actualFunctionSymbol,
                    actualFunctionSymbol.expectForActual as Map<ExpectActualCompatibility.Incompatible<FirBasedSymbol<*>>, Collection<FirBasedSymbol<*>>>,
                    context
                )
            }
        }
    }
}