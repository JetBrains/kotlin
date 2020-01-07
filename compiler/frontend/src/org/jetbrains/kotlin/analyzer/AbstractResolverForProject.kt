/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analyzer

import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

abstract class AbstractResolverForProject<M : ModuleInfo>(
    private val debugName: String,
    protected val projectContext: ProjectContext,
    modules: Collection<M>,
    private val fallbackModificationTracker: ModificationTracker? = null,
    private val delegateResolver: ResolverForProject<M> = EmptyResolverForProject(),
    private val packageOracleFactory: PackageOracleFactory = PackageOracleFactory.OptimisticFactory
) : ResolverForProject<M>() {

    private class ModuleData(
        val moduleDescriptor: ModuleDescriptorImpl,
        val modificationTracker: ModificationTracker?
    ) {
        val modificationCount: Long = modificationTracker?.modificationCount ?: Long.MIN_VALUE

        fun isOutOfDate(): Boolean {
            val currentModCount = modificationTracker?.modificationCount
            return currentModCount != null && currentModCount > modificationCount
        }
    }

    // Protected by ("projectContext.storageManager.lock")
    private val descriptorByModule = mutableMapOf<M, ModuleData>()

    // Protected by ("projectContext.storageManager.lock")
    private val moduleInfoByDescriptor = mutableMapOf<ModuleDescriptorImpl, M>()

    @Suppress("UNCHECKED_CAST")
    private val moduleInfoToResolvableInfo: Map<M, M> =
        modules.flatMap { module -> module.flatten().map { modulePart -> modulePart to module } }.toMap() as Map<M, M>

    init {
        assert(moduleInfoToResolvableInfo.values.toSet() == modules.toSet())
    }

    abstract fun sdkDependency(module: M): M?
    abstract fun modulesContent(module: M): ModuleContent<M>
    abstract fun builtInsForModule(module: M): KotlinBuiltIns
    abstract fun createResolverForModule(descriptor: ModuleDescriptor, moduleInfo: M): ResolverForModule

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
                sdkDependency(module),
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

                createResolverForModule(descriptor, module)
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
        DiagnoseUnknownModuleInfoReporter.report(name, infos, allModules)
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
            builtInsForModule(module),
            module.platform,
            module.capabilities,
            module.stableName
        )
        moduleInfoByDescriptor[moduleDescriptor] = module
        setupModuleDescriptor(module, moduleDescriptor)
        val modificationTracker = (module as? TrackableModuleInfo)?.createModificationTracker() ?: fallbackModificationTracker
        return ModuleData(moduleDescriptor, modificationTracker)
    }
}

private class DelegatingPackageFragmentProvider<M : ModuleInfo>(
    private val resolverForProject: AbstractResolverForProject<M>,
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

private object DiagnoseUnknownModuleInfoReporter {
    fun report(name: String, infos: List<ModuleInfo>, allModules: Collection<ModuleInfo>): Nothing {
        val message = "$name does not know how to resolve $infos, allModules: $allModules"
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
