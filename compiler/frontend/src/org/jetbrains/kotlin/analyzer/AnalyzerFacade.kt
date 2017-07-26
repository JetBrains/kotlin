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

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.TargetPlatformVersion
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.context.withModule
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.PackagePartProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDependencies
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.MultiTargetPlatform
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.storage.NotNullLazyValue
import org.jetbrains.kotlin.storage.StorageManager
import java.util.*
import kotlin.coroutines.experimental.buildSequence

class ResolverForModule(
        val packageFragmentProvider: PackageFragmentProvider,
        val componentProvider: ComponentProvider
)

abstract class ResolverForProject<M : ModuleInfo> {
    fun resolverForModule(moduleInfo: M): ResolverForModule = resolverForModuleDescriptor(descriptorForModule(moduleInfo))
    abstract fun tryGetResolverForModule(moduleInfo: M): ResolverForModule?
    abstract fun descriptorForModule(moduleInfo: M): ModuleDescriptor
    abstract fun resolverForModuleDescriptor(descriptor: ModuleDescriptor): ResolverForModule

    abstract val name: String
    abstract val allModules: Collection<M>

    override fun toString() = name
}

class EmptyResolverForProject<M : ModuleInfo> : ResolverForProject<M>() {
    override val name: String
        get() = "Empty resolver"

    override fun tryGetResolverForModule(moduleInfo: M): ResolverForModule? = null
    override fun resolverForModuleDescriptor(descriptor: ModuleDescriptor): ResolverForModule = throw IllegalStateException("$descriptor is not contained in this resolver")
    override fun descriptorForModule(moduleInfo: M) = throw IllegalStateException("Should not be called for $moduleInfo")
    override val allModules: Collection<M> = listOf()
}

class ResolverForProjectImpl<M : ModuleInfo>(
        private val debugName: String,
        private val projectContext: ProjectContext,
        modules: Collection<M>,
        analyzerFacade: (M) -> AnalyzerFacade,
        private val modulesContent: (M) -> ModuleContent,
        platformParameters: PlatformAnalysisParameters,
        targetEnvironment: TargetEnvironment = CompilerEnvironment,
        builtIns: KotlinBuiltIns = DefaultBuiltIns.Instance,
        val delegateResolver: ResolverForProject<M> = EmptyResolverForProject(),
        packagePartProviderFactory: (M, ModuleContent) -> PackagePartProvider = { _, _ -> PackagePartProvider.Empty },
        private val firstDependency: M? = null,
        private val modulePlatforms: (M) -> MultiTargetPlatform?,
        private val packageOracleFactory: PackageOracleFactory = PackageOracleFactory.OptimisticFactory
) : ResolverForProject<M>() {
    val descriptorByModule = mutableMapOf<M, ModuleDescriptorImpl>()

    init {
        for (module in modules) {
            descriptorByModule[module] = ModuleDescriptorImpl(module.name,
                 projectContext.storageManager, builtIns, modulePlatforms(module), module.capabilities)
        }

        for ((module, moduleDescriptor) in descriptorByModule) {
            setupModuleDescriptor(module, moduleDescriptor)
        }
    }

    override fun tryGetResolverForModule(moduleInfo: M): ResolverForModule? {
        if (!isCorrectModuleInfo(moduleInfo)) {
            return null
        }
        return resolverForModuleDescriptor(doGetDescriptorForModule(moduleInfo))
    }

    private fun setupModuleDescriptor(module: M, moduleDescriptor: ModuleDescriptorImpl) {
        moduleDescriptor.setDependencies(LazyModuleDependencies(
                projectContext.storageManager,
                module,
                modulePlatforms,
                firstDependency,
                this))

        val content = modulesContent(module)
        moduleDescriptor.initialize(
                DelegatingPackageFragmentProvider(this, moduleDescriptor, content,
                                                  packageOracleFactory.createOracle(module)))
    }
    internal val resolverByModuleDescriptor: Map<ModuleDescriptor, NotNullLazyValue<ResolverForModule>> = descriptorByModule.map { (module, descriptor) ->
        descriptor to projectContext.storageManager.createLazyValue {
            ResolverForModuleComputationTracker.getInstance(projectContext.project)?.onResolverComputed(module)

            analyzerFacade(module).createResolverForModule(
                    module, descriptor, projectContext.withModule(descriptor), modulesContent(module),
                    platformParameters, targetEnvironment, this@ResolverForProjectImpl,
                    packagePartProviderFactory(module, modulesContent(module))
            )
        }
    }.toMap()

    override val allModules: Collection<M> by lazy {
        (descriptorByModule.keys + delegateResolver.allModules).toSet()
    }

    override val name: String
        get() = "Resolver for '$debugName'"

    private fun isCorrectModuleInfo(moduleInfo: M) = moduleInfo in allModules

    override fun resolverForModuleDescriptor(descriptor: ModuleDescriptor): ResolverForModule {
        val computation = resolverByModuleDescriptor[descriptor] ?: run {
            if (delegateResolver is EmptyResolverForProject<*>) {
                throw IllegalStateException("$descriptor is not contained in resolver $name")
            }
            return delegateResolver.resolverForModuleDescriptor(descriptor)
        }
        return computation()
    }

    override fun descriptorForModule(moduleInfo: M): ModuleDescriptorImpl {
        if (!isCorrectModuleInfo(moduleInfo)) {
            throw AssertionError("$name does not know how to resolve $moduleInfo")
        }
        return doGetDescriptorForModule(moduleInfo)
    }

    private fun doGetDescriptorForModule(moduleInfo: M): ModuleDescriptorImpl {
        return descriptorByModule[moduleInfo] ?: delegateResolver.descriptorForModule(moduleInfo) as ModuleDescriptorImpl
    }
}

