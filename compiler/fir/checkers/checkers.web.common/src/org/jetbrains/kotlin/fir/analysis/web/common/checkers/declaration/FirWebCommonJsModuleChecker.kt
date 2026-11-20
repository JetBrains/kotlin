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
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.declarations.utils.isEffectivelyExternal
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.WebCommonStandardClassIds.Annotations.JsModule
import org.jetbrains.kotlin.name.WebCommonStandardClassIds.Annotations.JsQualifier

abstract class FirWebCommonJsModuleChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    protected abstract fun FirBasedSymbol<*>.findModuleAnnotation(session: FirSession): FirAnnotation?

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        if (declaration is FirFile) return
        val annotation = declaration.symbol.findModuleAnnotation(context.session) ?: return
        val annotationType = annotation.resolvedType

        val isExternal = declaration.symbol.isEffectivelyExternal(context.session)
        val isTopLevel = context.isTopLevel

        if (!isTopLevel) {
            reporter.reportOn(annotation.source, FirWebCommonErrors.JS_MODULE_PROHIBITED_ON_MEMBER, annotationType)
        }

        // An external interface has no runtime value, unless its companion object is imported from the module.
        if (isTopLevel && isExternal && declaration is FirRegularClass && declaration.isInterface &&
            declaration.companionObjectSymbol == null
        ) {
            reporter.reportOn(annotation.source, FirWebCommonErrors.JS_MODULE_PROHIBITED_ON_EXTERNAL_INTERFACE, annotationType)
        }

        if (declaration is FirProperty && declaration.isVar) {
            reporter.reportOn(declaration.source, FirWebCommonErrors.JS_MODULE_PROHIBITED_ON_VAR, annotationType)
        }

        if (!isExternal) {
            reporter.reportOn(annotation.source, FirWebCommonErrors.JS_MODULE_PROHIBITED_ON_NON_EXTERNAL, annotationType)
        }

        if (isTopLevel && context.containingFileSymbol?.findModuleAnnotation(context.session) != null) {
            reporter.reportOn(annotation.source, FirWebCommonErrors.NESTED_JS_MODULE_PROHIBITED, annotationType)
        }

        // A file qualifier is not applied consistently to a declaration that is imported from its own module.
        val jsModuleAnnotation = declaration.symbol.resolvedAnnotationsWithClassIds.getAnnotationByClassId(JsModule, context.session)
        if (isTopLevel && jsModuleAnnotation != null &&
            context.containingFileSymbol?.hasAnnotation(JsQualifier, context.session) == true
        ) {
            reporter.reportOn(jsModuleAnnotation.source, FirWebCommonErrors.JS_MODULE_PROHIBITED_IN_FILE_WITH_JS_QUALIFIER)
        }
    }
}
