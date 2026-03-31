/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dependencies

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.isInterface
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.utils.fromPrimaryConstructor
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.scopes.ScopeFunctionRequiresPrewarm
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousInitializerSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase

class FirInheritancePropagatedDeclarationsStorage(session: FirSession) : FirSessionComponent {

    // We ensure the non-subsumed symbols are retrieved inside the callback of the processAllProperties function
    @OptIn(DirectDeclarationsAccess::class, ScopeFunctionRequiresPrewarm::class)
    val propagatedDeclarations: FirCache<FirClassSymbol<*>, Set<FirBasedSymbol<*>>, DependencyGraph.Builder> =
        session.firCachesFactory.createCache { classSymbol, builder ->
            // At least resolve to the STATUS phase which ensures we have access to the super type info and the overriding info
            classSymbol.lazyResolveToPhase(FirResolvePhase.STATUS)

            // We require the use-site scope to provide us with all possible intersections and substitution overrides
//            val useSiteScope = classSymbol.unsubstitutedScope(
//                builder.session,
//                builder.scopeSession,
//                true,
//                memberRequiredPhase = FirResolvePhase.STATUS
//            )

            // Cache the collected declarations recursively respecting JVM's initialization rules
            //
            // JVMS25 (5.5.7):
            // ... if C is a class rather than an interface, then let SC be its superclass and let SI1, ..., SIn be all superinterfaces of C
            // (whether direct or indirect) that declare at least one non-abstract, non-static method. The order of superinterfaces is given
            // by a recursive enumeration over the superinterface hierarchy of each interface directly implemented by C. For each interface I
            // directly implemented by C (in the order of the interfaces array of C), the enumeration recurs on I's superinterfaces (in the
            // order of the interfaces array of I) before returning I. ...
            linkedSetOf<FirBasedSymbol<*>>().apply {
                // We do not need to enumerate the supertype hierarchy recursively because it is equivalent as recursively calling the cache
                // on each directly implemented supertype
                val superTypes = classSymbol.resolvedSuperTypes.mapNotNull {
                    it.fullyExpandedType(builder.session).toRegularClassSymbol(builder.session)
                }

                // Add declarations from the superclass (if exists), as it is always initialized first
                superTypes.find { it.classKind == ClassKind.CLASS || it.classKind == ClassKind.ENUM_CLASS }?.let {
                    propagatedDeclarations.getValue(it, builder).forEach { declaration ->
//                        if (declaration !in overridden) {
                            add(declaration)
//                        }
                    }
                }

                // Add (default) declarations from the superinterfaces in order of declaration in the supertype specifiers
                // Interfaces without (declared or inherited) default methods will cache an empty set
                superTypes.filter { it.classKind == ClassKind.INTERFACE }.forEach {
                    propagatedDeclarations.getValue(it, builder).forEach { declaration ->
//                        if (declaration !in overridden) {
                            add(declaration)
//                        }
                    }
                }

                // For library classes, we cache their declared symbols
                if (classSymbol.moduleData.session.kind == FirSession.Kind.Library) {
                    classSymbol.declarationSymbols.forEach {
                        // Skip constructors
                        if (it !is FirConstructorSymbol) add(it)
                    }
                }
                // For user classes, we cache the declarations from the primary constructor and the body in their order of declaration
                else {
                    // Populate the declared public declarations (properties and init blocks), and overridden properties
                    val fromPrimaryConstructor = linkedSetOf<FirVariableSymbol<*>>()
                    val fromBody = linkedSetOf<FirBasedSymbol<*>>()
                    val overridden = mutableSetOf<FirCallableSymbol<*>>()
                    classSymbol.declarationSymbols.forEach { symbol ->
                        when (symbol) {
                            // For all properties...
                            is FirPropertySymbol -> {
                                if (symbol.fromPrimaryConstructor) {
                                    // If they are declared in the primary constructor, add them to the primary constructor declarations
                                    fromPrimaryConstructor += symbol
                                } else if (!classSymbol.classKind.isInterface || classSymbol.classKind.isInterface && symbol.hasAnyImplementation()) {
                                    // If they are declared in the body, or we are caching an interface and the declaration has a default
                                    // implementation, add them to the body declarations
                                    fromBody += symbol
                                }
                                // Make sure we exclude all the directly overridden properties of its super types
//                        useSiteScope.processDirectOverriddenPropertiesWithBaseScope(symbol) { baseProperty, _ ->
//                            overridden += baseProperty
//                            ProcessorAction.NEXT
//                        }
                            }
                            // For all init blocks, add them to the body declarations
                            is FirAnonymousInitializerSymbol -> fromBody += symbol
                            // For all functions (excluding constructors), add them to the body declarations (we do not care about the order of their declaration)
                            is FirFunctionSymbol if symbol !is FirConstructorSymbol -> fromBody += symbol
                        }
                    }

                    // Resolve intersection and substitution overrides for callables (properties and functions)
                    /*useSiteScope.processAllCallables { callable ->
                        callable.originalIfFakeOverride()?.let {
                            // Ignore all callables that we have already processed
                            if (it in fromPrimaryConstructor || it in fromBody || it in overridden) return@processAllCallables

                            // For each intersection callable...
                            if (it is FirIntersectionCallableSymbol) {
                                context(builder) {
                                    it.getNonSubsumedOverriddenSymbols().singleOrNull()?.let { nonSubsumed ->
                                        // If it has one chosen symbol...
                                        it.intersections.forEach { intersection ->
                                            if (intersection.unwrapSubstitutionOverrides() != nonSubsumed) {
                                                // Mark the rest of the intersected symbols as overridden (by the chosen symbol)
                                                overridden += intersection
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }*/
                    // This set will be empty for interfaces
                    addAll(fromPrimaryConstructor)
                    // This will contain all default methods for interfaces
                    addAll(fromBody)
                }
            }
        }
}

val FirSession.propagatedDeclarationsStorage: FirInheritancePropagatedDeclarationsStorage by FirSession.sessionComponentAccessor()
