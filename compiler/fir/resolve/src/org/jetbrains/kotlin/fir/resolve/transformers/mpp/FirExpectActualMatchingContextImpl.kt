/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.mpp

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isJava
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.providers.getRegularClassSymbolByClassId
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.toSymbol
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.resolvedAnnotationsWithArguments
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.mpp.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.mpp.ExpectActualCollectionArgumentsCompatibilityCheckStrategy
import org.jetbrains.kotlin.resolve.calls.mpp.ExpectActualMatchingContext
import org.jetbrains.kotlin.resolve.calls.mpp.ExpectActualMatchingContext.AnnotationCallInfo
import org.jetbrains.kotlin.resolve.checkers.OptInNames
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.AbstractTypeRefiner
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.zipIfSizesAreEqual

class FirExpectActualMatchingContextImpl private constructor(
    private val actualSession: FirSession,
    private val actualScopeSession: ScopeSession,
    private val allowedWritingMemberExpectForActualMapping: Boolean,
) : FirExpectActualMatchingContext, TypeSystemContext by actualSession.typeContext {
    override val shouldCheckDefaultParams: Boolean
        get() = true

    override val allowClassActualizationWithWiderVisibility: Boolean
        get() = true

    override val allowTransitiveSupertypesActualization: Boolean
        get() = true

    override val expectScopeSession: ScopeSession
        // todo KT-63773 design a way for managing scope sessions for common scopes during matching.
        //  Right now we create a new session every time
        get() = ScopeSession()

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

    override val ClassLikeSymbolMarker.modality: Modality
        get() = asSymbol().resolvedStatus.modality
    override val ClassLikeSymbolMarker.visibility: Visibility
        get() = asSymbol().resolvedStatus.visibility

    override val CallableSymbolMarker.modality: Modality
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

    override val PropertySymbolMarker.getter: FunctionSymbolMarker?
        get() = asSymbol().getterSymbol

    override val PropertySymbolMarker.setter: FunctionSymbolMarker?
        get() = asSymbol().setterSymbol

    override fun createExpectActualTypeParameterSubstitutor(
        expectActualTypeParameters: List<Pair<TypeParameterSymbolMarker, TypeParameterSymbolMarker>>,
        parentSubstitutor: TypeSubstitutorMarker?,
    ): TypeSubstitutorMarker {
        @Suppress("UNCHECKED_CAST")
        return createExpectActualTypeParameterSubstitutor(
            expectActualTypeParameters as List<Pair<FirTypeParameterSymbol, FirTypeParameterSymbol>>,
            actualSession,
            parentSubstitutor as ConeSubstitutor?
        )
    }

    override val RegularClassSymbolMarker.superTypes: List<KotlinTypeMarker>
        get() = asSymbol().resolvedSuperTypes

    override val RegularClassSymbolMarker.superTypesRefs: List<TypeRefMarker>
        get() = asSymbol().resolvedSuperTypeRefs

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
            if (isActualDeclaration) actualScopeSession else expectScopeSession,
            CallableCopyTypeCalculator.DoNothing,
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
            expectScopeSession,
            CallableCopyTypeCalculator.DoNothing,
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

    override fun RegularClassSymbolMarker.collectEnumEntries(): List<DeclarationSymbolMarker> {
        return asSymbol().fir.collectEnumEntries().map { it.symbol }
    }

    override val CallableSymbolMarker.dispatchReceiverType: SimpleTypeMarker?
        get() = asSymbol().dispatchReceiverType
    override val CallableSymbolMarker.extensionReceiverType: KotlinTypeMarker?
        get() = asSymbol().resolvedReceiverTypeRef?.coneType
    override val CallableSymbolMarker.extensionReceiverTypeRef: TypeRefMarker?
        get() = asSymbol().resolvedReceiverTypeRef
    override val CallableSymbolMarker.returnType: KotlinTypeMarker
        get() = asSymbol().resolvedReturnType.type
    override val CallableSymbolMarker.returnTypeRef: TypeRefMarker
        get() = asSymbol().resolvedReturnTypeRef
    override val CallableSymbolMarker.typeParameters: List<TypeParameterSymbolMarker>
        get() = asSymbol().typeParameterSymbols

    override fun FunctionSymbolMarker.allRecursivelyOverriddenDeclarationsIncludingSelf(containingClass: RegularClassSymbolMarker?): List<CallableSymbolMarker> {
        return when (val symbol = asSymbol()) {
            is FirConstructorSymbol, is FirFunctionWithoutNameSymbol -> listOf(symbol)
            is FirNamedFunctionSymbol -> {
                if (containingClass == null) return listOf(symbol)
                val session = symbol.moduleData.session
                (listOf(symbol) + symbol.overriddenFunctions(containingClass.asSymbol(), session, actualScopeSession).asSequence())
                    // Tests work even if you don't filter out fake-overrides. Filtering fake-overrides is needed because
                    // the returned descriptors are compared by `equals`. And `equals` for fake-overrides is weird.
                    // I didn't manage to invent a test that would check this condition
                    .filter { !it.isSubstitutionOrIntersectionOverride && it.origin != FirDeclarationOrigin.Delegated }
            }
        }
    }

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

    override val ValueParameterSymbolMarker.hasDefaultValueNonRecursive: Boolean
        get() = asSymbol().hasDefaultValue

    override fun CallableSymbolMarker.isAnnotationConstructor(): Boolean {
        val symbol = asSymbol()
        return symbol.isAnnotationConstructor(symbol.moduleData.session)
    }

    override val TypeParameterSymbolMarker.bounds: List<KotlinTypeMarker>
        get() = asSymbol().resolvedBounds.map { it.coneType }

    override val TypeParameterSymbolMarker.boundsTypeRefs: List<TypeRefMarker>
        get() = asSymbol().resolvedBounds

    override val TypeParameterSymbolMarker.variance: Variance
        get() = asSymbol().variance

    override val TypeParameterSymbolMarker.isReified: Boolean
        get() = asSymbol().isReified

    override fun areCompatibleExpectActualTypes(
        expectType: KotlinTypeMarker?,
        actualType: KotlinTypeMarker?,
        parameterOfAnnotationComparisonMode: Boolean,
        dynamicTypesEqualToAnything: Boolean
    ): Boolean {
        if (expectType == null) return actualType == null
        if (actualType == null) return false

        if (!dynamicTypesEqualToAnything) {
            val isExpectedDynamic = expectType is ConeDynamicType
            val isActualDynamic = actualType is ConeDynamicType
            if (isExpectedDynamic && !isActualDynamic || !isExpectedDynamic && isActualDynamic) {
                return false
            }
        }
        val actualizedExpectType = (expectType as ConeKotlinType).actualize()
        val actualizedActualType = (actualType as ConeKotlinType).actualize()

        if (parameterOfAnnotationComparisonMode && actualizedExpectType is ConeClassLikeType && actualizedExpectType.isArrayType &&
            actualizedActualType is ConeClassLikeType && actualizedActualType.isArrayType
        ) {
            return AbstractTypeChecker.equalTypes(
                createTypeCheckerState(),
                actualizedExpectType.convertToArrayWithOutProjections(),
                actualizedActualType.convertToArrayWithOutProjections()
            )
        }

        return AbstractTypeChecker.equalTypes(
            actualSession.typeContext,
            actualizedExpectType,
            actualizedActualType
        )
    }

    private fun ConeClassLikeType.convertToArrayWithOutProjections(): ConeClassLikeType {
        val argumentsWithOutProjection = Array(typeArguments.size) { i ->
            val typeArgument = typeArguments[i]
            if (typeArgument !is ConeKotlinType) typeArgument
            else ConeKotlinTypeProjectionOut(typeArgument)
        }
        return ConeClassLikeTypeImpl(lookupTag, argumentsWithOutProjection, isNullable)
    }

    override fun actualTypeIsSubtypeOfExpectType(expectType: KotlinTypeMarker, actualType: KotlinTypeMarker): Boolean {
        return AbstractTypeChecker.isSubtypeOf(
            createTypeCheckerState(),
            subType = actualType,
            superType = expectType
        )
    }

    private fun ConeKotlinType.actualize(): ConeKotlinType {
        val classId = classId
        if (this is ConeClassLikeType && classId?.isNestedClass == true) {
            val classSymbol = classId.toSymbol(actualSession)
            if (classSymbol is FirRegularClassSymbol && classSymbol.isExpect) {
                tryExpandExpectNestedClassActualizedViaTypealias(this, classSymbol)?.let {
                    return it.actualizeTypeArguments()
                }
            }
        }
        return fullyExpandedType(actualSession).actualizeTypeArguments()
    }

    private fun ConeKotlinType.actualizeTypeArguments(): ConeKotlinType {
        if (this !is ConeClassLikeType) {
            return this
        }
        return withArguments { arg ->
            if (arg is ConeKotlinTypeProjection) {
                arg.replaceType(arg.type.actualize()) as ConeTypeProjection
            } else arg
        }
    }

    /**
     * In case of `expect` nested classes actualized via typealias we can't simply find actual symbol by `expect` `ClassId`
     * (like we do for top-level classes), because `ClassId` is different.
     * For example, `expect` class `com/example/ExpectClass.Nested` may have actual with id `real/package/ActualTypeliasTarget.Nested`.
     * So, we first expand outermost class, and then construct `ClassId` for nested class.
     */
    private fun tryExpandExpectNestedClassActualizedViaTypealias(
        expectNestedClassType: ConeClassLikeType,
        expectNestedClassSymbol: FirRegularClassSymbol,
    ): ConeClassLikeType? {
        val expectNestedClassId = expectNestedClassSymbol.classId
        val expectOutermostClassId = expectNestedClassId.outermostClassId
        val actualTypealiasSymbol = expectOutermostClassId.toSymbol(actualSession) as? FirTypeAliasSymbol ?: return null
        val actualOutermostClassId = actualTypealiasSymbol.fullyExpandedClass(actualSession)?.classId ?: return null
        val actualNestedClassId = ClassId.fromString(
            expectNestedClassId.asString().replaceFirst(
                expectOutermostClassId.asString(), actualOutermostClassId.asString()
            )
        )
        return actualNestedClassId.constructClassLikeType(
            expectNestedClassType.typeArguments, expectNestedClassType.isNullable, expectNestedClassType.attributes
        )
    }

    private fun createTypeCheckerState(): TypeCheckerState {
        return actualSession.typeContext.newTypeCheckerState(errorTypesEqualToAnything = true, stubTypesEqualToAnything = false)
    }

    override fun RegularClassSymbolMarker.isNotSamInterface(): Boolean {
        val type = asSymbol().defaultType()
        val isSam = FirSamResolver(actualSession, actualScopeSession).isSamType(type)
        return !isSam
    }

    override fun CallableSymbolMarker.isFakeOverride(containingExpectClass: RegularClassSymbolMarker?): Boolean {
        if (containingExpectClass == null) {
            return false
        }
        val symbol = asSymbol()
        val classSymbol = containingExpectClass.asSymbol()
        if (symbol !is FirConstructorSymbol && symbol.dispatchReceiverType?.classId != classSymbol.classId) {
            return true
        }
        return symbol.isSubstitutionOrIntersectionOverride
    }

    override val CallableSymbolMarker.isDelegatedMember: Boolean
        get() = asSymbol().isDelegated

    override val CallableSymbolMarker.hasStableParameterNames: Boolean
        get() = asSymbol().rawStatus.hasStableParameterNames

    override val CallableSymbolMarker.isJavaField: Boolean
        get() = this is FirFieldSymbol && this.fir.unwrapFakeOverrides().isJava

    override val DeclarationSymbolMarker.annotations: List<AnnotationCallInfo>
        get() = asSymbol().resolvedAnnotationsWithArguments.map(::AnnotationCallInfoImpl)

    override fun areAnnotationArgumentsEqual(
        expectAnnotation: AnnotationCallInfo,
        actualAnnotation: AnnotationCallInfo,
        collectionArgumentsCompatibilityCheckStrategy: ExpectActualCollectionArgumentsCompatibilityCheckStrategy,
    ): Boolean {
        fun AnnotationCallInfo.getFirAnnotation(): FirAnnotation {
            return (this as AnnotationCallInfoImpl).annotation
        }
        return areFirAnnotationsEqual(expectAnnotation.getFirAnnotation(), actualAnnotation.getFirAnnotation())
    }

    private fun areFirAnnotationsEqual(annotation1: FirAnnotation, annotation2: FirAnnotation): Boolean {
        fun FirAnnotation.hasResolvedArguments(): Boolean {
            return resolved || (this is FirAnnotationCall && arguments.isEmpty())
        }

        check(annotation1.hasResolvedArguments() && annotation2.hasResolvedArguments()) {
            "By this time compared annotations are expected to have resolved arguments"
        }
        if (!areCompatibleExpectActualTypes(
                annotation1.resolvedType, annotation2.resolvedType, parameterOfAnnotationComparisonMode = false
            )
        ) {
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
            expression1 is FirLiteralExpression<*> && expression2 is FirLiteralExpression<*> -> {
                expression1.value == expression2.value
            }
            else -> true
        }
    }

    private inner class AnnotationCallInfoImpl(val annotation: FirAnnotation) : AnnotationCallInfo {
        override val annotationSymbol: FirAnnotation = annotation

        override val classId: ClassId?
            get() = getAnnotationConeType()?.lookupTag?.classId

        override val isRetentionSource: Boolean
            get() = getAnnotationClass()?.getRetention(actualSession) == AnnotationRetention.SOURCE

        override val isOptIn: Boolean
            get() = getAnnotationClass()?.hasAnnotation(OptInNames.REQUIRES_OPT_IN_CLASS_ID, actualSession) ?: false

        private fun getAnnotationClass(): FirRegularClassSymbol? =
            getAnnotationConeType()?.toRegularClassSymbol(actualSession)

        private fun getAnnotationConeType(): ConeClassLikeType? {
            val coneType = annotation.toAnnotationClassLikeType(actualSession)?.actualize() as? ConeClassLikeType
            if (coneType is ConeErrorType) {
                return null
            }
            return coneType
        }
    }

    override val DeclarationSymbolMarker.hasSourceAnnotationsErased: Boolean
        get() {
            val symbol = asSymbol()
            return symbol.source == null && symbol.origin !is FirDeclarationOrigin.Plugin
        }

    override fun onMatchedMembers(
        expectSymbol: DeclarationSymbolMarker,
        actualSymbol: DeclarationSymbolMarker,
        containingExpectClassSymbol: RegularClassSymbolMarker?,
        containingActualClassSymbol: RegularClassSymbolMarker?
    ) {
        if (containingActualClassSymbol == null || containingExpectClassSymbol == null) return

        containingActualClassSymbol.asSymbol().addMemberExpectForActualMapping(
            expectSymbol.asSymbol(),
            actualSymbol.asSymbol(),
            containingExpectClassSymbol.asSymbol(),
            ExpectActualMatchingCompatibility.MatchedSuccessfully
        )
    }

    override fun onMismatchedMembersFromClassScope(
        expectSymbol: DeclarationSymbolMarker,
        actualSymbolsByIncompatibility: Map<ExpectActualMatchingCompatibility.Mismatch, List<DeclarationSymbolMarker>>,
        containingExpectClassSymbol: RegularClassSymbolMarker?,
        containingActualClassSymbol: RegularClassSymbolMarker?
    ) {
        if (containingExpectClassSymbol == null || containingActualClassSymbol == null) return

        for ((incompatibility, actualSymbols) in actualSymbolsByIncompatibility.entries) {
            for (actualSymbol in actualSymbols) {
                containingActualClassSymbol.asSymbol().addMemberExpectForActualMapping(
                    expectSymbol.asSymbol(),
                    actualSymbol.asSymbol(),
                    containingExpectClassSymbol.asSymbol(),
                    incompatibility,
                )
            }
        }
    }

    private fun FirRegularClassSymbol.addMemberExpectForActualMapping(
        expectMember: FirBasedSymbol<*>, actualMember: FirBasedSymbol<*>,
        expectClassSymbol: FirRegularClassSymbol, compatibility: ExpectActualMatchingCompatibility,
    ) {
        check(allowedWritingMemberExpectForActualMapping) { "Writing memberExpectForActual is not allowed in this context" }
        val fir = fir
        val expectForActualMap = fir.memberExpectForActual ?: mutableMapOf()
        fir.memberExpectForActual = expectForActualMap

        val expectToCompatibilityMap = expectForActualMap.asMutableMap()
            .computeIfAbsent(actualMember to expectClassSymbol) { mutableMapOf() }

        /*
        Don't report when value is overwritten, because it's the case for actual inner classes:
        actual class A {
            actual class B {
                actual fun foo() {} <-- twice checked (from A and B) and added to mapping
            }
        }
        Can be fixed after KT-61361.
         */
        expectToCompatibilityMap.asMutableMap()[expectMember] = compatibility
    }

    private fun <K, V> Map<K, V>.asMutableMap(): MutableMap<K, V> = this as MutableMap

    override val checkClassScopesForAnnotationCompatibility = true

    override fun skipCheckingAnnotationsOfActualClassMember(actualMember: DeclarationSymbolMarker): Boolean {
        return (actualMember.asSymbol().fir as? FirMemberDeclaration)?.isActual == true
    }

    override fun findPotentialExpectClassMembersForActual(
        expectClass: RegularClassSymbolMarker,
        actualClass: RegularClassSymbolMarker,
        actualMember: DeclarationSymbolMarker,
    ): Map<FirBasedSymbol<*>, ExpectActualMatchingCompatibility> {
        val mapping = actualClass.asSymbol().fir.memberExpectForActual
        return mapping?.get(actualMember to expectClass) ?: emptyMap()
    }

    override fun DeclarationSymbolMarker.getSourceElement(): SourceElementMarker = FirSourceElement(asSymbol().source)

    override fun TypeRefMarker.getClassId(): ClassId? = (this as FirResolvedTypeRef).type.fullyExpandedType(actualSession).classId

    override fun checkAnnotationsOnTypeRefAndArguments(
        expectContainingSymbol: DeclarationSymbolMarker,
        actualContainingSymbol: DeclarationSymbolMarker,
        expectTypeRef: TypeRefMarker,
        actualTypeRef: TypeRefMarker,
        checker: ExpectActualMatchingContext.AnnotationsCheckerCallback,
    ) {
        check(expectTypeRef is FirResolvedTypeRef && actualTypeRef is FirResolvedTypeRef)
        checkAnnotationsOnTypeRefAndArgumentsImpl(
            expectContainingSymbol.asSymbol(), actualContainingSymbol.asSymbol(),
            expectTypeRef, actualTypeRef, checker
        )
    }

    private fun checkAnnotationsOnTypeRefAndArgumentsImpl(
        expectContainingSymbol: FirBasedSymbol<*>,
        actualContainingSymbol: FirBasedSymbol<*>,
        expectTypeRef: FirTypeRef?,
        actualTypeRef: FirTypeRef?,
        checker: ExpectActualMatchingContext.AnnotationsCheckerCallback,
    ) {
        fun FirAnnotationContainer.getAnnotations(anchor: FirBasedSymbol<*>): List<AnnotationCallInfoImpl> {
            return resolvedAnnotationsWithArguments(anchor).map(::AnnotationCallInfoImpl)
        }

        if (expectTypeRef == null || actualTypeRef == null) return
        if (expectTypeRef is FirErrorTypeRef || actualTypeRef is FirErrorTypeRef) return

        checker.check(
            expectTypeRef.getAnnotations(expectContainingSymbol), actualTypeRef.getAnnotations(actualContainingSymbol),
            FirSourceElement(actualTypeRef.source)
        )

        val expectDelegatedTypeRef = (expectTypeRef as? FirResolvedTypeRef)?.delegatedTypeRef ?: return
        val actualDelegatedTypeRef = (actualTypeRef as? FirResolvedTypeRef?)?.delegatedTypeRef ?: return

        when {
            expectDelegatedTypeRef is FirUserTypeRef && actualDelegatedTypeRef is FirUserTypeRef -> {
                val expectQualifier = expectDelegatedTypeRef.qualifier
                val actualQualifier = actualDelegatedTypeRef.qualifier
                for ((expectPart, actualPart) in expectQualifier.zipIfSizesAreEqual(actualQualifier).orEmpty()) {
                    val expectPartTypeArguments = expectPart.typeArgumentList.typeArguments
                    val actualPartTypeArguments = actualPart.typeArgumentList.typeArguments
                    val zippedArgs = expectPartTypeArguments.zipIfSizesAreEqual(actualPartTypeArguments).orEmpty()
                    for ((expectTypeArgument, actualTypeArgument) in zippedArgs) {
                        if (expectTypeArgument !is FirTypeProjectionWithVariance || actualTypeArgument !is FirTypeProjectionWithVariance) {
                            continue
                        }
                        checkAnnotationsOnTypeRefAndArgumentsImpl(
                            expectContainingSymbol, actualContainingSymbol,
                            expectTypeArgument.typeRef, actualTypeArgument.typeRef, checker
                        )
                    }
                }
            }
            expectDelegatedTypeRef is FirFunctionTypeRef && actualDelegatedTypeRef is FirFunctionTypeRef -> {
                checkAnnotationsOnTypeRefAndArgumentsImpl(
                    expectContainingSymbol, actualContainingSymbol,
                    expectDelegatedTypeRef.receiverTypeRef, actualDelegatedTypeRef.receiverTypeRef, checker,
                )
                checkAnnotationsOnTypeRefAndArgumentsImpl(
                    expectContainingSymbol, actualContainingSymbol,
                    expectDelegatedTypeRef.returnTypeRef, actualDelegatedTypeRef.returnTypeRef, checker,
                )

                val expectParams = expectDelegatedTypeRef.parameters
                val actualParams = actualDelegatedTypeRef.parameters
                for ((expectParam, actualParam) in expectParams.zipIfSizesAreEqual(actualParams).orEmpty()) {
                    checkAnnotationsOnTypeRefAndArgumentsImpl(
                        expectContainingSymbol, actualContainingSymbol,
                        expectParam.returnTypeRef, actualParam.returnTypeRef, checker
                    )
                }
            }
        }
    }

    object Factory : FirExpectActualMatchingContextFactory {
        override fun create(
            actualSession: FirSession, actualScopeSession: ScopeSession,
            allowedWritingMemberExpectForActualMapping: Boolean,
        ): FirExpectActualMatchingContextImpl =
            FirExpectActualMatchingContextImpl(actualSession, actualScopeSession, allowedWritingMemberExpectForActualMapping)
    }
}
