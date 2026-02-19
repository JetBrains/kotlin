/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.descriptors.EffectiveVisibility.Permissiveness
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.types.model.TypeCheckerProviderContext
import org.jetbrains.kotlin.types.model.TypeConstructorMarker

/**
 * Yet another class representing declaration visibility, along with [org.jetbrains.kotlin.descriptors.DescriptorVisibility] and
 * [org.jetbrains.kotlin.descriptors.Visibility]. Unlike those two, this one represents visibility from the resulting binary artifact
 * standpoint.
 *
 * For example, an `internal` declaration annotated with [PublishedApi] will have the **effective** visibility of [Public].
 *
 * Normally used for checking correctness of inline functions, making sure they don't call less visible functions than themselves
 * (but it's not that simple of course).
 *
 * The general scheme is as follows (arrows point from more visible to less visible):
 *
 * ```
 *                                    Public                             Unknown
 *                                       │                                  │
 *           ┌────────────────────┬──────┴──────────────────┐               │
 *           ▼                    ▼                         ▼               │
 *   Protected (Base)     Protected (Other)     Internal = PackagePrivate   │
 *           │                    │                         │               │
 *           ▼                    │ ┌───────────────────────┤               │
 *  Protected (Derived)           │ │                       ▼               │
 *           │                    │ │           InternalProtected (Base)    │
 *           └────────┬───────────┘ │                       │               │
 *                    ▼             │                       ▼               │
 *             ProtectedBound       │          InternalProtected (Derived)  │
 *                    │             │                       │               │
 *                    └─────────────┼───────────────────────┘               │
 *                                  ▼                                       │
 *                       InternalProtectedBound                             │
 *                                  │                                       │
 *                                  ▼                                       │
 *                            PrivateInFile                                 │
 *                                  │                                       │
 *                                  ├───────────────────────────────────────┘
 *                                  ▼
 *                       PrivateInClass = Local
 * ```
 *
 * @property name A human-readable name for this visibility (useful for rendering in error messages).
 * @property publicApi Whether a declaration with this visibility can be referenced from another module in any way.
 * @property privateApi `true` iff a declaration with this visibility can only be referenced from the same scope. Generally, used
 *   for avoiding reporting diagnostics for private inline functions that reference other private declarations.
 */
sealed class EffectiveVisibility(val name: String, val publicApi: Boolean = false, val privateApi: Boolean = false) {
    override fun toString() = name

    /**
     * Represents a private class/interface member.
     */
    object PrivateInClass : EffectiveVisibility("private-in-class", privateApi = true) {
        override fun relation(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): Permissiveness =
            if (this == other || Local == other) Permissiveness.SAME else Permissiveness.LESS

        override fun toVisibility(): Visibility = Visibilities.Private
    }

    /**
     * Represents a local class/object/interface member, effectively the same as [PrivateInClass].
     */
    object Local : EffectiveVisibility("local") {
        override fun relation(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): Permissiveness =
            if (this == other || PrivateInClass == other) Permissiveness.SAME else Permissiveness.LESS

        override fun toVisibility(): Visibility = Visibilities.Local
    }

    /**
     * Reflects the `CANNOT_INFER_VISIBILITY` diagnostic.
     */
    object Unknown : EffectiveVisibility("unknown") {
        override fun relation(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): Permissiveness =
            if (other == Unknown)
                Permissiveness.SAME
            else
                Permissiveness.UNKNOWN

        override fun toVisibility(): Visibility = Visibilities.Unknown
    }

    /**
     * A declaration with this visibility is only visible within the file it's declared in.
     */
    object PrivateInFile : EffectiveVisibility("private-in-file", privateApi = true) {
        override fun relation(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): Permissiveness =
            when (other) {
                this -> Permissiveness.SAME
                PrivateInClass, Local -> Permissiveness.MORE
                else -> Permissiveness.LESS
            }

        override fun toVisibility(): Visibility = Visibilities.Private
    }

    /**
     * The broadest visibility possible in the language.
     */
    object Public : EffectiveVisibility("public", publicApi = true) {
        override fun relation(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): Permissiveness =
            when (other) {
                this -> Permissiveness.SAME
                Unknown -> Permissiveness.UNKNOWN
                else -> Permissiveness.MORE
            }

        override fun toVisibility(): Visibility = Visibilities.Public
    }

    sealed class InternalOrPackage(internal: Boolean) : EffectiveVisibility(
        if (internal) "internal" else "public/*package*/"
    ) {
        override fun relation(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): Permissiveness =
            when (other) {
                Public -> Permissiveness.LESS
                PrivateInClass, PrivateInFile, Local, InternalProtectedBound, is InternalProtected -> Permissiveness.MORE
                this -> Permissiveness.SAME
                is InternalOrPackage, ProtectedBound, is Protected, Unknown -> Permissiveness.UNKNOWN
            }

        override fun lowerBound(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): EffectiveVisibility =
            when (other) {
                Public -> this
                PrivateInClass, PrivateInFile, Local, InternalProtectedBound, is InternalOrPackage, is InternalProtected -> other
                is Protected -> InternalProtected(other.containerTypeConstructor)
                is Unknown -> Local
                ProtectedBound -> InternalProtectedBound
            }
    }

    /**
     * A declaration with this visibility is generally visible only within the same module, or from friend modules.
     */
    object Internal : InternalOrPackage(true) {
        override fun toVisibility(): Visibility = Visibilities.Internal
    }

