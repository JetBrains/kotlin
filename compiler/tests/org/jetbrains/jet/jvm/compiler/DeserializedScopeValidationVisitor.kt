/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.jvm.compiler

import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberScope
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import java.util.Collections
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import com.intellij.testFramework.UsefulTestCase
import java.util.ArrayList
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.jet.test.util.DescriptorValidator
import org.jetbrains.jet.test.util.DescriptorValidator.ValidationVisitor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.resolve.MemberComparator

class DeserializedScopeValidationVisitor : DescriptorValidator.ValidationVisitor() {
    override fun validateScope(scope: JetScope, collector: DescriptorValidator.DiagnosticCollector) {
        super.validateScope(scope, collector)
        validateDeserializedScope(scope)
    }
}

private fun validateDeserializedScope(scope: JetScope) {
    val isPackageViewScope = scope.safeGetContainingDeclaration() is PackageViewDescriptor
    if (scope is DeserializedMemberScope || isPackageViewScope) {
        val relevantDescriptors = scope.getAllDescriptors().filter { member ->
            member is CallableMemberDescriptor && member.getKind().isReal() || (!isPackageViewScope && member is ClassDescriptor)
        }
        checkSorted(relevantDescriptors, scope.getContainingDeclaration())
    }
}

//NOTE: see TypeUtils#IntersectionScope#getContainingDeclaration()
private fun JetScope.safeGetContainingDeclaration(): DeclarationDescriptor? {
    return try {
        getContainingDeclaration()
    }
    catch (e: UnsupportedOperationException) {
        null
    }
}

private fun checkSorted(descriptors: Collection<DeclarationDescriptor>, declaration: DeclarationDescriptor) {
    UsefulTestCase.assertOrderedEquals(
            "Members of $declaration should be sorted by serialization.",
            descriptors,
            descriptors.sortBy(MemberComparator.INSTANCE)
    )
}
