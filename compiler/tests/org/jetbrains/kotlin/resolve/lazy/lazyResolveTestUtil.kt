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

package org.jetbrains.kotlin.resolve.lazy

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.ModuleContent
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.jvm.JvmAnalyzerFacade
import org.jetbrains.kotlin.resolve.jvm.JvmPlatformParameters

public fun createResolveSessionForFiles(
        project: Project,
        syntheticFiles: Collection<JetFile>,
        addBuiltIns: Boolean
): ResolveSession {
    val projectContext = ProjectContext(project)
    val testModule = TestModule(addBuiltIns)
    val resolverForProject = JvmAnalyzerFacade.setupResolverForProject(
            projectContext, listOf(testModule),
            { ModuleContent(syntheticFiles, GlobalSearchScope.allScope(project)) },
            JvmPlatformParameters { testModule }
    )
    return resolverForProject.resolverForModule(testModule).lazyResolveSession
}

private class TestModule(val dependsOnBuiltins: Boolean) : ModuleInfo {
    override val name: Name = Name.special("<Test module for lazy resolve>")
    override fun dependencies() = listOf(this)
    override fun dependencyOnBuiltins() =
            if (dependsOnBuiltins)
                ModuleInfo.DependenciesOnBuiltins.LAST
            else
                ModuleInfo.DependenciesOnBuiltins.NONE
}
