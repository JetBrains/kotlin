/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.descriptors.EffectiveVisibility.Permissiveness
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.types.model.TypeCheckerProviderContext
import org.jetbrains.kotlin.types.model.TypeConstructorMarker

sealed class EffectiveVisibility(val name: String, val publicApi: Boolean = false, val privateApi: Boolean = false) {
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
    //                        PrivateInFile
    //                              |
    //                        PrivateInClass = Local

    // Private class (interface) member
    object PrivateInClass : EffectiveVisibility("private-in-class", privateApi = true) {
        override fun relation(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): Permissiveness =
            if (this == other || Local == other) Permissiveness.SAME else Permissiveness.LESS

        override fun toVisibility(): Visibility = Visibilities.Private
    }

    // Effectively same as PrivateInClass
    object Local : EffectiveVisibility("local") {
        override fun relation(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): Permissiveness =
            if (this == other || PrivateInClass == other) Permissiveness.SAME else Permissiveness.LESS

        override fun toVisibility(): Visibility = Visibilities.Local
    }

    // Private with File container
    object PrivateInFile : EffectiveVisibility("private-in-file", privateApi = true) {
        override fun relation(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): Permissiveness =
            when (other) {
                this -> Permissiveness.SAME
                PrivateInClass, Local -> Permissiveness.MORE
                else -> Permissiveness.LESS
            }

        override fun toVisibility(): Visibility = Visibilities.Private
    }

    object Public : EffectiveVisibility("public", publicApi = true) {
        override fun relation(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): Permissiveness =
            if (this == other) Permissiveness.SAME else Permissiveness.MORE

        override fun toVisibility(): Visibility = Visibilities.Public
    }

    abstract class InternalOrPackage protected constructor(internal: Boolean) : EffectiveVisibility(
        if (internal) "internal" else "public/*package*/"
    ) {
        override fun relation(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): Permissiveness =
            when (other) {
                Public -> Permissiveness.LESS
                PrivateInClass, PrivateInFile, Local, InternalProtectedBound, is InternalProtected -> Permissiveness.MORE
                is InternalOrPackage -> Permissiveness.SAME
                ProtectedBound, is Protected -> Permissiveness.UNKNOWN
            }

        override fun lowerBound(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): EffectiveVisibility =
            when (other) {
                Public -> this
                PrivateInClass, PrivateInFile, Local, InternalProtectedBound, is InternalOrPackage, is InternalProtected -> other
                is Protected -> InternalProtected(other.containerTypeConstructor)
                ProtectedBound -> InternalProtectedBound
            }
    }

    object Internal : InternalOrPackage(true) {
        override fun toVisibility(): Visibility = Visibilities.Internal
    }

    object PackagePrivate : InternalOrPackage(false) {
        override fun toVisibility(): Visibility = Visibilities.Private
    }

    class Protected(val containerTypeConstructor: TypeConstructorMarker?) : EffectiveVisibility("protected", publicApi = true) {

        override fun equals(other: Any?) = (other is Protected && containerTypeConstructor == other.containerTypeConstructor)

        override fun hashCode() = containerTypeConstructor?.hashCode() ?: 0

        override fun toString() = "${super.toString()} (in ${containerTypeConstructor ?: '?'})"

        override fun relation(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): Permissiveness =
            when (other) {
                Public -> Permissiveness.LESS
                PrivateInClass, PrivateInFile, Local, ProtectedBound, InternalProtectedBound -> Permissiveness.MORE
                is Protected -> containerRelation(containerTypeConstructor, other.containerTypeConstructor, typeCheckerContextProvider)
                is InternalProtected -> when (containerRelation(
                    containerTypeConstructor,
                    other.containerTypeConstructor,
                    typeCheckerContextProvider
                )) {
                    // Protected never can be less permissive than internal & protected
                    Permissiveness.SAME, Permissiveness.MORE -> Permissiveness.MORE
                    Permissiveness.UNKNOWN, Permissiveness.LESS -> Permissiveness.UNKNOWN
                }
                is InternalOrPackage -> Permissiveness.UNKNOWN
            }

        override fun lowerBound(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): EffectiveVisibility =
            when (other) {
                Public -> this
                PrivateInClass, PrivateInFile, Local, ProtectedBound, InternalProtectedBound -> other
                is Protected -> when (relation(other, typeCheckerContextProvider)) {
                    Permissiveness.SAME, Permissiveness.MORE -> this
                    Permissiveness.LESS -> other
                    Permissiveness.UNKNOWN -> ProtectedBound
                }
                is InternalProtected -> when (relation(other, typeCheckerContextProvider)) {
                    Permissiveness.LESS -> other
                    else -> InternalProtectedBound
                }
                is InternalOrPackage -> InternalProtected(containerTypeConstructor)
            }

        override fun toVisibility(): Visibility = Visibilities.Protected
    }

