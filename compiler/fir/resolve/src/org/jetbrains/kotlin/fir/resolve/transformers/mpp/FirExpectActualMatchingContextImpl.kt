/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.mpp

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirExpectActualMatchingContext
import org.jetbrains.kotlin.fir.FirExpectActualMatchingContextFactory
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.getRetention
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
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
import org.jetbrains.kotlin.resolve.calls.mpp.ExpectActualCollectionArgumentsCompatibilityCheckStrategy
import org.jetbrains.kotlin.resolve.calls.mpp.ExpectActualMatchingContext.AnnotationCallInfo
import org.jetbrains.kotlin.resolve.checkers.OptInNames
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.kotlin.types.model.TypeSubstitutorMarker
import org.jetbrains.kotlin.types.model.TypeSystemContext
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.castAll

class FirExpectActualMatchingContextImpl private constructor(
    private val actualSession: FirSession,
    private val scopeSession: ScopeSession
) : FirExpectActualMatchingContext, TypeSystemContext by actualSession.typeContext {
    override val shouldCheckReturnTypesOfCallables: Boolean
        get() = false

    override val enumConstructorsAreAlwaysCompatible: Boolean
        get() = true

    override val allowClassActualizationWithWiderVisibility: Boolean
        get() = true

    override val allowTransitiveSupertypesActualization: Boolean
        get() = true

    private fun DeclarationSymbolMarker.asSymbol(): FirBasedSymbol<*> = this as FirBasedSymbol<*>
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

    override val RegularClassSymbolMarker.defaultType: KotlinTypeMarker
        get() = asSymbol().defaultType()

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

            for (name in scope.getClassifierNames()) {
                scope.processClassifiersByName(name) {
                    // We should skip nested classes from supertypes here
                    if (it is FirRegularClassSymbol && it.classId.parentClassId == symbol.classId) {
                        add(it)
                    }
                }
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

    override fun FirClassSymbol<*>.getConstructors(
        scopeSession: ScopeSession,
        session: FirSession,
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
            createTypeCheckerState(),
            expectType,
            actualType
        )
    }

    override fun actualTypeIsSubtypeOfExpectType(expectType: KotlinTypeMarker, actualType: KotlinTypeMarker): Boolean {
        return AbstractTypeChecker.isSubtypeOf(
            createTypeCheckerState(),
            subType = actualType,
            superType = expectType
        )
    }

    private fun createTypeCheckerState(): TypeCheckerState {
        return actualSession.typeContext.newTypeCheckerState(errorTypesEqualToAnything = true, stubTypesEqualToAnything = false)
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
        get() = asSymbol().rawStatus.hasStableParameterNames

    override val DeclarationSymbolMarker.annotations: List<AnnotationCallInfo>
        get() = asSymbol().resolvedAnnotationsWithArguments.map(::AnnotationCallInfoImpl)

    override fun areAnnotationArgumentsEqual(
        annotation1: AnnotationCallInfo,
        annotation2: AnnotationCallInfo,
        collectionArgumentsCompatibilityCheckStrategy: ExpectActualCollectionArgumentsCompatibilityCheckStrategy,
    ): Boolean {
        fun AnnotationCallInfo.getFirAnnotation(): FirAnnotation {
            return (this as AnnotationCallInfoImpl).annotation
        }
        return areFirAnnotationsEqual(annotation1.getFirAnnotation(), annotation2.getFirAnnotation())
    }

    private fun areFirAnnotationsEqual(annotation1: FirAnnotation, annotation2: FirAnnotation): Boolean {
        if (!areCompatibleExpectActualTypes(annotation1.typeRef.coneType, annotation2.typeRef.coneType)) {
            return false
        }
        val args1 = annotation1.argumentMapping.mapping
        val args2 = annotation2.argumentMapping.mapping
        if (args1.size != args2.size) {
            return false
        }
        return args1.all { (key, value1) ->
            val value2 = args2[key]
            value2 != null && areAnnotationArgumentsEqual(value1, value2)
        }
    }

    private fun areAnnotationArgumentsEqual(expression1: FirExpression, expression2: FirExpression): Boolean {
        // In K2 const expression calculated in backend.
        // Because of that, we have "honest" checker at backend IR stage
        // and "only simplest case" checker in frontend, so that we have at least some reporting in the IDE.
        return when {
            expression1 is FirConstExpression<*> && expression2 is FirConstExpression<*> -> {
                expression1.value == expression2.value
            }
            else -> true
        }
    }

    private inner class AnnotationCallInfoImpl(val annotation: FirAnnotation) : AnnotationCallInfo {
        override val annotationSymbol: FirAnnotation = annotation

        override val classId: ClassId?
            get() = annotation.toAnnotationClassId(actualSession)

        override val isRetentionSource: Boolean
            get() = getAnnotationClass()?.getRetention(actualSession) == AnnotationRetention.SOURCE

        override val isOptIn: Boolean
            get() = getAnnotationClass()?.hasAnnotation(OptInNames.REQUIRES_OPT_IN_CLASS_ID, actualSession) ?: false

        private fun getAnnotationClass(): FirRegularClassSymbol? =
            annotation.annotationTypeRef.coneType.toRegularClassSymbol(actualSession)
    }

    override val DeclarationSymbolMarker.hasSourceAnnotationsErased: Boolean
        get() {
            val symbol = asSymbol()
            return symbol.source == null && symbol.origin !is FirDeclarationOrigin.Plugin
        }

    object Factory : FirExpectActualMatchingContextFactory {
        override fun create(session: FirSession, scopeSession: ScopeSession): FirExpectActualMatchingContextImpl =
            FirExpectActualMatchingContextImpl(session, scopeSession)
    }
}
