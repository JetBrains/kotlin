/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isNullable

object FirInapplicableLateinitChecker : FirMemberDeclarationChecker() {
    var primitives: Set<ConeKotlinType>? = null

    private fun getPrimitiveTypes(context: CheckerContext) = primitives ?: mutableSetOf<ConeKotlinType>().apply {
        with(context.session.builtinTypes) {
            add(intType.coneType)
            add(booleanType.coneType)
            add(charType.coneType)
            add(shortType.coneType)
            add(byteType.coneType)
            add(longType.coneType)
            add(doubleType.coneType)
            add(floatType.coneType)
        }
        primitives = this
    }

    override fun check(declaration: FirMemberDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirProperty || !declaration.isLateInit) {
            return
        }

        when {
            declaration.isVal ->
                reporter.report(declaration.source, "is allowed only on mutable properties")

            declaration.initializer != null -> if (declaration.isLocal) {
                reporter.report(declaration.source, "is not allowed on local variables with initializer")
            } else {
                reporter.report(declaration.source, "is not allowed on properties with initializer")
            }

            declaration.delegate != null ->
                reporter.report(declaration.source, "is not allowed on delegated properties")

            declaration.isNullable() ->
                reporter.report(declaration.source, "is not allowed on properties of a type with nullable upper bound")

            declaration.returnTypeRef.coneType in getPrimitiveTypes(context) -> if (declaration.isLocal) {
                reporter.report(declaration.source, "is not allowed on local variables of primitive types")
            } else {
                reporter.report(declaration.source, "is not allowed on properties of primitive types")
            }

            declaration.hasGetter() || declaration.hasSetter() ->
                reporter.report(declaration.source, "is not allowed on properties with a custom getter or setter")
        }
    }

    private fun FirProperty.isNullable() = when (val type = returnTypeRef.coneType) {
        is ConeTypeParameterType -> type.isNullable || type.lookupTag.typeParameterSymbol.fir.bounds.any { it.coneType.isNullable }
        else -> type.isNullable
    }

    private fun FirProperty.hasGetter() = getter != null && getter?.source != null && getter?.source?.kind !is FirFakeSourceElementKind
    private fun FirProperty.hasSetter() = setter != null && setter?.source != null && setter?.source?.kind !is FirFakeSourceElementKind

    private fun DiagnosticReporter.report(source: FirSourceElement?, target: String) {
        source?.let { report(FirErrors.INAPPLICABLE_LATEINIT_MODIFIER.on(it, target)) }
    }
}