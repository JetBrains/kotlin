/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.delegatedWrapperData
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.scopes.CallableCopyTypeCalculator
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.processAllFunctions
import org.jetbrains.kotlin.fir.scopes.processOverriddenFunctions
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.unwrapFakeOverrides

object FirImplementationByDelegationWithDifferentGenericSignatureChecker : FirClassChecker(MppCheckerKind.Platform) {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val classScope = declaration.unsubstitutedScope(context)
        classScope.processAllFunctions { symbol ->
            val delegatedWrapperData = symbol.delegatedWrapperData ?: return@processAllFunctions
            val wrappedGenericFunction = delegatedWrapperData.wrapped
            if (wrappedGenericFunction.typeParameters.isEmpty()) return@processAllFunctions
            val fieldScope = delegatedWrapperData.delegateField.symbol.resolvedInitializer?.resolvedType?.scope(
                context.session, context.scopeSession, CallableCopyTypeCalculator.DoNothing, null
            ) ?: return@processAllFunctions
            var reported = false
            val genericSymbolToCompare = wrappedGenericFunction.symbol.unwrapFakeOverrides()
            fieldScope.collectFunctionsByName(symbol.name).forEach { clashedSymbol ->
                if (reported || clashedSymbol.typeParameterSymbols.isNotEmpty()) return@forEach
                fieldScope.processOverriddenFunctions(clashedSymbol) { overriddenSymbol ->
                    if (overriddenSymbol.unwrapFakeOverrides() === genericSymbolToCompare) {
                        reporter.reportOn(
                            delegatedWrapperData.delegateField.returnTypeRef.source,
                            FirJvmErrors.IMPLEMENTATION_BY_DELEGATION_WITH_DIFFERENT_GENERIC_SIGNATURE,
                            wrappedGenericFunction.symbol,
                            clashedSymbol,
                            context
                        )
                        reported = true
                        ProcessorAction.STOP
                    } else {
                        ProcessorAction.NEXT
                    }
                }
            }
        }
    }
}
