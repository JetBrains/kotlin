/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.jvm

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsSignatures
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.dispatchReceiverClassLookupTagOrNull
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.isSubstitutionOrIntersectionOverride
import org.jetbrains.kotlin.fir.java.JavaScopeProvider
import org.jetbrains.kotlin.fir.java.createConstantOrError
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.FirClassSubstitutionScope
import org.jetbrains.kotlin.fir.scopes.impl.FirFakeOverrideGenerator
import org.jetbrains.kotlin.fir.scopes.impl.FirStandardOverrideChecker
import org.jetbrains.kotlin.fir.scopes.impl.buildSubstitutorForOverridesCheck
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.load.kotlin.SignatureBuildingComponents
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

class JvmMappedScope(
    private val session: FirSession,
    private val firKotlinClass: FirRegularClass,
    private val firJavaClass: FirJavaClass,
    private val declaredMemberScope: FirContainingNamesAwareScope,
    private val scopeSession: ScopeSession,
) : FirTypeScope() {
    private val functionsCache = mutableMapOf<FirNamedFunctionSymbol, FirNamedFunctionSymbol>()

    private val constructorsCache = mutableMapOf<FirConstructorSymbol, FirConstructorSymbol>()

    private val overrideChecker = FirStandardOverrideChecker(session)

    private val substitutor = createMappingSubstitutor(firJavaClass, firKotlinClass, session)
    private val kotlinDispatchReceiverType = firKotlinClass.defaultType()

    // This scope looks just as regular java.util.Collection, but we pretend like the class inherits kotlin.collections.MutableCollection.
    // We do it because we have a logic in JavaClassUseSiteMemberScope that once sees that a Java class inherits some built-in collection-like class,
    // that "renames" and "change" signature for all the Java methods that are effective overrides of built-in's members.
    //
    // Here, it helps us to detect that we don't need to add additional method to the scope of kotlin.collection.*
    // if the relevant JDK method is already mapped to existing built-in member.
    // Namely, with this "fake" inheritance, the member from JDK is considered to be mapped whenever it "overrides" some existing built-in member.
    private val javaMappedClassUseSiteScopeWithCustomSupertype =
        JavaScopeProvider.getUseSiteMemberScope(
            firJavaClass, session,
            // We explicitly use fresh ScopeSession, to make sure we do not leak irregular scope to the common session
            scopeSession,
            memberRequiredPhase = null,
            additionalSupertypeScope = buildUseSiteScopeForKotlinMirrorClassWithRegularDeclaredScope(firJavaClass, session, scopeSession)
        )

    private val isMutable = JavaToKotlinClassMap.isMutable(firKotlinClass.classId)

    private val myCallableNames by lazy {
        if (firKotlinClass.isFinal) {

            val signaturePrefix = firJavaClass.symbol.classId.toString()
            val names = (JvmBuiltInsSignatures.VISIBLE_METHOD_SIGNATURES).filter { signature ->
                signature in JvmBuiltInsSignatures.MUTABLE_METHOD_SIGNATURES == isMutable &&
                        signature.startsWith(signaturePrefix)
            }.map { signature ->
                // +1 to delete dot before function name
                Name.identifier(signature.substring(signaturePrefix.length + 1, signature.indexOf("(")))
            }

            declaredMemberScope.getCallableNames() + names
        } else
            declaredMemberScope.getCallableNames() + javaMappedClassUseSiteScopeWithCustomSupertype.getCallableNames()
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        val declared = mutableListOf<FirNamedFunctionSymbol>()
        declaredMemberScope.processFunctionsByName(name) { symbol ->
            declared += symbol
            processor(symbol)
        }

        val declaredSignatures by lazy {
            declared.mapTo(mutableSetOf()) { it.fir.computeJvmDescriptor() }
        }

        javaMappedClassUseSiteScopeWithCustomSupertype.processFunctionsByName(name) processor@{ symbol ->
            if (!symbol.isDeclaredInMappedJavaClass() || !(symbol.fir.status as FirResolvedDeclarationStatus).visibility.isPublicAPI) {
                return@processor
            }

            val jvmDescriptor = symbol.fir.computeJvmDescriptor()
            if (jvmDescriptor in declaredSignatures || isOverrideOfKotlinDeclaredFunction(symbol) || isMutabilityViolation(
                    symbol,
                    jvmDescriptor
                )
            ) {
                return@processor
            }

            val jdkMemberStatus = getJdkMethodStatus(jvmDescriptor)

            if (jdkMemberStatus == JDKMemberStatus.DROP) return@processor
            // hidden methods in final class can't be overridden or called with 'super'
            if (jdkMemberStatus == JDKMemberStatus.HIDDEN && firKotlinClass.isFinal) return@processor

            val newSymbol = getOrCreateSubstitutedCopy(symbol, jdkMemberStatus)
            processor(newSymbol)
        }
    }

    private fun isOverrideOfKotlinDeclaredFunction(symbol: FirNamedFunctionSymbol) =
        javaMappedClassUseSiteScopeWithCustomSupertype.anyOverriddenOf(symbol, ::isDeclaredInBuiltinClass)

    private fun isMutabilityViolation(symbol: FirNamedFunctionSymbol, jvmDescriptor: String): Boolean {
        val signature = SignatureBuildingComponents.signature(firJavaClass.classId, jvmDescriptor)
        if ((signature in JvmBuiltInsSignatures.MUTABLE_METHOD_SIGNATURES) xor isMutable) return true

        return javaMappedClassUseSiteScopeWithCustomSupertype.anyOverriddenOf(symbol) {
            !it.origin.fromSupertypes && it.containingClassLookupTag()?.classId?.let(JavaToKotlinClassMap::isMutable) == true
        }
    }

    private fun isDeclaredInBuiltinClass(it: FirNamedFunctionSymbol) =
        it.origin == FirDeclarationOrigin.BuiltIns || it.origin == FirDeclarationOrigin.Library

    private fun FirNamedFunctionSymbol.isDeclaredInMappedJavaClass(): Boolean {
        return !fir.isSubstitutionOrIntersectionOverride && fir.dispatchReceiverClassLookupTagOrNull() == firJavaClass.symbol.toLookupTag()
    }

    private fun getJdkMethodStatus(jvmDescriptor: String): JDKMemberStatus {
        val allClassIds = buildList {
            add(firJavaClass.classId)
            lookupSuperTypes(firJavaClass.symbol, lookupInterfaces = true, deep = true, session).mapTo(this) { superType ->
                val originalClassId = superType.fullyExpandedType(session).lookupTag.classId
                JavaToKotlinClassMap.mapKotlinToJava(originalClassId.asSingleFqName().toUnsafe()) ?: originalClassId
            }
        }

        for (classId in allClassIds) {
            when (SignatureBuildingComponents.signature(classId, jvmDescriptor)) {
                in JvmBuiltInsSignatures.HIDDEN_METHOD_SIGNATURES -> return JDKMemberStatus.HIDDEN
                in JvmBuiltInsSignatures.VISIBLE_METHOD_SIGNATURES -> return JDKMemberStatus.VISIBLE
                in JvmBuiltInsSignatures.DROP_LIST_METHOD_SIGNATURES -> return JDKMemberStatus.DROP
            }
        }

        return JDKMemberStatus.NOT_CONSIDERED
    }

    private enum class JDKMemberStatus {
        HIDDEN, VISIBLE, NOT_CONSIDERED, DROP
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        declaredMemberScope.processPropertiesByName(name, processor)
    }

    private fun getOrCreateSubstitutedCopy(symbol: FirNamedFunctionSymbol, jdkMemberStatus: JDKMemberStatus): FirNamedFunctionSymbol {
        return functionsCache.getOrPut(symbol) {
            val oldFunction = symbol.fir
            val newSymbol = FirNamedFunctionSymbol(CallableId(firKotlinClass.classId, symbol.callableId.callableName))
            FirFakeOverrideGenerator.createCopyForFirFunction(
                newSymbol,
                baseFunction = symbol.fir,
                derivedClassLookupTag = firKotlinClass.symbol.toLookupTag(),
                session,
                symbol.fir.origin,
                newDispatchReceiverType = kotlinDispatchReceiverType,
                newParameterTypes = oldFunction.valueParameters.map { substitutor.substituteOrSelf(it.returnTypeRef.coneType) },
                newReturnType = substitutor.substituteOrSelf(oldFunction.returnTypeRef.coneType),
                additionalAnnotations = when (jdkMemberStatus) {
                    JDKMemberStatus.NOT_CONSIDERED -> listOf(createDeprecatedAnnotationForNotConsidered())
                    else -> emptyList()
                }
            ).apply {
                if (jdkMemberStatus == JDKMemberStatus.HIDDEN) {
                    isHiddenEverywhereBesideSuperCalls = true
                }
            }
            newSymbol
        }
    }

    private fun createDeprecatedAnnotationForNotConsidered(): FirAnnotation =
        buildAnnotation {
            val lookupTag = StandardClassIds.Annotations.Deprecated.toLookupTag()
            annotationTypeRef = buildResolvedTypeRef {
                type = ConeClassLikeTypeImpl(lookupTag, emptyArray(), isNullable = false)
            }

            argumentMapping = buildAnnotationArgumentMapping {
                mapping[StandardClassIds.Annotations.ParameterNames.deprecatedMessage] =
                    "This member is not fully supported by Kotlin compiler, so it may be absent or have different signature in next major version"
                        .createConstantOrError(session)
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
        javaMappedClassUseSiteScopeWithCustomSupertype.processDeclaredConstructors processor@{ javaCtorSymbol ->

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
            if (signature in JvmBuiltInsSignatures.HIDDEN_CONSTRUCTOR_SIGNATURES) return@processor
            if (javaCtor.isTrivialCopyConstructor()) return@processor
            if (firKotlinClassConstructors.any { javaCtor.isShadowedBy(it) }) return@processor

            val newSymbol = getOrCreateCopy(javaCtorSymbol)
            processor(newSymbol)
        }

        declaredMemberScope.processDeclaredConstructors(processor)
    }

    private fun FirDeclaration.isDeprecated(): Boolean = symbol.getDeprecation(session, callSite = null) != null

    private fun getOrCreateCopy(symbol: FirConstructorSymbol): FirConstructorSymbol {
        return constructorsCache.getOrPut(symbol) {
            val oldConstructor = symbol.fir
            val classId = firKotlinClass.classId
            val newSymbol = FirConstructorSymbol(CallableId(classId, classId.shortClassName))
            FirFakeOverrideGenerator.createCopyForFirConstructor(
                newSymbol,
                session,
                oldConstructor,
                derivedClassLookupTag = firKotlinClass.symbol.toLookupTag(),
                symbol.fir.origin,
                newDispatchReceiverType = null,
                newReturnType = substitutor.substituteOrSelf(oldConstructor.returnTypeRef.coneType),
                newParameterTypes = oldConstructor.valueParameters.map { substitutor.substituteOrSelf(it.returnTypeRef.coneType) },
                newTypeParameters = null,
                newContextReceiverTypes = emptyList(),
                isExpect = false,
                fakeOverrideSubstitution = null
            )
            newSymbol
        }
    }

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction = ProcessorAction.NONE

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        declaredMemberScope.processClassifiersByNameWithSubstitution(name, processor)
    }

    override fun getCallableNames(): Set<Name> {
        // It's ok to return a super set of actually available member names
        return myCallableNames
    }

    override fun getClassifierNames(): Set<Name> {
        return declaredMemberScope.getClassifierNames()
    }

    companion object {
        private object ScopeSessionForCommonLikeBuiltIn : ScopeSessionKey<ScopeSessionForCommonLikeBuiltIn, ScopeSession>()
        /**
         * @return platform-independent scope of (probably) mutable version of built-in class that is mapped to firJavaClass
         */
        private fun buildUseSiteScopeForKotlinMirrorClassWithRegularDeclaredScope(
            firJavaClass: FirJavaClass,
            session: FirSession,
            mainScopeSession: ScopeSession,
        ): FirTypeScope? {
            val kotlinMutableVersionClassId =
                JavaToKotlinClassMap.mapJavaToKotlin(firJavaClass.classId.asSingleFqName())
                    ?.let { JavaToKotlinClassMap.readOnlyToMutable(it) ?: it } ?: return null

            val kotlinMutableVersionFirClass =
                session.symbolProvider.getClassLikeSymbolByClassId(kotlinMutableVersionClassId)?.fir as? FirRegularClass ?: return null

            return session.kotlinScopeProvider.getUseSiteMemberScope(
                kotlinMutableVersionFirClass, session,
                // While we're currently building JvmMappedScope wrapper for the declaration scope for mapped Kotlin built-in class,
                // it's necessary to avoid computation recursion like
                // kotlin.collection.MutableCollection (1) -> java.util.Collection (2) -> kotlin.collection.MutableCollection (3)
                // So we make sure that when we're at step (3) we don't go again to JvmMappedScope there and build something like
                // the scope of platform-independent version for kotlin.collection.MutableCollection.
                //
                // In K1, that was handled via using FallbackBuiltIns.Instance at JvmBuiltInsCustomizer.getAdditionalFunctions
                // Having some weird ScopeSession flag looks really code-smell, but alternatives:
                // - having specific scope provider/session for such classes (where `declaredMemberScopeDecorator` does nothing)
                // - passing that flag through all the places where scopes are created
                //
                // Both of them looks quite complicated and non-local, so they don't seem like worth it.
                mainScopeSession.getOrBuild(ScopeSessionForCommonLikeBuiltIn, ScopeSessionForCommonLikeBuiltIn) { ScopeSession(skipDeclaredMemberScopeDecorator = true) },
                memberRequiredPhase = null
            ).let {
                val substitutor = createMappingSubstitutor(kotlinMutableVersionFirClass, firJavaClass, session)

                FirClassSubstitutionScope(
                    session, it,
                    SubstitutionKeyForMappedClass(firJavaClass.symbol.toLookupTag(), substitutor),
                    substitutor,
                    substitutor.substituteOrSelf(kotlinMutableVersionFirClass.symbol.defaultType()) as ConeClassLikeType,
                    skipPrivateMembers = true,
                    derivedClassLookupTag = firJavaClass.symbol.toLookupTag()
                )
            }
        }

        private data class SubstitutionKeyForMappedClass(
            val lookupTag: ConeClassLikeLookupTag,
            val substitutor: ConeSubstitutor,
        ) : ScopeSessionKey<FirClass, FirClassSubstitutionScope>()

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
    }

    override fun toString(): String {
        return "JVM mapped scope for ${firKotlinClass.classId}"
    }
}
