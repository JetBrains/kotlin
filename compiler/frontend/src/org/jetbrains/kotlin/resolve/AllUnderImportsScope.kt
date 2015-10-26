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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.BaseImportingScope
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.utils.Printer

class AllUnderImportsScope(descriptor: DeclarationDescriptor) : BaseImportingScope(null) {
    private val scopes = if (descriptor is ClassDescriptor) {
        listOf(descriptor.staticScope, descriptor.unsubstitutedInnerClassesScope)
    }
    else {
        assert(descriptor is PackageViewDescriptor) {
            "Must be class or package view descriptor: $descriptor"
        }
        listOf(NoSubpackagesInPackageScope(descriptor as PackageViewDescriptor))
    }

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean)
            = scopes.flatMap { it.getDescriptors(kindFilter, nameFilter) }

    override fun getContributedClassifier(name: Name, location: LookupLocation)
            = scopes.asSequence().map { it.getClassifier(name, location) }.filterNotNull().singleOrNull()

    override fun getContributedVariables(name: Name, location: LookupLocation)
            = scopes.flatMap { it.getProperties(name, location) }

    override fun getContributedFunctions(name: Name, location: LookupLocation)
            = scopes.flatMap { it.getFunctions(name, location) }

    override fun printStructure(p: Printer) {
        p.println(javaClass.simpleName)
    }
}

