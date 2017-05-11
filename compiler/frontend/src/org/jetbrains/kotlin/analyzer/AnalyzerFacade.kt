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
import org.jetbrains.kotlin.descriptors.impl.LazyModuleDependencies
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.MultiTargetPlatform
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.utils.keysToMap
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
        val descriptorByModule: Map<M, ModuleDescriptorImpl>,
        val delegateResolver: ResolverForProject<M> = EmptyResolverForProject()
) : ResolverForProject<M>() {
    override fun tryGetResolverForModule(moduleInfo: M): ResolverForModule? {
        if (!isCorrectModuleInfo(moduleInfo)) {
            return null
        }
        return resolverForModuleDescriptor(doGetDescriptorForModule(moduleInfo))
    }

    internal val resolverByModuleDescriptor: MutableMap<ModuleDescriptor, () -> ResolverForModule> = HashMap()

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

abstract class AnalyzerFacade<in P : PlatformAnalysisParameters> {
    companion object {
        fun <P : PlatformAnalysisParameters, M : ModuleInfo> setupResolverForProject(
                debugName: String,
                projectContext: ProjectContext,
                modules: Collection<M>,
                analyzerFacade: (M) -> AnalyzerFacade<P>,
                modulesContent: (M) -> ModuleContent,
                platformParameters: P,
                targetEnvironment: TargetEnvironment = CompilerEnvironment,
                builtIns: (M) -> KotlinBuiltIns = { DefaultBuiltIns.Instance },
                delegateResolver: ResolverForProject<M> = EmptyResolverForProject(),
                packagePartProviderFactory: (M, ModuleContent) -> PackagePartProvider = { _, _ -> PackagePartProvider.Empty },
                firstDependency: M? = null,
                modulePlatforms: (M) -> MultiTargetPlatform?
        ): ResolverForProject<M> {
            val storageManager = projectContext.storageManager

            val resolverForProject = ResolverForProjectImpl(debugName, modules.keysToMap { module ->
                ModuleDescriptorImpl(module.name, storageManager, builtIns(module), modulePlatforms(module), module.capabilities)
            }, delegateResolver)

            for (module in modules) {
                val moduleDescriptor = resolverForProject.descriptorForModule(module)
                moduleDescriptor.setDependencies(LazyModuleDependencies(
                        storageManager,
                        computeDependencies = {
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
                        },
                        computeModulesWhoseInternalsAreVisible = {
                            module.modulesWhoseInternalsAreVisible().mapTo(LinkedHashSet()) {
                                resolverForProject.descriptorForModule(it as M)
                            }
                        },
                        computeImplementingModules = {
                            if (modulePlatforms(module) != MultiTargetPlatform.Common) emptySet()
                            else modules
                                    .filter { modulePlatforms(it) != MultiTargetPlatform.Common && module in it.dependencies() }
                                    .mapTo(mutableSetOf(), resolverForProject::descriptorForModule)
                        }
                ))
            }

            for (module in modules) {
                val descriptor = resolverForProject.descriptorForModule(module)
                val computeResolverForModule = storageManager.createLazyValue {
                    ResolverForModuleComputationTracker.getInstance(projectContext.project)?.onResolverComputed(module)

                    val content = modulesContent(module)
                    analyzerFacade(module).createResolverForModule(
                            module, descriptor, projectContext.withModule(descriptor), modulesContent(module),
                            platformParameters, targetEnvironment, resolverForProject,
                            packagePartProviderFactory(module, content)
                    )
                }

                descriptor.initialize(DelegatingPackageFragmentProvider { computeResolverForModule().packageFragmentProvider })
                resolverForProject.resolverByModuleDescriptor[descriptor] = computeResolverForModule
            }

            return resolverForProject
        }
    }

    protected abstract fun <M : ModuleInfo> createResolverForModule(
            moduleInfo: M,
            moduleDescriptor: ModuleDescriptorImpl,
            moduleContext: ModuleContext,
            moduleContent: ModuleContent,
            platformParameters: P,
            targetEnvironment: TargetEnvironment,
            resolverForProject: ResolverForProject<M>,
            packagePartProvider: PackagePartProvider
    ): ResolverForModule

    abstract val targetPlatform: TargetPlatform
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

interface LanguageSettingsProvider {
    fun getLanguageVersionSettings(moduleInfo: ModuleInfo, project: Project): LanguageVersionSettings

    fun getTargetPlatform(moduleInfo: ModuleInfo): TargetPlatformVersion

    object Default : LanguageSettingsProvider {
        override fun getLanguageVersionSettings(moduleInfo: ModuleInfo, project: Project) = LanguageVersionSettingsImpl.DEFAULT

        override fun getTargetPlatform(moduleInfo: ModuleInfo): TargetPlatformVersion = TargetPlatformVersion.NoVersion
    }

    companion object {
        fun getInstance(project: Project) = ServiceManager.getService(project, LanguageSettingsProvider::class.java) ?: Default
    }
}

interface ResolverForModuleComputationTracker {

    fun onResolverComputed(moduleInfo: ModuleInfo)

    companion object {
        fun getInstance(project: Project): ResolverForModuleComputationTracker? = ServiceManager.getService(project, ResolverForModuleComputationTracker::class.java) ?: null
    }
}