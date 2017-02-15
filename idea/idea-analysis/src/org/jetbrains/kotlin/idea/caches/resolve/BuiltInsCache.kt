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

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.util.containers.SLRUCache
import org.jetbrains.kotlin.analyzer.LanguageSettingsProvider
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.context.GlobalContextImpl
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.project.getLanguageVersionSettings
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.platform.JvmBuiltIns
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform

class BuiltInsCache private constructor(
        private val project: Project,
        platform: TargetPlatform,
        sdk: Sdk?,
        sdkModuleDescriptor: ModuleDescriptor?,
        sdkContext: GlobalContextImpl
) {

    private val cache: SLRUCache<Boolean, KotlinBuiltIns> = object : SLRUCache<Boolean, KotlinBuiltIns>(2, 2) {
        override fun createValue(key: Boolean): KotlinBuiltIns {
            val builtIns = calculateBuiltIns(platform, sdk, sdkContext)

            if (builtIns is JvmBuiltIns) {
                builtIns.initialize(sdkModuleDescriptor!!, key)
            }
            return builtIns
        }
    }

    fun getBuiltIns(moduleInfo: ModuleInfo): KotlinBuiltIns {
        val languageFeatureSettings = LanguageSettingsProvider.getInstance(project).getLanguageVersionSettings(moduleInfo, project)
        return cache.get(languageFeatureSettings.supportsFeature(LanguageFeature.AdditionalBuiltInsMembers))
    }

    companion object {
        fun calculateBuiltIns(platform: TargetPlatform, sdk: Sdk?, sdkContext: GlobalContextImpl): KotlinBuiltIns = when {
            platform is JsPlatform -> JsPlatform.builtIns
            platform is JvmPlatform && sdk != null -> JvmBuiltIns(sdkContext.storageManager)
            else -> DefaultBuiltIns.Instance
        }

        fun createCacheAndInitializeBuiltIns(
                project: Project,
                platform: TargetPlatform,
                sdk: Sdk?,
                sdkModuleDescriptor: ModuleDescriptor?,
                sdkBuiltIns: KotlinBuiltIns,
                sdkContext: GlobalContextImpl
        ): BuiltInsCache {
            val builtInsCache = BuiltInsCache(project, platform, sdk, sdkModuleDescriptor, sdkContext)

            if (sdkBuiltIns is JvmBuiltIns) {
                val isAdditionalBuiltInsFeatureSupported = project.getLanguageVersionSettings().supportsFeature(LanguageFeature.AdditionalBuiltInsMembers)
                sdkBuiltIns.initialize(
                        sdkModuleDescriptor!!, // sdk is not null for JvmBuiltIns because of calculateBuiltIns
                        isAdditionalBuiltInsFeatureSupported)
                builtInsCache.cache.put(isAdditionalBuiltInsFeatureSupported, sdkBuiltIns)
            }
            return builtInsCache
        }
    }
}