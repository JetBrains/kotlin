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
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.sure

class ModuleDescriptorImpl @JvmOverloads constructor(
    moduleName: Name,
    private val storageManager: StorageManager,
    override val builtIns: KotlinBuiltIns,
    // May be null in compiler context, should be not-null in IDE context
    override val platform: TargetPlatform? = null,
    capabilities: Map<ModuleCapability<*>, Any?> = emptyMap(),
    override val stableName: Name? = null,
) : DeclarationDescriptorImpl(Annotations.EMPTY, moduleName), ModuleDescriptor {
    private val capabilities: Map<ModuleCapability<*>, Any?>
    private val packageViewDescriptorFactory: PackageViewDescriptorFactory

    init {
        if (!moduleName.isSpecial) {
            throw IllegalArgumentException("Module name must be special: $moduleName")
        }
        this.capabilities = capabilities
        packageViewDescriptorFactory = getCapability(PackageViewDescriptorFactory.CAPABILITY) ?: PackageViewDescriptorFactory.Default
    }

    private var dependencies: ModuleDependencies? = null
    private var packageFragmentProviderForModuleContent: PackageFragmentProvider? = null

    val packageFragmentProviderForModuleContentWithoutDependencies: PackageFragmentProvider
        get() = packageFragmentProviderForModuleContent
            ?: throw IllegalStateException("Module $id was not initialized by the time it's content without dependencies was queried")

    override var isValid: Boolean = true

    override fun assertValid() {
        if (!isValid) {
            moduleInvalidated()
        }
    }

    private val packages = storageManager.createMemoizedFunction { fqName: FqName ->
        packageViewDescriptorFactory.compute(this, fqName, storageManager)
    }

    @Deprecated("This method is not going to be supported. Please do not use it")
    val testOnly_AllDependentModules: List<ModuleDescriptorImpl>
        get() = this.dependencies!!.allDependencies

    override val allDependencyModules: List<ModuleDescriptor>
        get() = this.dependencies.sure { "Dependencies of module $id were not set" }.allDependencies.filter { it != this }

    override val expectedByModules: List<ModuleDescriptor>
        get() = this.dependencies.sure { "Dependencies of module $id were not set" }.directExpectedByDependencies

    override val allExpectedByModules: Set<ModuleDescriptor>
        get() = this.dependencies.sure { "Dependencies of module $id were not set" }.allExpectedByDependencies

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
        assertValid()
        assert(this in dependenciesDescriptors) { "Module $id is not contained in its own dependencies, this is probably a misconfiguration" }
        dependenciesDescriptors.forEach { dependency ->
            assert(dependency.isInitialized) {
                "Dependency module ${dependency.id} was not initialized by the time contents of dependent module ${this.id} were queried"
            }
        }
        CompositePackageFragmentProvider(
            dependenciesDescriptors.map {
                it.packageFragmentProviderForModuleContent!!
            },
            "CompositeProvider@ModuleDescriptor for $name"
        )
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
        setDependencies(ModuleDependenciesImpl(descriptors, friends, emptyList(), emptySet()))
    }

    override fun shouldSeeInternalsOf(targetModule: ModuleDescriptor): Boolean {
        if (this == targetModule) return true
        if (targetModule in dependencies!!.modulesWhoseInternalsAreVisible) return true
        if (targetModule in expectedByModules) return true
        if (this in targetModule.expectedByModules) return true

        return false
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

    @Suppress("UNCHECKED_CAST")
    override fun <T> getCapability(capability: ModuleCapability<T>) = capabilities[capability] as? T

    override fun toString(): String {
        return buildString {
            append(super.toString())
            if (!isValid) append(" !isValid")
            append(" packageFragmentProvider: ")
            append(packageFragmentProviderForModuleContent?.javaClass?.simpleName)
        }
    }
}

interface ModuleDependencies {
    val allDependencies: List<ModuleDescriptorImpl>
    val modulesWhoseInternalsAreVisible: Set<ModuleDescriptorImpl>
    val directExpectedByDependencies: List<ModuleDescriptorImpl>
    val allExpectedByDependencies: Set<ModuleDescriptorImpl>
}

class ModuleDependenciesImpl(
    override val allDependencies: List<ModuleDescriptorImpl>,
    override val modulesWhoseInternalsAreVisible: Set<ModuleDescriptorImpl>,
    override val directExpectedByDependencies: List<ModuleDescriptorImpl>,
    override val allExpectedByDependencies: Set<ModuleDescriptorImpl>,
) : ModuleDependencies
