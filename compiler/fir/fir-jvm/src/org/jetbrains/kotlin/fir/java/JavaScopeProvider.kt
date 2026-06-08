/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isJava
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.scopes.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.FirNameAwareOnlyCallablesScope
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScopeWithLazyNestedScope
import org.jetbrains.kotlin.fir.scopes.impl.lazyNestedClassifierScope
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.isAny
import org.jetbrains.kotlin.fir.types.lookupTagIfAny
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

object JavaScopeProvider : FirScopeProvider() {
    override fun getUseSiteMemberScope(
        klass: FirClass,
        useSiteSession: FirSession,
        scopeSession: ScopeSession,
        memberRequiredPhase: FirResolvePhase?,
    ): FirTypeScope {
        return buildJavaUseSiteScope(
            useSiteSession,
            klass.symbol as FirRegularClassSymbol,
            scopeSession,
            useSiteScopeKey = JAVA_ALL_MEMBERS_USE_SITE,
            enhancementScopeKey = JAVA_ALL_MEMBERS_ENHANCEMENT,
            memberRequiredPhase,
            includeSuperTypeMembers = true,
        )
    }

    override fun getDeclaredUseSiteMemberScope(
        klass: FirClass,
        useSiteSession: FirSession,
        scopeSession: ScopeSession,
        memberRequiredPhase: FirResolvePhase?
    ): FirTypeScope {
        return buildJavaUseSiteScope(
            useSiteSession,
            klass.symbol as FirRegularClassSymbol,
            scopeSession,
            useSiteScopeKey = JAVA_DECLARED_MEMBERS_USE_SITE,
            enhancementScopeKey = JAVA_DECLARED_MEMBERS_ENHANCEMENT,
            memberRequiredPhase,
            includeSuperTypeMembers = false,
        )
    }

    override fun getTypealiasConstructorScope(
        typeAlias: FirTypeAlias,
        useSiteSession: FirSession,
        scopeSession: ScopeSession,
    ): FirScope = shouldNotBeCalled("Java doesn't support typealias")

    private fun buildSyntheticScopeForAnnotations(
        session: FirSession,
        symbol: FirRegularClassSymbol,
        scopeSession: ScopeSession,
        enhancementScope: JavaClassMembersEnhancementScope
    ): FirTypeScope {
        return scopeSession.getOrBuild(symbol, JAVA_SYNTHETIC_FOR_ANNOTATIONS) {
            JavaAnnotationSyntheticPropertiesScope(session, symbol, enhancementScope)
        }
    }

    private fun buildJavaUseSiteScope(
        useSiteSession: FirSession,
        symbol: FirRegularClassSymbol,
        scopeSession: ScopeSession,
        useSiteScopeKey: ScopeSessionKey<FirRegularClassSymbol, JavaClassUseSiteMemberScope>,
        enhancementScopeKey: ScopeSessionKey<FirRegularClassSymbol, JavaClassMembersEnhancementScope>,
        memberRequiredPhase: FirResolvePhase?,
        includeSuperTypeMembers: Boolean,
    ): FirTypeScope {
        val enhancementScope = scopeSession.getOrBuild(symbol, enhancementScopeKey) {
            val firJavaClass = symbol.fir
            require(firJavaClass is FirJavaClass) {
                "${firJavaClass.classId} is expected to be FirJavaClass, but ${firJavaClass::class} found"
            }

            val memberScope = buildUseSiteMemberScopeWithJavaTypes(
                firJavaClass,
                useSiteSession,
                scopeSession,
                useSiteScopeKey,
                memberRequiredPhase,
                includeSuperTypeMembers
            )

            JavaClassMembersEnhancementScope(useSiteSession, symbol, memberScope)
        }

        if (symbol.classKind == ClassKind.ANNOTATION_CLASS) {
            return buildSyntheticScopeForAnnotations(useSiteSession, symbol, scopeSession, enhancementScope)
        }

        return enhancementScope
    }

    private fun buildDeclaredMemberScope(useSiteSession: FirSession, regularClass: FirRegularClass): FirContainingNamesAwareScope {
        return if (regularClass is FirJavaClass) {
            useSiteSession.declaredMemberScopeWithLazyNestedScope(
                regularClass,
                existingNames = regularClass.existingNestedClassifierNames,
            )
        } else {
            useSiteSession.declaredMemberScope(regularClass, memberRequiredPhase = null)
        }
    }

    private fun buildUseSiteMemberScopeWithJavaTypes(
        regularClass: FirJavaClass,
        useSiteSession: FirSession,
        scopeSession: ScopeSession,
        useSiteScopeKey: ScopeSessionKey<FirRegularClassSymbol, JavaClassUseSiteMemberScope>,
        memberRequiredPhase: FirResolvePhase?,
        includeSuperTypeMembers: Boolean,
    ): JavaClassUseSiteMemberScope {
        return scopeSession.getOrBuild(regularClass.symbol, useSiteScopeKey) {
            val declaredScope = buildDeclaredMemberScope(useSiteSession, regularClass)

            val superTypeScopes: List<FirTypeScope> = if (includeSuperTypeMembers) {
                val superTypes = if (regularClass.isThereLoopInSupertypes(useSiteSession)) {
                    listOf(StandardClassIds.Any.constructClassLikeType())
                } else {
                    lookupSuperTypes(
                        regularClass,
                        lookupInterfaces = true,
                        deep = false,
                        useSiteSession = useSiteSession,
                        substituteTypes = true
                    )
                }

                superTypes.mapNotNull { superType ->
                    superType.scopeForSupertype(useSiteSession, scopeSession, regularClass, memberRequiredPhase = memberRequiredPhase)
                }
            } else {
                emptyList()
            }

            JavaClassUseSiteMemberScope(
                regularClass,
                useSiteSession,
                superTypeScopes,
                declaredScope
            )
        }
    }

