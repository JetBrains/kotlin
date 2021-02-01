/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenProperties
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext
import org.jetbrains.kotlin.utils.addToStdlib.min
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirOverrideChecker : FirRegularClassChecker() {
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

    private fun checkModality(
        overriddenSymbols: List<FirCallableSymbol<*>>,
    ): FirCallableDeclaration<*>? {
        for (overridden in overriddenSymbols) {
            if (overridden.fir !is FirMemberDeclaration) continue
            val modality = (overridden.fir as FirMemberDeclaration).status.modality
            val isEffectivelyFinal = modality == null || modality == Modality.FINAL
            if (isEffectivelyFinal) {
                return overridden.fir
            }
        }
        return null
    }

    private fun FirProperty.checkMutability(
        overriddenSymbols: List<FirCallableSymbol<*>>,
    ): FirMemberDeclaration? {
        if (isVar) return null
        return overriddenSymbols.find { (it.fir as? FirProperty)?.isVar == true }?.fir?.safeAs()
    }

    private fun FirCallableMemberDeclaration<*>.checkVisibility(
        reporter: DiagnosticReporter,
        overriddenSymbols: List<FirCallableSymbol<*>>,
    ) {
        val visibilities = overriddenSymbols.mapNotNull {
            if (it.fir !is FirMemberDeclaration) return@mapNotNull null
            it to (it.fir as FirMemberDeclaration).visibility
        }.sortedBy { pair ->
            // Regard `null` compare as Int.MIN so that we can report CANNOT_CHANGE_... first deterministically
            visibility.compareTo(pair.second) ?: Int.MIN_VALUE
        }

        for ((overridden, overriddenVisibility) in visibilities) {
            val compare = visibility.compareTo(overriddenVisibility)
            if (compare == null) {
                // TODO: not ready yet (even after determinism massage), e.g., a Kotlin class that extends a Java class
                // reporter.reportCannotChangeAccessPrivilege(this, overridden.fir)
                return
            } else if (compare < 0) {
                reporter.reportCannotWeakenAccessPrivilege(this, overridden.fir)
                return
            }
        }
    }

    private fun FirCallableMemberDeclaration<*>.checkReturnType(
        overriddenSymbols: List<FirCallableSymbol<*>>,
        typeCheckerContext: AbstractTypeCheckerContext,
        context: CheckerContext,
    ): FirMemberDeclaration? {
        val returnType = returnTypeRef.safeAs<FirResolvedTypeRef>()?.type
            ?: return null

        // Don't report *_ON_OVERRIDE diagnostics according to an error return type. That should be reported separately.
        if (returnType is ConeKotlinErrorType) {
            return null
        }

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
            reporter.reportNothingToOverride(function)
            return
        }

        checkModality(overriddenFunctionSymbols)?.let {
            reporter.reportOverridingFinalMember(function, it)
        }

        function.checkVisibility(reporter, overriddenFunctionSymbols)

        val restriction = function.checkReturnType(
            overriddenSymbols = overriddenFunctionSymbols,
            typeCheckerContext = typeCheckerContext,
            context = context,
        )

        restriction?.let {
            reporter.reportReturnTypeMismatchOnFunction(function, it)
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
            reporter.reportNothingToOverride(property)
            return
        }

        checkModality(overriddenPropertySymbols)?.let {
            reporter.reportOverridingFinalMember(property, it)
        }

        property.checkMutability(overriddenPropertySymbols)?.let {
            reporter.reportVarOverriddenByVal(property, it)
        }

        property.checkVisibility(reporter, overriddenPropertySymbols)

        val restriction = property.checkReturnType(
            overriddenSymbols = overriddenPropertySymbols,
            typeCheckerContext = typeCheckerContext,
            context = context,
        )

        restriction?.let {
            if (property.isVar) {
                reporter.reportTypeMismatchOnVariable(property, it)
            } else {
                reporter.reportTypeMismatchOnProperty(property, it)
            }
        }
    }

    private fun DiagnosticReporter.reportNothingToOverride(declaration: FirMemberDeclaration) {
        // TODO: not ready yet, e.g., Collections
        // declaration.source?.let { report(FirErrors.NOTHING_TO_OVERRIDE.on(it, declaration)) }
    }

    private fun DiagnosticReporter.reportOverridingFinalMember(
        overriding: FirMemberDeclaration,
        overridden: FirCallableDeclaration<*>,
    ) {
        overriding.source?.let { source ->
            overridden.containingClass()?.let { containingClass ->
                report(FirErrors.OVERRIDING_FINAL_MEMBER.on(source, overridden, containingClass.name))
            }
        }
    }

    private fun DiagnosticReporter.reportVarOverriddenByVal(
        overriding: FirMemberDeclaration,
        overridden: FirMemberDeclaration
    ) {
        overriding.source?.let { report(FirErrors.VAR_OVERRIDDEN_BY_VAL.on(it, overriding, overridden)) }
    }

    private fun DiagnosticReporter.reportCannotWeakenAccessPrivilege(
        overriding: FirMemberDeclaration,
        overridden: FirCallableDeclaration<*>,
    ) {
        overriding.source?.let { source ->
            overridden.containingClass()?.let { containingClass ->
                report(FirErrors.CANNOT_WEAKEN_ACCESS_PRIVILEGE.on(source, overriding.visibility, overridden, containingClass.name))
            }
        }
    }

    private fun DiagnosticReporter.reportCannotChangeAccessPrivilege(
        overriding: FirMemberDeclaration,
        overridden: FirCallableDeclaration<*>,
    ) {
        overriding.source?.let { source ->
            overridden.containingClass()?.let { containingClass ->
                report(FirErrors.CANNOT_CHANGE_ACCESS_PRIVILEGE.on(source, overriding.visibility, overridden, containingClass.name))
            }
        }
    }

    private fun DiagnosticReporter.reportReturnTypeMismatchOnFunction(
        overriding: FirMemberDeclaration,
        overridden: FirMemberDeclaration
    ) {
        overriding.source?.let { report(FirErrors.RETURN_TYPE_MISMATCH_ON_OVERRIDE.on(it, overriding, overridden)) }
    }

    private fun DiagnosticReporter.reportTypeMismatchOnProperty(
        overriding: FirMemberDeclaration,
        overridden: FirMemberDeclaration
    ) {
        overriding.source?.let { report(FirErrors.PROPERTY_TYPE_MISMATCH_ON_OVERRIDE.on(it, overriding, overridden)) }
    }

    private fun DiagnosticReporter.reportTypeMismatchOnVariable(
        overriding: FirMemberDeclaration,
        overridden: FirMemberDeclaration
    ) {
        overriding.source?.let { report(FirErrors.VAR_TYPE_MISMATCH_ON_OVERRIDE.on(it, overriding, overridden)) }
    }
}
