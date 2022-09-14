/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getDirectBases
import org.jetbrains.kotlin.fir.analysis.checkers.isEffectivelyExternal
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.isSubstitutionOrIntersectionOverride
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name

object FirJsInheritanceChecker : FirBasicDeclarationChecker() {
    private val noNameProvided = Name.special("<no name provided>")

    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration is FirValueParameter && declaration.name == noNameProvided) return

        if (declaration.isNotEffectivelyExternalFunctionButOverridesExternal(context)) {
            reporter.reportOn(declaration.source, FirJsErrors.OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS, context)
        } else if (declaration is FirClass && !declaration.symbol.isEffectivelyExternal(context)) {
            val fakeOverriddenMethod = declaration.findFakeMethodOverridingExternalWithOptionalParams(context)

            if (fakeOverriddenMethod != null) {
                reporter.reportOn(
                    declaration.source, FirJsErrors.OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS_WITH_FAKE,
                    fakeOverriddenMethod, context
                )
            }
        }

        if (declaration is FirClass &&
            declaration.superConeTypes.any { it.isBuiltinFunctionalTypeOrSubtype(context.session) && !it.isSuspendFunctionType(context.session) }
        ) {
            reporter.reportOn(declaration.source, FirJsErrors.IMPLEMENTING_FUNCTION_INTERFACE, context)
        }
    }

    private fun ConeClassLikeType.isBuiltinFunctionalTypeOrSubtype(session: FirSession): Boolean {
        return with(session.typeContext) { isBuiltinFunctionalTypeOrSubtype() }
    }

    private fun FirClass.findFakeMethodOverridingExternalWithOptionalParams(context: CheckerContext): FirNamedFunctionSymbol? {
        val scope = symbol.unsubstitutedScope(context)

        val members = scope.collectAllFunctions()
            .filterIsInstance<FirIntersectionOverrideFunctionSymbol>()
            .filter {
                val container = it.getContainingClassSymbol(context.session)
                container == symbol && it.intersections.isNotEmpty()
            }

        return members.firstOrNull {
            it.isOverridingExternalWithOptionalParams(context)
        }
    }

    private fun FirDeclaration.isNotEffectivelyExternalFunctionButOverridesExternal(context: CheckerContext): Boolean {
        if (this !is FirFunction || symbol.isEffectivelyExternal(context)) return false
        return symbol.isOverridingExternalWithOptionalParams(context)
    }

    private val FirFunctionSymbol<*>.isReal get() = !isSubstitutionOrIntersectionOverride

    private fun FirFunctionSymbol<*>.isOverridingExternalWithOptionalParams(context: CheckerContext): Boolean {
        if (!isReal && modality == Modality.ABSTRACT) return false

        val overridden = getDirectBases(context).mapNotNull { it as? FirFunctionSymbol<*> }

        for (overriddenFunction in overridden.filter { it.isEffectivelyExternal(context) }) {
            if (overriddenFunction.valueParameterSymbols.any { it.hasDefaultValue }) return true
        }

        return false
    }
}
