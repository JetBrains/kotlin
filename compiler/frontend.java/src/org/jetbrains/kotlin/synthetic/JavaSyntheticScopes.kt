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

package org.jetbrains.kotlin.synthetic

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.java.components.SamConversionResolver
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.scopes.SyntheticScope
import org.jetbrains.kotlin.resolve.scopes.SyntheticScopes
import org.jetbrains.kotlin.storage.StorageManager

class JavaSyntheticScopes(
        private val project: Project,
        private val moduleDescriptor: ModuleDescriptor,
        storageManager: StorageManager,
        lookupTracker: LookupTracker,
        languageVersionSettings: LanguageVersionSettings,
        samConventionResolver: SamConversionResolver,
        deprecationResolver: DeprecationResolver
): SyntheticScopes {
    override val scopes = run {
        val javaSyntheticPropertiesScope = JavaSyntheticPropertiesScope(storageManager, lookupTracker)

        val scopesFromExtensions = SyntheticScopeProviderExtension
            .getInstances(project)
            .flatMap { it.getScopes(moduleDescriptor, javaSyntheticPropertiesScope) }

        listOf(
            javaSyntheticPropertiesScope,
            SamAdapterFunctionsScope(
                storageManager, languageVersionSettings, samConventionResolver, deprecationResolver,
                lookupTracker
            )
        ) + scopesFromExtensions
    }
}

interface SyntheticScopeProviderExtension {
    companion object : ProjectExtensionDescriptor<SyntheticScopeProviderExtension>(
        "org.jetbrains.kotlin.syntheticScopeProviderExtension", SyntheticScopeProviderExtension::class.java)

    fun getScopes(moduleDescriptor: ModuleDescriptor, javaSyntheticPropertiesScope: JavaSyntheticPropertiesScope): List<SyntheticScope>
}