/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.descriptors.impl

import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.PlatformToKotlinClassMap
import org.jetbrains.jet.lang.descriptors.PackageFragmentProvider
import java.util.ArrayList
import org.jetbrains.jet.lang.resolve.ImportPath
import org.jetbrains.jet.lang.descriptors.annotations.Annotations
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import kotlin.properties.Delegates

public class ModuleDescriptorImpl(
        moduleName: Name,
        override val defaultImports: List<ImportPath>,
        override val platformToKotlinClassMap: PlatformToKotlinClassMap
) : DeclarationDescriptorImpl(Annotations.EMPTY, moduleName), ModuleDescriptor {
    {
        if (!moduleName.isSpecial()) {
            throw IllegalArgumentException("Module name must be special: $moduleName")
        }
    }
    private var isSealed = false

    /*
     * Sealed module cannot have its dependencies modified. Seal the module after you're done configuring it.
     * Module will be sealed automatically as soon as you query its contents.
     */
    public fun seal() {
        if (isSealed) return

        assert(this in dependencies, "Module $id is not contained in his own dependencies, this is probably a misconfiguration")
        isSealed = true
    }

    private val dependencies: MutableList<ModuleDescriptorImpl> = ArrayList()
    private var packageFragmentProviderForModuleContent: PackageFragmentProvider? = null

    private val packageFragmentProviderForWholeModuleWithDependencies by Delegates.lazy {
        seal()
        dependencies.forEach {
            dependency ->
            assert(dependency.isInitialized, "Dependency module ${dependency.id} was not initialized by the time contents of dependent module ${this.id} were queried")
        }
        CompositePackageFragmentProvider(dependencies.map {
            it.packageFragmentProviderForModuleContent!!
        })
    }

    private val isInitialized: Boolean
        get() = packageFragmentProviderForModuleContent != null

    public fun addDependencyOnModule(dependency: ModuleDescriptorImpl) {
        assert(!isSealed, "Can't modify dependencies of sealed module $id")
        assert(dependency !in dependencies, "Trying to add dependency on module ${dependency.id} a second time for module ${this.id}, this is probably a misconfiguration")
        dependencies.add(dependency)
    }

    private val id: String
        get() = getName().toString()

    /*
     * Call initialize() to set module contents. Uninitialized module cannot be queried for its contents.
     * Initialize() and seal() can be called in any order.
     */
    public fun initialize(providerForModuleContent: PackageFragmentProvider) {
        assert(!isInitialized, "Attempt to initialize module $id twice")
        packageFragmentProviderForModuleContent = providerForModuleContent
    }

    override fun getPackageFragmentProvider() = packageFragmentProviderForWholeModuleWithDependencies
}
