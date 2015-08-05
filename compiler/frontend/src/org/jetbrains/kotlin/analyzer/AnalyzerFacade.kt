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

package org.jetbrains.kotlin.analyzer

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.context.withModule
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.ModuleParameters
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.LazyModuleDependencies
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.load.java.lazy.PackageMappingProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.TargetEnvironment
import java.util.ArrayList
import java.util.HashMap

public class ResolverForModule(
    public val packageFragmentProvider: PackageFragmentProvider,
    public val componentProvider: ComponentProvider
)

public interface ResolverForProject<M : ModuleInfo> {
    public fun resolverForModule(moduleInfo: M): ResolverForModule = resolverForModuleDescriptor(descriptorForModule(moduleInfo))
    public fun descriptorForModule(moduleInfo: M): ModuleDescriptor
    public fun resolverForModuleDescriptor(descriptor: ModuleDescriptor): ResolverForModule

    val allModules: Collection<M>
}

public class EmptyResolverForProject<M : ModuleInfo> : ResolverForProject<M> {
    override fun resolverForModuleDescriptor(descriptor: ModuleDescriptor): ResolverForModule = throw IllegalStateException("$descriptor is not contained in this resolver")
    override fun descriptorForModule(moduleInfo: M) = throw IllegalStateException("Should not be called for $moduleInfo")
    override val allModules: Collection<M> = listOf()
}

public class ResolverForProjectImpl<M : ModuleInfo>(
        val descriptorByModule: Map<M, ModuleDescriptorImpl>,
        val delegateResolver: ResolverForProject<M> = EmptyResolverForProject()
) : ResolverForProject<M> {
    val resolverByModuleDescriptor: MutableMap<ModuleDescriptor, () -> ResolverForModule> = HashMap()

    override val allModules: Collection<M> by lazy {
        (descriptorByModule.keySet() + delegateResolver.allModules).toSet()
    }

    private fun assertCorrectModuleInfo(moduleInfo: M) {
        if (moduleInfo !in allModules) {
            throw AssertionError("Requested data for $moduleInfo not contained in this resolver.\nThis resolver was created for following infos:\n${allModules.joinToString("\n")}")
        }
    }

    override fun resolverForModuleDescriptor(descriptor: ModuleDescriptor): ResolverForModule {
        val computation = resolverByModuleDescriptor[descriptor] ?: return delegateResolver.resolverForModuleDescriptor(descriptor)
        return computation()
    }

    override fun descriptorForModule(moduleInfo: M): ModuleDescriptorImpl {
        assertCorrectModuleInfo(moduleInfo)
        return descriptorByModule[moduleInfo] ?: return delegateResolver.descriptorForModule(moduleInfo) as ModuleDescriptorImpl
    }
}

public data class ModuleContent(
        public val syntheticFiles: Collection<JetFile>,
        public val moduleContentScope: GlobalSearchScope
)

public interface PlatformAnalysisParameters

public interface ModuleInfo {
    public val isLibrary: Boolean
        get() = false
    public val name: Name
    public fun dependencies(): List<ModuleInfo>
    public fun friends(): Collection<ModuleInfo> = listOf()
    public fun dependencyOnBuiltins(): DependencyOnBuiltins = DependenciesOnBuiltins.LAST

    //TODO: (module refactoring) provide dependency on builtins after runtime in IDEA
    public interface DependencyOnBuiltins {
        public fun adjustDependencies(builtinsModule: ModuleDescriptorImpl, dependencies: MutableList<ModuleDescriptorImpl>)
    }

    public enum class DependenciesOnBuiltins : DependencyOnBuiltins {

        NONE {
            override fun adjustDependencies(builtinsModule: ModuleDescriptorImpl, dependencies: MutableList<ModuleDescriptorImpl>) {
                //do nothing
            }
        },
        LAST {
            override fun adjustDependencies(builtinsModule: ModuleDescriptorImpl, dependencies: MutableList<ModuleDescriptorImpl>) {
                dependencies.add(builtinsModule)
            }
        };

