/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isJava
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.scopes.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.scopes.scopeForSupertype
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.DFS

object JavaScopeProvider : FirScopeProvider() {
    override fun getUseSiteMemberScope(
        klass: FirClass,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirTypeScope {
        val symbol = klass.symbol as FirRegularClassSymbol
        val enhancementScope = buildJavaEnhancementScope(useSiteSession, symbol, scopeSession)
        if (klass.classKind == ClassKind.ANNOTATION_CLASS) {
            return buildSyntheticScopeForAnnotations(useSiteSession, symbol, scopeSession, enhancementScope)
        }
        return enhancementScope
    }

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

    private fun buildJavaEnhancementScope(
        useSiteSession: FirSession,
        symbol: FirRegularClassSymbol,
        scopeSession: ScopeSession
    ): JavaClassMembersEnhancementScope {
        return scopeSession.getOrBuild(symbol, JAVA_ENHANCEMENT) {
            val firJavaClass = symbol.fir
            require(firJavaClass is FirJavaClass) {
                "${firJavaClass.classId} is expected to be FirJavaClass, but ${firJavaClass::class} found"
            }
            JavaClassMembersEnhancementScope(
                useSiteSession,
                symbol,
                buildUseSiteMemberScopeWithJavaTypes(firJavaClass, useSiteSession, scopeSession)
            )
        }
    }

    private fun buildDeclaredMemberScope(useSiteSession: FirSession, regularClass: FirRegularClass): FirContainingNamesAwareScope {
        return if (regularClass is FirJavaClass) useSiteSession.declaredMemberScopeWithLazyNestedScope(
            regularClass,
            existingNames = regularClass.existingNestedClassifierNames,
            symbolProvider = useSiteSession.symbolProvider
        ) else useSiteSession.declaredMemberScope(regularClass)
    }

    private fun buildUseSiteMemberScopeWithJavaTypes(
        regularClass: FirJavaClass,
        useSiteSession: FirSession,
        scopeSession: ScopeSession,
    ): JavaClassUseSiteMemberScope {
        return scopeSession.getOrBuild(regularClass.symbol, JAVA_USE_SITE) {
            val declaredScope = buildDeclaredMemberScope(useSiteSession, regularClass)
            val superTypes =
                if (regularClass.isThereLoopInSupertypes(useSiteSession))
                    listOf(StandardClassIds.Any.constructClassLikeType(emptyArray(), isNullable = false))
                else
                    lookupSuperTypes(
                        regularClass, lookupInterfaces = true, deep = false, useSiteSession = useSiteSession, substituteTypes = true
                    )

            val superTypeScopes = superTypes.mapNotNull {
                it.scopeForSupertype(useSiteSession, scopeSession, subClass = regularClass)
            }

            JavaClassUseSiteMemberScope(
                regularClass, useSiteSession,
                FirTypeIntersectionScope.prepareIntersectionScope(
                    useSiteSession,
                    JavaOverrideChecker(
                        useSiteSession,
                        regularClass.javaTypeParameterStack,
                        baseScope = null,
                        considerReturnTypeKinds = false,
                    ),
                    superTypeScopes,
                    regularClass.defaultType(),
                ), declaredScope
            )
        }
    }

    override fun getStaticMemberScopeForCallables(
        klass: FirClass,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirContainingNamesAwareScope? {
        val scope = getStaticMemberScopeForCallables(klass, useSiteSession, scopeSession, hashSetOf()) ?: return null
        return FirNameAwareOnlyCallablesScope(FirStaticScope(scope))
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
                    klass.javaTypeParameterStack
                )
            )
        }.also {
            visitedClasses.remove(klass)
        }
    }

    private tailrec fun FirRegularClass.findJavaSuperClass(useSiteSession: FirSession): FirRegularClass? {
        val superClass = superConeTypes.firstNotNullOfOrNull {
            (it.lookupTag.toSymbol(useSiteSession)?.fir as? FirRegularClass)?.takeIf { superClass ->
                superClass.classKind == ClassKind.CLASS
            }
        } ?: return null

        if (superClass.isJava) return superClass

        return superClass.findJavaSuperClass(useSiteSession)
    }

    private fun FirRegularClass.findClosestJavaSuperTypes(useSiteSession: FirSession): Collection<FirRegularClass> {
        val result = mutableListOf<FirRegularClass>()
        DFS.dfs(listOf(this),
                { regularClass ->
                    regularClass.superConeTypes.mapNotNull {
                        it.lookupTag.toSymbol(useSiteSession)?.fir as? FirRegularClass
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
            klass.classId,
            (klass as FirJavaClass).existingNestedClassifierNames,
            useSiteSession.symbolProvider
        )
    }
}

private val JAVA_SYNTHETIC_FOR_ANNOTATIONS = scopeSessionKey<FirRegularClassSymbol, JavaAnnotationSyntheticPropertiesScope>()
private val JAVA_ENHANCEMENT_FOR_STATIC = scopeSessionKey<FirRegularClassSymbol, JavaClassStaticEnhancementScope>()
private val JAVA_ENHANCEMENT = scopeSessionKey<FirRegularClassSymbol, JavaClassMembersEnhancementScope>()
private val JAVA_USE_SITE = scopeSessionKey<FirRegularClassSymbol, JavaClassUseSiteMemberScope>()
