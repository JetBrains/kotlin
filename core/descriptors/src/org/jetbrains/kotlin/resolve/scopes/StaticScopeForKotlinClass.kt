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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorFactory.createEnumValueOfMethod
import org.jetbrains.kotlin.resolve.DescriptorFactory.createEnumValuesMethod
import org.jetbrains.kotlin.utils.Printer
import java.util.ArrayList

public class StaticScopeForKotlinClass(
        private val containingClass: ClassDescriptor
) : JetScopeImpl() {
    override fun getClassifier(name: Name, location: LookupLocation) = null // TODO

    private val functions: List<FunctionDescriptor> by lazy {
        if (containingClass.getKind() != ClassKind.ENUM_CLASS) {
            listOf<FunctionDescriptor>()
        }
        else {
            listOf(createEnumValueOfMethod(containingClass), createEnumValuesMethod(containingClass))
        }
    }

    override fun getDescriptors(kindFilter: DescriptorKindFilter,
                                nameFilter: (Name) -> Boolean) = functions

    override fun getOwnDeclaredDescriptors() = functions

    override fun getFunctions(name: Name, location: LookupLocation) = functions.filterTo(ArrayList<FunctionDescriptor>(2)) { it.getName() == name }

    override fun getContainingDeclaration() = containingClass

    override fun printScopeStructure(p: Printer) {
        p.println("Static scope for $containingClass")
    }
}
