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

package org.jetbrains.kotlin.jvm.compiler

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.resolve.MemberComparator
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberScope
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.test.util.DescriptorValidator
import org.jetbrains.kotlin.test.util.DescriptorValidator.ValidationVisitor

class DeserializedScopeValidationVisitor : ValidationVisitor() {
    override fun validateScope(scopeOwner: DeclarationDescriptor, scope: MemberScope, collector: DescriptorValidator.DiagnosticCollector) {
        super.validateScope(scopeOwner, scope, collector)
        validateDeserializedScope(scopeOwner, scope)
    }
}

private fun validateDeserializedScope(scopeOwner: DeclarationDescriptor, scope: MemberScope) {
    val isPackageViewScope = scopeOwner is PackageViewDescriptor
    if (scope is DeserializedMemberScope || isPackageViewScope) {
        val relevantDescriptors = scope.getContributedDescriptors().filter { member ->
            member is CallableMemberDescriptor && member.kind.isReal || (!isPackageViewScope && member is ClassDescriptor)
        }
        checkSorted(relevantDescriptors, scopeOwner)
    }
}

private fun checkSorted(descriptors: Collection<DeclarationDescriptor>, declaration: DeclarationDescriptor) {
    val serializedOnly = descriptors.filterNot { it is JavaCallableMemberDescriptor }
    KtUsefulTestCase.assertOrderedEquals(
            "Members of $declaration should be sorted by serialization.",
            serializedOnly,
            serializedOnly.sortedWith(MemberComparator.INSTANCE)
    )
}
