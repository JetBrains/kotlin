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

package org.jetbrains.kotlin.descriptors.impl

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.JetScopeImpl
import org.jetbrains.kotlin.utils.*

import java.util.ArrayList
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter

public class SubpackagesScope(private val containingDeclaration: PackageViewDescriptor) : JetScopeImpl() {
    override fun getContainingDeclaration(): DeclarationDescriptor {
        return containingDeclaration
    }

    override fun getPackage(name: Name): PackageViewDescriptor? {
        return if (name.isSpecial()) null else containingDeclaration.getModule().getPackage(containingDeclaration.getFqName().child(name))
    }

    override fun getDescriptors(kindFilter: DescriptorKindFilter,
                                nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        if (!kindFilter.acceptsKinds(DescriptorKindFilter.PACKAGES_MASK)) return listOf()

        val subFqNames = containingDeclaration.getModule().getSubPackagesOf(containingDeclaration.getFqName(), nameFilter)
        val result = ArrayList<DeclarationDescriptor>(subFqNames.size())
        for (subFqName in subFqNames) {
            val shortName = subFqName.shortName()
            if (nameFilter(shortName)) {
                result.addIfNotNull(getPackage(shortName))
            }
        }
        return result
    }

    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.getSimpleName(), " {")
        p.pushIndent()

        p.println("thisDescriptor = ", containingDeclaration)

        p.popIndent()
        p.println("}")
    }
}
