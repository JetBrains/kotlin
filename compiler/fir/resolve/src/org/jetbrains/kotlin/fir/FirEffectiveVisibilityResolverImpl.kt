/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

open class FirEffectiveVisibilityResolverImpl(private val session: FirSession) : FirEffectiveVisibilityResolver() {
    private val cache = mutableMapOf<FirSourceElement, FirEffectiveVisibility>()

    final override fun resolveFor(
        declaration: FirMemberDeclaration,
        containingDeclarations: List<FirDeclaration>?,
        scopeSession: ScopeSession
    ): FirEffectiveVisibility {
        if (declaration.source != null) {
            cache[declaration.source]?.let {
                return it
            }
        }

        val (parentSymbol, parentEffectiveVisibility) =
            declaration.getParentInfo(containingDeclarations, scopeSession)
        var visibility = declaration.visibility

        // this is a temporary solution until
        // visibility starts to understand
        // overrides itself.
        if (declaration is FirSimpleFunction) {
            parentSymbol?.fir.safeAs<FirClass<*>>()?.let {
                visibility = declaration.lowestVisibilityAmongOverrides(it, session, scopeSession)
            }
        }

        var selfEffectiveVisibility = computeEffectiveVisibility(visibility, parentSymbol)
        selfEffectiveVisibility = parentEffectiveVisibility.lowerBound(selfEffectiveVisibility)
        return declaration.remember(selfEffectiveVisibility)
    }

    protected open fun computeEffectiveVisibility(visibility: Visibility, containerSymbol: FirClassLikeSymbol<*>?): FirEffectiveVisibility {
        return visibility.normalize().forVisibility(containerSymbol)
    }

    protected fun Visibility.forVisibility(
        containerSymbol: FirClassLikeSymbol<*>?
    ): FirEffectiveVisibility =
        when (this) {
            Visibilities.Private, Visibilities.PrivateToThis, Visibilities.InvisibleFake -> FirEffectiveVisibilityImpl.Private
            Visibilities.Protected -> FirEffectiveVisibilityImpl.Protected(containerSymbol, session)
            Visibilities.Internal -> FirEffectiveVisibilityImpl.Internal
            Visibilities.Public -> FirEffectiveVisibilityImpl.Public
            Visibilities.Local -> FirEffectiveVisibilityImpl.Local
            Visibilities.Unknown -> FirEffectiveVisibilityImpl.Private
            // NB: visibility must be already normalized here, so e.g. no JavaVisibilities are possible at this point
            else -> error("Visibility $name is not allowed in forVisibility")
        }

    private fun FirElement.remember(effectiveVisibility: FirEffectiveVisibility): FirEffectiveVisibility {
        val source = source
        if (source != null) {
            cache[source] = effectiveVisibility
        }
        return effectiveVisibility
    }

    private fun FirMemberDeclaration.getParentInfo(
        containingDeclarations: List<FirDeclaration>?,
        scopeSession: ScopeSession
    ): Pair<FirClassLikeSymbol<*>?, FirEffectiveVisibility> {
        var parentEffectiveVisibility: FirEffectiveVisibility = FirEffectiveVisibilityImpl.Public
        // because for now effective visibility
        // only works with FirClassLikeSymbol's
        var parentSymbol: FirClassLikeSymbol<*>? = null
        val parentClassId = this.getParentClassId()

        // for some reason ClassId for "/<anonymous>"
        // has local = false but still returns
        // null instead of a symbol
        // TODO: fix
        var succeededToGetSymbol = false

        // look for the containing class
        // in the containingDeclarations or
        // try using the firSymbolProvider
        if (parentClassId != null && containingDeclarations != null) {
            parentClassId.findOuterContainerInfo(containingDeclarations, scopeSession)?.let {
                parentSymbol = it.first
                parentEffectiveVisibility = it.second
                succeededToGetSymbol = true
            }
        }

        if (!succeededToGetSymbol) {
            if (parentClassId?.isLocal == false) {
                // ?: is needed to get enum from enum entry
                parentSymbol = session.firSymbolProvider.getClassLikeSymbolByFqName(parentClassId)
                    ?: parentClassId.outerClassId?.let { session.firSymbolProvider.getClassLikeSymbolByFqName(it) }
                parentSymbol?.fir.safeAs<FirMemberDeclaration>()?.let {
                    parentEffectiveVisibility = resolveFor(it, null, scopeSession)
                }
            } else if (parentClassId?.isLocal == true) {
                parentEffectiveVisibility = FirEffectiveVisibilityImpl.Local
            }
        }

        return parentSymbol to parentEffectiveVisibility
    }

    private fun FirDeclaration.getParentClassId(): ClassId? = when (this) {
        is FirCallableMemberDeclaration<*> -> this.symbol.callableId.classId
        is FirClassLikeDeclaration<*> -> this.symbol.classId.outerClassId
        else -> null
    }

    private fun FirDeclaration.getClassId(): ClassId? = when (this) {
        is FirCallableMemberDeclaration<*> -> this.symbol.callableId.classId
        is FirClassLikeDeclaration<*> -> this.symbol.classId
        else -> null
    }

    private fun ClassId.findOuterContainerInfo(
        containingDeclarations: List<FirDeclaration>,
        scopeSession: ScopeSession
    ): Pair<FirClassLikeSymbol<*>, FirEffectiveVisibility>? {
        for (index in containingDeclarations.indices) {
            val declaration = containingDeclarations[index]
            val declarationClassId = declaration.getClassId()

            if (this.relativeClassName == declarationClassId?.relativeClassName) {
                return when (declaration) {
                    is FirRegularClass -> {
                        declaration.symbol to resolveFor(declaration, containingDeclarations.subList(0, index), scopeSession)
                    }
                    is FirAnonymousObject -> {
                        declaration.symbol to declaration.remember(FirEffectiveVisibilityImpl.Local)
                    }
                    else -> {
                        null
                    }
                }
            }
        }

        return null
    }
}
