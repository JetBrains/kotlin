/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.config.isValhallaSupportEnabled
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.isFullValueClass
import org.jetbrains.kotlin.fir.declarations.utils.sourceElement
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.load.kotlin.FileBasedKotlinClass
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.load.kotlin.isValueClassCompiledAsIdentityClass

/**
 * A Kotlin value class compiled without Valhalla value classes is an identity class in its class file. A Valhalla value class can't extend
 * it, so a value class extending it is compiled as an identity class too, with a warning.
 */
object FirJvmValueClassIdentitySupertypeChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        if (!declaration.symbol.isFullValueClass || !context.languageVersionSettings.isValhallaSupportEnabled()) return
        for (supertypeRef in declaration.superTypeRefs) {
            val supertypeSymbol = supertypeRef.toRegularClassSymbol(context.session) ?: continue
            if (supertypeSymbol.isFullValueClass && supertypeSymbol.isCompiledAsIdentityClass(context.session)) {
                reporter.reportOn(
                    supertypeRef.source,
                    FirJvmErrors.VALUE_CLASS_EXTENDS_VALUE_CLASS_COMPILED_AS_IDENTITY_CLASS,
                    declaration.symbol,
                    supertypeRef.coneType,
                )
            }
        }
    }

    private fun FirRegularClassSymbol.isCompiledAsIdentityClass(session: FirSession): Boolean {
        @OptIn(SymbolInternals::class)
        val binaryClass = (fir.sourceElement as? KotlinJvmBinarySourceElement)?.binaryClass as? FileBasedKotlinClass
        val isValhallaSupportEnabled = moduleData.session.languageVersionSettings.isValhallaSupportEnabled()
        return isValueClassCompiledAsIdentityClass(binaryClass, isValhallaSupportEnabled) {
            resolvedSuperTypes.any { supertype ->
                val supertypeSymbol = supertype.toRegularClassSymbol(session)
                supertypeSymbol != null && supertypeSymbol.isFullValueClass && supertypeSymbol.isCompiledAsIdentityClass(session)
            }
        }
    }
}