    // Lower bound for all protected visibilities
    object ProtectedBound : EffectiveVisibility("protected (in different classes)", publicApi = true) {
        override fun relation(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): Permissiveness =
            when (other) {
                Public, is Protected -> Permissiveness.LESS
                PrivateInClass, PrivateInFile, Local, InternalProtectedBound -> Permissiveness.MORE
                ProtectedBound -> Permissiveness.SAME
                is InternalOrPackage, is InternalProtected -> Permissiveness.UNKNOWN
            }

        override fun lowerBound(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): EffectiveVisibility =
            when (other) {
                Public, is Protected -> this
                PrivateInClass, PrivateInFile, Local, ProtectedBound, InternalProtectedBound -> other
                is InternalOrPackage, is InternalProtected -> InternalProtectedBound
            }

        override fun toVisibility(): Visibility = Visibilities.Protected
    }

    // Lower bound for internal and protected(C)
    class InternalProtected(
        val containerTypeConstructor: TypeConstructorMarker?
    ) : EffectiveVisibility("internal & protected", publicApi = false) {

        override fun equals(other: Any?) = (other is InternalProtected && containerTypeConstructor == other.containerTypeConstructor)

        override fun hashCode() = containerTypeConstructor?.hashCode() ?: 0

        override fun toString() = "${super.toString()} (in ${containerTypeConstructor ?: '?'})"

        override fun relation(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): Permissiveness =
            when (other) {
                Public, is InternalOrPackage -> Permissiveness.LESS
                PrivateInClass, PrivateInFile, Local, InternalProtectedBound -> Permissiveness.MORE
                is InternalProtected -> containerRelation(
                    containerTypeConstructor,
                    other.containerTypeConstructor,
                    typeCheckerContextProvider
                )
                is Protected -> when (containerRelation(
                    containerTypeConstructor,
                    other.containerTypeConstructor,
                    typeCheckerContextProvider
                )) {
                    // Internal & protected never can be more permissive than just protected
                    Permissiveness.SAME, Permissiveness.LESS -> Permissiveness.LESS
                    Permissiveness.UNKNOWN, Permissiveness.MORE -> Permissiveness.UNKNOWN
                }
                ProtectedBound -> Permissiveness.UNKNOWN
            }

        override fun lowerBound(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): EffectiveVisibility =
            when (other) {
                Public, is InternalOrPackage -> this
                PrivateInClass, PrivateInFile, Local, InternalProtectedBound -> other
                is Protected, is InternalProtected -> when (relation(other, typeCheckerContextProvider)) {
                    Permissiveness.SAME, Permissiveness.MORE -> this
                    Permissiveness.LESS -> other
                    Permissiveness.UNKNOWN -> InternalProtectedBound
                }
                ProtectedBound -> InternalProtectedBound
            }

        override fun toVisibility(): Visibility = Visibilities.Private
    }

    // Lower bound for internal and protected lower bound
    object InternalProtectedBound : EffectiveVisibility("internal & protected (in different classes)") {
        override fun relation(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): Permissiveness =
            when (other) {
                Public, is Protected, is InternalProtected, ProtectedBound, is InternalOrPackage -> Permissiveness.LESS
                PrivateInClass, PrivateInFile, Local -> Permissiveness.MORE
                InternalProtectedBound -> Permissiveness.SAME
            }

        override fun toVisibility(): Visibility = Visibilities.Private
    }

    enum class Permissiveness {
        LESS,
        SAME,
        MORE,
        UNKNOWN
    }

    abstract fun relation(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): Permissiveness

    abstract fun toVisibility(): Visibility

    open fun lowerBound(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): EffectiveVisibility =
        when (relation(other, typeCheckerContextProvider)) {
            Permissiveness.SAME, Permissiveness.LESS -> this
            Permissiveness.MORE -> other
            Permissiveness.UNKNOWN -> PrivateInClass
        }
}

enum class RelationToType(val description: String) {
    CONSTRUCTOR(""),
    CONTAINER(" containing declaration"),
    ARGUMENT(" argument"),
    ARGUMENT_CONTAINER(" argument containing declaration");

    fun containerRelation() = when (this) {
        CONSTRUCTOR, CONTAINER -> CONTAINER
        ARGUMENT, ARGUMENT_CONTAINER -> ARGUMENT_CONTAINER
    }

    override fun toString() = description
}

internal fun containerRelation(
    first: TypeConstructorMarker?,
    second: TypeConstructorMarker?,
    typeCheckerContextProvider: TypeCheckerProviderContext
): Permissiveness {
    return when {
        first == null || second == null -> Permissiveness.UNKNOWN
        first == second -> Permissiveness.SAME
        AbstractTypeChecker.isSubtypeOfClass(typeCheckerContextProvider.createTypeCheckerContext(), first, second) -> Permissiveness.LESS
        AbstractTypeChecker.isSubtypeOfClass(typeCheckerContextProvider.createTypeCheckerContext(), second, first) -> Permissiveness.MORE
        else -> Permissiveness.UNKNOWN
    }
}

private fun TypeCheckerProviderContext.createTypeCheckerContext(): TypeCheckerState = newTypeCheckerState(
    errorTypesEqualToAnything = false,
    stubTypesEqualToAnything = true
)
