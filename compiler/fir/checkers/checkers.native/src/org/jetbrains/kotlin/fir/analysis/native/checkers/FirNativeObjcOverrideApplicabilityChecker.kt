/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.PlatformConflictDeclarationsDiagnosticDispatcher
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors
import org.jetbrains.kotlin.fir.backend.native.interop.getObjCMethodInfoFromOverriddenFunctions
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.name.NativeStandardInteropNames.Annotations.objCSignatureOverrideClassId
import org.jetbrains.kotlin.utils.SmartSet

private fun FirFunctionSymbol<*>.isInheritedFromObjc(context: CheckerContext): Boolean {
    return getObjCMethodInfoFromOverriddenFunctions(context.session, context.scopeSession) != null
}

/**
 * This function basically checks that these two functions have different objective-C signature.
 *
 * This signature consists of function name and parameter names except first.
 *
 * So we ignore the first parameter name, but check others
 */
private fun FirFunctionSymbol<*>.hasDifferentParameterNames(other: FirFunctionSymbol<*>) : Boolean {
    return valueParameterSymbols.drop(1).map { it.name } != other.valueParameterSymbols.drop(1).map { it.name }
}

object NativeConflictDeclarationsDiagnosticDispatcher : PlatformConflictDeclarationsDiagnosticDispatcher {
    override fun getDiagnostic(
        conflictingDeclaration: FirBasedSymbol<*>,
        symbols: SmartSet<FirBasedSymbol<*>>,
        context: CheckerContext
    ): KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>>? {
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.ObjCSignatureOverrideAnnotation)) {
            if (conflictingDeclaration is FirFunctionSymbol<*> && symbols.all { it is FirFunctionSymbol<*> }) {
                if (conflictingDeclaration.isInheritedFromObjc(context) && symbols.all { (it as FirFunctionSymbol<*>).isInheritedFromObjc(context) }) {
                    if (symbols.all { (it as FirFunctionSymbol<*>).hasDifferentParameterNames(conflictingDeclaration) }) {
                        if (conflictingDeclaration.hasAnnotation(objCSignatureOverrideClassId, context.session)) {
                            return null
                        } else {
                            return FirNativeErrors.CONFLICTING_OBJC_OVERLOADS
                        }
                    }
                }
            }
        }
        return PlatformConflictDeclarationsDiagnosticDispatcher.DEFAULT.getDiagnostic(conflictingDeclaration, symbols, context)
    }
}

object FirNativeObjcOverrideApplicabilityChecker : FirFunctionChecker(MppCheckerKind.Platform) {
    override fun check(
        declaration: FirFunction,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        if (declaration.hasAnnotation(objCSignatureOverrideClassId, context.session)) {
            if (!declaration.symbol.isInheritedFromObjc(context)) {
                reporter.reportOn(declaration.getAnnotationByClassId(objCSignatureOverrideClassId, context.session)?.source, FirNativeErrors.INAPPLICABLE_OBJC_OVERRIDE, context)
            }
        }
    }
}