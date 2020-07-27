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

class FirEffectiveVisibilityResolverImpl(private val session: FirSession) : FirEffectiveVisibilityResolver {
    val cache = mutableMapOf<FirSourceElement, FirEffectiveVisibility>()

    private fun FirElement.remember(effectiveVisibility: FirEffectiveVisibility): FirEffectiveVisibility {
        val source = source
        if (source != null) {
            cache[source] = effectiveVisibility
        }
        return effectiveVisibility
    }

    override fun resolveFor(
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

        var selfEffectiveVisibility = visibility.firEffectiveVisibility(session, parentSymbol)
        selfEffectiveVisibility = parentEffectiveVisibility.lowerBound(selfEffectiveVisibility)
        return declaration.remember(selfEffectiveVisibility)
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
                parentSymbol = session.firSymbolProvider.getClassLikeSymbolByFqName(parentClassId)
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

            // because classId's we take from firs are
            // not the same instances we find in containingDeclarations
            // TODO: fix
            if (this.asSingleFqName() == declarationClassId?.asSingleFqName()) {
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