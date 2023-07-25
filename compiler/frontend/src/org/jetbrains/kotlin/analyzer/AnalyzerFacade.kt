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

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.ModuleCapability
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDependencies
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.TargetPlatformVersion
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.SealedClassInheritorsProvider
import org.jetbrains.kotlin.resolve.lazy.AbsentDescriptorHandler
import org.jetbrains.kotlin.resolve.scopes.optimization.OptimizingOptions
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue

class ResolverForModule(
    val packageFragmentProvider: PackageFragmentProvider,
    val componentProvider: ComponentProvider
)

abstract class ResolverForProject<M : ModuleInfo> {
    fun resolverForModule(moduleInfo: M): ResolverForModule = resolverForModuleDescriptor(descriptorForModule(moduleInfo))
    abstract fun tryGetResolverForModule(moduleInfo: M): ResolverForModule?
    abstract fun descriptorForModule(moduleInfo: M): ModuleDescriptor
    abstract fun moduleInfoForModuleDescriptor(moduleDescriptor: ModuleDescriptor): M
    abstract fun resolverForModuleDescriptor(descriptor: ModuleDescriptor): ResolverForModule
    abstract fun diagnoseUnknownModuleInfo(infos: List<ModuleInfo>): Nothing

    abstract val name: String
    abstract val allModules: Collection<M>

    override fun toString() = name

    companion object {
        const val resolverForSdkName = "sdk"
        const val resolverForLibrariesName = "project libraries"
        const val resolverForModulesName = "project source roots and libraries"
        const val resolverForScriptDependenciesName = "dependencies of scripts"

        const val resolverForSpecialInfoName = "completion/highlighting in "
    }
}

class EmptyResolverForProject<M : ModuleInfo> : ResolverForProject<M>() {
    override val name: String
        get() = "Empty resolver"

    override fun tryGetResolverForModule(moduleInfo: M): ResolverForModule? = null
    override fun resolverForModuleDescriptor(descriptor: ModuleDescriptor): ResolverForModule =
        throw IllegalStateException("$descriptor is not contained in this resolver")

    override fun descriptorForModule(moduleInfo: M) = diagnoseUnknownModuleInfo(listOf(moduleInfo))
    override val allModules: Collection<M> = listOf()
    override fun diagnoseUnknownModuleInfo(infos: List<ModuleInfo>) = throw IllegalStateException("Should not be called for $infos")

    override fun moduleInfoForModuleDescriptor(moduleDescriptor: ModuleDescriptor): M {
        throw IllegalStateException("$moduleDescriptor is not contained in this resolver")
    }
}

data class ModuleContent<out M : ModuleInfo>(
    val moduleInfo: M,
    val syntheticFiles: Collection<KtFile>,
    val moduleContentScope: GlobalSearchScope
)

interface PlatformAnalysisParameters {
    object Empty : PlatformAnalysisParameters
}

interface CombinedModuleInfo : ModuleInfo {
    val containedModules: List<ModuleInfo>
    val platformModule: ModuleInfo
}

/**
 * Special-purpose module info that allows implementors to provide different behavior compared to the [originalModule]'s.
 * E.g. may be used to resolve common code as if it were target-specific, or to change the dependencies visible to the code.
 *
 * Resolvers should accept a derived module info, iff the [originalModule] is accepted.
 */
interface DerivedModuleInfo : ModuleInfo {
    val originalModule: ModuleInfo
}

fun ModuleInfo.flatten(): List<ModuleInfo> = when (this) {
    is CombinedModuleInfo -> listOf(this) + containedModules
    else -> listOf(this)
}

fun ModuleInfo.unwrapPlatform(): ModuleInfo = if (this is CombinedModuleInfo) platformModule else this

interface LibraryModuleSourceInfoBase : ModuleInfo
interface NonSourceModuleInfoBase : ModuleInfo

abstract class ResolverForModuleFactory {
    open fun <M : ModuleInfo> createResolverForModule(
        moduleDescriptor: ModuleDescriptorImpl,
        moduleContext: ModuleContext,
        moduleContent: ModuleContent<M>,
        resolverForProject: ResolverForProject<M>,
        languageVersionSettings: LanguageVersionSettings,
        sealedInheritorsProvider: SealedClassInheritorsProvider,
        resolveOptimizingOptions: OptimizingOptions?,
        absentDescriptorHandlerClass: Class<out AbsentDescriptorHandler>?
    ): ResolverForModule {
        @Suppress("DEPRECATION")
        return createResolverForModule(
            moduleDescriptor,
            moduleContext,
            moduleContent,
            resolverForProject,
            languageVersionSettings,
            sealedInheritorsProvider,
            resolveOptimizingOptions
        )
    }

    @Deprecated(
        "Left only for compatibility, please use full version",
        ReplaceWith("createResolverForModule(moduleDescriptor, moduleContext, moduleContent, resolverForProject, languageVersionSettings, sealedInheritorsProvider, null, null)")
    )
    open fun <M : ModuleInfo> createResolverForModule(
        moduleDescriptor: ModuleDescriptorImpl,
        moduleContext: ModuleContext,
        moduleContent: ModuleContent<M>,
        resolverForProject: ResolverForProject<M>,
        languageVersionSettings: LanguageVersionSettings,
        sealedInheritorsProvider: SealedClassInheritorsProvider,
        resolveOptimizingOptions: OptimizingOptions?,
    ): ResolverForModule {
        @Suppress("DEPRECATION")
        return createResolverForModule(
            moduleDescriptor,
            moduleContext,
            moduleContent,
            resolverForProject,
            languageVersionSettings,
            sealedInheritorsProvider
        )
    }

