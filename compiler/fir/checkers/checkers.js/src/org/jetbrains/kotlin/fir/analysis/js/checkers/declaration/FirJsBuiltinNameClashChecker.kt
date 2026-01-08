/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.js.checkers.FirJsStableName
import org.jetbrains.kotlin.fir.declarations.utils.isNativeObject
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.name.JsStandardClassIds

object FirJsBuiltinNameClashChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    private val PROHIBITED_MEMBER_NAMES = setOf("constructor")
    private val PROHIBITED_STATIC_NAMES = setOf("prototype", "length", "\$metadata\$")

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        if (declaration.symbol.isNativeObject(context.session)) {
            return
        }
        if (declaration.getContainingClassSymbol() == null) {
            return
        }

        val stableName = FirJsStableName.createStableNameOrNull(declaration.symbol, context.session)?.name ?: return

        if (declaration.couldBeCompiledAsStaticMember && stableName in PROHIBITED_STATIC_NAMES) {
            reporter.reportOn(declaration.source, FirJsErrors.JS_BUILTIN_NAME_CLASH, "Function.$stableName")
        }

        if (declaration is FirCallableDeclaration && stableName in PROHIBITED_MEMBER_NAMES) {
            reporter.reportOn(declaration.source, FirJsErrors.JS_BUILTIN_NAME_CLASH, "Object.prototype.$stableName")
        }
    }

    context(context: CheckerContext)
    private val FirDeclaration.couldBeCompiledAsStaticMember: Boolean
        get() = this is FirClassLikeDeclaration || hasAnnotation(JsStandardClassIds.Annotations.JsStatic, context.session)
}