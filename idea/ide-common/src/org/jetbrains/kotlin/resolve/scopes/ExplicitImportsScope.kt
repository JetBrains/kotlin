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

package org.jetbrains.kotlin.resolve.scopes

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class ExplicitImportsScope(private val descriptors: Collection<DeclarationDescriptor>) : BaseImportingScope(null) {
    override fun getContributedClassifier(name: Name, location: LookupLocation)
            = descriptors.filter { it.name == name }.firstIsInstanceOrNull<ClassifierDescriptor>()

    override fun getContributedPackage(name: Name)
            = descriptors.filter { it.name == name }.firstIsInstanceOrNull<PackageViewDescriptor>()

    override fun getContributedVariables(name: Name, location: LookupLocation)
            = descriptors.filter { it.name == name }.filterIsInstance<VariableDescriptor>()

    override fun getContributedFunctions(name: Name, location: LookupLocation)
            = descriptors.filter { it.name == name }.filterIsInstance<FunctionDescriptor>()

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean, changeNamesForAliased: Boolean)
            = descriptors

    override fun printStructure(p: Printer) {
        p.println(this::class.java.name)
    }
}
