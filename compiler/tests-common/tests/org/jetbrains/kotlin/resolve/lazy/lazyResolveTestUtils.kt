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
import org.jetbrains.kotlin.analyzer.ResolverForProjectImpl
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.MultiTargetPlatform
import org.jetbrains.kotlin.resolve.jvm.JvmAnalyzerFacade
import org.jetbrains.kotlin.resolve.jvm.JvmPlatformParameters

fun createResolveSessionForFiles(
        project: Project,
        syntheticFiles: Collection<KtFile>,
        addBuiltIns: Boolean
): ResolveSession {
    val projectContext = ProjectContext(project)
    val testModule = TestModule(addBuiltIns)
    val resolverForProject = ResolverForProjectImpl(
            "test",
            projectContext, listOf(testModule), { JvmAnalyzerFacade },
            { ModuleContent(syntheticFiles, GlobalSearchScope.allScope(project)) },
            JvmPlatformParameters { testModule },
            modulePlatforms = { MultiTargetPlatform.Specific("JVM") }
    )
    return resolverForProject.resolverForModule(testModule).componentProvider.get<ResolveSession>()
}

private class TestModule(val dependsOnBuiltIns: Boolean) : ModuleInfo {
    override val name: Name = Name.special("<Test module for lazy resolve>")
    override fun dependencies() = listOf(this)
    override fun dependencyOnBuiltIns() =
            if (dependsOnBuiltIns)
                ModuleInfo.DependencyOnBuiltIns.LAST
            else
                ModuleInfo.DependencyOnBuiltIns.NONE
}
