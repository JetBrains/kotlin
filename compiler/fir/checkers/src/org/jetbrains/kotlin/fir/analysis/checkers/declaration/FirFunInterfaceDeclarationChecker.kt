/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.FUN_INTERFACE_ABSTRACT_METHOD_WITH_DEFAULT_VALUE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.FUN_INTERFACE_ABSTRACT_METHOD_WITH_TYPE_PARAMETERS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.FUN_INTERFACE_CANNOT_HAVE_ABSTRACT_PROPERTIES
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.FUN_INTERFACE_WITH_SUSPEND_FUNCTION
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol


sealed class FirFunInterfaceDeclarationChecker(mppKind: MppCheckerKind) : FirRegularClassChecker(mppKind) {
    object Regular : FirFunInterfaceDeclarationChecker(MppCheckerKind.Platform) {
        override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
            if (declaration.isExpect) return
            super.check(declaration, context, reporter)
        }
    }

    object ForExpectClass : FirFunInterfaceDeclarationChecker(MppCheckerKind.Common) {
        override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
            if (!declaration.isExpect) return
            super.check(declaration, context, reporter)
        }
    }

    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isInterface || !declaration.isFun) return

        val scope = declaration.unsubstitutedScope(context)
        val classSymbol = declaration.symbol

        var abstractFunctionSymbol: FirNamedFunctionSymbol? = null

        for (name in scope.getCallableNames()) {
            val functions = scope.getFunctions(name)
            val properties = scope.getProperties(name)

            for (function in functions) {
                if (function.isAbstract) {
                    if (abstractFunctionSymbol == null) {
                        abstractFunctionSymbol = function
                    } else {
                        reporter.reportOn(declaration.source, FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS, context)
                    }
                }
            }

            for (property in properties) {
                val firProperty = property as? FirPropertySymbol ?: continue
                if (firProperty.isAbstract) {
                    val source =
                        if (firProperty.getContainingClassSymbol(context.session) != classSymbol)
                            declaration.source
                        else
                            firProperty.source

                    reporter.reportOn(source, FUN_INTERFACE_CANNOT_HAVE_ABSTRACT_PROPERTIES, context)
                }
            }
        }

        if (abstractFunctionSymbol == null) {
            reporter.reportOn(declaration.source, FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS, context)
            return
        }

        val inFunInterface = abstractFunctionSymbol.getContainingClassSymbol(context.session) === classSymbol

        when {
            abstractFunctionSymbol.typeParameterSymbols.isNotEmpty() ->
                reporter.reportOn(
                    if (inFunInterface) abstractFunctionSymbol.source else declaration.source,
                    FUN_INTERFACE_ABSTRACT_METHOD_WITH_TYPE_PARAMETERS,
                    context
                )

            abstractFunctionSymbol.isSuspend ->
                if (!context.session.languageVersionSettings.supportsFeature(LanguageFeature.SuspendFunctionsInFunInterfaces)) {
                    reporter.reportOn(
                        if (inFunInterface) abstractFunctionSymbol.source else declaration.source,
                        FUN_INTERFACE_WITH_SUSPEND_FUNCTION,
                        context
                    )
                }
        }

        abstractFunctionSymbol.valueParameterSymbols.forEach {
            if (it.hasDefaultValue) {
                reporter.reportOn(
                    if (inFunInterface) it.source else declaration.source,
                    FUN_INTERFACE_ABSTRACT_METHOD_WITH_DEFAULT_VALUE,
                    context
                )
            }
        }
    }
}