    @Deprecated(
        "Left only for compatibility, please use full version",
        ReplaceWith("createResolverForModule(moduleDescriptor, moduleContext, moduleContent, resolverForProject, languageVersionSettings, sealedInheritorsProvider, null, null)")
    )
    open fun <M : ModuleInfo> createResolverForModule(
        moduleDescriptor: ModuleDescriptorImpl,
        moduleContext: ModuleContext,
        moduleContent: ModuleContent<M>,
        resolverForProject: ResolverForProject<M>,
        languageVersionSettings: LanguageVersionSettings,
        sealedInheritorsProvider: SealedClassInheritorsProvider
    ): ResolverForModule {
        return createResolverForModule(
            moduleDescriptor,
            moduleContext,
            moduleContent,
            resolverForProject,
            languageVersionSettings,
            sealedInheritorsProvider,
            resolveOptimizingOptions = null,
            absentDescriptorHandlerClass = null
        )
    }
}

class LazyModuleDependencies<M : ModuleInfo>(
    storageManager: StorageManager,
    private val module: M,
    firstDependency: M?,
    private val resolverForProject: AbstractResolverForProject<M>
) : ModuleDependencies {

    private val dependencies = storageManager.createLazyValue {
        val moduleDescriptors = mutableSetOf<ModuleDescriptorImpl>()
        firstDependency?.let {
            module.assertModuleDependencyIsCorrect(it)
            moduleDescriptors.add(resolverForProject.descriptorForModule(it))
        }
        val moduleDescriptor = resolverForProject.descriptorForModule(module)
        val dependencyOnBuiltIns = module.dependencyOnBuiltIns()
        if (dependencyOnBuiltIns == ModuleInfo.DependencyOnBuiltIns.AFTER_SDK) {
            val builtInsModule = moduleDescriptor.builtIns.builtInsModule
            module.assertModuleDependencyIsCorrect(builtInsModule)
            moduleDescriptors.add(builtInsModule)
        }
        for (dependency in module.dependencies()) {
            if (dependency == firstDependency) continue
            module.assertModuleDependencyIsCorrect(dependency)

            @Suppress("UNCHECKED_CAST")
            moduleDescriptors.add(resolverForProject.descriptorForModule(dependency as M))
        }
        if (dependencyOnBuiltIns == ModuleInfo.DependencyOnBuiltIns.LAST) {
            val builtInsModule = moduleDescriptor.builtIns.builtInsModule
            module.assertModuleDependencyIsCorrect(builtInsModule)
            moduleDescriptors.add(builtInsModule)
        }
        moduleDescriptors.toList()
    }

    override val allDependencies: List<ModuleDescriptorImpl> get() = dependencies()

    override val directExpectedByDependencies by storageManager.createLazyValue {
        module.expectedBy.map {
            module.assertModuleDependencyIsCorrect(it)
            @Suppress("UNCHECKED_CAST")
            resolverForProject.descriptorForModule(it as M)
        }
    }

    override val allExpectedByDependencies: Set<ModuleDescriptorImpl> by storageManager.createLazyValue {
        collectAllExpectedByModules(module).mapTo(HashSet<ModuleDescriptorImpl>()) {
            module.assertModuleDependencyIsCorrect(it)
            @Suppress("UNCHECKED_CAST")
            resolverForProject.descriptorForModule(it as M)
        }
    }

    override val modulesWhoseInternalsAreVisible: Set<ModuleDescriptorImpl>
        get() =
            module.modulesWhoseInternalsAreVisible().mapTo(LinkedHashSet()) {
                module.assertModuleDependencyIsCorrect(it)
                @Suppress("UNCHECKED_CAST")
                resolverForProject.descriptorForModule(it as M)
            }

    companion object {
        private fun ModuleInfo.assertModuleDependencyIsCorrect(dependency: ModuleDescriptor) {
            assertModuleDependencyIsCorrect(dependency.getCapability(ModuleInfo.Capability) ?: return)
        }

        private fun ModuleInfo.assertModuleDependencyIsCorrect(dependency: ModuleInfo) {
            assert(dependency !is DerivedModuleInfo || this is DerivedModuleInfo) {
                "Derived module infos may not be referenced from regular ones"
            }
        }
    }
}

interface PackageOracle {
    fun packageExists(fqName: FqName): Boolean

    object Optimistic : PackageOracle {
        override fun packageExists(fqName: FqName): Boolean = true
    }
}

interface PackageOracleFactory {
    fun createOracle(moduleInfo: ModuleInfo): PackageOracle

    object OptimisticFactory : PackageOracleFactory {
        override fun createOracle(moduleInfo: ModuleInfo) = PackageOracle.Optimistic
    }
}

interface LanguageSettingsProvider {
    fun getLanguageVersionSettings(
        moduleInfo: ModuleInfo,
        project: Project
    ): LanguageVersionSettings

    fun getTargetPlatform(moduleInfo: ModuleInfo, project: Project): TargetPlatformVersion

    object Default : LanguageSettingsProvider {
        override fun getLanguageVersionSettings(
            moduleInfo: ModuleInfo,
            project: Project
        ) = LanguageVersionSettingsImpl.DEFAULT

        override fun getTargetPlatform(moduleInfo: ModuleInfo, project: Project): TargetPlatformVersion = TargetPlatformVersion.NoVersion
    }
}

interface ResolverForModuleComputationTracker {

    fun onResolverComputed(moduleInfo: ModuleInfo)

    companion object {
        fun getInstance(project: Project): ResolverForModuleComputationTracker? =
            project.getComponent(ResolverForModuleComputationTracker::class.java) ?: null
    }
}


@Suppress("UNCHECKED_CAST")
fun <T> ModuleInfo.getCapability(capability: ModuleCapability<T>) = capabilities[capability] as? T

