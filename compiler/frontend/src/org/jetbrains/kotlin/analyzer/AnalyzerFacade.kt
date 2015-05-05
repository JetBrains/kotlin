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

import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import java.util.HashMap
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.platform.PlatformToKotlinClassMap
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.context.GlobalContext
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import java.util.ArrayList
import org.jetbrains.kotlin.psi.JetFile
import com.intellij.psi.search.GlobalSearchScope
import kotlin.properties.Delegates

public trait ResolverForModule {
    public val lazyResolveSession: ResolveSession
}

public trait ResolverForProject<M : ModuleInfo,out R : ResolverForModule> {
    public fun resolverForModule(moduleInfo: M): R
    public fun descriptorForModule(moduleInfo: M): ModuleDescriptor
    val allModules: Collection<M>
}

public class EmptyResolverForProject<M : ModuleInfo, R : ResolverForModule> : ResolverForProject<M, R> {
    override fun resolverForModule(moduleInfo: M): R = throw IllegalStateException("Should not be called for $moduleInfo")
    override fun descriptorForModule(moduleInfo: M) = throw IllegalStateException("Should not be called for $moduleInfo")
    override val allModules: Collection<M> = listOf()
}

public class ResolverForProjectImpl<M : ModuleInfo, R : ResolverForModule>(
        val descriptorByModule: Map<M, ModuleDescriptorImpl>,
        val delegateResolver: ResolverForProject<M, R> = EmptyResolverForProject()
) : ResolverForProject<M, R> {
    val resolverByModuleDescriptor: MutableMap<ModuleDescriptor, R> = HashMap()

    override val allModules: Collection<M> by Delegates.lazy {
        (descriptorByModule.keySet() + delegateResolver.allModules).toSet()
    }

    private fun assertCorrectModuleInfo(moduleInfo: M) {
        if (moduleInfo !in allModules) {
            throw AssertionError("Requested data for $moduleInfo not contained in this resolver.\nThis resolver was created for following infos:\n${allModules.joinToString("\n")}")
        }
    }

    override fun resolverForModule(moduleInfo: M): R {
        assertCorrectModuleInfo(moduleInfo)
        val descriptor = descriptorByModule[moduleInfo] ?: return delegateResolver.resolverForModule(moduleInfo)
        return resolverByModuleDescriptor[descriptor]!!
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

public trait PlatformAnalysisParameters

public trait ModuleInfo {
    public val isLibrary: Boolean
        get() = false
    public val name: Name
    public fun dependencies(): List<ModuleInfo>
    public fun friends(): Collection<ModuleInfo> = listOf()
    public fun dependencyOnBuiltins(): DependencyOnBuiltins = DependenciesOnBuiltins.LAST

    //TODO: (module refactoring) provide dependency on builtins after runtime in IDEA
    public trait DependencyOnBuiltins {
        public fun adjustDependencies(builtinsModule: ModuleDescriptorImpl, dependencies: MutableList<ModuleDescriptorImpl>)
    }

    public enum class DependenciesOnBuiltins : DependencyOnBuiltins {

        NONE {
            override fun adjustDependencies(builtinsModule: ModuleDescriptorImpl, dependencies: MutableList<ModuleDescriptorImpl>) {
                //do nothing
            }
        }
        LAST {
            override fun adjustDependencies(builtinsModule: ModuleDescriptorImpl, dependencies: MutableList<ModuleDescriptorImpl>) {
                dependencies.add(builtinsModule)
            }
        }

        override fun adjustDependencies(builtinsModule: ModuleDescriptorImpl, dependencies: MutableList<ModuleDescriptorImpl>) {
            //TODO: KT-5457
        }

    }
}

//TODO: (module refactoring) extract project context
public trait AnalyzerFacade<A : ResolverForModule, in P : PlatformAnalysisParameters> {
    public fun <M : ModuleInfo> setupResolverForProject(
            globalContext: GlobalContext,
            project: Project,
            modules: Collection<M>,
            modulesContent: (M) -> ModuleContent,
            platformParameters: P,
            delegateResolver: ResolverForProject<M, A> = EmptyResolverForProject()
    ): ResolverForProject<M, A> {

        fun createResolverForProject(): ResolverForProjectImpl<M, A> {
            val descriptorByModule = HashMap<M, ModuleDescriptorImpl>()
            modules.forEach {
                module ->
                descriptorByModule[module] = ModuleDescriptorImpl(module.name, defaultImports, platformToKotlinClassMap)
            }
            return ResolverForProjectImpl(descriptorByModule, delegateResolver)
        }

        val resolverForProject = createResolverForProject()

        fun setupModuleDependencies() {
            modules.forEach {
                module ->
                val currentModule = resolverForProject.descriptorForModule(module)
                val dependenciesDescriptors = module.dependencies().mapTo(ArrayList<ModuleDescriptorImpl>()) {
                    dependencyInfo ->
                    resolverForProject.descriptorForModule(dependencyInfo as M)
                }

                val builtinsModule = KotlinBuiltIns.getInstance().getBuiltInsModule()
                module.dependencyOnBuiltins().adjustDependencies(builtinsModule, dependenciesDescriptors)
                dependenciesDescriptors.forEach { currentModule.addDependencyOnModule(it) }
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

        resolverForProject.descriptorByModule.values().forEach { it.seal() }

        fun initializeResolverForProject() {
            modules.forEach {
                module ->
                val descriptor = resolverForProject.descriptorForModule(module)
                val resolverForModule = createResolverForModule(
                        module, project, globalContext, descriptor, modulesContent(module), platformParameters, resolverForProject
                )
                assert(descriptor.isInitialized, "ModuleDescriptorImpl#initialize() should be called in createResolverForModule")
                resolverForProject.resolverByModuleDescriptor[descriptor] = resolverForModule
            }
        }

        initializeResolverForProject()
        return resolverForProject
    }

    protected fun <M : ModuleInfo> createResolverForModule(
            moduleInfo: M,
            project: Project,
            globalContext: GlobalContext,
            moduleDescriptor: ModuleDescriptorImpl,
            moduleContent: ModuleContent,
            platformParameters: P,
            resolverForProject: ResolverForProject<M, A>
    ): A

    public val defaultImports: List<ImportPath>
    public val platformToKotlinClassMap: PlatformToKotlinClassMap
}