    override fun getStaticCallableMemberScope(
        klass: FirClass,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirContainingNamesAwareScope? {
        val scope = getStaticMemberScopeForCallables(klass, useSiteSession, scopeSession, hashSetOf()) ?: return null
        return FirNameAwareOnlyCallablesScope(scope)
    }

    override fun getStaticCallableMemberScopeForBackend(
        klass: FirClass,
        useSiteSession: FirSession,
        scopeSession: ScopeSession,
    ): FirContainingNamesAwareScope? {
        return getStaticCallableMemberScope(klass, useSiteSession, scopeSession)
    }

    private fun getStaticMemberScopeForCallables(
        klass: FirClass,
        useSiteSession: FirSession,
        scopeSession: ScopeSession,
        visitedClasses: MutableSet<FirRegularClass>
    ): JavaClassStaticEnhancementScope? {
        if (klass !is FirJavaClass) return null
        if (!visitedClasses.add(klass)) return null

        return scopeSession.getOrBuild(klass.symbol, JAVA_ENHANCEMENT_FOR_STATIC) {
            val declaredScope = buildDeclaredMemberScope(useSiteSession, klass)

            val superClassScope = klass.findJavaSuperClass(useSiteSession)?.let {
                (it.scopeProvider as? JavaScopeProvider)
                    ?.getStaticMemberScopeForCallables(it, useSiteSession, scopeSession, visitedClasses)
            } ?: FirTypeScope.Empty

            val superTypesScopes = klass.findClosestJavaSuperTypes(useSiteSession).mapNotNull {
                (it.scopeProvider as? JavaScopeProvider)
                    ?.getStaticMemberScopeForCallables(it, useSiteSession, scopeSession, visitedClasses)
            }

            JavaClassStaticEnhancementScope(
                useSiteSession,
                klass.symbol,
                JavaClassStaticUseSiteScope(
                    useSiteSession,
                    declaredMemberScope = declaredScope,
                    superClassScope, superTypesScopes,
                    klass,
                )
            )
        }.also {
            visitedClasses.remove(klass)
        }
    }

    private tailrec fun FirRegularClass.findJavaSuperClass(useSiteSession: FirSession): FirRegularClass? {
        val superClass = symbol.resolvedSuperTypes.firstNotNullOfOrNull {
            if (it.isAny) return@firstNotNullOfOrNull null
            it.lookupTagIfAny?.toRegularClassSymbol(useSiteSession)?.fir?.takeIf { superClass ->
                superClass.classKind == ClassKind.CLASS
            }
        } ?: return null

        if (superClass.isJava) return superClass

        return superClass.findJavaSuperClass(useSiteSession)
    }

    private fun FirRegularClass.findClosestJavaSuperTypes(useSiteSession: FirSession): Collection<FirRegularClass> {
        val result = mutableListOf<FirRegularClass>()
        DFS.dfs(
            listOf(this),
            { regularClass ->
                regularClass.symbol.resolvedSuperTypes.mapNotNull {
                    it.lookupTagIfAny?.toRegularClassSymbol(useSiteSession)?.fir
                }
            },
            object : DFS.AbstractNodeHandler<FirRegularClass, Unit>() {
                override fun beforeChildren(current: FirRegularClass?): Boolean {
                    if (this@findClosestJavaSuperTypes === current) return true
                    if (current is FirJavaClass) {
                        result.add(current)
                        return false
                    }

                    return true
                }

                override fun result() {}

            }
        )

        return result
    }

    override fun getNestedClassifierScope(
        klass: FirClass,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirContainingNamesAwareScope? {
        return lazyNestedClassifierScope(
            useSiteSession,
            klass.classId,
            (klass as FirJavaClass).existingNestedClassifierNames
        )
    }
}

private val JAVA_SYNTHETIC_FOR_ANNOTATIONS = scopeSessionKey<FirRegularClassSymbol, JavaAnnotationSyntheticPropertiesScope>()
private val JAVA_ENHANCEMENT_FOR_STATIC = scopeSessionKey<FirRegularClassSymbol, JavaClassStaticEnhancementScope>()

private val JAVA_ALL_MEMBERS_ENHANCEMENT = scopeSessionKey<FirRegularClassSymbol, JavaClassMembersEnhancementScope>()
private val JAVA_DECLARED_MEMBERS_ENHANCEMENT = scopeSessionKey<FirRegularClassSymbol, JavaClassMembersEnhancementScope>()

private val JAVA_ALL_MEMBERS_USE_SITE = scopeSessionKey<FirRegularClassSymbol, JavaClassUseSiteMemberScope>()
private val JAVA_DECLARED_MEMBERS_USE_SITE = scopeSessionKey<FirRegularClassSymbol, JavaClassUseSiteMemberScope>()
