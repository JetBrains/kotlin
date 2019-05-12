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
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.KotlinFacetSettingsProvider
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.platform.TargetPlatformVersion
import org.jetbrains.kotlin.idea.caches.project.*
import org.jetbrains.kotlin.idea.project.getLanguageVersionSettings
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.platform.jvm.JdkPlatform
import org.jetbrains.kotlin.platform.subplatformOfType
import org.jetbrains.kotlin.utils.Jsr305State

object IDELanguageSettingsProvider : LanguageSettingsProvider {
    override fun getLanguageVersionSettings(
        moduleInfo: ModuleInfo,
        project: Project,
        isReleaseCoroutines: Boolean?
    ): LanguageVersionSettings =
        when (moduleInfo) {
            is ModuleSourceInfo -> moduleInfo.module.languageVersionSettings
            is LibraryInfo -> project.getLanguageVersionSettings(
                jsr305State = computeJsr305State(project), isReleaseCoroutines = isReleaseCoroutines
            )
            is ScriptModuleInfo -> getLanguageSettingsForScripts(project, moduleInfo.scriptDefinition).languageVersionSettings
            is ScriptDependenciesInfo.ForFile -> getLanguageSettingsForScripts(project, moduleInfo.scriptDefinition).languageVersionSettings
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

    // TODO(dsavvinov): get rid of this method; instead store proper instance of TargetPlatformVersion in platform-instance
    override fun getTargetPlatform(moduleInfo: ModuleInfo, project: Project): TargetPlatformVersion =
        when (moduleInfo) {
            is ModuleSourceInfo -> (moduleInfo.module.platform?.subplatformOfType<JdkPlatform>())?.targetVersion
                ?: TargetPlatformVersion.NoVersion
            is ScriptModuleInfo -> getLanguageSettingsForScripts(project, moduleInfo.scriptDefinition).targetPlatformVersion
            is ScriptDependenciesInfo.ForFile -> getLanguageSettingsForScripts(project, moduleInfo.scriptDefinition).targetPlatformVersion
            else -> TargetPlatformVersion.NoVersion
        }
}

private data class ScriptLanguageSettings(
    val languageVersionSettings: LanguageVersionSettings,
    val targetPlatformVersion: TargetPlatformVersion
)

private val SCRIPT_LANGUAGE_SETTINGS = Key.create<CachedValue<ScriptLanguageSettings>>("SCRIPT_LANGUAGE_SETTINGS")

private fun getLanguageSettingsForScripts(project: Project, scriptDefinition: ScriptDefinition): ScriptLanguageSettings {
    val args = scriptDefinition.compilerOptions
    return if (args == null || args.none()) {
        ScriptLanguageSettings(project.getLanguageVersionSettings(), TargetPlatformVersion.NoVersion)
    } else {
        val settings = scriptDefinition.getUserData(SCRIPT_LANGUAGE_SETTINGS) ?: createCachedValue(project) {
            val compilerArguments = K2JVMCompilerArguments()
            parseCommandLineArguments(args.toList(), compilerArguments)
            // TODO: reporting
            val verSettings = compilerArguments.toLanguageVersionSettings(MessageCollector.NONE)
            val jvmTarget = compilerArguments.jvmTarget?.let { JvmTarget.fromString(it) } ?: TargetPlatformVersion.NoVersion
            ScriptLanguageSettings(verSettings, jvmTarget)
        }.also { scriptDefinition.putUserData(SCRIPT_LANGUAGE_SETTINGS, it) }
        settings.value
    }
}

private fun createCachedValue(project: Project, body: () -> ScriptLanguageSettings): CachedValue<ScriptLanguageSettings> {
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
