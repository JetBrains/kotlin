/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.descriptors.EffectiveVisibility.*
import org.jetbrains.kotlin.descriptors.RelationToType.*

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
    //                           Private = Local


    object Private : EffectiveVisibility("private", privateApi = true) {
        override fun relation(other: EffectiveVisibility) =
                if (this == other || Local == other) Permissiveness.SAME else Permissiveness.LESS
    }

    // Effectively same as Private
    object Local : EffectiveVisibility("local") {
        override fun relation(other: EffectiveVisibility) =
                if (this == other || Private == other) Permissiveness.SAME else Permissiveness.LESS
    }

    object Public : EffectiveVisibility("public", publicApi = true) {
        override fun relation(other: EffectiveVisibility) =
                if (this == other) Permissiveness.SAME else Permissiveness.MORE
    }

    abstract class InternalOrPackage protected constructor(internal: Boolean) : EffectiveVisibility(
            if (internal) "internal" else "public/*package*/"
    ) {
        override fun relation(other: EffectiveVisibility) = when (other) {
            Public -> Permissiveness.LESS
            Private, Local, InternalProtectedBound, is InternalProtected -> Permissiveness.MORE
            is InternalOrPackage -> Permissiveness.SAME
            ProtectedBound, is Protected -> Permissiveness.UNKNOWN
        }

        override fun lowerBound(other: EffectiveVisibility) = when (other) {
            Public -> this
            Private, Local, InternalProtectedBound, is InternalOrPackage, is InternalProtected -> other
            is Protected -> InternalProtected(other.container)
            ProtectedBound -> InternalProtectedBound
        }
    }

    object Internal : InternalOrPackage(true)

    object PackagePrivate : InternalOrPackage(false)

    class Protected(val container: ClassDescriptor?) : EffectiveVisibility("protected", publicApi = true) {

        override fun equals(other: Any?) = (other is Protected && container == other.container)

        override fun hashCode() = container?.hashCode() ?: 0

        override fun toString() = "${super.toString()} (in ${container?.name ?: '?'})"

        override fun relation(other: EffectiveVisibility) = when (other) {
            Public -> Permissiveness.LESS
            Private, Local, ProtectedBound, InternalProtectedBound -> Permissiveness.MORE
            is Protected -> containerRelation(container, other.container)
            is InternalProtected -> when (containerRelation(container, other.container)) {
                // Protected never can be less permissive than internal & protected
                Permissiveness.SAME, Permissiveness.MORE -> Permissiveness.MORE
                Permissiveness.UNKNOWN, Permissiveness.LESS -> Permissiveness.UNKNOWN
            }
            is InternalOrPackage -> Permissiveness.UNKNOWN
        }

        override fun lowerBound(other: EffectiveVisibility) = when (other) {
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
    }

    // Lower bound for all protected visibilities
    object ProtectedBound : EffectiveVisibility("protected (in different classes)", publicApi = true) {
        override fun relation(other: EffectiveVisibility) = when (other) {
            Public, is Protected -> Permissiveness.LESS
            Private, Local, InternalProtectedBound -> Permissiveness.MORE
            ProtectedBound -> Permissiveness.SAME
            is InternalOrPackage, is InternalProtected -> Permissiveness.UNKNOWN
        }

        override fun lowerBound(other: EffectiveVisibility) = when (other) {
            Public, is Protected -> this
            Private, Local, ProtectedBound, InternalProtectedBound -> other
            is InternalOrPackage, is InternalProtected -> InternalProtectedBound
        }
    }

    // Lower bound for internal and protected(C)
    class InternalProtected(val container: ClassDescriptor?): EffectiveVisibility("internal & protected") {

        override fun equals(other: Any?) = (other is InternalProtected && container == other.container)

        override fun hashCode() = container?.hashCode() ?: 0

        override fun toString() = "${super.toString()} (in ${container?.name ?: '?'})"

        override fun relation(other: EffectiveVisibility) = when (other) {
            Public, is InternalOrPackage -> Permissiveness.LESS
            Private, Local, InternalProtectedBound -> Permissiveness.MORE
            is InternalProtected -> containerRelation(container, other.container)
            is Protected -> when (containerRelation(container, other.container)) {
                // Internal & protected never can be more permissive than just protected
                Permissiveness.SAME, Permissiveness.LESS -> Permissiveness.LESS
                Permissiveness.UNKNOWN, Permissiveness.MORE -> Permissiveness.UNKNOWN
            }
            ProtectedBound -> Permissiveness.UNKNOWN
        }

        override fun lowerBound(other: EffectiveVisibility) = when (other) {
            Public, is InternalOrPackage -> this
            Private, Local, InternalProtectedBound -> other
            is Protected, is InternalProtected -> when (relation(other)) {
                Permissiveness.SAME, Permissiveness.MORE -> this
                Permissiveness.LESS -> other
                Permissiveness.UNKNOWN -> InternalProtectedBound
            }
            ProtectedBound -> InternalProtectedBound
        }
    }

    // Lower bound for internal and protected lower bound
    object InternalProtectedBound : EffectiveVisibility("internal & protected (in different classes)") {
        override fun relation(other: EffectiveVisibility) = when (other) {
            Public, is Protected, is InternalProtected, ProtectedBound, is InternalOrPackage -> Permissiveness.LESS
            Private, Local -> Permissiveness.MORE
            InternalProtectedBound -> Permissiveness.SAME
        }
    }

    internal enum class Permissiveness {
        LESS,
        SAME,
        MORE,
        UNKNOWN
    }

    abstract internal fun relation(other: EffectiveVisibility): Permissiveness

    open internal fun lowerBound(other: EffectiveVisibility) = when (relation(other)) {
        Permissiveness.SAME, Permissiveness.LESS -> this
        Permissiveness.MORE -> other
        Permissiveness.UNKNOWN -> Private
    }
}

