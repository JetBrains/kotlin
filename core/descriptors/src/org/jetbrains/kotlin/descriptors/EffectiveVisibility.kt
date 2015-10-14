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
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeConstructor

sealed class EffectiveVisibility(val name: String) {

    override fun toString() = name

    //                    Public
    //                /--/   |  \-------------\
    // Protected(Base)       |                 \
    //       |         Protected(Other)        Internal
    // Protected(Derived) |                   /     \
    //             |      |                  /    InternalProtected(Base)
    //       ProtectedBound                 /        \
    //                    \                /       /InternalProtected(Derived)
    //                     \InternalProtectedBound/
    //                              |
    //                           Private

    object Private : EffectiveVisibility("private") {
        override fun relation(other: EffectiveVisibility) =
                if (this == other) Permissiveness.SAME else Permissiveness.LESS
    }

    object Public : EffectiveVisibility("public") {
        override fun relation(other: EffectiveVisibility) =
                if (this == other) Permissiveness.SAME else Permissiveness.MORE
    }

    object Internal : EffectiveVisibility("internal") {
        override fun relation(other: EffectiveVisibility) = when (other) {
            Public -> Permissiveness.LESS
            Private, InternalProtectedBound, is InternalProtected -> Permissiveness.MORE
            Internal -> Permissiveness.SAME
            ProtectedBound, is Protected -> Permissiveness.UNKNOWN
        }

        override fun lowerBound(other: EffectiveVisibility) = when (other) {
            Public -> this
            Private, InternalProtectedBound, Internal, is InternalProtected -> other
            is Protected -> InternalProtected(other.container)
            ProtectedBound -> InternalProtectedBound
        }
    }

    class Protected(val container: ClassDescriptor?) : EffectiveVisibility("protected") {

        override fun equals(other: Any?) = (other is Protected && container == other.container)

        override fun hashCode() = container?.hashCode() ?: 0

        override fun toString() = "${super.toString()}(${container?.name ?: '?'})"

        override fun relation(other: EffectiveVisibility) = when (other) {
            Public -> Permissiveness.LESS
            Private, ProtectedBound, InternalProtectedBound -> Permissiveness.MORE
            is Protected -> containerRelation(container, other.container)
            is InternalProtected -> when (containerRelation(container, other.container)) {
                    Permissiveness.SAME, Permissiveness.LESS -> Permissiveness.LESS
                    Permissiveness.UNKNOWN, Permissiveness.MORE -> Permissiveness.UNKNOWN
            }
            Internal -> Permissiveness.UNKNOWN
        }

        override fun lowerBound(other: EffectiveVisibility) = when (other) {
            Public -> this
            Private, ProtectedBound, InternalProtectedBound -> other
            is Protected -> when (relation(other)) {
                Permissiveness.SAME, Permissiveness.MORE -> this
                Permissiveness.LESS -> other
                Permissiveness.UNKNOWN -> ProtectedBound
            }
            is InternalProtected -> when (relation(other)) {
                Permissiveness.LESS -> other
                else -> InternalProtectedBound
            }
            Internal -> InternalProtected(container)
        }
    }

    // Lower bound for all protected visibilities
    object ProtectedBound : EffectiveVisibility("most protected") {
        override fun relation(other: EffectiveVisibility) = when (other) {
            Public, is Protected -> Permissiveness.LESS
            Private, InternalProtectedBound -> Permissiveness.MORE
            ProtectedBound -> Permissiveness.SAME
            Internal, is InternalProtected -> Permissiveness.UNKNOWN
        }

        override fun lowerBound(other: EffectiveVisibility) = when (other) {
            Public, is Protected -> this
            Private, ProtectedBound, InternalProtectedBound -> other
            Internal, is InternalProtected -> InternalProtectedBound
        }
    }

    // Lower bound for internal and protected(C)
    class InternalProtected(val container: ClassDescriptor?): EffectiveVisibility("protected & internal") {

        override fun equals(other: Any?) = (other is InternalProtected && container == other.container)

        override fun hashCode() = container?.hashCode() ?: 0