data class ModuleContent(
        val syntheticFiles: Collection<KtFile>,
        val moduleContentScope: GlobalSearchScope
)

interface PlatformAnalysisParameters

interface ModuleInfo {
    val isLibrary: Boolean
        get() = false
    val name: Name
    val displayedName: String get() = name.asString()
    fun dependencies(): List<ModuleInfo>
    val platform: TargetPlatform? get() = null
    fun modulesWhoseInternalsAreVisible(): Collection<ModuleInfo> = listOf()
    val capabilities: Map<ModuleDescriptor.Capability<*>, Any?>
        get() = mapOf(Capability to this)

    // For common modules, we add built-ins at the beginning of the dependencies list, after the SDK.
    // This is needed because if a JVM module depends on the common module, we should use JVM built-ins for resolution of both modules.
    // The common module usually depends on kotlin-stdlib-common which may or may not have its own (common, non-JVM) built-ins,
    // but if they are present, they should come after JVM built-ins in the dependencies list, because JVM built-ins contain
    // additional members dependent on the JDK
    fun dependencyOnBuiltIns(): ModuleInfo.DependencyOnBuiltIns =
            if (platform == TargetPlatform.Default)
                ModuleInfo.DependencyOnBuiltIns.AFTER_SDK
            else
                ModuleInfo.DependencyOnBuiltIns.LAST

    //TODO: (module refactoring) provide dependency on builtins after runtime in IDEA
    enum class DependencyOnBuiltIns { NONE, AFTER_SDK, LAST }

    companion object {
        val Capability = ModuleDescriptor.Capability<ModuleInfo>("ModuleInfo")
    }
}

abstract class AnalyzerFacade {
    abstract fun <M : ModuleInfo> createResolverForModule(
            moduleInfo: M,
            moduleDescriptor: ModuleDescriptorImpl,
            moduleContext: ModuleContext,
            moduleContent: ModuleContent,
            platformParameters: PlatformAnalysisParameters,
            targetEnvironment: TargetEnvironment,
            resolverForProject: ResolverForProject<M>,
            languageSettingsProvider: LanguageSettingsProvider,
            packagePartProvider: PackagePartProvider
    ): ResolverForModule

    abstract val targetPlatform: TargetPlatform
}