internal fun containerRelation(first: ClassDescriptor?, second: ClassDescriptor?): Permissiveness =
        if (first == null || second == null) {
            Permissiveness.UNKNOWN
        }
        else if (first == second) {
            Permissiveness.SAME
        }
        else if (DescriptorUtils.isSubclass(first, second)) {
            Permissiveness.LESS
        }
        else if (DescriptorUtils.isSubclass(second, first)) {
            Permissiveness.MORE
        }
        else {
            Permissiveness.UNKNOWN
        }

private fun lowerBound(first: EffectiveVisibility, second: EffectiveVisibility) =
        first.lowerBound(second)

private fun lowerBound(first: EffectiveVisibility, args: List<EffectiveVisibility>) =
        args.fold(first, { x, y -> x.lowerBound(y) })

private fun lowerBound(args: List<EffectiveVisibility>) =
        if (args.isEmpty()) Public else lowerBound(args.first(), args.subList(1, args.size))

private fun Visibility.forVisibility(descriptor: ClassDescriptor? = null): EffectiveVisibility = when (this) {
    Visibilities.PRIVATE, Visibilities.PRIVATE_TO_THIS -> Private
    Visibilities.PROTECTED -> Protected(descriptor)
    Visibilities.INTERNAL -> Internal
    Visibilities.PUBLIC -> Public
    Visibilities.LOCAL -> Local
    else -> this.effectiveVisibility(descriptor)
}

fun effectiveVisibility(visibility: Visibility, descriptor: ClassDescriptor?) = visibility.forVisibility(descriptor)

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

data class DescriptorWithRelation(val descriptor: ClassifierDescriptor, val relation: RelationToType) {
    fun effectiveVisibility() =
            (descriptor as? ClassDescriptor)?.visibility?.effectiveVisibility(descriptor.containingDeclaration as? ClassDescriptor) ?: Public

    override fun toString() = "$relation ${descriptor.name}"
}

private fun ClassifierDescriptor.dependentDescriptors(ownRelation: RelationToType): Set<DescriptorWithRelation> =
        setOf(DescriptorWithRelation(this, ownRelation)) +
        ((this.containingDeclaration as? ClassifierDescriptor)?.dependentDescriptors(ownRelation.containerRelation()) ?: emptySet())

fun ClassDescriptor.effectiveVisibility() = effectiveVisibility(emptySet())

private fun ClassDescriptor.effectiveVisibility(classes: Set<ClassDescriptor>): EffectiveVisibility =
        if (this in classes) Public
        else with(this.containingDeclaration as? ClassDescriptor) {
            lowerBound(visibility.effectiveVisibility(this), this?.effectiveVisibility(classes + this@effectiveVisibility) ?: Public)
        }

// Should collect all dependent classifier descriptors, to get verbose diagnostic
fun KotlinType.dependentDescriptors() = dependentDescriptors(emptySet(), CONSTRUCTOR)

private fun KotlinType.dependentDescriptors(types: Set<KotlinType>, ownRelation: RelationToType): Set<DescriptorWithRelation> {
    if (this in types) return emptySet()
    val ownDependent = constructor.declarationDescriptor?.dependentDescriptors(ownRelation) ?: emptySet()
    val argumentDependent = arguments.map { it.type.dependentDescriptors(types + this, ARGUMENT) }.flatten()
    return ownDependent + argumentDependent
}

fun Set<DescriptorWithRelation>.leastPermissive(base: EffectiveVisibility): DescriptorWithRelation? {
    for (descriptorWithRelation in this) {
        val currentVisibility = descriptorWithRelation.effectiveVisibility()
        when (currentVisibility.relation(base)) {
            Permissiveness.LESS, Permissiveness.UNKNOWN -> {
                return descriptorWithRelation
            }
            else -> {}
        }
    }
    return null
}

fun DeclarationDescriptorWithVisibility.effectiveVisibility(): EffectiveVisibility =
        lowerBound(visibility.effectiveVisibility(this.containingDeclaration as? ClassDescriptor),
                   (this.containingDeclaration as? ClassDescriptor)?.effectiveVisibility() ?: Public)

