/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenProperties
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.upperBoundIfFlexible
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext
import org.jetbrains.kotlin.utils.addToStdlib.min
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirTypeMismatchOnOverrideChecker : FirRegularClassChecker() {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val typeCheckerContext = context.session.typeContext.newBaseTypeCheckerContext(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = false
        )

        val firTypeScope = declaration.unsubstitutedScope(
            context.sessionHolder.session,
            context.sessionHolder.scopeSession,
            withForcedTypeCalculator = true
        )

        for (it in declaration.declarations) {
            when (it) {
                is FirSimpleFunction -> checkFunction(it, reporter, typeCheckerContext, firTypeScope, context)
                is FirProperty -> checkProperty(it, reporter, typeCheckerContext, firTypeScope, context)
            }
        }
    }

    private fun FirTypeScope.retrieveDirectOverriddenOf(function: FirSimpleFunction): List<FirFunctionSymbol<*>> {
        processFunctionsByName(function.name) {}

        return getDirectOverriddenFunctions(function.symbol)
    }

    private fun FirTypeScope.retrieveDirectOverriddenOf(property: FirProperty): List<FirPropertySymbol> {
        processPropertiesByName(property.name) {}

        return getDirectOverriddenProperties(property.symbol)
    }

    private fun ConeKotlinType.substituteAllTypeParameters(
        overrideDeclaration: FirCallableMemberDeclaration<*>,
        baseDeclarationSymbol: FirCallableSymbol<*>,
    ): ConeKotlinType {
        if (overrideDeclaration.typeParameters.isEmpty()) {
            return this
        }

        val parametersOwner = baseDeclarationSymbol.fir.safeAs<FirTypeParametersOwner>()
            ?: return this

        val map = mutableMapOf<FirTypeParameterSymbol, ConeKotlinType>()
        val size = min(overrideDeclaration.typeParameters.size, parametersOwner.typeParameters.size)

        for (it in 0 until size) {
            val to = overrideDeclaration.typeParameters[it]
            val from = parametersOwner.typeParameters[it]

            map[from.symbol] = to.toConeType()
        }

        return substitutorByMap(map).substituteOrSelf(this)
    }

    private fun FirCallableMemberDeclaration<*>.checkReturnType(
        overriddenSymbols: List<FirCallableSymbol<*>>,
        typeCheckerContext: AbstractTypeCheckerContext,
        context: CheckerContext,
    ): FirMemberDeclaration? {
        val returnType = returnTypeRef.safeAs<FirResolvedTypeRef>()?.type
            ?: return null

        val bounds = overriddenSymbols.map { context.returnTypeCalculator.tryCalculateReturnType(it.fir).coneType.upperBoundIfFlexible() }

        for (it in bounds.indices) {
            val restriction = bounds[it]
                .substituteAllTypeParameters(this, overriddenSymbols[it])

            if (!AbstractTypeChecker.isSubtypeOf(typeCheckerContext, returnType, restriction)) {
                return overriddenSymbols[it].fir.safeAs()
            }
        }

        return null
    }

    private fun checkFunction(
        function: FirSimpleFunction,
        reporter: DiagnosticReporter,
        typeCheckerContext: AbstractTypeCheckerContext,
        firTypeScope: FirTypeScope,
        context: CheckerContext,
    ) {
        if (!function.isOverride) {
            return
        }

        val overriddenFunctionSymbols = firTypeScope.retrieveDirectOverriddenOf(function)

        if (overriddenFunctionSymbols.isEmpty()) {
            return
        }

        val restriction = function.checkReturnType(
            overriddenSymbols = overriddenFunctionSymbols,
            typeCheckerContext = typeCheckerContext,
            context = context,
        )

        restriction?.let {
            reporter.reportMismatchOnFunction(
                function.returnTypeRef.source,
                function.returnTypeRef.coneType.toString(),
                it
            )
        }
    }

    private fun checkProperty(
        property: FirProperty,
        reporter: DiagnosticReporter,
        typeCheckerContext: AbstractTypeCheckerContext,
        firTypeScope: FirTypeScope,
        context: CheckerContext,
    ) {
        if (!property.isOverride) {
            return
        }

        val overriddenPropertySymbols = firTypeScope.retrieveDirectOverriddenOf(property)

        if (overriddenPropertySymbols.isEmpty()) {
            return
        }

        val restriction = property.checkReturnType(
            overriddenSymbols = overriddenPropertySymbols,
            typeCheckerContext = typeCheckerContext,
            context = context,
        )

        restriction?.let {
            if (property.isVar) {
                reporter.reportMismatchOnVariable(
                    property.returnTypeRef.source,
                    property.returnTypeRef.coneType.toString(),
                    it
                )
            } else {
                reporter.reportMismatchOnProperty(
                    property.returnTypeRef.source,
                    property.returnTypeRef.coneType.toString(),
                    it
                )
            }
        }
    }

    private fun DiagnosticReporter.reportMismatchOnFunction(source: FirSourceElement?, type: String, declaration: FirMemberDeclaration) {
        source?.let { report(FirErrors.RETURN_TYPE_MISMATCH_ON_OVERRIDE.on(it, type, declaration)) }
    }

    private fun DiagnosticReporter.reportMismatchOnProperty(source: FirSourceElement?, type: String, declaration: FirMemberDeclaration) {
        source?.let { report(FirErrors.PROPERTY_TYPE_MISMATCH_ON_OVERRIDE.on(it, type, declaration)) }
    }

    private fun DiagnosticReporter.reportMismatchOnVariable(source: FirSourceElement?, type: String, declaration: FirMemberDeclaration) {
        source?.let { report(FirErrors.VAR_TYPE_MISMATCH_ON_OVERRIDE.on(it, type, declaration)) }
    }
}
