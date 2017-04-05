/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
