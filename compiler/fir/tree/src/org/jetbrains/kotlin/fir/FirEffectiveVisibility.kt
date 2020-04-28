/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirEffectiveVisibility.*
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.classId
import org.jetbrains.kotlin.name.ClassId

sealed class FirEffectiveVisibility(val name: String, val publicApi: Boolean = false, val privateApi: Boolean = false) {

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


    object Private : FirEffectiveVisibility("private", privateApi = true) {
        override fun relation(
            other: FirEffectiveVisibility,
            thisContainerSupertypes: List<ClassId?>?,
            otherContainerSupertypes: List<ClassId?>?
        ): Permissiveness =
            if (this == other || Local == other) Permissiveness.SAME else Permissiveness.LESS

        override fun toVisibility() = Visibilities.PRIVATE
    }

    // Effectively same as Private
    object Local : FirEffectiveVisibility("local") {
        override fun relation(
            other: FirEffectiveVisibility,
            thisContainerSupertypes: List<ClassId?>?,
            otherContainerSupertypes: List<ClassId?>?
        ): Permissiveness =
            if (this == other || Private == other) Permissiveness.SAME else Permissiveness.LESS

        override fun toVisibility() = Visibilities.LOCAL
    }

    object Public : FirEffectiveVisibility("public", publicApi = true) {
        override fun relation(
            other: FirEffectiveVisibility,
            thisContainerSupertypes: List<ClassId?>?,
            otherContainerSupertypes: List<ClassId?>?
        ): Permissiveness =
            if (this == other) Permissiveness.SAME else Permissiveness.MORE

        override fun toVisibility() = Visibilities.PUBLIC
    }

    abstract class InternalOrPackage protected constructor(internal: Boolean) : FirEffectiveVisibility(
        if (internal) "internal" else "public/*package*/"
    ) {
        override fun relation(
            other: FirEffectiveVisibility,
            thisContainerSupertypes: List<ClassId?>?,
            otherContainerSupertypes: List<ClassId?>?
        ): Permissiveness = when (other) {
            Public -> Permissiveness.LESS
            Private, Local, InternalProtectedBound, is InternalProtected -> Permissiveness.MORE
            is InternalOrPackage -> Permissiveness.SAME
            ProtectedBound, is Protected -> Permissiveness.UNKNOWN
        }

        override fun lowerBound(
            other: FirEffectiveVisibility,
            thisContainerSupertypes: List<ClassId?>?,
            otherContainerSupertypes: List<ClassId?>?
        ): FirEffectiveVisibility = when (other) {
            Public -> this
            Private, Local, InternalProtectedBound, is InternalOrPackage, is InternalProtected -> other
            is Protected -> InternalProtected(other.container)
            ProtectedBound -> InternalProtectedBound
        }
    }

    object Internal : InternalOrPackage(true) {
        override fun toVisibility() = Visibilities.INTERNAL
    }

    object PackagePrivate : InternalOrPackage(false) {
        override fun toVisibility() = Visibilities.PRIVATE
    }

    class Protected(val container: FirRegularClass?) : FirEffectiveVisibility("protected", publicApi = true) {

        override fun equals(other: Any?) = (other is Protected && container == other.container)

        override fun hashCode() = container?.hashCode() ?: 0

        override fun toString() = "${super.toString()} (in ${container?.name ?: '?'})"

        override fun relation(
            other: FirEffectiveVisibility,
            thisContainerSupertypes: List<ClassId?>?,
            otherContainerSupertypes: List<ClassId?>?
        ): Permissiveness = when (other) {
            Public -> Permissiveness.LESS
            Private, Local, ProtectedBound, InternalProtectedBound -> Permissiveness.MORE
            is Protected -> containerRelation(container, other.container, thisContainerSupertypes, otherContainerSupertypes)
            is InternalProtected -> when (containerRelation(
                container,
                other.container,
                thisContainerSupertypes,
                otherContainerSupertypes
            )) {
                // Protected never can be less permissive than internal & protected
                Permissiveness.SAME, Permissiveness.MORE -> Permissiveness.MORE
                Permissiveness.UNKNOWN, Permissiveness.LESS -> Permissiveness.UNKNOWN
            }
            is InternalOrPackage -> Permissiveness.UNKNOWN
        }

        override fun lowerBound(
            other: FirEffectiveVisibility,
            thisContainerSupertypes: List<ClassId?>?,
            otherContainerSupertypes: List<ClassId?>?
        ): FirEffectiveVisibility = when (other) {
            Public -> this
            Private, Local, ProtectedBound, InternalProtectedBound -> other
            is Protected -> when (relation(other, thisContainerSupertypes, otherContainerSupertypes)) {
                Permissiveness.SAME, Permissiveness.MORE -> this
                Permissiveness.LESS -> other
                Permissiveness.UNKNOWN -> ProtectedBound
            }
            is InternalProtected -> when (relation(other, thisContainerSupertypes, otherContainerSupertypes)) {
                Permissiveness.LESS -> other
                else -> InternalProtectedBound
            }
            is InternalOrPackage -> InternalProtected(container)
        }

        override fun toVisibility() = Visibilities.PROTECTED

    }

    // Lower bound for all protected visibilities
    object ProtectedBound : FirEffectiveVisibility("protected (in different classes)", publicApi = true) {
        override fun relation(
            other: FirEffectiveVisibility,
            thisContainerSupertypes: List<ClassId?>?,
            otherContainerSupertypes: List<ClassId?>?
        ): Permissiveness = when (other) {
            Public, is Protected -> Permissiveness.LESS
            Private, Local, InternalProtectedBound -> Permissiveness.MORE
            ProtectedBound -> Permissiveness.SAME
            is InternalOrPackage, is InternalProtected -> Permissiveness.UNKNOWN
        }

