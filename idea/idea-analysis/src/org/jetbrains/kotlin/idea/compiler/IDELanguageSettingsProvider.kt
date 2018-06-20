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
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.analyzer.LanguageSettingsProvider
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.cli.common.arguments.Jsr305Parser
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.KotlinFacetSettingsProvider
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.TargetPlatformVersion
import org.jetbrains.kotlin.idea.caches.project.*
import org.jetbrains.kotlin.idea.project.getLanguageVersionSettings
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.project.targetPlatform
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.utils.Jsr305State

object IDELanguageSettingsProvider : LanguageSettingsProvider {
    override fun getLanguageVersionSettings(moduleInfo: ModuleInfo, project: Project): LanguageVersionSettings =
        when (moduleInfo) {
            is ModuleSourceInfo -> moduleInfo.module.languageVersionSettings
            is LibraryInfo -> project.getLanguageVersionSettings(jsr305State = computeJsr305State(project))
            is ScriptModuleInfo -> getVersionLanguageSettingsForScripts(project, moduleInfo.scriptDefinition)
            is ScriptDependenciesInfo.ForFile -> getVersionLanguageSettingsForScripts(project, moduleInfo.scriptDefinition)
            is PlatformModuleInfo -> moduleInfo.platformModule.module.languageVersionSettings
            else -> project.getLanguageVersionSettings()
        }

    private fun computeJsr305State(project: Project): Jsr305State? {
        var result: Jsr305State? = null
        for (module in ModuleManager.getInstance(project).modules) {
            val settings = KotlinFacetSettingsProvider.getInstance(project).getSettings(module) ?: continue
            val compilerArguments = settings.mergedCompilerArguments as? K2JVMCompilerArguments ?: continue

            result = Jsr305Parser(MessageCollector.NONE).parse(
                compilerArguments.jsr305,
                compilerArguments.supportCompatqualCheckerFrameworkAnnotations
            )

        }
        return result
    }

    override fun getTargetPlatform(moduleInfo: ModuleInfo): TargetPlatformVersion {
        return (moduleInfo as? ModuleSourceInfo)?.module?.targetPlatform?.version ?: TargetPlatformVersion.NoVersion
    }
}

private val LANGUAGE_VERSION_SETTINGS = Key.create<CachedValue<LanguageVersionSettings>>("LANGUAGE_VERSION_SETTINGS")

private fun getVersionLanguageSettingsForScripts(project: Project, scriptDefinition: KotlinScriptDefinition): LanguageVersionSettings {
    val args = scriptDefinition.additionalCompilerArguments
    return if (args == null || args.none()) {
        project.getLanguageVersionSettings()
    } else {
        val settings = scriptDefinition.getUserData(LANGUAGE_VERSION_SETTINGS) ?: createCachedValue(project) {
            val compilerArguments = K2JVMCompilerArguments()
            parseCommandLineArguments(args.toList(), compilerArguments)
            // TODO: reporting
            compilerArguments.configureLanguageVersionSettings(MessageCollector.NONE)
        }.also { scriptDefinition.putUserData(LANGUAGE_VERSION_SETTINGS, it) }
        settings.value
    }
}

private fun createCachedValue(project: Project, body: () -> LanguageVersionSettings): CachedValue<LanguageVersionSettings> {
    return CachedValuesManager
        .getManager(project)
        .createCachedValue(
            {
                CachedValueProvider.Result(
                    body(),
                    ProjectRootModificationTracker.getInstance(project)
                )
            }, false
        )
}
