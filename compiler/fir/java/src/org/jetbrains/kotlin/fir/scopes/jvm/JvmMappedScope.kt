/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.jvm

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsSignatures
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.caches.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.isRealOwnerOf
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.FirFakeOverrideGenerator
import org.jetbrains.kotlin.fir.scopes.impl.FirStandardOverrideChecker
import org.jetbrains.kotlin.fir.scopes.impl.buildSubstitutorForOverridesCheck
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.load.java.BuiltinSpecialProperties
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures
import org.jetbrains.kotlin.load.java.getPropertyNamesCandidatesByAccessorName
import org.jetbrains.kotlin.load.kotlin.SignatureBuildingComponents
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

/**
 * @param firKotlinClass Kotlin version of built-in class mapped to some JDK class (e.g. kotlin.collections.List)
 * @param firJavaClass JDK version of some built-in class (e.g. java.util.List)
 * @param declaredMemberScope basic/common declared scope (without any additional members) of a Kotlin version
 * @param javaMappedClassUseSiteScope use-site scope of JDK class
 */
class JvmMappedScope(
    private val session: FirSession,
    private val firKotlinClass: FirRegularClass,
    private val firJavaClass: FirRegularClass,
    private val declaredMemberScope: FirContainingNamesAwareScope,
    private val javaMappedClassUseSiteScope: FirTypeScope,
) : FirTypeScope() {
    private val mappedSymbolCache = session.mappedSymbolStorage.cacheByOwner.getValue(firKotlinClass.symbol)

    private val overrideChecker = FirStandardOverrideChecker(session)

    private val substitutor = createMappingSubstitutor(firJavaClass, firKotlinClass, session)
    private val kotlinDispatchReceiverType = firKotlinClass.defaultType()

    private val declaredScopeOfMutableVersion = JavaToKotlinClassMap.readOnlyToMutable(firKotlinClass.classId)?.let {
        session.symbolProvider.getClassLikeSymbolByClassId(it) as? FirClassSymbol
    }?.let {
        session.declaredMemberScope(it, memberRequiredPhase = null)
    }

    private val isMutableContainer = JavaToKotlinClassMap.isMutable(firKotlinClass.classId)

    private val allJavaMappedSuperClassIds: List<ClassId> by lazy {
        buildList {
            add(firJavaClass.classId)
            lookupSuperTypes(firJavaClass.symbol, lookupInterfaces = true, deep = true, session).mapTo(this) { superType ->
                val originalClassId = superType.lookupTag.classId
                JavaToKotlinClassMap.mapKotlinToJava(originalClassId.asSingleFqName().toUnsafe()) ?: originalClassId
            }
        }
    }

    private val isList: Boolean = firKotlinClass.classId == StandardClassIds.List

    // It's ok to have a super set of actually available member names
    private val myCallableNames: Set<Name> by lazy {
        if (firKotlinClass.isFinal) {
            // For final classes we don't need to load HIDDEN members at all because they might not be overridden
            val signaturePrefix = firJavaClass.symbol.classId.toString()
            val names = (JvmBuiltInsSignatures.VISIBLE_METHOD_SIGNATURES).filter { signature ->
                signature in JvmBuiltInsSignatures.MUTABLE_METHOD_SIGNATURES == isMutableContainer &&
                        signature.startsWith(signaturePrefix)
            }.map { signature ->
                // +1 to delete dot before function name
                Name.identifier(signature.substring(signaturePrefix.length + 1, signature.indexOf("(")))
            }

            declaredMemberScope.getCallableNames() + names
        } else {
            declaredMemberScope.getCallableNames() + javaMappedClassUseSiteScope.getCallableNames()
        }.let {
            // If getFirst/getLast don't exist, we need to add them so that we can mark overrides as deprecated (KT-65440)
            if (isList && (GET_FIRST_NAME !in it || GET_LAST_NAME !in it)) {
                it + listOf(GET_FIRST_NAME, GET_LAST_NAME)
            } else {
                it
            }
        }
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        val declared = mutableListOf<FirNamedFunctionSymbol>()
        declaredMemberScope.processFunctionsByName(name) { symbol ->
            if (FirJvmPlatformDeclarationFilter.isFunctionAvailable(symbol.fir, javaMappedClassUseSiteScope, session)) {
                declared += symbol
                processor(symbol)
            }
        }

        val declaredSignatures: Set<String> by lazy {
            buildSet {
                declared.mapTo(this) { it.fir.computeJvmDescriptor() }
                declaredScopeOfMutableVersion?.processFunctionsByName(name) {
                    add(it.fir.computeJvmDescriptor())
                }
            }
        }

        var needsHiddenFake = isList && (name == GET_FIRST_NAME || name == GET_LAST_NAME)

        javaMappedClassUseSiteScope.processFunctionsByName(name) processor@{ symbol ->
            if (!symbol.isDeclaredInMappedJavaClass() || !(symbol.fir.status as FirResolvedDeclarationStatus).visibility.isPublicAPI) {
                return@processor
            }

            val jvmDescriptor = symbol.fir.computeJvmDescriptor()
            // We don't need adding what is already declared
            if (jvmDescriptor in declaredSignatures) return@processor

            // That condition means that the member is already declared in the built-in class, but has a non-trivially mapped JVM descriptor
            if (isRenamedJdkMethod(jvmDescriptor) || symbol.isOverrideOfKotlinBuiltinPropertyGetter()) return@processor

            // If it's java.lang.List.contains(Object) it being loaded as contains(E) and treated as an override
            // of kotlin.collections.Collection.contains(E), thus we're not loading it as an additional JDK member
            if (isOverrideOfKotlinDeclaredFunction(symbol)) return@processor

            if (isMutabilityViolation(symbol, jvmDescriptor)) return@processor

            val jdkMemberStatus = getJdkMethodStatus(jvmDescriptor)

            if (jdkMemberStatus == JDKMemberStatus.DROP) return@processor
            // hidden methods in final class can't be overridden or called with 'super'
            if ((jdkMemberStatus == JDKMemberStatus.HIDDEN || jdkMemberStatus == JDKMemberStatus.HIDDEN_IN_DECLARING_CLASS_ONLY) && firKotlinClass.isFinal) return@processor

            val newSymbol = mappedSymbolCache.mappedFunctions.getValue(symbol, this to jdkMemberStatus)

            if (needsHiddenFake &&
                allJavaMappedSuperClassIds.any {
                    SignatureBuildingComponents.signature(it, jvmDescriptor) in JvmBuiltInsSignatures.DEPRECATED_LIST_METHODS
                }
            ) {
                needsHiddenFake = false
            }

            processor(newSymbol)
        }

        if (needsHiddenFake) {
            // We're in JDK < 21, i.e., getFirst/getLast don't exist in the List interface yet.
            // We create a fake version of them for the sole purpose of reporting deprecations on to-become-overrides like in LinkedList.
            // This is because we want to rename these two methods in the future,
            // and we want to warn users of older JDKs of a potential breaking change caused by upgrading to JDK >= 21.
            // See KT-65440.
            val fakeSymbol = mappedSymbolCache.hiddenFakeFunctions.getValue(name, this)
            processor(fakeSymbol)
        }
    }

    private fun createHiddenFakeFunction(name: Name): FirNamedFunctionSymbol {
        return buildSimpleFunction {
            moduleData = firKotlinClass.moduleData
            origin = FirDeclarationOrigin.Synthetic.FakeHiddenInPreparationForNewJdk
            status = FirResolvedDeclarationStatusImpl(Visibilities.Public, Modality.OPEN, EffectiveVisibility.Public)
            returnTypeRef = buildResolvedTypeRef {
                type = firKotlinClass.typeParameters.firstOrNull()
                    ?.let { ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), isNullable = false) }
                    ?: ConeErrorType(ConeSimpleDiagnostic("No type parameter found on '${firKotlinClass.classKind}'"))
            }
            this.name = name
            dispatchReceiverType = firKotlinClass.defaultType()
            symbol = FirNamedFunctionSymbol(CallableId(firKotlinClass.classId, name))
            resolvePhase = FirResolvePhase.BODY_RESOLVE
        }.apply {
            hiddenEverywhereBesideSuperCallsStatus = HiddenEverywhereBesideSuperCallsStatus.HIDDEN_FAKE
        }.symbol
    }

    private fun isOverrideOfKotlinDeclaredFunction(symbol: FirNamedFunctionSymbol) =
        javaMappedClassUseSiteScope.anyOverriddenOf(symbol, ::isDeclaredInBuiltinClass)

    private fun isMutabilityViolation(symbol: FirNamedFunctionSymbol, jvmDescriptor: String): Boolean {
        val signature = SignatureBuildingComponents.signature(firJavaClass.classId, jvmDescriptor)
        val isAmongMutableSignatures = signature in JvmBuiltInsSignatures.MUTABLE_METHOD_SIGNATURES
        // If the method belongs to MUTABLE_METHOD_SIGNATURES, but the class is a read-only collection we shouldn't add it.
        // For example, we don't want j.u.Collection.removeIf would got to read-only kotlin.collections.Collection
        // But if the method is not among MUTABLE_METHOD_SIGNATURES, but the class is a mutable version, we skip it too,
        // because it has already been added to the read-only version from which we inherit it.
        // For example, we don't need regular j.u.Collection.stream was duplicated in MutableCollection
        // as it's already present in the read-only version.
        if (isAmongMutableSignatures != isMutableContainer) return true

        return javaMappedClassUseSiteScope.anyOverriddenOf(symbol) {
            !it.isSubstitutionOrIntersectionOverride && it.containingClassLookupTag()?.classId?.let(JavaToKotlinClassMap::isMutable) == true
        }
    }

    private fun FirNamedFunctionSymbol.isOverrideOfKotlinBuiltinPropertyGetter(): Boolean {
        val fqName = firJavaClass.classId.asSingleFqName().child(name)
        if (valueParameterSymbols.isEmpty()) {
            if (fqName in BuiltinSpecialProperties.GETTER_FQ_NAMES) return true
            if (getPropertyNamesCandidatesByAccessorName(name).any(::isTherePropertyWithNameInKotlinClass)) return true
        }

        return false
    }

    // j/l/Number.intValue(), j/u/Collection.remove(I), etc.
    private fun isRenamedJdkMethod(jvmDescriptor: String): Boolean {
        val signature = SignatureBuildingComponents.signature(firJavaClass.classId, jvmDescriptor)
        return signature in SpecialGenericSignatures.JVM_SIGNATURES_FOR_RENAMED_BUILT_INS
    }

    private fun isTherePropertyWithNameInKotlinClass(name: Name): Boolean {
        if (name !in declaredMemberScope.getCallableNames()) return false

        return declaredMemberScope.getProperties(name).isNotEmpty()
    }

    // Mostly, what this function checks is if the member was serialized to built-ins, but not loaded from JDK.
    // Currently, we use FirDeclarationOrigin.Library for all deserialized members, including built-in ones.
    // Another implementation might be `it.origin != FirDeclarationOrigin.Enhancement`, but that shouldn't really matter.
    private fun isDeclaredInBuiltinClass(it: FirNamedFunctionSymbol) =
        it.origin == FirDeclarationOrigin.Library

    private fun FirNamedFunctionSymbol.isDeclaredInMappedJavaClass(): Boolean {
        return !fir.isSubstitutionOrIntersectionOverride && firJavaClass.symbol.toLookupTag().isRealOwnerOf(fir.symbol)
    }

    private fun getJdkMethodStatus(jvmDescriptor: String): JDKMemberStatus {
        for (classId in allJavaMappedSuperClassIds) {
            when (SignatureBuildingComponents.signature(classId, jvmDescriptor)) {
                in JvmBuiltInsSignatures.HIDDEN_METHOD_SIGNATURES -> return JDKMemberStatus.HIDDEN
                in JvmBuiltInsSignatures.VISIBLE_METHOD_SIGNATURES -> return JDKMemberStatus.VISIBLE
                in JvmBuiltInsSignatures.DROP_LIST_METHOD_SIGNATURES -> return JDKMemberStatus.DROP
            }
        }

        // For unknown methods, we use HIDDEN_IN_DECLARING_CLASS_ONLY policy by default,
        // meaning they are hidden in the declaring class but visible with deprecation in overrides.
        return JDKMemberStatus.HIDDEN_IN_DECLARING_CLASS_ONLY
    }

    internal enum class JDKMemberStatus {
        HIDDEN, VISIBLE, DROP, HIDDEN_IN_DECLARING_CLASS_ONLY,
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        declaredMemberScope.processPropertiesByName(name, processor)
    }

    private fun createMappedFunction(symbol: FirNamedFunctionSymbol, jdkMemberStatus: JDKMemberStatus): FirNamedFunctionSymbol {
        val oldFunction = symbol.fir
        val newSymbol = FirNamedFunctionSymbol(CallableId(firKotlinClass.classId, symbol.callableId.callableName))
        FirFakeOverrideGenerator.createCopyForFirFunction(
            newSymbol,
            baseFunction = oldFunction,
            derivedClassLookupTag = firKotlinClass.symbol.toLookupTag(),
            session,
            oldFunction.origin,
            newDispatchReceiverType = kotlinDispatchReceiverType,
            newParameterTypes = oldFunction.valueParameters.map { substitutor.substituteOrSelf(it.returnTypeRef.coneType) },
            newReturnType = substitutor.substituteOrSelf(oldFunction.returnTypeRef.coneType),
            newSource = oldFunction.source,
        ).apply {
            setHiddenAttributeIfNecessary(jdkMemberStatus)
        }
        return newSymbol
    }

    private fun FirCallableDeclaration.setHiddenAttributeIfNecessary(jdkMemberStatus: JDKMemberStatus) {
        if (jdkMemberStatus == JDKMemberStatus.HIDDEN) {
            hiddenEverywhereBesideSuperCallsStatus = HiddenEverywhereBesideSuperCallsStatus.HIDDEN
        } else if (jdkMemberStatus == JDKMemberStatus.HIDDEN_IN_DECLARING_CLASS_ONLY) {
            hiddenEverywhereBesideSuperCallsStatus = HiddenEverywhereBesideSuperCallsStatus.HIDDEN_IN_DECLARING_CLASS_ONLY
        }
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirNamedFunctionSymbol,
        processor: (FirNamedFunctionSymbol, FirTypeScope) -> ProcessorAction
    ) = ProcessorAction.NONE

    private val firKotlinClassConstructors by lazy(LazyThreadSafetyMode.PUBLICATION) {
        firKotlinClass.constructors(session)
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        javaMappedClassUseSiteScope.processDeclaredConstructors processor@{ javaCtorSymbol ->

            fun FirConstructor.isShadowedBy(ctorFromKotlin: FirConstructorSymbol): Boolean {
                // assuming already checked for visibility
                val valueParams = valueParameters
                val valueParamsFromKotlin = ctorFromKotlin.fir.valueParameters
                if (valueParams.size != valueParamsFromKotlin.size) return false
                val substitutor = buildSubstitutorForOverridesCheck(ctorFromKotlin.fir, this@isShadowedBy, session) ?: return false
                return valueParamsFromKotlin.zip(valueParams).all { (kotlinCtorParam, javaCtorParam) ->
                    overrideChecker.isEqualTypes(kotlinCtorParam.returnTypeRef, javaCtorParam.returnTypeRef, substitutor)
                }
            }

            fun FirConstructor.isTrivialCopyConstructor(): Boolean =
                valueParameters.singleOrNull()?.let {
                    (it.returnTypeRef.coneType.lowerBoundIfFlexible() as? ConeClassLikeType)?.lookupTag == firKotlinClass.symbol.toLookupTag()
                } ?: false

            // In K1 it is handled by JvmBuiltInsCustomizer.getConstructors
            // Here the logic is generally the same, but simplified for performance by reordering checks and avoiding checking
            // for the impossible combinations
            val javaCtor = javaCtorSymbol.fir

            if (!javaCtor.status.visibility.isPublicAPI || javaCtor.isDeprecated()) return@processor
            val signature = SignatureBuildingComponents.signature(firJavaClass.classId, javaCtor.computeJvmDescriptor())
            if (signature !in JvmBuiltInsSignatures.VISIBLE_CONSTRUCTOR_SIGNATURES) return@processor
            if (javaCtor.isTrivialCopyConstructor()) return@processor
            if (firKotlinClassConstructors.any { javaCtor.isShadowedBy(it) }) return@processor

            val newSymbol = mappedSymbolCache.mappedConstructors.getValue(javaCtorSymbol, this)
            processor(newSymbol)
        }

        declaredMemberScope.processDeclaredConstructors(processor)
    }

    private fun FirDeclaration.isDeprecated(): Boolean = symbol.getDeprecation(session, callSite = null) != null

    private fun createMappedConstructor(symbol: FirConstructorSymbol): FirConstructorSymbol {
        val oldConstructor = symbol.fir
        val classId = firKotlinClass.classId
        val newSymbol = FirConstructorSymbol(CallableId(classId, classId.shortClassName))
        FirFakeOverrideGenerator.createCopyForFirConstructor(
            newSymbol,
            session,
            oldConstructor,
            derivedClassLookupTag = firKotlinClass.symbol.toLookupTag(),
            oldConstructor.origin,
            newDispatchReceiverType = null,
            newReturnType = substitutor.substituteOrSelf(oldConstructor.returnTypeRef.coneType),
            newParameterTypes = oldConstructor.valueParameters.map { substitutor.substituteOrSelf(it.returnTypeRef.coneType) },
            newTypeParameters = null,
            newContextReceiverTypes = emptyList(),
            isExpect = false,
            deferredReturnTypeCalculation = null,
            newSource = oldConstructor.source,
        )
        return newSymbol
    }

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction = ProcessorAction.NONE

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        declaredMemberScope.processClassifiersByNameWithSubstitution(name, processor)
    }

    override fun getCallableNames(): Set<Name> = myCallableNames

    override fun getClassifierNames(): Set<Name> {
        return declaredMemberScope.getClassifierNames()
    }

    class FirMappedSymbolStorage(private val cachesFactory: FirCachesFactory) : FirSessionComponent {
        constructor(session: FirSession) : this(session.firCachesFactory)

        // Key is the kotlin class
        internal val cacheByOwner: FirCache<FirRegularClassSymbol, MappedSymbolsCache, Nothing?> =
            cachesFactory.createCache { _ -> MappedSymbolsCache(cachesFactory) }

        internal class MappedSymbolsCache(cachesFactory: FirCachesFactory) {
            val mappedFunctions: FirCache<FirNamedFunctionSymbol, FirNamedFunctionSymbol, Pair<JvmMappedScope, JDKMemberStatus>> =
                cachesFactory.createCache { symbol, (scope, jdkMemberStatus) ->
                    scope.createMappedFunction(symbol, jdkMemberStatus)
                }

            val hiddenFakeFunctions: FirCache<Name, FirNamedFunctionSymbol, JvmMappedScope> =
                cachesFactory.createCache { name, scope ->
                    scope.createHiddenFakeFunction(name)
                }

            val mappedConstructors: FirCache<FirConstructorSymbol, FirConstructorSymbol, JvmMappedScope> =
                cachesFactory.createCache { symbol, scope ->
                    scope.createMappedConstructor(symbol)
                }
        }
    }

    companion object {
        /**
         * For fromClass=A<T1, T2>, toClass=B<F1, F1> classes
         * @returns {T1  -> F1, T2 -> F2} substitution
         */
        private fun createMappingSubstitutor(fromClass: FirRegularClass, toClass: FirRegularClass, session: FirSession): ConeSubstitutor =
            ConeSubstitutorByMap(
                fromClass.typeParameters.zip(toClass.typeParameters).associate { (fromTypeParameter, toTypeParameter) ->
                    fromTypeParameter.symbol to ConeTypeParameterTypeImpl(
                        ConeTypeParameterLookupTag(toTypeParameter.symbol),
                        isNullable = false
                    )
                },
                session
            )

        private val GET_FIRST_NAME = Name.identifier("getFirst")
        private val GET_LAST_NAME = Name.identifier("getLast")
    }

    override fun toString(): String {
        return "JVM mapped scope for ${firKotlinClass.classId}"
    }
}

private val FirSession.mappedSymbolStorage: JvmMappedScope.FirMappedSymbolStorage by FirSession.sessionComponentAccessor()
