/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClass
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.FUN_INTERFACE_ABSTRACT_METHOD_WITH_DEFAULT_VALUE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.FUN_INTERFACE_ABSTRACT_METHOD_WITH_TYPE_PARAMETERS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.FUN_INTERFACE_CANNOT_HAVE_ABSTRACT_PROPERTIES
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.FUN_INTERFACE_WITH_SUSPEND_FUNCTION
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isAbstract
import org.jetbrains.kotlin.fir.declarations.utils.isFun
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.declarations.utils.isSuspend
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.getProperties

object FirFunInterfaceDeclarationChecker : FirRegularClassChecker() {

    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isInterface || !declaration.isFun) return

        val scope = declaration.unsubstitutedScope(context)

        var abstractFunction: FirSimpleFunction? = null

        for (name in scope.getCallableNames()) {
            val functions = scope.getFunctions(name)
            val properties = scope.getProperties(name)

            for (function in functions) {
                if (function.fir.isAbstract) {
                    if (abstractFunction == null) {
                        abstractFunction = function.fir
                    } else {
                        reporter.reportOn(declaration.source, FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS, context)
                    }
                }
            }

            for (property in properties) {
                val firProperty = property.fir as? FirProperty ?: continue
                if (firProperty.isAbstract) {
                    val source =
                        if (firProperty.getContainingClass(context) != declaration)
                            declaration.source
                        else
                            firProperty.source

                    reporter.reportOn(source, FUN_INTERFACE_CANNOT_HAVE_ABSTRACT_PROPERTIES, context)
                }
            }
        }

        if (abstractFunction == null) {
            reporter.reportOn(declaration.source, FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS, context)
            return
        }

        val inFunInterface = abstractFunction.getContainingClass(context) === declaration

        when {
            abstractFunction.typeParameters.isNotEmpty() ->
                reporter.reportOn(
                    if (inFunInterface) abstractFunction.source else declaration.source,
                    FUN_INTERFACE_ABSTRACT_METHOD_WITH_TYPE_PARAMETERS,
                    context
                )

            abstractFunction.isSuspend ->
                if (!context.session.languageVersionSettings.supportsFeature(LanguageFeature.SuspendFunctionsInFunInterfaces)) {
                    reporter.reportOn(
                        if (inFunInterface) abstractFunction.source else declaration.source,
                        FUN_INTERFACE_WITH_SUSPEND_FUNCTION,
                        context
                    )
                }
        }

        abstractFunction.valueParameters.forEach {
            if (it.defaultValue != null) {
                reporter.reportOn(
                    if (inFunInterface) it.source else declaration.source,
                    FUN_INTERFACE_ABSTRACT_METHOD_WITH_DEFAULT_VALUE,
                    context
                )
            }
        }
    }
}

