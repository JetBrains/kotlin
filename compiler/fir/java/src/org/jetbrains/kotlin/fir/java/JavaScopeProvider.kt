/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.scopes.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassErrorType
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

class JavaScopeProvider(
    val declaredMemberScopeDecorator: (
        klass: FirClass<*>,
        declaredMemberScope: FirScope,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ) -> FirScope = { _, declaredMemberScope, _, _ -> declaredMemberScope },
    val symbolProvider: JavaSymbolProvider
) : FirScopeProvider() {
    override fun getUseSiteMemberScope(
        klass: FirClass<*>,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirTypeScope {
        val symbol = klass.symbol as FirRegularClassSymbol
        val enhancementScope = buildJavaEnhancementScope(useSiteSession, symbol, scopeSession, mutableSetOf())
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
        scopeSession: ScopeSession,
        visitedSymbols: MutableSet<FirClassLikeSymbol<*>>
    ): JavaClassMembersEnhancementScope {
        return scopeSession.getOrBuild(symbol, JAVA_ENHANCEMENT) {
            JavaClassMembersEnhancementScope(
                useSiteSession,
                symbol,
                buildUseSiteMemberScopeWithJavaTypes(symbol.fir, useSiteSession, scopeSession, visitedSymbols)
            )
        }
    }

    private fun buildDeclaredMemberScope(regularClass: FirRegularClass): FirScope {
        return if (regularClass is FirJavaClass) declaredMemberScopeWithLazyNestedScope(
            regularClass,
            existingNames = regularClass.existingNestedClassifierNames,
            symbolProvider = symbolProvider
        ) else declaredMemberScope(regularClass)
    }

    private fun buildUseSiteMemberScopeWithJavaTypes(
        regularClass: FirRegularClass,
        useSiteSession: FirSession,
        scopeSession: ScopeSession,
        visitedSymbols: MutableSet<FirClassLikeSymbol<*>>
    ): JavaClassUseSiteMemberScope {
        return scopeSession.getOrBuild(regularClass.symbol, JAVA_USE_SITE) {
            val declaredScope = buildDeclaredMemberScope(regularClass)
            val wrappedDeclaredScope = declaredMemberScopeDecorator(regularClass, declaredScope, useSiteSession, scopeSession)
            val superTypeEnhancementScopes =
                lookupSuperTypes(regularClass, lookupInterfaces = true, deep = false, useSiteSession = useSiteSession)
                    .mapNotNull { useSiteSuperType ->
                        if (useSiteSuperType is ConeClassErrorType) return@mapNotNull null
                        val symbol = useSiteSuperType.lookupTag.toSymbol(useSiteSession)
                        if (symbol is FirRegularClassSymbol && visitedSymbols.add(symbol)) {
                            // We need JavaClassEnhancementScope here to have already enhanced signatures from supertypes
                            val scope = buildJavaEnhancementScope(useSiteSession, symbol, scopeSession, visitedSymbols)
                            visitedSymbols.remove(symbol)
                            useSiteSuperType.wrapSubstitutionScopeIfNeed(
                                useSiteSession, scope, symbol.fir, scopeSession, derivedClass = regularClass
                            )
                        } else {
                            null
                        }
                    }
            JavaClassUseSiteMemberScope(
                regularClass, useSiteSession,
                FirTypeIntersectionScope.prepareIntersectionScope(
                    useSiteSession,
                    JavaOverrideChecker(
                        useSiteSession,
                        if (regularClass is FirJavaClass) regularClass.javaTypeParameterStack
                        else JavaTypeParameterStack.EMPTY
                    ),
                    superTypeEnhancementScopes,
                    regularClass.defaultType(),
                ), wrappedDeclaredScope
            )
        }
    }

    override fun getStaticMemberScopeForCallables(
        klass: FirClass<*>,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirScope? {
        val scope = getStaticMemberScopeForCallables(klass, useSiteSession, scopeSession, hashSetOf()) ?: return null
        return FirOnlyCallablesScope(FirStaticScope(scope))
    }

    private fun getStaticMemberScopeForCallables(
        klass: FirClass<*>,
        useSiteSession: FirSession,
        scopeSession: ScopeSession,
        visitedClasses: MutableSet<FirRegularClass>
    ): JavaClassStaticEnhancementScope? {
        if (klass !is FirJavaClass) return null
        if (!visitedClasses.add(klass)) return null

        return scopeSession.getOrBuild(klass.symbol, JAVA_ENHANCEMENT_FOR_STATIC) {
            val declaredScope = buildDeclaredMemberScope(klass)
            val wrappedDeclaredScope = declaredMemberScopeDecorator(klass, declaredScope, useSiteSession, scopeSession)

            val superClassScope = klass.findJavaSuperClass()?.let {
                (it.scopeProvider as? JavaScopeProvider)
                    ?.getStaticMemberScopeForCallables(it, useSiteSession, scopeSession, visitedClasses)
            } ?: FirTypeScope.Empty

            val superTypesScopes = klass.findClosestJavaSuperTypes().mapNotNull {
                (it.scopeProvider as? JavaScopeProvider)
                    ?.getStaticMemberScopeForCallables(it, useSiteSession, scopeSession, visitedClasses)
            }

            JavaClassStaticEnhancementScope(
                useSiteSession,
                klass.symbol,
                JavaClassStaticUseSiteScope(
                    useSiteSession,
                    declaredMemberScope = wrappedDeclaredScope,
                    superClassScope, superTypesScopes,
                    klass.javaTypeParameterStack
                )
            )
        }.also {
            visitedClasses.remove(klass)
        }
    }

    private tailrec fun FirRegularClass.findJavaSuperClass(): FirRegularClass? {
        val superClass = superConeTypes.firstNotNullResult {
            (it.lookupTag.toSymbol(session)?.fir as? FirRegularClass)?.takeIf { superClass ->
                superClass.classKind == ClassKind.CLASS
            }
        } ?: return null

        if (superClass.origin is FirDeclarationOrigin.Java) return superClass

        return superClass.findJavaSuperClass()
    }

    private fun FirRegularClass.findClosestJavaSuperTypes(): Collection<FirRegularClass> {
        val result = mutableListOf<FirRegularClass>()
        DFS.dfs(listOf(this),
                { regularClass ->
                    regularClass.superConeTypes.mapNotNull {
                        it.lookupTag.toSymbol(session)?.fir as? FirRegularClass
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

    override fun getNestedClassifierScope(klass: FirClass<*>, useSiteSession: FirSession, scopeSession: ScopeSession): FirScope? {
        return lazyNestedClassifierScope(
            klass.classId,
            (klass as FirJavaClass).existingNestedClassifierNames,
            useSiteSession.firSymbolProvider
        )
    }
}

private val JAVA_SYNTHETIC_FOR_ANNOTATIONS = scopeSessionKey<FirRegularClassSymbol, JavaAnnotationSyntheticPropertiesScope>()
private val JAVA_ENHANCEMENT_FOR_STATIC = scopeSessionKey<FirRegularClassSymbol, JavaClassStaticEnhancementScope>()
private val JAVA_ENHANCEMENT = scopeSessionKey<FirRegularClassSymbol, JavaClassMembersEnhancementScope>()
private val JAVA_USE_SITE = scopeSessionKey<FirRegularClassSymbol, JavaClassUseSiteMemberScope>()
