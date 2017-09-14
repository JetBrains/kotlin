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

package org.jetbrains.kotlin.idea.compiler

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.LanguageSettingsProvider
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.caches.resolve.LibraryInfo
import org.jetbrains.kotlin.idea.caches.resolve.ModuleSourceInfo
import org.jetbrains.kotlin.idea.project.getLanguageVersionSettings
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.project.targetPlatform
import org.jetbrains.kotlin.utils.Jsr305State

object IDELanguageSettingsProvider : LanguageSettingsProvider {
    override fun getLanguageVersionSettings(moduleInfo: ModuleInfo, project: Project): LanguageVersionSettings =
            when (moduleInfo) {
                is ModuleSourceInfo -> moduleInfo.module.languageVersionSettings
                is LibraryInfo -> project.getLanguageVersionSettings(extraAnalysisFlags = getExtraAnalysisFlags(project))
                else -> project.getLanguageVersionSettings()
            }

    private fun getExtraAnalysisFlags(project: Project): Map<AnalysisFlag<*>, Any?> {
        val map = mutableMapOf<AnalysisFlag<*>, Any>()
        for (module in ModuleManager.getInstance(project).modules) {
            val settings = KotlinFacetSettingsProvider.getInstance(project).getSettings(module) ?: continue
            val compilerArguments = settings.compilerArguments as? K2JVMCompilerArguments ?: continue

            val jsr305state = Jsr305State.findByDescription(compilerArguments.jsr305)
            if (jsr305state != null && jsr305state != Jsr305State.IGNORE) {
                map.put(AnalysisFlag.jsr305, jsr305state)
                break
            }
        }
        return map
    }

    override fun getTargetPlatform(moduleInfo: ModuleInfo): TargetPlatformVersion {
        return (moduleInfo as? ModuleSourceInfo)?.module?.targetPlatform?.version ?: TargetPlatformVersion.NoVersion
    }
}
