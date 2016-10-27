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

package org.jetbrains.kotlin.idea.project

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.TargetPlatform

val KtElement.platform: TargetPlatform
    get() = TargetPlatformDetector.getPlatform(getContainingKtFile())

val KtElement.builtIns: KotlinBuiltIns
    get() = getResolutionFacade().moduleDescriptor.builtIns

val Module.languageVersionSettings: LanguageVersionSettings
    get() {
        val versionInfo = KotlinFacetSettingsProvider.getInstance(project).getSettings(this).versionInfo
        val languageVersion = versionInfo.languageLevel ?: LanguageVersion.LATEST
        val apiVersion = versionInfo.apiLevel ?: languageVersion
        return LanguageVersionSettingsImpl(languageVersion, ApiVersion.createByLanguageVersion(apiVersion))
    }

val KtElement.languageVersionSettings: LanguageVersionSettings
    get() = ModuleUtilCore.findModuleForPsiElement(this)?.languageVersionSettings ?: LanguageVersionSettingsImpl.DEFAULT