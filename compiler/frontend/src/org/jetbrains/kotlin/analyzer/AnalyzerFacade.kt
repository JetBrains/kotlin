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
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.context.withModule
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDependencies
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue
import java.util.*

class ResolverForModule(
    val packageFragmentProvider: PackageFragmentProvider,
    val componentProvider: ComponentProvider
)

abstract class ResolverForProject<M : ModuleInfo> {
    fun resolverForModule(moduleInfo: M): ResolverForModule = resolverForModuleDescriptor(descriptorForModule(moduleInfo))
    abstract fun tryGetResolverForModule(moduleInfo: M): ResolverForModule?
    abstract fun descriptorForModule(moduleInfo: M): ModuleDescriptor
    abstract fun resolverForModuleDescriptor(descriptor: ModuleDescriptor): ResolverForModule
    abstract fun diagnoseUnknownModuleInfo(infos: List<ModuleInfo>): Nothing

    abstract val name: String
    abstract val allModules: Collection<M>
    abstract val builtIns: KotlinBuiltIns

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
    override val builtIns get() = DefaultBuiltIns.Instance
}

class ResolverForProjectImpl<M : ModuleInfo>(
    private val debugName: String,
    private val projectContext: ProjectContext,
    modules: Collection<M>,
    private val modulesContent: (M) -> ModuleContent<M>,
    private val moduleLanguageSettingsProvider: LanguageSettingsProvider,
    private val resolverForModuleFactoryByPlatform: (TargetPlatform?) -> ResolverForModuleFactory,
    private val platformParameters: (TargetPlatform) -> PlatformAnalysisParameters,
    private val targetEnvironment: TargetEnvironment = CompilerEnvironment,
    override val builtIns: KotlinBuiltIns = DefaultBuiltIns.Instance,
    private val delegateResolver: ResolverForProject<M> = EmptyResolverForProject(),
    private val firstDependency: M? = null,
    private val packageOracleFactory: PackageOracleFactory = PackageOracleFactory.OptimisticFactory,
    private val invalidateOnOOCB: Boolean = true,
    private val isReleaseCoroutines: Boolean? = null
) : ResolverForProject<M>() {

    private class ModuleData(
        val moduleDescriptor: ModuleDescriptorImpl,
        val modificationTracker: ModificationTracker?,
        val modificationCount: Long?
    ) {
        fun isOutOfDate(): Boolean {
            val currentModCount = modificationTracker?.modificationCount
            return currentModCount != null && currentModCount > modificationCount!!
        }
    }

    // Protected by ("projectContext.storageManager.lock")
    private val descriptorByModule = mutableMapOf<M, ModuleData>()

    // Protected by ("projectContext.storageManager.lock")
    private val moduleInfoByDescriptor = mutableMapOf<ModuleDescriptorImpl, M>()

    private val moduleInfoToResolvableInfo: Map<M, M> =
        modules.flatMap { module -> module.flatten().map { modulePart -> modulePart to module } }.toMap() as Map<M, M>

    init {
        assert(moduleInfoToResolvableInfo.values.toSet() == modules.toSet())
    }

    override fun tryGetResolverForModule(moduleInfo: M): ResolverForModule? {
        if (!isCorrectModuleInfo(moduleInfo)) {
            return null
        }
        return resolverForModuleDescriptor(doGetDescriptorForModule(moduleInfo))
    }

    private fun setupModuleDescriptor(module: M, moduleDescriptor: ModuleDescriptorImpl) {
        moduleDescriptor.setDependencies(
            LazyModuleDependencies(
                projectContext.storageManager,
                module,
                firstDependency,
                this
            )
        )

        val content = modulesContent(module)
        moduleDescriptor.initialize(
            DelegatingPackageFragmentProvider(
                this, moduleDescriptor, content,
                packageOracleFactory.createOracle(module)
            )
        )
    }

    // Protected by ("projectContext.storageManager.lock")
    private val resolverByModuleDescriptor = mutableMapOf<ModuleDescriptor, ResolverForModule>()

    override val allModules: Collection<M> by lazy {
        this.moduleInfoToResolvableInfo.keys + delegateResolver.allModules
    }

    override val name: String
        get() = "Resolver for '$debugName'"

    private fun isCorrectModuleInfo(moduleInfo: M) = moduleInfo in allModules

    override fun resolverForModuleDescriptor(descriptor: ModuleDescriptor): ResolverForModule {
        return projectContext.storageManager.compute {
            val module = moduleInfoByDescriptor[descriptor]
            if (module == null) {
                if (delegateResolver is EmptyResolverForProject<*>) {
                    throw IllegalStateException("$descriptor is not contained in resolver $name")
                }
                return@compute delegateResolver.resolverForModuleDescriptor(descriptor)
            }
            resolverByModuleDescriptor.getOrPut(descriptor) {
                checkModuleIsCorrect(module)

                ResolverForModuleComputationTracker.getInstance(projectContext.project)?.onResolverComputed(module)

                val moduleContent = modulesContent(module)

                val languageVersionSettings =
                    moduleLanguageSettingsProvider.getLanguageVersionSettings(module, projectContext.project, isReleaseCoroutines)
                val targetPlatformVersion = moduleLanguageSettingsProvider.getTargetPlatform(module, projectContext.project)

                // FIXME(dsavvinov): temporary hack; ideally, module.platform should already return JvmPlatform with proper target.
                // (maybe it does already, have to check out)
                val platform = module.platform?.let {
                    if (it.isJvm() && targetPlatformVersion is JvmTarget)
                        DefaultBuiltInPlatforms.jvmPlatformByTargetVersion(targetPlatformVersion)
                    else
                        it
                }

                val resolverForModuleFactory = resolverForModuleFactoryByPlatform(platform)
                resolverForModuleFactory.createResolverForModule(
                    descriptor as ModuleDescriptorImpl,
                    projectContext.withModule(descriptor),
                    moduleContent,
                    platformParameters(module.platform ?: TODO("Missing platform!")),
                    targetEnvironment,
                    this@ResolverForProjectImpl,
                    languageVersionSettings
                )
            }
        }
    }

    internal fun isResolverForModuleDescriptorComputed(descriptor: ModuleDescriptor) =
        projectContext.storageManager.compute {
            descriptor in resolverByModuleDescriptor
        }

    override fun descriptorForModule(moduleInfo: M): ModuleDescriptorImpl {
        checkModuleIsCorrect(moduleInfo)
        return doGetDescriptorForModule(moduleInfo)
    }

    override fun diagnoseUnknownModuleInfo(infos: List<ModuleInfo>): Nothing {
        DiagnoseUnknownModuleInfoReporter.report(name, infos)
    }

    private fun checkModuleIsCorrect(moduleInfo: M) {
        if (!isCorrectModuleInfo(moduleInfo)) {
            diagnoseUnknownModuleInfo(listOf(moduleInfo))
        }
    }

    private fun doGetDescriptorForModule(module: M): ModuleDescriptorImpl {
        val moduleFromThisResolver = moduleInfoToResolvableInfo[module]
                ?: return delegateResolver.descriptorForModule(module) as ModuleDescriptorImpl

        return projectContext.storageManager.compute {
            var moduleData = descriptorByModule.getOrPut(moduleFromThisResolver) {
                createModuleDescriptor(moduleFromThisResolver)
            }
            if (moduleData.isOutOfDate()) {
                moduleData = recreateModuleDescriptor(moduleFromThisResolver)
            }
            moduleData.moduleDescriptor
        }
    }

    private fun recreateModuleDescriptor(module: M): ModuleData {
        val oldDescriptor = descriptorByModule[module]?.moduleDescriptor
        if (oldDescriptor != null) {
            oldDescriptor.isValid = false
            moduleInfoByDescriptor.remove(oldDescriptor)
            resolverByModuleDescriptor.remove(oldDescriptor)
        }

        val moduleData = createModuleDescriptor(module)
        descriptorByModule[module] = moduleData
        return moduleData
    }

    private fun createModuleDescriptor(module: M): ModuleData {
        val moduleDescriptor = ModuleDescriptorImpl(
            module.name,
            projectContext.storageManager,
            builtIns,
            module.platform,
            module.capabilities,
            module.stableName
        )
        moduleInfoByDescriptor[moduleDescriptor] = module
        setupModuleDescriptor(module, moduleDescriptor)
        val modificationTracker = (module as? TrackableModuleInfo)?.createModificationTracker()
                ?: (PsiModificationTracker.SERVICE.getInstance(projectContext.project).outOfCodeBlockModificationTracker.takeIf { invalidateOnOOCB })
        return ModuleData(moduleDescriptor, modificationTracker, modificationTracker?.modificationCount)
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
}

fun ModuleInfo.flatten(): List<ModuleInfo> = when (this) {
    is CombinedModuleInfo -> listOf(this) + containedModules
    else -> listOf(this)
}

interface TrackableModuleInfo : ModuleInfo {
    fun createModificationTracker(): ModificationTracker
}

interface LibraryModuleInfo : ModuleInfo {
    override val platform: TargetPlatform

    fun getLibraryRoots(): Collection<String>
}

abstract class ResolverForModuleFactory {
    abstract fun <M : ModuleInfo> createResolverForModule(
        moduleDescriptor: ModuleDescriptorImpl,
        moduleContext: ModuleContext,
        moduleContent: ModuleContent<M>,
        platformParameters: PlatformAnalysisParameters,
        targetEnvironment: TargetEnvironment,
        resolverForProject: ResolverForProject<M>,
        languageVersionSettings: LanguageVersionSettings
    ): ResolverForModule
}

class LazyModuleDependencies<M : ModuleInfo>(
    storageManager: StorageManager,
    private val module: M,
    firstDependency: M? = null,
    private val resolverForProject: ResolverForProjectImpl<M>
) : ModuleDependencies {
    private val dependencies = storageManager.createLazyValue {
        val moduleDescriptor = resolverForProject.descriptorForModule(module)
        sequence {
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

    override val allDependencies: List<ModuleDescriptorImpl> get() = dependencies()

    override val expectedByDependencies by storageManager.createLazyValue {
        module.expectedBy.map { resolverForProject.descriptorForModule(it as M) }
    }

    override val modulesWhoseInternalsAreVisible: Set<ModuleDescriptorImpl>
        get() =
            module.modulesWhoseInternalsAreVisible().mapTo(LinkedHashSet()) {
                resolverForProject.descriptorForModule(it as M)
            }

}


private class DelegatingPackageFragmentProvider<M : ModuleInfo>(
    private val resolverForProject: ResolverForProjectImpl<M>,
    private val module: ModuleDescriptor,
    moduleContent: ModuleContent<M>,
    private val packageOracle: PackageOracle
) : PackageFragmentProvider {
    private val syntheticFilePackages = moduleContent.syntheticFiles.map { it.packageFqName }.toSet()

    override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
        if (certainlyDoesNotExist(fqName)) return emptyList()

        return resolverForProject.resolverForModuleDescriptor(module).packageFragmentProvider.getPackageFragments(fqName)
    }

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
        if (certainlyDoesNotExist(fqName)) return emptyList()

        return resolverForProject.resolverForModuleDescriptor(module).packageFragmentProvider.getSubPackagesOf(fqName, nameFilter)
    }

    private fun certainlyDoesNotExist(fqName: FqName): Boolean {
        if (resolverForProject.isResolverForModuleDescriptorComputed(module)) return false // let this request get cached inside delegate

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
    fun getLanguageVersionSettings(
        moduleInfo: ModuleInfo,
        project: Project,
        isReleaseCoroutines: Boolean? = null
    ): LanguageVersionSettings

    fun getTargetPlatform(moduleInfo: ModuleInfo, project: Project): TargetPlatformVersion

    object Default : LanguageSettingsProvider {
        override fun getLanguageVersionSettings(
            moduleInfo: ModuleInfo,
            project: Project,
            isReleaseCoroutines: Boolean?
        ) = LanguageVersionSettingsImpl.DEFAULT

        override fun getTargetPlatform(moduleInfo: ModuleInfo, project: Project): TargetPlatformVersion = TargetPlatformVersion.NoVersion
    }
}

interface ResolverForModuleComputationTracker {

    fun onResolverComputed(moduleInfo: ModuleInfo)

    companion object {
        fun getInstance(project: Project): ResolverForModuleComputationTracker? =
            ServiceManager.getService(project, ResolverForModuleComputationTracker::class.java) ?: null
    }
}

private object DiagnoseUnknownModuleInfoReporter {
    fun report(name: String, infos: List<ModuleInfo>): Nothing {
        val message = "$name does not know how to resolve $infos"
        when {
            name.contains(ResolverForProject.resolverForSdkName) -> errorInSdkResolver(message)
            name.contains(ResolverForProject.resolverForLibrariesName) -> errorInLibrariesResolver(message)
            name.contains(ResolverForProject.resolverForModulesName) -> {
                when {
                    infos.isEmpty() -> errorInModulesResolverWithEmptyInfos(message)
                    infos.size == 1 -> {
                        val infoAsString = infos.single().toString()
                        when {
                            infoAsString.contains("ScriptDependencies") -> errorInModulesResolverWithScriptDependencies(message)
                            infoAsString.contains("Library") -> errorInModulesResolverWithLibraryInfo(message)
                            else -> errorInModulesResolver(message)
                        }
                    }
                    else -> throw errorInModulesResolver(message)
                }
            }
            name.contains(ResolverForProject.resolverForScriptDependenciesName) -> errorInScriptDependenciesInfoResolver(message)
            name.contains(ResolverForProject.resolverForSpecialInfoName) -> {
                when {
                    name.contains("ScriptModuleInfo") -> errorInScriptModuleInfoResolver(message)
                    else -> errorInSpecialModuleInfoResolver(message)
                }
            }
            else -> otherError(message)
        }
    }

    // Do not inline 'error*'-methods, they are needed to avoid Exception Analyzer merging those AssertionErrors

    private fun errorInSdkResolver(message: String): Nothing = throw AssertionError(message)
    private fun errorInLibrariesResolver(message: String): Nothing = throw AssertionError(message)
    private fun errorInModulesResolver(message: String): Nothing = throw AssertionError(message)

    private fun errorInModulesResolverWithEmptyInfos(message: String): Nothing = throw AssertionError(message)
    private fun errorInModulesResolverWithScriptDependencies(message: String): Nothing = throw AssertionError(message)
    private fun errorInModulesResolverWithLibraryInfo(message: String): Nothing = throw AssertionError(message)

    private fun errorInScriptDependenciesInfoResolver(message: String): Nothing = throw AssertionError(message)
    private fun errorInScriptModuleInfoResolver(message: String): Nothing = throw AssertionError(message)
    private fun errorInSpecialModuleInfoResolver(message: String): Nothing = throw AssertionError(message)

    private fun otherError(message: String): Nothing = throw AssertionError(message)
}

@Suppress("UNCHECKED_CAST")
fun <T> ModuleInfo.getCapability(capability: ModuleDescriptor.Capability<T>) = capabilities[capability] as? T
