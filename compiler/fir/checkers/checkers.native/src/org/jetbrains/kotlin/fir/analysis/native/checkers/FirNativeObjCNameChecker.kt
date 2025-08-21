/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.EMPTY_OBJC_NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INAPPLICABLE_EXACT_OBJC_NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INAPPLICABLE_OBJC_NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INVALID_OBJC_NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INVALID_OBJC_NAME_CHARS
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INVALID_OBJC_NAME_FIRST_CHAR
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.MISSING_EXACT_OBJC_NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.NON_LITERAL_OBJC_NAME_ARG
import org.jetbrains.kotlin.fir.analysis.native.checkers.FirNativeObjCNameUtilities.ObjCName
import org.jetbrains.kotlin.fir.analysis.native.checkers.FirNativeObjCNameUtilities.getObjCNames
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression

object FirNativeObjCNameChecker : FirBasicDeclarationChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        if (declaration is FirValueParameter) return // those are checked with the FirFunction
        val objCNames = declaration.symbol.getObjCNames(context.session).filterNotNull()
        if (objCNames.isEmpty()) return
        if (declaration is FirCallableDeclaration && declaration.isOverride) {
            for (objCName in objCNames) {
                reporter.reportOn(objCName.annotation.source, INAPPLICABLE_OBJC_NAME)
            }
        }
        objCNames.forEach { checkObjCName(it, declaration) }
    }

    // We only allow valid ObjC identifiers (even for Swift names)
    private val validFirstChars = ('A'..'Z').toSet() + ('a'..'z').toSet() + '_'
    private val validChars = validFirstChars + ('0'..'9').toSet()

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkObjCName(
        objCName: ObjCName,
        declaration: FirDeclaration,
    ) {
        val annotationSource = objCName.annotation.source
        for ((_, argument) in objCName.annotation.argumentMapping.mapping) {
            if (argument is FirLiteralExpression) continue
            reporter.reportOn(argument.source, NON_LITERAL_OBJC_NAME_ARG)
        }
        if (objCName.name == null && objCName.swiftName == null) {
            reporter.reportOn(annotationSource, INVALID_OBJC_NAME)
        }
        val invalidNameFirstChar = objCName.name?.firstOrNull()?.takeUnless(validFirstChars::contains)
        val invalidSwiftNameFirstChar = objCName.swiftName?.firstOrNull()?.takeUnless(validFirstChars::contains)
        val invalidFirstChars = setOfNotNull(invalidNameFirstChar, invalidSwiftNameFirstChar)
        if (invalidFirstChars.isNotEmpty()) {
            reporter.reportOn(annotationSource, INVALID_OBJC_NAME_FIRST_CHAR, invalidFirstChars.joinToString(""))
        }
        if (objCName.name?.isEmpty() == true || objCName.swiftName?.isEmpty() == true) {
            reporter.reportOn(annotationSource, EMPTY_OBJC_NAME)
        }
        val invalidNameChars = objCName.name?.toSet()?.subtract(validChars) ?: emptySet()
        val invalidSwiftNameChars = objCName.swiftName?.toSet()?.subtract(validChars) ?: emptySet()
        val invalidChars = invalidNameChars + invalidSwiftNameChars
        if (invalidChars.isNotEmpty()) {
            reporter.reportOn(annotationSource, INVALID_OBJC_NAME_CHARS, invalidFirstChars.joinToString(""))
        }
        if (objCName.exact && (declaration !is FirClass || declaration.classKind == ClassKind.ENUM_ENTRY)) {
            reporter.reportOn(annotationSource, INAPPLICABLE_EXACT_OBJC_NAME)
        }
        if (objCName.exact && objCName.name == null) {
            reporter.reportOn(annotationSource, MISSING_EXACT_OBJC_NAME)
        }
    }
}
