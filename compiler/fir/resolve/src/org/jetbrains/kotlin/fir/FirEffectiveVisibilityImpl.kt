/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirEffectiveVisibility.Permissiveness
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.types.classId

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

        override fun toVisibility() = Visibilities.PRIVATE
    }

    // Effectively same as Private
    object Local : FirEffectiveVisibilityImpl("local") {
        override fun relation(other: FirEffectiveVisibility) =
            if (this == other || Private == other) Permissiveness.SAME else Permissiveness.LESS

        override fun toVisibility() = Visibilities.LOCAL
    }

    object Public : FirEffectiveVisibilityImpl("public", publicApi = true) {
        override fun relation(other: FirEffectiveVisibility) =
            if (this == other) Permissiveness.SAME else Permissiveness.MORE

        override fun toVisibility() = Visibilities.PUBLIC
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
            is Protected -> InternalProtected(o.container)
            ProtectedBound -> InternalProtectedBound
        }
    }

    object Internal : InternalOrPackage(true) {
        override fun toVisibility() = Visibilities.INTERNAL
    }

    object PackagePrivate : InternalOrPackage(false) {
        override fun toVisibility() = Visibilities.PRIVATE
    }

    class Protected(val container: FirRegularClass?) : FirEffectiveVisibilityImpl("protected", publicApi = true) {

        override fun equals(other: Any?) = (other is Protected && container == other.container)

        override fun hashCode() = container?.hashCode() ?: 0

        override fun toString() = "${super.toString()} (in ${container?.name ?: '?'})"

        override fun relation(other: FirEffectiveVisibility) = when (val o = other as FirEffectiveVisibilityImpl) {
            Public -> Permissiveness.LESS
            Private, Local, ProtectedBound, InternalProtectedBound -> Permissiveness.MORE
            is Protected -> containerRelation(container, o.container)
            is InternalProtected -> when (containerRelation(container, o.container)) {
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
            is InternalOrPackage -> InternalProtected(container)
        }

        override fun toVisibility() = Visibilities.PROTECTED
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

        override fun toVisibility() = Visibilities.PROTECTED
    }

    // Lower bound for internal and protected(C)
    class InternalProtected(val container: FirRegularClass?) : FirEffectiveVisibilityImpl("internal & protected") {

        override fun equals(other: Any?) = (other is InternalProtected && container == other.container)

        override fun hashCode() = container?.hashCode() ?: 0

        override fun toString() = "${super.toString()} (in ${container?.name ?: '?'})"

        override fun relation(other: FirEffectiveVisibility) = when (val o = other as FirEffectiveVisibilityImpl) {
            Public, is InternalOrPackage -> Permissiveness.LESS
            Private, Local, InternalProtectedBound -> Permissiveness.MORE
            is InternalProtected -> containerRelation(container, o.container)
            is Protected -> when (containerRelation(container, o.container)) {
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

        override fun toVisibility() = Visibilities.PRIVATE
    }

    // Lower bound for internal and protected lower bound
    object InternalProtectedBound : FirEffectiveVisibilityImpl("internal & protected (in different classes)") {
        override fun relation(other: FirEffectiveVisibility) = when (other as FirEffectiveVisibilityImpl) {
            Public, is Protected, is InternalProtected, ProtectedBound, is InternalOrPackage -> Permissiveness.LESS
            Private, Local -> Permissiveness.MORE
            InternalProtectedBound -> Permissiveness.SAME
        }

        override fun toVisibility() = Visibilities.PRIVATE
    }

    override fun lowerBound(other: FirEffectiveVisibility) = when (relation(other)) {
        Permissiveness.SAME, Permissiveness.LESS -> this
        Permissiveness.MORE -> other
        Permissiveness.UNKNOWN -> Private
    }

    companion object {
        private fun containerRelation(first: FirRegularClass?, second: FirRegularClass?): Permissiveness =
            if (first == null || second == null) {
                Permissiveness.UNKNOWN
            } else if (first == second) {
                Permissiveness.SAME
            } else {
                when {
                    first.collectSupertypes().any { it.classId == second.symbol.classId } -> Permissiveness.LESS
                    second.collectSupertypes().any { it.classId == first.symbol.classId } -> Permissiveness.MORE
                    else -> Permissiveness.UNKNOWN
                }
            }

        private fun FirRegularClass.collectSupertypes() = lookupSuperTypes(
            this as FirClass<*>,
            lookupInterfaces = true,
            deep = true,
            useSiteSession = this.session
        )
    }
}

