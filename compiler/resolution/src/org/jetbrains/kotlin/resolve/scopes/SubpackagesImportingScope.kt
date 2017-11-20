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

package org.jetbrains.kotlin.resolve.scopes

import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.descriptors.impl.SubpackagesScope
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.Printer

class SubpackagesImportingScope(
        override val parent: ImportingScope?,
        moduleDescriptor: ModuleDescriptor,
        fqName: FqName
) : SubpackagesScope(moduleDescriptor, fqName), ImportingScope by ImportingScope.Empty {

    override fun getContributedPackage(name: Name): PackageViewDescriptor? = getPackage(name)

    override fun printStructure(p: Printer) = printScopeStructure(p)

    override fun getContributedVariables(name: Name, location: LookupLocation) = super.getContributedVariables(name, location)
    override fun getContributedFunctions(name: Name, location: LookupLocation) = super.getContributedFunctions(name, location)

    //TODO: kept old behavior, but it seems very strange (super call seems more applicable)
    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? = null

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> =
            emptyList()

    //TODO: kept old behavior, but it seems very strange (super call seems more applicable)
    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean, changeNamesForAliased: Boolean): Collection<DeclarationDescriptor>
            = emptyList()
    override fun computeImportedNames() = emptySet<Name>()
}
