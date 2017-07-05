/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.script

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.ex.PathUtilEx
import org.jetbrains.kotlin.idea.core.script.dependencies.KotlinScriptResolveScopeProvider
import org.jetbrains.kotlin.script.ScriptTemplatesProvider
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import kotlin.script.dependencies.*
import kotlin.script.dependencies.DependenciesResolver.ResolveResult
import kotlin.script.templates.standard.ScriptTemplateWithArgs

class StandardKotlinScriptTemplateProvider(val project: Project) : ScriptTemplatesProvider {
    override val id: String = "StandardKotlinScriptTemplateProvider"
    override val isValid: Boolean = true

    override val templateClassNames: Iterable<String> get() = listOf(ScriptTemplateWithArgs::class.qualifiedName!!)
    override val templateClasspath get() = emptyList<File>()

    override val environment: Map<String, Any?>? get() {
        return mapOf(
                KotlinScriptResolveScopeProvider.USE_NULL_RESOLVE_SCOPE to true,
                "sdk" to getScriptSDK(project)
        )
    }

    override val resolver: DependenciesResolver = BundledKotlinScriptDependenciesResolver()

    private fun getScriptSDK(project: Project): String? {
        val jdk = PathUtilEx.getAnyJdk(project) ?:
                  ProjectJdkTable.getInstance().allJdks.firstOrNull { sdk -> sdk.sdkType is JavaSdk }
        
        return jdk?.homePath
    }
}

class BundledKotlinScriptDependenciesResolver : DependenciesResolver {
    override fun resolve(
            scriptContents: ScriptContents,
            environment: Environment
    ): ResolveResult {
        val javaHome = environment.get("sdk") as String?
        val dependencies = ScriptDependencies(
                javaHome = javaHome?.let(::File),
                classpath = with(PathUtil.kotlinPathsForIdeaPlugin) {
                    listOf(
                            reflectPath,
                            stdlibPath,
                            scriptRuntimePath
                    )
                }
        )
        return dependencies.asSuccess()
    }
}
