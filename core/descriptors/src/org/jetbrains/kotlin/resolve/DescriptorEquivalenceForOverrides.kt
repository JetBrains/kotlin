/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.OverridingUtil.OverrideCompatibilityInfo
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner

object DescriptorEquivalenceForOverrides {

    fun areEquivalent(
        a: DeclarationDescriptor?,
        b: DeclarationDescriptor?,
        allowCopiesFromTheSameDeclaration: Boolean,
        distinguishExpectsAndNonExpects: Boolean = true
    ): Boolean {
        return when {
            a is ClassDescriptor && b is ClassDescriptor -> areClassesEquivalent(a, b)

            a is TypeParameterDescriptor && b is TypeParameterDescriptor -> areTypeParametersEquivalent(
                a,
                b,
                allowCopiesFromTheSameDeclaration
            )

            a is CallableDescriptor && b is CallableDescriptor -> areCallableDescriptorsEquivalent(
                a,
                b,
                allowCopiesFromTheSameDeclaration = allowCopiesFromTheSameDeclaration,
                distinguishExpectsAndNonExpects = distinguishExpectsAndNonExpects,
                kotlinTypeRefiner = KotlinTypeRefiner.Default
            )

            a is PackageFragmentDescriptor && b is PackageFragmentDescriptor -> (a).fqName == (b).fqName

            else -> a == b
        }
    }

    private fun areClassesEquivalent(a: ClassDescriptor, b: ClassDescriptor): Boolean {
        // type constructors are compared by fqName
        return a.typeConstructor == b.typeConstructor
    }

    @JvmOverloads
    fun areTypeParametersEquivalent(
        a: TypeParameterDescriptor,
        b: TypeParameterDescriptor,
        allowCopiesFromTheSameDeclaration: Boolean,
        equivalentCallables: (DeclarationDescriptor?, DeclarationDescriptor?) -> Boolean = { _, _ -> false }
    ): Boolean {
        if (a == b) return true
        if (a.containingDeclaration == b.containingDeclaration) return false

        if (!ownersEquivalent(a, b, equivalentCallables, allowCopiesFromTheSameDeclaration)) return false

        return a.index == b.index // We ignore type parameter names
    }

    @Suppress("NO_TAIL_CALLS_FOUND", "NON_TAIL_RECURSIVE_CALL") // K2 warning suppression, TODO: KT-62472
    private tailrec fun CallableDescriptor.singleSource(): SourceElement? {
        if (this !is CallableMemberDescriptor || kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE) return source

        return overriddenDescriptors.singleOrNull()?.singleSource()
    }

    fun areCallableDescriptorsEquivalent(
        a: CallableDescriptor,
        b: CallableDescriptor,
        allowCopiesFromTheSameDeclaration: Boolean,
        distinguishExpectsAndNonExpects: Boolean = true,
        ignoreReturnType: Boolean = false,
        kotlinTypeRefiner: KotlinTypeRefiner
    ): Boolean {
        if (a == b) return true
        if (a.name != b.name) return false
        if (distinguishExpectsAndNonExpects && a is MemberDescriptor && b is MemberDescriptor && a.isExpect != b.isExpect) return false
        if (a.containingDeclaration == b.containingDeclaration) {
            if (!allowCopiesFromTheSameDeclaration) return false
            if (a.singleSource() != b.singleSource()) return false
        }

        // Distinct locals are not equivalent
        if (DescriptorUtils.isLocal(a) || DescriptorUtils.isLocal(b)) return false

        if (!ownersEquivalent(a, b, { _, _ -> false }, allowCopiesFromTheSameDeclaration)) return false

        val overridingUtil = OverridingUtil.create(kotlinTypeRefiner) eq@{ c1, c2 ->
            if (c1 == c2) return@eq true

            val d1 = c1.declarationDescriptor
            val d2 = c2.declarationDescriptor

            if (d1 !is TypeParameterDescriptor || d2 !is TypeParameterDescriptor) return@eq false

            areTypeParametersEquivalent(d1, d2, allowCopiesFromTheSameDeclaration) { x, y -> x == a && y == b }
        }

        return overridingUtil.isOverridableBy(a, b, null, !ignoreReturnType).result == OverrideCompatibilityInfo.Result.OVERRIDABLE
                && overridingUtil.isOverridableBy(b, a, null, !ignoreReturnType).result == OverrideCompatibilityInfo.Result.OVERRIDABLE

    }

    private fun ownersEquivalent(
        a: DeclarationDescriptor,
        b: DeclarationDescriptor,
        equivalentCallables: (DeclarationDescriptor?, DeclarationDescriptor?) -> Boolean,
        allowCopiesFromTheSameDeclaration: Boolean
    ): Boolean {
        val aOwner = a.containingDeclaration
        val bOwner = b.containingDeclaration

        // This check is needed when we call areTypeParametersEquivalent() from areCallableMemberDescriptorsEquivalent:
        // if the type parameter owners are, e.g.,  functions, we'll go into infinite recursion here
        return if (aOwner is CallableMemberDescriptor || bOwner is CallableMemberDescriptor) {
            equivalentCallables(aOwner, bOwner)
        } else {
            areEquivalent(aOwner, bOwner, allowCopiesFromTheSameDeclaration)
        }
    }

}
