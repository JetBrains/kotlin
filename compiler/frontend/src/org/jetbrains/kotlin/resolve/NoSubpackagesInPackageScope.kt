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

import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.AbstractScopeAdapter
import org.jetbrains.kotlin.resolve.scopes.JetScope

class NoSubpackagesInPackageScope(packageDescriptor: PackageViewDescriptor) : AbstractScopeAdapter() {
    override val workerScope: JetScope = packageDescriptor.getMemberScope()

    override fun getPackage(name: Name): PackageViewDescriptor? = null

    override fun getDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        val modifiedFilter = kindFilter.withoutKinds(DescriptorKindFilter.PACKAGES_MASK)
        if (modifiedFilter.kindMask == 0) return listOf()
        return workerScope.getDescriptors(modifiedFilter, nameFilter).filter { it !is PackageViewDescriptor }
    }

    override fun getOwnDeclaredDescriptors(): Collection<DeclarationDescriptor> {
        return workerScope.getOwnDeclaredDescriptors().filter { it !is PackageViewDescriptor }
    }
}
