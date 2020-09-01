/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirEffectiveVisibility.Permissiveness
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.effectiveVisibilityResolver
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

sealed class FirEffectiveVisibilityImpl(
    override val name: String,
    override val publicApi: Boolean = false,
    override val privateApi: Boolean = false
) : FirEffectiveVisibility {

    override fun toString() = name

    //                    Public
    //                /--/   |  \-------------\
    // Protected(Base)       |                 \
    //       |         Protected(Other)        Internal = PackagePrivate
    // Protected(Derived) |                   /     \
    //             |      |                  /    InternalProtected(Base)
    //       ProtectedBound                 /        \
    //                    \                /       /InternalProtected(Derived)
    //                     \InternalProtectedBound/
    //                              |
    //                           Private = Local


    object Private : FirEffectiveVisibilityImpl("private", privateApi = true) {
        override fun relation(other: FirEffectiveVisibility) =
            if (this == other || Local == other) Permissiveness.SAME else Permissiveness.LESS

        override fun toVisibility() = Visibilities.Private
    }

    // Effectively same as Private
    object Local : FirEffectiveVisibilityImpl("local") {
        override fun relation(other: FirEffectiveVisibility) =
            if (this == other || Private == other) Permissiveness.SAME else Permissiveness.LESS

        override fun toVisibility() = Visibilities.Local
    }

    object Public : FirEffectiveVisibilityImpl("public", publicApi = true) {
        override fun relation(other: FirEffectiveVisibility) =
            if (this == other) Permissiveness.SAME else Permissiveness.MORE

        override fun toVisibility() = Visibilities.Public
    }

    abstract class InternalOrPackage protected constructor(internal: Boolean) : FirEffectiveVisibilityImpl(
        if (internal) "internal" else "public/*package*/"
    ) {
        override fun relation(other: FirEffectiveVisibility) = when (other as FirEffectiveVisibilityImpl) {
            Public -> Permissiveness.LESS
            Private, Local, InternalProtectedBound, is InternalProtected -> Permissiveness.MORE
            is InternalOrPackage -> Permissiveness.SAME
            ProtectedBound, is Protected -> Permissiveness.UNKNOWN
        }

        override fun lowerBound(other: FirEffectiveVisibility) = when (val o = other as FirEffectiveVisibilityImpl) {
            Public -> this
            Private, Local, InternalProtectedBound, is InternalOrPackage, is InternalProtected -> other
            is Protected -> InternalProtected(o.containerSymbol, o.session)
            ProtectedBound -> InternalProtectedBound
        }
    }

    object Internal : InternalOrPackage(true) {
        override fun toVisibility() = Visibilities.Internal
    }

    object PackagePrivate : InternalOrPackage(false) {
        override fun toVisibility() = Visibilities.Private
    }

    class Protected(
        val containerSymbol: FirClassLikeSymbol<*>?,
        internal val session: FirSession
    ) : FirEffectiveVisibilityImpl("protected", publicApi = true) {

        override fun equals(other: Any?) = (other is Protected && containerSymbol == other.containerSymbol)

        override fun hashCode() = containerSymbol?.hashCode() ?: 0

        override fun toString() = "${super.toString()} (in ${containerSymbol.safeAs<FirRegularClassSymbol>()?.fir?.name ?: '?'})"

        override fun relation(other: FirEffectiveVisibility) = when (val o = other as FirEffectiveVisibilityImpl) {
            Public -> Permissiveness.LESS
            Private, Local, ProtectedBound, InternalProtectedBound -> Permissiveness.MORE
            is Protected -> containerRelation(containerSymbol, o.containerSymbol, session)
            is InternalProtected -> when (containerRelation(containerSymbol, o.containerSymbol, session)) {
                // Protected never can be less permissive than internal & protected
                Permissiveness.SAME, Permissiveness.MORE -> Permissiveness.MORE
                Permissiveness.UNKNOWN, Permissiveness.LESS -> Permissiveness.UNKNOWN
            }
            is InternalOrPackage -> Permissiveness.UNKNOWN
        }

        override fun lowerBound(other: FirEffectiveVisibility) = when (other as FirEffectiveVisibilityImpl) {
            Public -> this
            Private, Local, ProtectedBound, InternalProtectedBound -> other
            is Protected -> when (relation(other)) {
                Permissiveness.SAME, Permissiveness.MORE -> this
                Permissiveness.LESS -> other
                Permissiveness.UNKNOWN -> ProtectedBound
            }
            is InternalProtected -> when (relation(other)) {
                Permissiveness.LESS -> other
                else -> InternalProtectedBound
            }
            is InternalOrPackage -> InternalProtected(containerSymbol, session)
        }

        override fun toVisibility() = Visibilities.Protected
    }

    // Lower bound for all protected visibilities
    object ProtectedBound : FirEffectiveVisibilityImpl("protected (in different classes)", publicApi = true) {
        override fun relation(other: FirEffectiveVisibility) = when (other as FirEffectiveVisibilityImpl) {
            Public, is Protected -> Permissiveness.LESS
            Private, Local, InternalProtectedBound -> Permissiveness.MORE
            ProtectedBound -> Permissiveness.SAME
            is InternalOrPackage, is InternalProtected -> Permissiveness.UNKNOWN
        }

        override fun lowerBound(other: FirEffectiveVisibility) = when (other as FirEffectiveVisibilityImpl) {
            Public, is Protected -> this
            Private, Local, ProtectedBound, InternalProtectedBound -> other
            is InternalOrPackage, is InternalProtected -> InternalProtectedBound
        }

        override fun toVisibility() = Visibilities.Protected
    }

    // Lower bound for internal and protected(C)
    class InternalProtected(
        val containerSymbol: FirClassLikeSymbol<*>?,
        private val session: FirSession
    ) : FirEffectiveVisibilityImpl("internal & protected") {

        override fun equals(other: Any?) = (other is InternalProtected && containerSymbol == other.containerSymbol)

        override fun hashCode() = containerSymbol?.hashCode() ?: 0

        override fun toString() = "${super.toString()} (in ${containerSymbol.safeAs<FirRegularClassSymbol>()?.fir?.name ?: '?'})"

        override fun relation(other: FirEffectiveVisibility) = when (val o = other as FirEffectiveVisibilityImpl) {
            Public, is InternalOrPackage -> Permissiveness.LESS
            Private, Local, InternalProtectedBound -> Permissiveness.MORE
            is InternalProtected -> containerRelation(containerSymbol, o.containerSymbol, session)
            is Protected -> when (containerRelation(containerSymbol, o.containerSymbol, session)) {
                // Internal & protected never can be more permissive than just protected
                Permissiveness.SAME, Permissiveness.LESS -> Permissiveness.LESS
                Permissiveness.UNKNOWN, Permissiveness.MORE -> Permissiveness.UNKNOWN
            }
            ProtectedBound -> Permissiveness.UNKNOWN
        }

        override fun lowerBound(other: FirEffectiveVisibility) = when (other as FirEffectiveVisibilityImpl) {
            Public, is InternalOrPackage -> this
            Private, Local, InternalProtectedBound -> other
            is Protected, is InternalProtected -> when (relation(other)) {
                Permissiveness.SAME, Permissiveness.MORE -> this
                Permissiveness.LESS -> other
                Permissiveness.UNKNOWN -> InternalProtectedBound
            }
            ProtectedBound -> InternalProtectedBound
        }

        override fun toVisibility() = Visibilities.Private
    }

    // Lower bound for internal and protected lower bound
    object InternalProtectedBound : FirEffectiveVisibilityImpl("internal & protected (in different classes)") {
        override fun relation(other: FirEffectiveVisibility) = when (other as FirEffectiveVisibilityImpl) {
            Public, is Protected, is InternalProtected, ProtectedBound, is InternalOrPackage -> Permissiveness.LESS
            Private, Local -> Permissiveness.MORE
            InternalProtectedBound -> Permissiveness.SAME
        }

        override fun toVisibility() = Visibilities.Private
    }

    override fun lowerBound(other: FirEffectiveVisibility): FirEffectiveVisibility {
        if (this == Local || other == Local) return Local
        return when (relation(other)) {
            Permissiveness.SAME, Permissiveness.LESS -> this
            Permissiveness.MORE -> other
            Permissiveness.UNKNOWN -> Private
        }
    }

    companion object {
        private fun containerRelation(first: FirClassLikeSymbol<*>?, second: FirClassLikeSymbol<*>?, session: FirSession): Permissiveness =
            if (first == null || second == null) {
                Permissiveness.UNKNOWN
            } else if (first == second) {
                Permissiveness.SAME
            } else {
                when {
                    first.collectSupertypes(session).any { it.classId == second.classId } -> Permissiveness.LESS
                    second.collectSupertypes(session).any { it.classId == first.classId } -> Permissiveness.MORE
                    else -> Permissiveness.UNKNOWN
                }
            }

        private fun FirClassifierSymbol<*>.collectSupertypes(session: FirSession) = lookupSuperTypes(
            this,
            lookupInterfaces = true,
            deep = true,
            useSiteSession = session
        )
    }
}

fun FirMemberDeclaration.getEffectiveVisibility(
    session: FirSession,
    containingDeclarations: List<FirDeclaration>,
    scopeSession: ScopeSession
): FirEffectiveVisibility = session.effectiveVisibilityResolver.resolveFor(this, containingDeclarations, scopeSession)
