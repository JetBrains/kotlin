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

import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.MutablePackageFragmentDescriptor
import org.jetbrains.kotlin.name.FqName
import java.util.HashMap
import org.jetbrains.kotlin.name.Name

public class MutablePackageFragmentProvider(public val module: ModuleDescriptor) : PackageFragmentProvider {

    private val fqNameToPackage = HashMap<FqName, MutablePackageFragmentDescriptor>()
    private val subPackages = MultiMap.create<FqName, FqName>()

    ;{
        fqNameToPackage.put(FqName.ROOT, MutablePackageFragmentDescriptor(module, FqName.ROOT))
    }

    override fun getPackageFragments(fqName: FqName)
            = ContainerUtil.createMaybeSingletonList<PackageFragmentDescriptor>(fqNameToPackage.get(fqName))

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean) = subPackages[fqName]

    public fun getOrCreateFragment(fqName: FqName): MutablePackageFragmentDescriptor {
        if (!fqNameToPackage.containsKey(fqName)) {
            val parent = fqName.parent()
            getOrCreateFragment(parent) // assure that parent exists

            fqNameToPackage.put(fqName, MutablePackageFragmentDescriptor(module, fqName))
            subPackages.putValue(parent, fqName)
        }

        return fqNameToPackage[fqName]
    }

    public fun getAllFragments(): Collection<MutablePackageFragmentDescriptor> = fqNameToPackage.values()
}
