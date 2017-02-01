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

package org.jetbrains.kotlin.idea.maven.facet

import com.intellij.openapi.module.Module
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.idea.facet.KotlinVersionInfoProvider
import org.jetbrains.kotlin.idea.facet.mavenLibraryIds
import org.jetbrains.kotlin.idea.maven.configuration.KotlinMavenConfigurator

class MavenKotlinVersionInfoProvider : KotlinVersionInfoProvider {
    override fun getCompilerVersion(module: Module): String? {
        val projectsManager = MavenProjectsManager.getInstance(module.project)
        val mavenProject = projectsManager.findProject(module) ?: return null
        return mavenProject.findPlugin(KotlinMavenConfigurator.GROUP_ID, KotlinMavenConfigurator.MAVEN_PLUGIN_ID)?.version
    }

    override fun getLibraryVersions(module: Module, targetPlatform: TargetPlatformKind<*>): Collection<String> {
        val projectsManager = MavenProjectsManager.getInstance(module.project)
        val mavenProject = projectsManager.findProject(module) ?: return emptyList()
        return targetPlatform
                .mavenLibraryIds
                .flatMap { mavenProject.findDependencies(KotlinMavenConfigurator.GROUP_ID, it) }
                .map { it.version }
                .distinct()
    }
}