class LazyModuleDependencies<M: ModuleInfo>(
        storageManager: StorageManager,
        module: M,
        modulePlatforms: (M) -> MultiTargetPlatform?,
        firstDependency: M? = null,
        resolverForProject: ResolverForProjectImpl<M>
) : ModuleDependencies {
    private val dependencies = storageManager.createLazyValue {
        val moduleDescriptor = resolverForProject.descriptorForModule(module)
        buildSequence {
            if (firstDependency != null) {
                yield(resolverForProject.descriptorForModule(firstDependency))
            }
            if (module.dependencyOnBuiltIns() == ModuleInfo.DependencyOnBuiltIns.AFTER_SDK) {
                yield(moduleDescriptor.builtIns.builtInsModule)
            }
            for (dependency in module.dependencies()) {
                yield(resolverForProject.descriptorForModule(dependency as M))
            }
            if (module.dependencyOnBuiltIns() == ModuleInfo.DependencyOnBuiltIns.LAST) {
                yield(moduleDescriptor.builtIns.builtInsModule)
            }
        }.toList()
    }

    private val visibleInternals = storageManager.createLazyValue {
        module.modulesWhoseInternalsAreVisible().mapTo(LinkedHashSet()) {
            resolverForProject.descriptorForModule(it as M)
        }
    }

    private val implementingModules = storageManager.createLazyValue {
        if (modulePlatforms(module) != MultiTargetPlatform.Common) emptySet<ModuleDescriptorImpl>()
        else resolverForProject.descriptorByModule.keys
                .filter { modulePlatforms(it) != MultiTargetPlatform.Common && module in it.dependencies() }
                .mapTo(mutableSetOf(), resolverForProject::descriptorForModule)
    }

    override val allDependencies: List<ModuleDescriptorImpl> get() = dependencies()
    override val modulesWhoseInternalsAreVisible: Set<ModuleDescriptorImpl> get() = visibleInternals()
    override val allImplementingModules: Set<ModuleDescriptorImpl> get() = implementingModules()
}


private class DelegatingPackageFragmentProvider<M : ModuleInfo>(
        private val resolverForProject: ResolverForProjectImpl<M>,
        private val module: ModuleDescriptor,
        moduleContent: ModuleContent,
        private val packageOracle: PackageOracle
) : PackageFragmentProvider {
    private val syntheticFilePackages = moduleContent.syntheticFiles.map { it.packageFqName }.toSet()

    override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
        if (certainlyDoesNotExist(fqName)) return emptyList()

        return resolverForModuleCachedValue().packageFragmentProvider.getPackageFragments(fqName)
    }

    private val resolverForModuleCachedValue
            get() = resolverForProject.resolverByModuleDescriptor[module]!!

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
        if (certainlyDoesNotExist(fqName)) return emptyList()

        return resolverForModuleCachedValue().packageFragmentProvider.getSubPackagesOf(fqName, nameFilter)
    }

    private fun certainlyDoesNotExist(fqName: FqName): Boolean {
        if (resolverForModuleCachedValue.isComputed()) return false // let this request get cached inside delegate

        return !packageOracle.packageExists(fqName) && fqName !in syntheticFilePackages
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
    fun getLanguageVersionSettings(moduleInfo: ModuleInfo, project: Project): LanguageVersionSettings

    fun getTargetPlatform(moduleInfo: ModuleInfo): TargetPlatformVersion

    object Default : LanguageSettingsProvider {
        override fun getLanguageVersionSettings(moduleInfo: ModuleInfo, project: Project) = LanguageVersionSettingsImpl.DEFAULT

        override fun getTargetPlatform(moduleInfo: ModuleInfo): TargetPlatformVersion = TargetPlatformVersion.NoVersion
    }
}

interface ResolverForModuleComputationTracker {

    fun onResolverComputed(moduleInfo: ModuleInfo)

    companion object {
        fun getInstance(project: Project): ResolverForModuleComputationTracker? = ServiceManager.getService(project, ResolverForModuleComputationTracker::class.java) ?: null
    }
}
