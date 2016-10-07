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
import org.jetbrains.kotlin.name.isChildOf
import org.jetbrains.kotlin.name.isSubpackageOf
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.utils.addToStdlib.check
import org.jetbrains.kotlin.utils.sure
import java.lang.IllegalArgumentException

class ModuleDescriptorImpl @JvmOverloads constructor(
        moduleName: Name,
        private val storageManager: StorageManager,
        private val moduleParameters: ModuleParameters,
        override val builtIns: KotlinBuiltIns,
        private val capabilities: Map<ModuleDescriptor.Capability<*>, Any?> = emptyMap()
) : DeclarationDescriptorImpl(Annotations.EMPTY, moduleName), ModuleDescriptor, ModuleParameters by moduleParameters {
    init {
        if (!moduleName.isSpecial) {
            throw IllegalArgumentException("Module name must be special: $moduleName")
        }
    }

    private var dependencies: ModuleDependencies? = null
    private var packageFragmentProviderForModuleContent: PackageFragmentProvider? = null

    private val packages = storageManager.createMemoizedFunction<FqName, PackageViewDescriptor> {
        fqName: FqName -> LazyPackageViewDescriptorImpl(this, fqName, storageManager)
    }

    override val effectivelyExcludedImports: List<FqName> by storageManager.createLazyValue {
        val packagesWithAliases = listOf(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME, KotlinBuiltIns.TEXT_PACKAGE_FQ_NAME)
        val dependencies = this.dependencies.sure { "Dependencies of module $id were not set" }
        val builtinTypeAliases = dependencies.allDependencies.filter { it != this }.flatMap {
            packagesWithAliases.map(it::getPackage).flatMap {
                it.memberScope.getContributedDescriptors(DescriptorKindFilter.TYPE_ALIASES).filterIsInstance<TypeAliasDescriptor>()
            }
        }

        val nonKotlinDefaultImportedPackages =
                defaultImports
                    .filter { it.isAllUnder }
                    .mapNotNull {
                        it.fqnPart().check { !it.isSubpackageOf(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME) }
                    }
        val nonKotlinAliasedTypeFqNames =
                builtinTypeAliases
                    .mapNotNull { it.expandedType.constructor.declarationDescriptor?.fqNameSafe }
                    .filter { nonKotlinDefaultImportedPackages.any(it::isChildOf) }

        nonKotlinAliasedTypeFqNames
    }

    override fun getPackage(fqName: FqName): PackageViewDescriptor = packages(fqName)

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
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
        setDependencies(ModuleDependenciesImpl(descriptors, emptySet()))
    }

    override fun shouldSeeInternalsOf(targetModule: ModuleDescriptor): Boolean {
        return this == targetModule || targetModule in dependencies!!.modulesWhoseInternalsAreVisible
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
        get() = packageFragmentProviderForWholeModuleWithDependencies

    @Suppress("UNCHECKED_CAST")
    override fun <T> getCapability(capability: ModuleDescriptor.Capability<T>) = capabilities[capability] as? T
}

interface ModuleDependencies {
    val allDependencies: List<ModuleDescriptorImpl>
    val modulesWhoseInternalsAreVisible: Set<ModuleDescriptorImpl>
}

class ModuleDependenciesImpl(
        override val allDependencies: List<ModuleDescriptorImpl>,
        override val modulesWhoseInternalsAreVisible: Set<ModuleDescriptorImpl>
) : ModuleDependencies

class LazyModuleDependencies(
        storageManager: StorageManager,
        computeDependencies: () -> List<ModuleDescriptorImpl>,
        computeModulesWhoseInternalsAreVisible: () -> Set<ModuleDescriptorImpl>
) : ModuleDependencies {
    private val dependencies = storageManager.createLazyValue(computeDependencies)
    private val visibleInternals = storageManager.createLazyValue(computeModulesWhoseInternalsAreVisible)

    override val allDependencies: List<ModuleDescriptorImpl> get() = dependencies()
    override val modulesWhoseInternalsAreVisible: Set<ModuleDescriptorImpl> get() = visibleInternals()
}
