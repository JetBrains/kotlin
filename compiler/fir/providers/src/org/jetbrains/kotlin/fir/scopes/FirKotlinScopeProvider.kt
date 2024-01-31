/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.delegateFields
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.substitution.ConeRawScopeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhaseWithCallableMembers
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name

class FirKotlinScopeProvider(
    val declaredMemberScopeDecorator: (
        klass: FirClass,
        declaredMemberScope: FirContainingNamesAwareScope,
        useSiteSession: FirSession,
        scopeSession: ScopeSession,
        memberRequiredPhase: FirResolvePhase?,
    ) -> FirContainingNamesAwareScope = { _, declaredMemberScope, session, _, _ ->
        PlatformDependentFilteringScope(declaredMemberScope, session)
    }
) : FirScopeProvider(), FirSessionComponent {
    override fun getUseSiteMemberScope(
        klass: FirClass,
        useSiteSession: FirSession,
        scopeSession: ScopeSession,
        memberRequiredPhase: FirResolvePhase?,
    ): FirTypeScope {
        memberRequiredPhase?.let {
            klass.lazyResolveToPhaseWithCallableMembers(it)
        }

        return scopeSession.getOrBuild(useSiteSession to klass.symbol, USE_SITE) {
            val declaredScope = useSiteSession.declaredMemberScope(klass, memberRequiredPhase)
            val possiblyDelegatedDeclaredMemberScope = declaredMemberScopeDecorator(
                klass,
                declaredScope,
                useSiteSession,
                scopeSession,
                memberRequiredPhase
            ).let {
                val delegateFields = klass.delegateFields
                if (delegateFields.isEmpty())
                    it
                else
                    FirDelegatedMemberScope(useSiteSession, scopeSession, klass, it, delegateFields)
            }
            val declaredMemberScopeWithPossiblySynthesizedMembers =
                // Related: https://youtrack.jetbrains.com/issue/KT-20427#focus=Comments-27-8652759.0-0
                if (klass is FirRegularClass && !klass.isExpect && (klass.isData || klass.isInline)) {
                    // See also KT-58926 (we apply delegation first, and data/value classes after it)
                    FirClassAnySynthesizedMemberScope(useSiteSession, possiblyDelegatedDeclaredMemberScope, klass, scopeSession)
                } else {
                    possiblyDelegatedDeclaredMemberScope
                }

            val scopes = lookupSuperTypes(
                klass, lookupInterfaces = true, deep = false, useSiteSession = useSiteSession, substituteTypes = true
            ).mapNotNull { useSiteSuperType ->
                useSiteSuperType.scopeForSupertype(useSiteSession, scopeSession, klass, memberRequiredPhase = memberRequiredPhase)
            }
            FirClassUseSiteMemberScope(
                klass,
                useSiteSession,
                scopes,
                declaredMemberScopeWithPossiblySynthesizedMembers,
            )
        }
    }

    override fun getStaticMemberScopeForCallables(
        klass: FirClass,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirContainingNamesAwareScope? {
        return when (klass.classKind) {
            ClassKind.ENUM_CLASS -> FirNameAwareOnlyCallablesScope(
                FirStaticScope(
                    useSiteSession.declaredMemberScope(
                        klass,
                        memberRequiredPhase = null,
                    )
                )
            )
            else -> null
        }
    }

    override fun getNestedClassifierScope(
        klass: FirClass,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirContainingNamesAwareScope? {
        return useSiteSession.nestedClassifierScope(klass)
    }

    class PlatformDependentFilteringScope(
        val declaredMemberScope: FirContainingNamesAwareScope,
        val session: FirSession,
    ) : FirContainingNamesAwareScope() {
        override fun getCallableNames(): Set<Name> = declaredMemberScope.getCallableNames()

        override fun getClassifierNames(): Set<Name> = declaredMemberScope.getClassifierNames()

        override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
            declaredMemberScope.processPropertiesByName(name, processor)
        }

        override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
            declaredMemberScope.processClassifiersByNameWithSubstitution(name, processor)
        }

        override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
            declaredMemberScope.processDeclaredConstructors(processor)
        }

        override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
            declaredMemberScope.processFunctionsByName(name) {
                if (FirPlatformDeclarationFilter.isFunctionAvailable(it.fir, session)) {
                    processor(it)
                }
            }
        }

        override fun mayContainName(name: Name): Boolean = declaredMemberScope.mayContainName(name)

        override val scopeOwnerLookupNames: List<String>
            get() = declaredMemberScope.scopeOwnerLookupNames
    }
}

object FirPlatformDeclarationFilter {
    fun isFunctionAvailable(function: FirSimpleFunction, session: FirSession): Boolean {
        // Optimization: only check the annotations for functions named "getOrDefault" and "remove",
        // since only two functions with these names in kotlin.collections.Map are currently annotated with @PlatformDependent.
        // This also allows to optimize more heavyweight FirJvmPlatformDeclarationFilter as it uses this function
        return function.name !in namesToCheck || !function.hasAnnotation(StandardNames.FqNames.platformDependentClassId, session)
    }

