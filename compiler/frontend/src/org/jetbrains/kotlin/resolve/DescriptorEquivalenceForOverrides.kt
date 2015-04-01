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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.OverridingUtil.OverrideCompatibilityInfo

object DescriptorEquivalenceForOverrides {

    public fun areEquivalent(a: DeclarationDescriptor?, b: DeclarationDescriptor?): Boolean {
        return when {
            a is ClassDescriptor &&
            b is ClassDescriptor -> areClassesEquivalent(a, b)

            a is TypeParameterDescriptor &&
            b is TypeParameterDescriptor -> areTypeParametersEquivalent(a, b)

            a is CallableMemberDescriptor &&
            b is CallableMemberDescriptor -> areCallableMemberDescriptorsEquivalent(a, b)

            a is PackageFragmentDescriptor &&
            b is PackageFragmentDescriptor -> (a).fqName == (b).fqName

            else -> a == b
        }
    }

    private fun areClassesEquivalent(a: ClassDescriptor, b: ClassDescriptor): Boolean {
        // type constructors are compared by fqName
        return a.getTypeConstructor() == b.getTypeConstructor()
    }

    private fun areTypeParametersEquivalent(
            a: TypeParameterDescriptor,
            b: TypeParameterDescriptor,
            equivalentCallables: (DeclarationDescriptor?, DeclarationDescriptor?) -> Boolean = {x, y -> false}
    ): Boolean {
        if (a == b) return true
        if (a.getContainingDeclaration() == b.getContainingDeclaration()) return false

        if (!ownersEquivalent(a, b, equivalentCallables)) return false

        return a.getIndex() == b.getIndex() // We ignore type parameter names
    }

    private fun areCallableMemberDescriptorsEquivalent(a: CallableMemberDescriptor, b: CallableMemberDescriptor): Boolean {
        if (a == b) return true
        if (a.getName() != b.getName()) return false
        if (a.getContainingDeclaration() == b.getContainingDeclaration()) return false

        // Distinct locals are not equivalent
        if (DescriptorUtils.isLocal(a) || DescriptorUtils.isLocal(b)) return false

        if (!ownersEquivalent(a, b, {x, y -> false})) return false

        val overridingUtil = OverridingUtil.createWithEqualityAxioms @eq {
            c1, c2 ->
            if (c1 == c2) return@eq true

            val d1 = c1.getDeclarationDescriptor()
            val d2 = c2.getDeclarationDescriptor()

            if (d1 !is TypeParameterDescriptor || d2 !is TypeParameterDescriptor) return@eq false

            areTypeParametersEquivalent(d1, d2, {x, y -> x == a && y == b})
        }

        return overridingUtil.isOverridableByIncludingReturnType(a, b).getResult() == OverrideCompatibilityInfo.Result.OVERRIDABLE
                && overridingUtil.isOverridableByIncludingReturnType(b, a).getResult() == OverrideCompatibilityInfo.Result.OVERRIDABLE

    }

    private fun ownersEquivalent(
            a: DeclarationDescriptor,
            b: DeclarationDescriptor,
            equivalentCallables: (DeclarationDescriptor?, DeclarationDescriptor?) -> Boolean
    ): Boolean {
        val aOwner = a.getContainingDeclaration()
        val bOwner = b.getContainingDeclaration()

        // This check is needed when we call areTypeParametersEquivalent() from areCallableMemberDescriptorsEquivalent:
        // if the type parameter owners are, e.g.,  functions, we'll go into infinite recursion here
        if (aOwner is CallableMemberDescriptor || bOwner is CallableMemberDescriptor) {
            return equivalentCallables(aOwner, bOwner)
        }
        else {
            return areEquivalent(aOwner, bOwner)
        }
    }

}
