/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.EMPTY_OBJC_NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INAPPLICABLE_EXACT_OBJC_NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INAPPLICABLE_OBJC_NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INVALID_OBJC_NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INVALID_OBJC_NAME_CHARS
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INVALID_OBJC_NAME_FIRST_CHAR
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.MISSING_EXACT_OBJC_NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.NON_LITERAL_OBJC_NAME_ARG
import org.jetbrains.kotlin.fir.analysis.native.checkers.FirNativeObjCNameOverridesChecker.check
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object FirNativeObjCNameChecker : FirBasicDeclarationChecker() {

    private val objCNameClassId = ClassId.topLevel(FqName("kotlin.native.ObjCName"))
    private val swiftNameName = Name.identifier("swiftName")
    private val exactName = Name.identifier("exact")

    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        checkDeclaration(declaration, context, reporter)
        if (declaration is FirCallableDeclaration && (declaration is FirSimpleFunction || declaration is FirProperty)) {
            val containingClass = context.containingDeclarations.lastOrNull() as? FirClass
            if (containingClass != null) {
                val firTypeScope = containingClass.unsubstitutedScope(context)
                check(firTypeScope, declaration.symbol, declaration, context, reporter)
            }
        }
    }

    private fun checkDeclaration(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration is FirValueParameter) return // those are checked with the FirFunction
        val objCNames = declaration.symbol.getObjCNames(context.session).filterNotNull()
        if (objCNames.isEmpty()) return
        if (declaration is FirCallableDeclaration && declaration.isOverride) {
            for (objCName in objCNames) {
                reporter.reportOn(objCName.annotation.source, INAPPLICABLE_OBJC_NAME, context)
            }
        }
        objCNames.forEach { checkObjCName(it, declaration, context, reporter) }
    }

    // We only allow valid ObjC identifiers (even for Swift names)
    private val validFirstChars = ('A'..'Z').toSet() + ('a'..'z').toSet() + '_'
    private val validChars = validFirstChars + ('0'..'9').toSet()

    private fun checkObjCName(objCName: ObjCName, declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        val annotationSource = objCName.annotation.source
        for ((_, argument) in objCName.annotation.argumentMapping.mapping) {
            if (argument is FirConstExpression<*>) continue
            reporter.reportOn(argument.source, NON_LITERAL_OBJC_NAME_ARG, context)
        }
        if (objCName.name == null && objCName.swiftName == null) {
            reporter.reportOn(annotationSource, INVALID_OBJC_NAME, context)
        }
        val invalidNameFirstChar = objCName.name?.firstOrNull()?.takeUnless(validFirstChars::contains)
        val invalidSwiftNameFirstChar = objCName.swiftName?.firstOrNull()?.takeUnless(validFirstChars::contains)
        val invalidFirstChars = setOfNotNull(invalidNameFirstChar, invalidSwiftNameFirstChar)
        if (invalidFirstChars.isNotEmpty()) {
            reporter.reportOn(annotationSource, INVALID_OBJC_NAME_FIRST_CHAR, invalidFirstChars.joinToString(""), context)
        }
        if (objCName.name?.isEmpty() == true || objCName.swiftName?.isEmpty() == true) {
            reporter.reportOn(annotationSource, EMPTY_OBJC_NAME, context)
        }
        val invalidNameChars = objCName.name?.toSet()?.subtract(validChars) ?: emptySet()
        val invalidSwiftNameChars = objCName.swiftName?.toSet()?.subtract(validChars) ?: emptySet()
        val invalidChars = invalidNameChars + invalidSwiftNameChars
        if (invalidChars.isNotEmpty()) {
            reporter.reportOn(annotationSource, INVALID_OBJC_NAME_CHARS, invalidFirstChars.joinToString(""), context)
        }
        if (objCName.exact && (declaration !is FirClass || declaration.classKind == ClassKind.ENUM_ENTRY)) {
            reporter.reportOn(annotationSource, INAPPLICABLE_EXACT_OBJC_NAME, context)
        }
        if (objCName.exact && objCName.name == null) {
            reporter.reportOn(annotationSource, MISSING_EXACT_OBJC_NAME, context)
        }
    }

    class ObjCName(
        val annotation: FirAnnotation
    ) {
        val name: String? = annotation.getStringArgument(StandardNames.NAME)
        val swiftName: String? = annotation.getStringArgument(swiftNameName)
        val exact: Boolean = annotation.getBooleanArgument(exactName) ?: false

        override fun equals(other: Any?): Boolean =
            other is ObjCName && name == other.name && swiftName == other.swiftName && exact == other.exact

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + swiftName.hashCode()
            result = 31 * result + exact.hashCode()
            return result
        }
    }

    private fun FirAnnotationContainer.getObjCName(session: FirSession): ObjCName? =
        getAnnotationByClassId(objCNameClassId, session)?.let(::ObjCName)

    private fun FirBasedSymbol<*>.getObjCName(session: FirSession): ObjCName? =
        getAnnotationByClassId(objCNameClassId, session)?.let(::ObjCName)

    fun FirBasedSymbol<*>.getObjCNames(session: FirSession): List<ObjCName?> = when (this) {
        is FirFunctionSymbol<*> -> buildList {
            add((this@getObjCNames as FirBasedSymbol<*>).getObjCName(session))
            add(resolvedReceiverTypeRef?.getObjCName(session))
            add(receiverParameter?.getObjCName(session))
            valueParameterSymbols.forEach { add(it.getObjCName(session)) }
        }

        else -> listOf(getObjCName(session))
    }
}