    /**
     * The default visibility in Java, if no visibility is specified explicitly.
     * A declaration with this visibility is visible from other modules, but only within the same package.
     */
    object PackagePrivate : InternalOrPackage(false) {
        override fun toVisibility(): Visibility = Visibilities.Private
    }

    /**
     * This visibility is effectively public, in the sense that declarations with this visibility can be referenced from other modules.
     *
     * @property containerTypeConstructor The class from whose subclasses a declaration with this visibility is visible.
     */
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
                is InternalOrPackage, is Unknown -> Permissiveness.UNKNOWN
            }

        override fun lowerBound(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): EffectiveVisibility =
            when (other) {
                Public -> this
                PrivateInClass, PrivateInFile, Local, ProtectedBound, InternalProtectedBound -> other
                is Protected -> when (relation(other, typeCheckerContextProvider)) {
                    Permissiveness.SAME, Permissiveness.LESS -> this
                    Permissiveness.MORE -> other
                    Permissiveness.UNKNOWN -> ProtectedBound
                }
                is InternalProtected -> when (relation(other, typeCheckerContextProvider)) {
                    Permissiveness.MORE -> other
                    else -> InternalProtectedBound
                }
                is InternalOrPackage -> InternalProtected(containerTypeConstructor)
                is Unknown -> Local
            }

        override fun toVisibility(): Visibility = Visibilities.Protected
    }

    /**
     * The lower bound for all protected visibilities.
     */
    object ProtectedBound : EffectiveVisibility("protected (in different classes)", publicApi = true) {
        override fun relation(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): Permissiveness =
            when (other) {
                Public, is Protected -> Permissiveness.LESS
                PrivateInClass, PrivateInFile, Local, InternalProtectedBound -> Permissiveness.MORE
                ProtectedBound -> Permissiveness.SAME
                is InternalOrPackage, is InternalProtected, is Unknown -> Permissiveness.UNKNOWN
            }

        override fun lowerBound(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): EffectiveVisibility =
            when (other) {
                Public, is Protected -> this
                PrivateInClass, PrivateInFile, Local, ProtectedBound, InternalProtectedBound -> other
                is InternalOrPackage, is InternalProtected -> InternalProtectedBound
                is Unknown -> Local
            }

        override fun toVisibility(): Visibility = Visibilities.Protected
    }

    /**
     * The lower bound for [Internal] and [Protected].
     *
     * @property containerTypeConstructor See [Protected.containerTypeConstructor].
     */
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
                ProtectedBound, Unknown -> Permissiveness.UNKNOWN
            }

        override fun lowerBound(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): EffectiveVisibility =
            when (other) {
                Public, is InternalOrPackage -> this
                PrivateInClass, PrivateInFile, Local, InternalProtectedBound -> other
                is Protected, is InternalProtected -> when (relation(other, typeCheckerContextProvider)) {
                    Permissiveness.SAME, Permissiveness.LESS -> this
                    Permissiveness.MORE -> other
                    Permissiveness.UNKNOWN -> InternalProtectedBound
                }
                ProtectedBound -> InternalProtectedBound
                Unknown -> Local
            }

        override fun toVisibility(): Visibility = Visibilities.Private
    }

    /**
     * The lower bound for [Internal] and [ProtectedBound].
     */
    object InternalProtectedBound : EffectiveVisibility("internal & protected (in different classes)") {
        override fun relation(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): Permissiveness =
            when (other) {
                Public, is Protected, is InternalProtected, ProtectedBound, is InternalOrPackage -> Permissiveness.LESS
                PrivateInClass, PrivateInFile, Local -> Permissiveness.MORE
                InternalProtectedBound -> Permissiveness.SAME
                Unknown -> Permissiveness.UNKNOWN
            }

        override fun toVisibility(): Visibility = Visibilities.Private
    }

    enum class Permissiveness {
        LESS,
        SAME,
        MORE,
        UNKNOWN
    }

    /**
     * Calculates whether `this` is more visible or less visible then [other].
     */
    abstract fun relation(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): Permissiveness

    abstract fun toVisibility(): Visibility

    /**
     * Returns the most permissive visibility that is **not** more visible than both `this` and [other].
     */
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

/**
 * Used to compare two protected visibilities.
 *
 * A protected declaration in some class is considered less visible than a protected declaration in its superclass.
 *
 * If we have two unrelated class hierarchies, or class hierarchies for which we can't determine if they are related,
 * returns [Permissiveness.UNKNOWN].
 */
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

fun Visibility.toEffectiveVisibilityOrNull(
    container: TypeConstructorMarker?,
    forClass: Boolean = false,
    ownerIsPublishedApi: Boolean = false,
): EffectiveVisibility? {
    customEffectiveVisibility()?.let { return it }
    return when (this.normalize()) {
        Visibilities.PrivateToThis, Visibilities.InvisibleFake -> EffectiveVisibility.PrivateInClass
        Visibilities.Private -> if (container == null && forClass) EffectiveVisibility.PrivateInFile else EffectiveVisibility.PrivateInClass
        Visibilities.Protected -> EffectiveVisibility.Protected(container)
        Visibilities.Internal -> when (ownerIsPublishedApi) {
            true -> EffectiveVisibility.Public
            false -> EffectiveVisibility.Internal
        }
        Visibilities.Public -> EffectiveVisibility.Public
        Visibilities.Local -> EffectiveVisibility.Local
        // Unknown visibility should not provoke errors,
        // because they can naturally appear when intersection
        // overrides' bases have inconsistent forms.
        Visibilities.Unknown -> EffectiveVisibility.Unknown
        else -> null
    }
}
