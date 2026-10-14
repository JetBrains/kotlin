/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.web.common.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.isTopLevel
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.name.WebCommonStandardClassIds.Annotations.JsModule
import org.jetbrains.kotlin.name.WebCommonStandardClassIds.Annotations.JsQualifier

/**
 * Reports a declaration that has an annotation describing its relation to a JS module, but cannot have one.
 *
 * Which annotations these are and which declarations are external depends on a platform,
 * so these decisions are left to [hasModuleAnnotation] and [isExternal].
 */
abstract class FirWebCommonJsModuleChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    protected abstract fun FirBasedSymbol<*>.hasModuleAnnotation(session: FirSession): Boolean

    context(context: CheckerContext)
    protected abstract fun isExternal(declaration: FirDeclaration): Boolean

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        if (declaration is FirFile || !declaration.symbol.hasModuleAnnotation(context.session)) return

        val isExternal = isExternal(declaration)

        if (!context.isTopLevel) {
            reporter.reportOn(declaration.source, FirWebCommonErrors.JS_MODULE_PROHIBITED_ON_MEMBER)
        }

        // An external interface has no runtime value, unless its companion object is imported from the module.
        if (context.isTopLevel && isExternal && declaration is FirRegularClass && declaration.isInterface &&
            declaration.companionObjectSymbol == null
        ) {
            reporter.reportOn(declaration.source, FirWebCommonErrors.JS_MODULE_PROHIBITED_ON_EXTERNAL_INTERFACE)
        }

        if (declaration is FirProperty && declaration.isVar) {
            reporter.reportOn(declaration.source, FirWebCommonErrors.JS_MODULE_PROHIBITED_ON_VAR)
        }

        if (!isExternal) {
            reporter.reportOn(declaration.source, FirWebCommonErrors.JS_MODULE_PROHIBITED_ON_NON_EXTERNAL)
        }

        if (context.isTopLevel && context.containingFileSymbol?.hasModuleAnnotation(context.session) == true) {
            reporter.reportOn(declaration.source, FirWebCommonErrors.NESTED_JS_MODULE_PROHIBITED)
        }

        // A file qualifier is not applied consistently to a declaration that is imported from its own module.
        if (context.isTopLevel && declaration.symbol.hasAnnotation(JsModule, context.session) &&
            context.containingFileSymbol?.hasAnnotation(JsQualifier, context.session) == true
        ) {
            reporter.reportOn(declaration.source, FirWebCommonErrors.JS_MODULE_PROHIBITED_IN_FILE_WITH_JS_QUALIFIER)
        }
    }
}
