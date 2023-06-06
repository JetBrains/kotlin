/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.mpp

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.isEnumClass
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.isSubstitutionOrIntersectionOverride
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.mpp.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.mpp.ExpectActualMatchingContext
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.castAll

class FirExpectActualMatchingContext(
    private val actualSession: FirSession,
    private val scopeSession: ScopeSession
) : ExpectActualMatchingContext<FirBasedSymbol<*>>, TypeSystemContext by actualSession.typeContext {
    override val shouldCheckReturnTypesOfCallables: Boolean
        get() = false

    override val enumConstructorsAreAlwaysCompatible: Boolean
        get() = true

    private fun CallableSymbolMarker.asSymbol(): FirCallableSymbol<*> = this as FirCallableSymbol<*>
    private fun FunctionSymbolMarker.asSymbol(): FirFunctionSymbol<*> = this as FirFunctionSymbol<*>
    private fun PropertySymbolMarker.asSymbol(): FirPropertySymbol = this as FirPropertySymbol
    private fun ValueParameterSymbolMarker.asSymbol(): FirValueParameterSymbol = this as FirValueParameterSymbol
    private fun TypeParameterSymbolMarker.asSymbol(): FirTypeParameterSymbol = this as FirTypeParameterSymbol
    private fun ClassLikeSymbolMarker.asSymbol(): FirClassLikeSymbol<*> = this as FirClassLikeSymbol<*>
    private fun RegularClassSymbolMarker.asSymbol(): FirRegularClassSymbol = this as FirRegularClassSymbol
    private fun TypeAliasSymbolMarker.asSymbol(): FirTypeAliasSymbol = this as FirTypeAliasSymbol

    override val RegularClassSymbolMarker.classId: ClassId
        get() = asSymbol().classId
    override val TypeAliasSymbolMarker.classId: ClassId
        get() = asSymbol().classId
    override val CallableSymbolMarker.callableId: CallableId
        get() = asSymbol().callableId

    override val TypeParameterSymbolMarker.parameterName: Name
        get() = asSymbol().name
    override val ValueParameterSymbolMarker.parameterName: Name
        get() = asSymbol().name

    override fun TypeAliasSymbolMarker.expandToRegularClass(): RegularClassSymbolMarker? {
        return asSymbol()
            .resolvedExpandedTypeRef
            .coneType
            .fullyExpandedType(actualSession)
            .toSymbol(actualSession) as? FirRegularClassSymbol
    }

    override val RegularClassSymbolMarker.classKind: ClassKind
        get() = asSymbol().classKind
    override val RegularClassSymbolMarker.isCompanion: Boolean
        get() = asSymbol().resolvedStatus.isCompanion
    override val RegularClassSymbolMarker.isInner: Boolean
        get() = asSymbol().resolvedStatus.isInner
    override val RegularClassSymbolMarker.isInline: Boolean
        get() = asSymbol().resolvedStatus.isInline
    override val RegularClassSymbolMarker.isValue: Boolean
        get() = asSymbol().resolvedStatus.isInline

    /*
     * In this context java interfaces should be considered as not fun interface, so they will be later checked by [isNotSamInterface] function
     */
    override val RegularClassSymbolMarker.isFun: Boolean
        get() = asSymbol().takeUnless { it.origin is FirDeclarationOrigin.Java }?.resolvedStatus?.isFun ?: false

    override val ClassLikeSymbolMarker.typeParameters: List<TypeParameterSymbolMarker>
        get() = asSymbol().typeParameterSymbols

    override val ClassLikeSymbolMarker.modality: Modality?
        get() = asSymbol().resolvedStatus.modality
    override val ClassLikeSymbolMarker.visibility: Visibility
        get() = asSymbol().resolvedStatus.visibility

    override val CallableSymbolMarker.modality: Modality?
        get() = asSymbol().resolvedStatus.modality
    override val CallableSymbolMarker.visibility: Visibility
        get() = asSymbol().resolvedStatus.visibility

    override val CallableSymbolMarker.isExpect: Boolean
        get() = asSymbol().resolvedStatus.isExpect
    override val CallableSymbolMarker.isInline: Boolean
        get() = asSymbol().resolvedStatus.isInline
    override val CallableSymbolMarker.isSuspend: Boolean
        get() = asSymbol().resolvedStatus.isSuspend
    override val CallableSymbolMarker.isExternal: Boolean
        get() = asSymbol().resolvedStatus.isExternal
    override val CallableSymbolMarker.isInfix: Boolean
        get() = asSymbol().resolvedStatus.isInfix
    override val CallableSymbolMarker.isOperator: Boolean
        get() = asSymbol().resolvedStatus.isOperator
    override val CallableSymbolMarker.isTailrec: Boolean
        get() = asSymbol().resolvedStatus.isTailRec

    override val PropertySymbolMarker.isVar: Boolean
        get() = asSymbol().isVar
    override val PropertySymbolMarker.isLateinit: Boolean
        get() = asSymbol().resolvedStatus.isLateInit
    override val PropertySymbolMarker.isConst: Boolean
        get() = asSymbol().resolvedStatus.isConst

    override val PropertySymbolMarker.setter: FunctionSymbolMarker?
        get() = asSymbol().setterSymbol

    @OptIn(UnsafeCastFunction::class)
    override fun createExpectActualTypeParameterSubstitutor(
        expectTypeParameters: List<TypeParameterSymbolMarker>,
        actualTypeParameters: List<TypeParameterSymbolMarker>,
        parentSubstitutor: TypeSubstitutorMarker?,
    ): TypeSubstitutorMarker {
        return createExpectActualTypeParameterSubstitutor(
            expectTypeParameters.castAll<FirTypeParameterSymbol>(),
            actualTypeParameters.castAll<FirTypeParameterSymbol>(),
            actualSession,
            parentSubstitutor as ConeSubstitutor?
        )
    }

    override val RegularClassSymbolMarker.superTypes: List<KotlinTypeMarker>
        get() = asSymbol().resolvedSuperTypes

    override fun RegularClassSymbolMarker.collectAllMembers(isActualDeclaration: Boolean): List<FirBasedSymbol<*>> {
        val symbol = asSymbol()
        val session = when (isActualDeclaration) {
            true -> actualSession
            false -> symbol.moduleData.session
        }

        val scope = symbol.defaultType().scope(
            useSiteSession = session,
            scopeSession,
            FakeOverrideTypeCalculator.DoNothing,
            requiredMembersPhase = FirResolvePhase.STATUS,
        ) ?: return emptyList()

        return mutableListOf<FirBasedSymbol<*>>().apply {
            for (name in scope.getCallableNames()) {
                scope.getMembersTo(this, name)
            }

            // TODO: replace with scope lookup
            for (name in symbol.declarationSymbols.mapNotNull { (it as? FirRegularClassSymbol)?.classId?.shortClassName }) {
                addIfNotNull(scope.getSingleClassifier(name) as? FirRegularClassSymbol)
            }
            getConstructorsTo(this, scope)
        }
    }

    override fun RegularClassSymbolMarker.getMembersForExpectClass(name: Name): List<FirCallableSymbol<*>> {
        val symbol = asSymbol()
        val scope = symbol.defaultType().scope(
            useSiteSession = symbol.moduleData.session,
            scopeSession,
            FakeOverrideTypeCalculator.DoNothing,
            requiredMembersPhase = FirResolvePhase.STATUS,
        ) ?: return emptyList()

        return mutableListOf<FirCallableSymbol<*>>().apply {
            scope.getMembersTo(this, name)
        }
    }

    fun FirClassSymbol<*>.getConstructors(
        scopeSession: ScopeSession,
        session: FirSession = moduleData.session
    ): Collection<FirConstructorSymbol> = mutableListOf<FirConstructorSymbol>().apply {
        getConstructorsTo(
            this,
            unsubstitutedScope(
                session,
                scopeSession,
                withForcedTypeCalculator = false,
                memberRequiredPhase = FirResolvePhase.STATUS,
            )
        )
    }


    private fun getConstructorsTo(destination: MutableList<in FirConstructorSymbol>, scope: FirTypeScope) {
        scope.getDeclaredConstructors().mapTo(destination) { it }
    }

    private fun FirTypeScope.getMembersTo(destination: MutableList<in FirCallableSymbol<*>>, name: Name) {
        processFunctionsByName(name) { destination.add(it) }
        processPropertiesByName(name) { destination.add(it) }
    }

    override fun RegularClassSymbolMarker.collectEnumEntryNames(): List<Name> {
        return asSymbol().fir.collectEnumEntries().map { it.name }
    }

    override val CallableSymbolMarker.dispatchReceiverType: SimpleTypeMarker?
        get() = asSymbol().dispatchReceiverType
    override val CallableSymbolMarker.extensionReceiverType: KotlinTypeMarker?
        get() = asSymbol().resolvedReceiverTypeRef?.coneType
    override val CallableSymbolMarker.returnType: KotlinTypeMarker
        get() = asSymbol().resolvedReturnType.type
    override val CallableSymbolMarker.typeParameters: List<TypeParameterSymbolMarker>
        get() = asSymbol().typeParameterSymbols
    override val FunctionSymbolMarker.valueParameters: List<ValueParameterSymbolMarker>
        get() = asSymbol().valueParameterSymbols

    override val ValueParameterSymbolMarker.isVararg: Boolean
        get() = asSymbol().isVararg
    override val ValueParameterSymbolMarker.isNoinline: Boolean
        get() = asSymbol().isNoinline
    override val ValueParameterSymbolMarker.isCrossinline: Boolean
        get() = asSymbol().isCrossinline
    override val ValueParameterSymbolMarker.hasDefaultValue: Boolean
        get() = asSymbol().hasDefaultValue

    override fun CallableSymbolMarker.isAnnotationConstructor(): Boolean {
        val symbol = asSymbol()
        return symbol.isAnnotationConstructor(symbol.moduleData.session)
    }

    override val TypeParameterSymbolMarker.bounds: List<KotlinTypeMarker>
        get() = asSymbol().resolvedBounds.map { it.coneType }

    override val TypeParameterSymbolMarker.variance: Variance
        get() = asSymbol().variance

    override val TypeParameterSymbolMarker.isReified: Boolean
        get() = asSymbol().isReified

    override fun areCompatibleExpectActualTypes(
        expectType: KotlinTypeMarker?,
        actualType: KotlinTypeMarker?,
    ): Boolean {
        if (expectType == null) return actualType == null
        if (actualType == null) return false

        return AbstractTypeChecker.equalTypes(
            actualSession.typeContext.newTypeCheckerState(errorTypesEqualToAnything = true, stubTypesEqualToAnything = false),
            expectType,
            actualType
        )
    }

    override fun RegularClassSymbolMarker.isNotSamInterface(): Boolean {
        val type = asSymbol().defaultType()
        val isSam = FirSamResolver(actualSession, scopeSession).isSamType(type)
        return !isSam
    }

    override fun CallableSymbolMarker.shouldSkipMatching(containingExpectClass: RegularClassSymbolMarker): Boolean {
        val symbol = asSymbol()
        val classSymbol = containingExpectClass.asSymbol()
        if (symbol !is FirConstructorSymbol && symbol.dispatchReceiverType?.classId != classSymbol.classId) {
            // Skip fake overrides
            return true
        }
        return symbol.isSubstitutionOrIntersectionOverride // Skip fake overrides
                || !symbol.isExpect // Skip non-expect declarations like equals, hashCode, toString and any inherited declarations from non-expect super types
    }

    override val CallableSymbolMarker.hasStableParameterNames: Boolean
        get() = when (asSymbol().origin) {
            is FirDeclarationOrigin.Java,
            FirDeclarationOrigin.Enhancement,
            FirDeclarationOrigin.DynamicScope -> false
            else -> true
        }
}
