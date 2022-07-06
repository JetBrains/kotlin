/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(SymbolInternals::class)

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.FirDeclarationWithContext
import org.jetbrains.kotlin.fir.analysis.FirDeclarationWithParents
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.getAllOverridden
import org.jetbrains.kotlin.fir.analysis.isEffectivelyExternal
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.types.*

object FirJsInheritanceChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        val that = FirDeclarationWithParents(declaration, context.containingDeclarations, context)

        if (that.isNotEffectivelyExternalFunctionButOverridesExternal()) {
            reporter.reportOn(declaration.source, FirJsErrors.OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS, context)
        } else if (declaration is FirClass && !that.isEffectivelyExternal()) {
            @Suppress("UNCHECKED_CAST")
            that as FirDeclarationWithParents<FirClass>
            val fakeOverriddenMethod = that.findFakeMethodOverridingExternalWithOptionalParams()

            if (fakeOverriddenMethod != null) {
                reporter.reportOn(
                    declaration.source, FirJsErrors.OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS_WITH_FAKE,
                    fakeOverriddenMethod.symbol, context
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

    private fun FirDeclarationWithContext<FirClass>.findFakeMethodOverridingExternalWithOptionalParams(): FirSimpleFunction? {
        val scope = declaration.symbol.unsubstitutedScope(context)

        val members = scope.collectAllFunctions()
            .map { it.fir }
            .filter {
                val container = it.getContainingClassSymbol(session)
                container == declaration && !it.isReal && it.getAllOverridden(session, context).isNotEmpty()
            }

        return members.firstOrNull {
            FirDeclarationWithContext(it, context).isOverridingExternalWithOptionalParams()
        }
    }

    private fun FirDeclarationWithContext<*>.isNotEffectivelyExternalFunctionButOverridesExternal(): Boolean {
        if (declaration !is FirFunction || isEffectivelyExternal()) return false
        @Suppress("UNCHECKED_CAST")
        this as FirDeclarationWithParents<FirFunction>
        return isOverridingExternalWithOptionalParams()
    }

    private val FirFunction.isReal get() = attributes.fakeOverrideSubstitution == null

    private fun FirDeclarationWithContext<FirFunction>.isOverridingExternalWithOptionalParams(): Boolean {
        if (!declaration.isReal && declaration.modality == Modality.ABSTRACT) return false

        val overridden = declaration.getAllOverridden(session, context).mapNotNull { it.fir as? FirFunction }

        for (overriddenFunction in overridden.filter { FirDeclarationWithContext(it, context).isEffectivelyExternal() }) {
            if (overriddenFunction.valueParameters.any { it.symbol.hasDefaultValue }) return true
        }

        return false
    }
}