        override fun adjustDependencies(builtinsModule: ModuleDescriptorImpl, dependencies: MutableList<ModuleDescriptorImpl>) {
            //TODO: KT-5457
        }

    }
}

public abstract class AnalyzerFacade<in P : PlatformAnalysisParameters> {
    public fun <M : ModuleInfo> setupResolverForProject(
            projectContext: ProjectContext,
            modules: Collection<M>,
            modulesContent: (M) -> ModuleContent,
            platformParameters: P,
            targetEnvironment: TargetEnvironment,
            delegateResolver: ResolverForProject<M> = EmptyResolverForProject(),
            packageMappingProviderFactory: (M, ModuleContent) -> PackageMappingProvider = { (m, c) -> PackageMappingProvider.EMPTY }
    ): ResolverForProject<M> {
        val storageManager = projectContext.storageManager
        fun createResolverForProject(): ResolverForProjectImpl<M> {
            val descriptorByModule = HashMap<M, ModuleDescriptorImpl>()
            modules.forEach {
                module ->
                descriptorByModule[module] = ModuleDescriptorImpl(module.name, storageManager, moduleParameters)
            }
            return ResolverForProjectImpl(descriptorByModule, delegateResolver)
        }

        val resolverForProject = createResolverForProject()

        fun computeDependencyDescriptors(module: M): List<ModuleDescriptorImpl> {
            val dependenciesDescriptors = module.dependencies().mapTo(ArrayList<ModuleDescriptorImpl>()) {
                dependencyInfo ->
                resolverForProject.descriptorForModule(dependencyInfo as M)
            }

            val builtinsModule = KotlinBuiltIns.getInstance().getBuiltInsModule()
            module.dependencyOnBuiltins().adjustDependencies(builtinsModule, dependenciesDescriptors)
            return dependenciesDescriptors
        }

        fun setupModuleDependencies() {
            modules.forEach {
                module ->
                resolverForProject.descriptorForModule(module).setDependencies(
                        LazyModuleDependencies(storageManager) { computeDependencyDescriptors(module) }
                )
            }
        }

        setupModuleDependencies()

        fun addFriends() {
            modules.forEach {
                module ->
                val descriptor = resolverForProject.descriptorForModule(module)
                module.friends().forEach {
                    descriptor.addFriend(resolverForProject.descriptorForModule(it as M))
                }
            }
        }

        addFriends()

        fun initializeResolverForProject() {
            modules.forEach {
                module ->
                val descriptor = resolverForProject.descriptorForModule(module)
                val computeResolverForModule = storageManager.createLazyValue {
                    val content = modulesContent(module)
                    createResolverForModule(
                            module, descriptor, projectContext.withModule(descriptor), modulesContent(module),
                            platformParameters, targetEnvironment, resolverForProject,
                            packageMappingProviderFactory(module, content)
                    )
                }

                descriptor.initialize(DelegatingPackageFragmentProvider { computeResolverForModule().packageFragmentProvider })
                resolverForProject.resolverByModuleDescriptor[descriptor] = computeResolverForModule
            }
        }

        initializeResolverForProject()
        return resolverForProject
    }

    protected abstract fun <M : ModuleInfo> createResolverForModule(
            moduleInfo: M,
            moduleDescriptor: ModuleDescriptorImpl,
            moduleContext: ModuleContext,
            moduleContent: ModuleContent,
            platformParameters: P,
            targetEnvironment: TargetEnvironment,
            resolverForProject: ResolverForProject<M>,
            packageMappingProvider: PackageMappingProvider
    ): ResolverForModule

    public abstract val moduleParameters: ModuleParameters
}

//NOTE: relies on delegate to be lazily computed and cached
private class DelegatingPackageFragmentProvider(
        private val delegate: () -> PackageFragmentProvider
) : PackageFragmentProvider {

    override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
        return delegate().getPackageFragments(fqName)
    }

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
        return delegate().getSubPackagesOf(fqName, nameFilter)
    }
}