        override fun toString() = "${super.toString()}(${container?.name ?: '?'})"

        override fun relation(other: EffectiveVisibility) = when (other) {
            Public, Internal -> Permissiveness.LESS
            Private, InternalProtectedBound -> Permissiveness.MORE
            is InternalProtected -> containerRelation(container, other.container)
            is Protected -> when (containerRelation(container, other.container)) {
                Permissiveness.SAME, Permissiveness.MORE -> Permissiveness.MORE
                Permissiveness.UNKNOWN, Permissiveness.LESS -> Permissiveness.UNKNOWN
            }
            ProtectedBound -> Permissiveness.UNKNOWN
        }

        override fun lowerBound(other: EffectiveVisibility) = when (other) {
            Public, Internal -> this
            Private, InternalProtectedBound -> other
            is Protected, is InternalProtected -> when (relation(other)) {
                Permissiveness.SAME, Permissiveness.MORE -> this
                Permissiveness.LESS -> other
                Permissiveness.UNKNOWN -> InternalProtectedBound
            }
            ProtectedBound -> InternalProtectedBound
        }
    }

    // Lower bound for internal and protected lower bound
    object InternalProtectedBound : EffectiveVisibility("most protected & internal") {
        override fun relation(other: EffectiveVisibility) = when (other) {
            Public, is Protected, is InternalProtected, ProtectedBound, Internal -> Permissiveness.LESS
            Private -> Permissiveness.MORE
            InternalProtectedBound -> Permissiveness.SAME
        }
    }

    protected enum class Permissiveness {
        LESS,
        SAME,
        MORE,
        UNKNOWN
    }

    abstract protected fun relation(other: EffectiveVisibility): Permissiveness

    fun sameOrMorePermissive(other: EffectiveVisibility) = when (relation(other)) {
        Permissiveness.SAME, Permissiveness.MORE -> true
        Permissiveness.LESS, Permissiveness.UNKNOWN -> false
    }

    open protected fun lowerBound(other: EffectiveVisibility) = when (relation(other)) {
        Permissiveness.SAME, Permissiveness.LESS -> this
        Permissiveness.MORE -> other
        Permissiveness.UNKNOWN -> Private
    }

    companion object {

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

        private fun Visibility.forVisibility(descriptor: ClassDescriptor? = null): EffectiveVisibility = when (this) {
            Visibilities.PRIVATE, Visibilities.PRIVATE_TO_THIS -> Private
            Visibilities.PROTECTED -> Protected(descriptor)
            Visibilities.INTERNAL -> Internal
            Visibilities.PUBLIC -> Public
            // Considered effectively public
            Visibilities.LOCAL -> Public
            else -> this.effectiveVisibility(descriptor)
        }

        fun effectiveVisibility(visibility: Visibility, descriptor: ClassDescriptor?) = visibility.forVisibility(descriptor)

        private fun ClassifierDescriptor.forClassifier(): EffectiveVisibility =
                lowerBound(if (this is ClassDescriptor) this.forClass() else Public,
                           (this.containingDeclaration as? ClassifierDescriptor)?.forClassifier() ?: Public)

        fun ClassDescriptor.forClass() = forClass(emptySet())

        private fun ClassDescriptor.forClass(classes: Set<ClassDescriptor>): EffectiveVisibility =
                if (this in classes) Public
                else with(this.containingDeclaration as? ClassDescriptor) {
                    lowerBound(visibility.forVisibility(this), this?.forClass(classes + this@forClass) ?: Public)
                }

        fun JetType.forType() = forType(emptySet())

        private fun JetType.forType(types: Set<JetType>): EffectiveVisibility =
                if (this in types) Public
                else lowerBound(constructor.forTypeConstructor(),
                                arguments.map { it.type.forType(types + this) } )

        private fun TypeConstructor.forTypeConstructor() =
                this.declarationDescriptor?.forClassifier() ?: Public

        fun MemberDescriptor.forMember() =
                lowerBound(visibility.forVisibility(this.containingDeclaration as? ClassDescriptor),
                           (this.containingDeclaration as? ClassDescriptor)?.forClass() ?: Public)
    }
}