        override fun lowerBound(
            other: FirEffectiveVisibility,
            thisContainerSupertypes: List<ClassId?>?,
            otherContainerSupertypes: List<ClassId?>?
        ): FirEffectiveVisibility = when (other) {
            Public, is Protected -> this
            Private, Local, ProtectedBound, InternalProtectedBound -> other
            is InternalOrPackage, is InternalProtected -> InternalProtectedBound
        }

        override fun toVisibility() = Visibilities.PROTECTED
    }

    // Lower bound for internal and protected(C)
    class InternalProtected(val container: FirRegularClass?) : FirEffectiveVisibility("internal & protected") {

        override fun equals(other: Any?) = (other is InternalProtected && container == other.container)

        override fun hashCode() = container?.hashCode() ?: 0

        override fun toString() = "${super.toString()} (in ${container?.name ?: '?'})"

        override fun relation(
            other: FirEffectiveVisibility,
            thisContainerSupertypes: List<ClassId?>?,
            otherContainerSupertypes: List<ClassId?>?
        ): Permissiveness = when (other) {
            Public, is InternalOrPackage -> Permissiveness.LESS
            Private, Local, InternalProtectedBound -> Permissiveness.MORE
            is InternalProtected -> containerRelation(container, other.container, thisContainerSupertypes, otherContainerSupertypes)
            is Protected -> when (containerRelation(container, other.container, thisContainerSupertypes, otherContainerSupertypes)) {
                // Internal & protected never can be more permissive than just protected
                Permissiveness.SAME, Permissiveness.LESS -> Permissiveness.LESS
                Permissiveness.UNKNOWN, Permissiveness.MORE -> Permissiveness.UNKNOWN
            }
            ProtectedBound -> Permissiveness.UNKNOWN
        }

        override fun lowerBound(
            other: FirEffectiveVisibility,
            thisContainerSupertypes: List<ClassId?>?,
            otherContainerSupertypes: List<ClassId?>?
        ): FirEffectiveVisibility = when (other) {
            Public, is InternalOrPackage -> this
            Private, Local, InternalProtectedBound -> other
            is Protected, is InternalProtected -> when (relation(other, thisContainerSupertypes, otherContainerSupertypes)) {
                Permissiveness.SAME, Permissiveness.MORE -> this
                Permissiveness.LESS -> other
                Permissiveness.UNKNOWN -> InternalProtectedBound
            }
            ProtectedBound -> InternalProtectedBound
        }

        override fun toVisibility() = Visibilities.PRIVATE
    }

    // Lower bound for internal and protected lower bound
    object InternalProtectedBound : FirEffectiveVisibility("internal & protected (in different classes)") {
        override fun relation(
            other: FirEffectiveVisibility,
            thisContainerSupertypes: List<ClassId?>?,
            otherContainerSupertypes: List<ClassId?>?
        ): Permissiveness = when (other) {
            Public, is Protected, is InternalProtected, ProtectedBound, is InternalOrPackage -> Permissiveness.LESS
            Private, Local -> Permissiveness.MORE
            InternalProtectedBound -> Permissiveness.SAME
        }

        override fun toVisibility() = Visibilities.PRIVATE
    }

    enum class Permissiveness {
        LESS,
        SAME,
        MORE,
        UNKNOWN
    }

    abstract fun relation(
        other: FirEffectiveVisibility,
        thisContainerSupertypes: List<ClassId?>?,
        otherContainerSupertypes: List<ClassId?>?
    ): Permissiveness

    abstract fun toVisibility(): Visibility

    open fun getContainerClass(): FirRegularClass? =
        when (this) {
            is Protected -> this.container
            is InternalProtected -> this.container
            else -> null
        }

    open fun needSupertypes(other: FirEffectiveVisibility) = this.getContainerClass() != null && other.getContainerClass() != null

    open fun lowerBound(
        other: FirEffectiveVisibility,
        thisContainerSupertypes: List<ClassId?>?,
        otherContainerSupertypes: List<ClassId?>?
    ) =
        when (relation(other, thisContainerSupertypes, otherContainerSupertypes)) {
            Permissiveness.SAME, Permissiveness.LESS -> this
            Permissiveness.MORE -> other
            Permissiveness.UNKNOWN -> Private
        }
}

internal fun containerRelation(
    first: FirRegularClass?,
    second: FirRegularClass?,
    firstContainerSupertypes: List<ClassId?>?,
    secondContainerSupertypes: List<ClassId?>?
): Permissiveness =
    if (first == null || second == null || firstContainerSupertypes == null || secondContainerSupertypes == null) {
        Permissiveness.UNKNOWN
    } else if (first == second) {
        Permissiveness.SAME
    } else if (firstContainerSupertypes.contains(second.classId)) {
        Permissiveness.LESS
    } else if (secondContainerSupertypes.contains(first.classId)) {
        Permissiveness.MORE
    } else {
        Permissiveness.UNKNOWN
    }

internal fun Visibility.firEffectiveVisibilityApproximation(): FirEffectiveVisibility =
    when (this) {
        Visibilities.PUBLIC -> Public
        Visibilities.PRIVATE -> Private
        Visibilities.PRIVATE_TO_THIS -> Private
        Visibilities.INTERNAL -> Internal
        Visibilities.LOCAL -> Local
        Visibilities.PROTECTED -> ProtectedBound
        else -> Public
    }

