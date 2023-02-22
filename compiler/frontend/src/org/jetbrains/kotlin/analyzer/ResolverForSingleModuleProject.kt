/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analyzer

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.context.withModule
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.CliSealedClassInheritorsProvider

class ResolverForSingleModuleProject<M : ModuleInfo>(
    debugName: String,
    projectContext: ProjectContext,
    private val module: M,
    private val resolverForModuleFactory: ResolverForModuleFactory,
    private val searchScope: GlobalSearchScope,
    private val builtIns: KotlinBuiltIns = DefaultBuiltIns.Instance,
    private val languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
    private val syntheticFiles: Collection<KtFile> = emptyList(),
    private val sdkDependency: M? = null,
    knownDependencyModuleDescriptors: Map<M, ModuleDescriptor> = emptyMap()
) : AbstractResolverForProject<M>(
    debugName,
    projectContext,
    listOf(module) + knownDependencyModuleDescriptors.keys,
    null,
    EmptyResolverForProject(),
    PackageOracleFactory.OptimisticFactory
) {
    override fun sdkDependency(module: M): M? = sdkDependency

    init {
        knownDependencyModuleDescriptors.forEach { (module, descriptor) ->
            descriptorByModule[module] = ModuleData(
                descriptor as ModuleDescriptorImpl,
                (module as? TrackableModuleInfo)?.createModificationTracker() ?: fallbackModificationTracker
            )
        }
    }

    override fun modulesContent(module: M): ModuleContent<M> = when (module) {
        this.module -> ModuleContent(module, syntheticFiles, searchScope)
        else -> ModuleContent(module, emptyList(), searchScope)
    }

    override fun builtInsForModule(module: M): KotlinBuiltIns = builtIns

    override fun createResolverForModule(descriptor: ModuleDescriptor, moduleInfo: M): ResolverForModule =
        resolverForModuleFactory.createResolverForModule(
            descriptor as ModuleDescriptorImpl,
            projectContext.withModule(descriptor),
            modulesContent(moduleInfo),
            this,
            languageVersionSettings,
            CliSealedClassInheritorsProvider,
            resolveOptimizingOptions = null,
            absentDescriptorHandlerClass = null
        )
}
