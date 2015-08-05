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

public class ExplicitImportsScope(private val descriptors: Collection<DeclarationDescriptor>) : JetScopeImpl() {
    override fun getClassifier(name: Name, location: LookupLocation) = descriptors.filter { it.getName() == name }.firstIsInstanceOrNull<ClassifierDescriptor>()

    override fun getPackage(name: Name)= descriptors.filter { it.getName() == name }.firstIsInstanceOrNull<PackageViewDescriptor>()

    override fun getProperties(name: Name, location: LookupLocation) = descriptors.filter { it.getName() == name }.filterIsInstance<VariableDescriptor>()

    override fun getFunctions(name: Name, location: LookupLocation) = descriptors.filter { it.getName() == name }.filterIsInstance<FunctionDescriptor>()

    override fun getContainingDeclaration() = throw UnsupportedOperationException()

    override fun getDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean) = descriptors

    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.getName())
    }
}
