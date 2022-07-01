/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.FirDeclarationWithParents
import org.jetbrains.kotlin.fir.analysis.FirJsNameSuggestion
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.isNativeObject
import org.jetbrains.kotlin.fir.analysis.name
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames.NO_NAME_PROVIDED

private val PROHIBITED_STATIC_NAMES = setOf(
    Name.identifier("prototype"),
    Name.identifier("length"),
    Name.identifier("\$metadata\$"),
)

private val PROHIBITED_MEMBER_NAMES = setOf(
    Name.identifier("constructor"),
)

object FirJsBuiltinNameClashChecker : FirBasicDeclarationChecker() {
    private val nameSuggestion = FirJsNameSuggestion()

    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.name == NO_NAME_PROVIDED) return

        val it = FirDeclarationWithParents(declaration, context.containingDeclarations, context)

        if (it.isNativeObject()) return
        if (it.container !is FirClass) return

        val suggestedName = nameSuggestion.suggest(it)!!
        if (!suggestedName.stable) return
        val simpleName = Name.identifier(suggestedName.names.single())

        when {
            declaration is FirClass && simpleName in PROHIBITED_STATIC_NAMES -> {
                reporter.reportOn(declaration.source, FirJsErrors.JS_BUILTIN_NAME_CLASH, "<TODO: Suggested name>", context)
            }
            declaration is FirCallableDeclaration && simpleName in PROHIBITED_MEMBER_NAMES -> {
                reporter.reportOn(declaration.source, FirJsErrors.JS_BUILTIN_NAME_CLASH, "<TODO: Suggested name>", context)
            }
        }
    }
}
