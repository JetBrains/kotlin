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
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirTypeMismatchOnOverrideChecker : FirMemberDeclarationChecker() {
    override fun check(declaration: FirMemberDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirRegularClass) {
            return
        }

        val typeCheckerContext = context.session.typeContext.newBaseTypeCheckerContext(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = false
        )

        val firTypeScope = declaration.unsubstitutedScope(
            context.sessionHolder.session,
            context.sessionHolder.scopeSession
        )

        for (it in declaration.declarations) {
            when (it) {
                is FirSimpleFunction -> checkFunction(declaration, it, context, reporter, typeCheckerContext, firTypeScope)
                is FirProperty -> checkProperty(declaration, it, context, reporter, typeCheckerContext, firTypeScope)
            }
        }
    }

    private fun FirTypeScope.getDirectOverridesOf(function: FirSimpleFunction): List<FirFunctionSymbol<*>> {
        val overriddenFunctions = mutableSetOf<FirFunctionSymbol<*>>()

        processFunctionsByName(function.name) {}
        processDirectlyOverriddenFunctions(function.symbol) {
            overriddenFunctions.add(it)
            ProcessorAction.NEXT
        }

        return overriddenFunctions.toList()
    }

    private fun FirTypeScope.getDirectOverridesOf(property: FirProperty): List<FirPropertySymbol> {
        val overriddenProperties = mutableSetOf<FirPropertySymbol>()

        processPropertiesByName(property.name) {}
        processDirectlyOverriddenProperties(property.symbol) {
            overriddenProperties.add(it)
            ProcessorAction.NEXT
        }

        return overriddenProperties.toList()
    }

    private fun FirRegularClass.substituteAllSupertypeParameters(context: CheckerContext): ConeSubstitutor {
        val allSupertypesMapping = mutableMapOf<FirTypeParameterSymbol, ConeKotlinType>()
        val remapper = mutableMapOf<ConeKotlinType, ConeKotlinType>()

        fun FirRegularClass.collectSupertypes() {
            for (it in superTypeRefs) {
                val fir = it.coneType.safeAs<ConeClassLikeType>()?.lookupTag?.toSymbol(context.session)
                    ?.fir.safeAs<FirRegularClass>()
                    ?: continue

                if (it.coneType.typeArguments.size != fir.typeParameters.size) {
                    continue
                }

                for (that in fir.typeParameters.indices) {
                    val proto = fir.typeParameters[that].symbol
                    val actual = it.coneType.typeArguments[that].safeAs<ConeKotlinType>()
                        ?.lowerBoundIfFlexible()
                        ?: continue
                    val value = remapper.getOrDefault(actual, actual)
                    remapper[proto.fir.toConeType()] = value
                    allSupertypesMapping[proto] = value
                }

                fir.collectSupertypes()
            }
        }

        collectSupertypes()
        return substitutorByMap(allSupertypesMapping)
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

        overrideDeclaration.typeParameters.zip(parametersOwner.typeParameters).forEach { (to, from) ->
            map[from.symbol] = to.toConeType()
        }

        return substitutorByMap(map).substituteOrSelf(this)
    }

    private fun ConeKotlinType.substituteOrSelf(substitutor: ConeSubstitutor) = substitutor.substituteOrSelf(this)

    private fun FirCallableMemberDeclaration<*>.checkReturnType(
        regularClass: FirRegularClass,
        overriddenSymbols: List<FirCallableSymbol<*>>,
        context: CheckerContext,
        typeCheckerContext: AbstractTypeCheckerContext,
    ): FirMemberDeclaration? {
        val returnType = returnTypeRef.safeAs<FirResolvedTypeRef>()?.type
            ?: return null

        val bounds = overriddenSymbols.map { it.fir.returnTypeRef.coneType.upperBoundIfFlexible() }
        val supertypesParameterSubstitutor = regularClass.substituteAllSupertypeParameters(context)

        for (it in bounds.indices) {
            val restriction = bounds[it]
                .substituteAllTypeParameters(this, overriddenSymbols[it])
                .substituteOrSelf(supertypesParameterSubstitutor)

            if (!AbstractTypeChecker.isSubtypeOf(typeCheckerContext, returnType, restriction)) {
                return overriddenSymbols[it].fir.safeAs()
            }
        }

        return null
    }

    private fun checkFunction(
        regularClass: FirRegularClass,
        function: FirSimpleFunction,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        typeCheckerContext: AbstractTypeCheckerContext,
        firTypeScope: FirTypeScope
    ) {
        if (!function.isOverride) {
            return
        }

        val overriddenFunctionSymbols = firTypeScope.getDirectOverridesOf(function)

        if (overriddenFunctionSymbols.isEmpty()) {
            return
        }

        val restriction = function.checkReturnType(
            regularClass = regularClass,
            overriddenSymbols = overriddenFunctionSymbols,
            context = context,
            typeCheckerContext = typeCheckerContext
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
        regularClass: FirRegularClass,
        property: FirProperty,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        typeCheckerContext: AbstractTypeCheckerContext,
        firTypeScope: FirTypeScope
    ) {
        if (!property.isOverride) {
            return
        }

        val overriddenPropertySymbols = firTypeScope.getDirectOverridesOf(property)

        if (overriddenPropertySymbols.isEmpty()) {
            return
        }

        val restriction = property.checkReturnType(
            regularClass = regularClass,
            overriddenSymbols = overriddenPropertySymbols,
            context = context,
            typeCheckerContext = typeCheckerContext
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