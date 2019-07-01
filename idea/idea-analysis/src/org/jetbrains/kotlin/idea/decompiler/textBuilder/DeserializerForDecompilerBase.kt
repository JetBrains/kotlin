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

package org.jetbrains.kotlin.idea.decompiler.textBuilder

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.MutablePackageFragmentDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.DeserializationComponents
import org.jetbrains.kotlin.serialization.deserialization.LocalClassifierTypeSettings
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.SimpleType

abstract class DeserializerForDecompilerBase(val directoryPackageFqName: FqName) : ResolverForDecompiler {
    protected abstract val deserializationComponents: DeserializationComponents

    protected abstract val builtIns: KotlinBuiltIns

    protected val storageManager: StorageManager = LockBasedStorageManager.NO_LOCKS

    protected val moduleDescriptor: ModuleDescriptorImpl = createDummyModule("module for building decompiled sources")

    protected val packageFragmentProvider: PackageFragmentProvider = object : PackageFragmentProvider {
        override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
            return listOf(createDummyPackageFragment(fqName))
        }

        override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
            throw UnsupportedOperationException("This method is not supposed to be called.")
        }
    }

    override fun resolveTopLevelClass(classId: ClassId) = deserializationComponents.deserializeClass(classId)

    protected fun createDummyPackageFragment(fqName: FqName): MutablePackageFragmentDescriptor =
            MutablePackageFragmentDescriptor(moduleDescriptor, fqName)

    private fun createDummyModule(name: String) = ModuleDescriptorImpl(Name.special("<$name>"), storageManager, builtIns)

    init {
        moduleDescriptor.initialize(packageFragmentProvider)
        moduleDescriptor.setDependencies(moduleDescriptor, moduleDescriptor.builtIns.builtInsModule)
    }
}

class ResolveEverythingToKotlinAnyLocalClassifierResolver(private val builtIns: KotlinBuiltIns) : LocalClassifierTypeSettings {
    override val replacementTypeForLocalClassifiers: SimpleType?
        get() = builtIns.anyType
}
