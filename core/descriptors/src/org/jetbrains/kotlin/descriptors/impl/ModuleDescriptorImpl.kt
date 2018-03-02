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
import org.jetbrains.kotlin.descriptors.InvalidModuleException
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.MultiTargetPlatform
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.sure
import java.lang.IllegalArgumentException

class ModuleDescriptorImpl @JvmOverloads constructor(
        moduleName: Name,
        private val storageManager: StorageManager,
        override val builtIns: KotlinBuiltIns,
        // May be null in compiler context, should be not-null in IDE context
        multiTargetPlatform: MultiTargetPlatform? = null,
        capabilities: Map<ModuleDescriptor.Capability<*>, Any?> = emptyMap()
) : DeclarationDescriptorImpl(Annotations.EMPTY, moduleName), ModuleDescriptor {
    init {
        if (!moduleName.isSpecial) {
            throw IllegalArgumentException("Module name must be special: $moduleName")
        }
    }

    private val capabilities = capabilities + (multiTargetPlatform?.let { mapOf(MultiTargetPlatform.CAPABILITY to it) } ?: emptyMap())

    private var dependencies: ModuleDependencies? = null
    private var packageFragmentProviderForModuleContent: PackageFragmentProvider? = null

    override var isValid: Boolean = true

    override fun assertValid() {
        if (!isValid) {
            throw InvalidModuleException("Accessing invalid module descriptor $this")
        }
    }

    private val packages = storageManager.createMemoizedFunction<FqName, PackageViewDescriptor> {
        fqName: FqName -> LazyPackageViewDescriptorImpl(this, fqName, storageManager)
    }

    @Deprecated("This method is not going to be supported. Please do not use it")
    val testOnly_AllDependentModules: List<ModuleDescriptorImpl> get() = this.dependencies!!.allDependencies

    override val allDependencyModules: List<ModuleDescriptor>
        get() = this.dependencies.sure { "Dependencies of module $id were not set" }.allDependencies.filter { it != this }

    override val expectedByModule: ModuleDescriptor?
        get() = this.dependencies.sure { "Dependencies of module $id were not set" }.expectedByDependency

    override fun getPackage(fqName: FqName): PackageViewDescriptor {
        assertValid()
        return packages(fqName)
    }

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
        assertValid()
        return packageFragmentProvider.getSubPackagesOf(fqName, nameFilter)
    }

    private val packageFragmentProviderForWholeModuleWithDependencies by lazy {
        val moduleDependencies = dependencies.sure { "Dependencies of module $id were not set before querying module content" }
        val dependenciesDescriptors = moduleDependencies.allDependencies
        assert(this in dependenciesDescriptors) { "Module $id is not contained in his own dependencies, this is probably a misconfiguration" }
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

    private val isInitialized: Boolean
        get() = packageFragmentProviderForModuleContent != null

    fun setDependencies(dependencies: ModuleDependencies) {
        assert(this.dependencies == null) { "Dependencies of $id were already set" }
        this.dependencies = dependencies
    }

    fun setDependencies(vararg descriptors: ModuleDescriptorImpl) {
        setDependencies(descriptors.toList())
    }

    fun setDependencies(descriptors: List<ModuleDescriptorImpl>) {
        setDependencies(descriptors, emptySet())
    }

    fun setDependencies(descriptors: List<ModuleDescriptorImpl>, friends: Set<ModuleDescriptorImpl>) {
        setDependencies(ModuleDependenciesImpl(descriptors, friends, null))
    }

    override fun shouldSeeInternalsOf(targetModule: ModuleDescriptor): Boolean {
        return this == targetModule || targetModule in dependencies!!.modulesWhoseInternalsAreVisible || expectedByModule == targetModule
    }

    private val id: String
        get() = name.toString()

    /*
     * Call initialize() to set module contents. Uninitialized module cannot be queried for its contents.
     */
    fun initialize(providerForModuleContent: PackageFragmentProvider) {
        assert(!isInitialized) { "Attempt to initialize module $id twice" }
        this.packageFragmentProviderForModuleContent = providerForModuleContent
    }

    val packageFragmentProvider: PackageFragmentProvider
        get() {
            assertValid()
            return packageFragmentProviderForWholeModuleWithDependencies
        }

    val packageFragmentProviderForContent: PackageFragmentProvider
        get() {
            assertValid()
            return packageFragmentProviderForModuleContent.sure {
                "Module $id was not initialized by the time packageFragmentProviderForContent was requested"
            }
        }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getCapability(capability: ModuleDescriptor.Capability<T>) = capabilities[capability] as? T
}

interface ModuleDependencies {
    val allDependencies: List<ModuleDescriptorImpl>
    val modulesWhoseInternalsAreVisible: Set<ModuleDescriptorImpl>
    val expectedByDependency: ModuleDescriptorImpl?
}

class ModuleDependenciesImpl(
    override val allDependencies: List<ModuleDescriptorImpl>,
    override val modulesWhoseInternalsAreVisible: Set<ModuleDescriptorImpl>,
    override val expectedByDependency: ModuleDescriptorImpl?
) : ModuleDependencies
