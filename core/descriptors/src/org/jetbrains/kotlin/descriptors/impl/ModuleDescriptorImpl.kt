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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.ModuleParameters
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.sure
import java.util.LinkedHashSet
import kotlin.properties.Delegates

public class ModuleDescriptorImpl(
        moduleName: Name,
        private val storageManager: StorageManager,
        private val moduleParameters: ModuleParameters
) : DeclarationDescriptorImpl(Annotations.EMPTY, moduleName), ModuleDescriptor, ModuleParameters by moduleParameters {
    init {
        if (!moduleName.isSpecial()) {
            throw IllegalArgumentException("Module name must be special: $moduleName")
        }
    }

    private var dependencies: ModuleDependencies? = null
    private var packageFragmentProviderForModuleContent: PackageFragmentProvider? = null

    private val packageFragmentProviderForWholeModuleWithDependencies by Delegates.lazy {
        val moduleDependencies = dependencies.sure { "Dependencies of module $id were not set before querying module content" }
        val dependenciesDescriptors = moduleDependencies.descriptors
        assert(this in dependenciesDescriptors) { "Module ${id} is not contained in his own dependencies, this is probably a misconfiguration" }
        dependenciesDescriptors.forEach {
            dependency ->
            assert(dependency.isInitialized) {
                "Dependency module ${dependency.id} was not initialized by the time contents of dependent module ${this.id} were queried"
            }
        }
        CompositePackageFragmentProvider(dependenciesDescriptors.map {
            it.packageFragmentProviderForModuleContent!!
        })
    }

    public val isInitialized: Boolean
        get() = packageFragmentProviderForModuleContent != null

    public fun setDependencies(dependencies: ModuleDependencies) {
        assert(this.dependencies == null) { "Dependencies of $id were already set" }
        this.dependencies = dependencies
    }

    public fun setDependencies(vararg descriptors: ModuleDescriptorImpl) {
        setDependencies(descriptors.toList())
    }

    public fun setDependencies(descriptors: List<ModuleDescriptorImpl>) {
        setDependencies(ModuleDependenciesImpl(descriptors))
    }

    private val id: String
        get() = getName().toString()

    /*
     * Call initialize() to set module contents. Uninitialized module cannot be queried for its contents.
     */
    public fun initialize(providerForModuleContent: PackageFragmentProvider) {
        assert(!isInitialized) { "Attempt to initialize module $id twice" }
        packageFragmentProviderForModuleContent = providerForModuleContent
    }

    override fun getPackage(fqName: FqName): PackageViewDescriptor? {
        val fragments = packageFragmentProviderForWholeModuleWithDependencies.getPackageFragments(fqName)
        return if (!fragments.isEmpty()) PackageViewDescriptorImpl(this, fqName, fragments) else null
    }

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
        return packageFragmentProviderForWholeModuleWithDependencies.getSubPackagesOf(fqName, nameFilter)
    }

    private val friendModules = LinkedHashSet<ModuleDescriptor>()

    override fun isFriend(other: ModuleDescriptor) = other == this || other in friendModules

    public fun addFriend(friend: ModuleDescriptorImpl): Unit {
        assert(friend != this) { "Attempt to make module $id a friend to itself" }
        friendModules.add(friend)
    }

    override val builtIns: KotlinBuiltIns
        get() = KotlinBuiltIns.getInstance()
}

public class ModuleDependenciesImpl(override val descriptors: List<ModuleDescriptorImpl>) : ModuleDependencies

public interface ModuleDependencies {
    public val descriptors: List<ModuleDescriptorImpl>
}
