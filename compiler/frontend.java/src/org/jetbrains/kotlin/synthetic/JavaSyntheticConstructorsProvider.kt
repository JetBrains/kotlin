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

package org.jetbrains.kotlin.synthetic

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.load.java.descriptors.SamConstructorDescriptorImpl
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaClassDescriptor
import org.jetbrains.kotlin.load.java.sam.SingleAbstractMethodUtils
import org.jetbrains.kotlin.resolve.scopes.SyntheticConstructorsProvider
import java.lang.AssertionError

object JavaSyntheticConstructorsProvider : SyntheticConstructorsProvider {
    override fun getSyntheticConstructors(classifier: ClassifierDescriptor, location: LookupLocation): Collection<FunctionDescriptor> {
        if (classifier is TypeAliasDescriptor) {
            return getSyntheticTypeAliasConstructors(classifier, location)
        }

        return emptyList()
    }

    private fun getSyntheticTypeAliasConstructors(typeAliasDescriptor: TypeAliasDescriptor, location: LookupLocation): Collection<FunctionDescriptor> {
        val classDescriptor = typeAliasDescriptor.classDescriptor
        if (classDescriptor !is LazyJavaClassDescriptor || classDescriptor.functionTypeForSamInterface == null) return emptyList()

        val containingDeclaration = classDescriptor.containingDeclaration

        val outerScope = when (containingDeclaration) {
            is ClassDescriptor ->
                containingDeclaration.staticScope
            is PackageFragmentDescriptor ->
                containingDeclaration.getMemberScope()
            else ->
                throw AssertionError("Unexpected containing declaration for $classDescriptor: $containingDeclaration")
        }

        return outerScope.getContributedFunctions(classDescriptor.name, location)
                .filterIsInstance<SamConstructorDescriptorImpl>()
                .filter { it.baseDescriptorForSynthetic == classDescriptor }
                .map { SingleAbstractMethodUtils.createTypeAliasSamConstructorFunction(typeAliasDescriptor, it) }
    }
}