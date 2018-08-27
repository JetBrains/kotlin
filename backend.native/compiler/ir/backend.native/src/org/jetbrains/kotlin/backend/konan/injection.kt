/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.TargetPlatformVersion
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.container.useImpl
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.di.configureModule
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.konan.platform.KonanPlatform
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory

fun createTopDownAnalyzerForKonan(
        moduleContext: ModuleContext,
        bindingTrace: BindingTrace,
        declarationProviderFactory: DeclarationProviderFactory,
        languageVersionSettings: LanguageVersionSettings
): LazyTopDownAnalyzer {
    val storageComponentContainer = createContainer("TopDownAnalyzerForKonan", KonanPlatform) {
        configureModule(moduleContext, KonanPlatform, TargetPlatformVersion.NoVersion, bindingTrace)

        useInstance(declarationProviderFactory)
        useImpl<AnnotationResolverImpl>()

        CompilerEnvironment.configure(this)

        useInstance(languageVersionSettings)
        useImpl<ResolveSession>()
        useImpl<LazyTopDownAnalyzer>()
    }.apply {
        get<ModuleDescriptorImpl>().initialize(get<KotlinCodeAnalyzer>().packageFragmentProvider)
    }
    return storageComponentContainer.get<LazyTopDownAnalyzer>()
}