    private val namesToCheck = listOf("getOrDefault", "remove").map(Name::identifier)
}

data class ConeSubstitutionScopeKey(
    val lookupTag: ConeClassLikeLookupTag,
    val isFromExpectClass: Boolean,
    val substitutor: ConeSubstitutor,
    val derivedClassLookupTag: ConeClassLikeLookupTag?
) : ScopeSessionKey<FirClass, FirClassSubstitutionScope>()

fun FirClass.unsubstitutedScope(
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    withForcedTypeCalculator: Boolean,
    memberRequiredPhase: FirResolvePhase?,
): FirTypeScope {
    val scope = scopeProvider.getUseSiteMemberScope(this, useSiteSession, scopeSession, memberRequiredPhase)
    if (withForcedTypeCalculator) return FirScopeWithCallableCopyReturnTypeUpdater(scope, CallableCopyTypeCalculator.Forced)
    return scope
}

fun FirClassSymbol<*>.unsubstitutedScope(
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    withForcedTypeCalculator: Boolean,
    memberRequiredPhase: FirResolvePhase?,
): FirTypeScope {
    return fir.unsubstitutedScope(useSiteSession, scopeSession, withForcedTypeCalculator, memberRequiredPhase)
}

fun FirClass.scopeForClass(
    substitutor: ConeSubstitutor,
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    memberOwnerLookupTag: ConeClassLikeLookupTag,
    memberRequiredPhase: FirResolvePhase?,
): FirTypeScope = scopeForClassImpl(
    substitutor, useSiteSession, scopeSession,
    skipPrivateMembers = false,
    classFirDispatchReceiver = this,
    // TODO: why it's always false?
    isFromExpectClass = false,
    memberOwnerLookupTag = memberOwnerLookupTag,
    memberRequiredPhase = memberRequiredPhase,
)

fun ConeKotlinType.scopeForSupertype(
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    derivedClass: FirClass,
    memberRequiredPhase: FirResolvePhase?,
): FirTypeScope? {
    if (this !is ConeClassLikeType) return null
    if (this is ConeErrorType) return null

    val symbol = lookupTag.toSymbol(useSiteSession) as? FirRegularClassSymbol ?: return null

    val substitutor = substitutorForSuperType(useSiteSession, symbol)

    return symbol.fir.scopeForClassImpl(
        substitutor,
        useSiteSession,
        scopeSession,
        skipPrivateMembers = true,
        classFirDispatchReceiver = derivedClass,
        isFromExpectClass = (derivedClass as? FirRegularClass)?.isExpect == true,
        memberOwnerLookupTag = derivedClass.symbol.toLookupTag(),
        memberRequiredPhase = memberRequiredPhase,
    )
}

fun ConeClassLikeType.substitutorForSuperType(useSiteSession: FirSession, classTypeSymbol: FirRegularClassSymbol): ConeSubstitutor {
    return when {
        this.type.attributes.contains(CompilerConeAttributes.RawType) -> ConeRawScopeSubstitutor(useSiteSession)
        else -> substitutor(classTypeSymbol, this, useSiteSession)
    }
}

private fun substitutor(symbol: FirRegularClassSymbol, type: ConeClassLikeType, useSiteSession: FirSession): ConeSubstitutor {
    if (type.typeArguments.isEmpty()) return ConeSubstitutor.Empty
    val originalSubstitution = createSubstitutionForScope(symbol.fir.typeParameters, type, useSiteSession)
    return substitutorByMap(originalSubstitution, useSiteSession)
}

private fun FirClass.scopeForClassImpl(
    substitutor: ConeSubstitutor,
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    skipPrivateMembers: Boolean,
    classFirDispatchReceiver: FirClass,
    isFromExpectClass: Boolean,
    memberOwnerLookupTag: ConeClassLikeLookupTag?,
    memberRequiredPhase: FirResolvePhase?,
): FirTypeScope {
    val basicScope = unsubstitutedScope(useSiteSession, scopeSession, withForcedTypeCalculator = false, memberRequiredPhase)
    if (substitutor == ConeSubstitutor.Empty) return basicScope

    val key = ConeSubstitutionScopeKey(
        classFirDispatchReceiver.symbol.toLookupTag(),
        isFromExpectClass,
        substitutor,
        memberOwnerLookupTag
    )

    return scopeSession.getOrBuild(this, key) {
        FirClassSubstitutionScope(
            useSiteSession,
            basicScope,
            key, substitutor,
            substitutor.substituteOrSelf(classFirDispatchReceiver.defaultType()).lowerBoundIfFlexible() as ConeClassLikeType,
            skipPrivateMembers,
            makeExpect = isFromExpectClass,
            memberOwnerLookupTag ?: classFirDispatchReceiver.symbol.toLookupTag(),
            origin = if (classFirDispatchReceiver != this) {
                FirDeclarationOrigin.SubstitutionOverride.DeclarationSite
            } else {
                FirDeclarationOrigin.SubstitutionOverride.CallSite
            },
        )
    }
}

val FirSession.kotlinScopeProvider: FirKotlinScopeProvider by FirSession.sessionComponentAccessor()
