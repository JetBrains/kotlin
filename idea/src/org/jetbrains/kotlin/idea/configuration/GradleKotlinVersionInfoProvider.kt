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

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.idea.facet.KotlinVersionInfoProvider
import org.jetbrains.kotlin.idea.facet.mavenLibraryIds
import org.jetbrains.kotlin.idea.inspections.gradle.DifferentKotlinGradleVersionInspection
import org.jetbrains.kotlin.idea.inspections.gradle.DifferentStdlibGradleVersionInspection
import org.jetbrains.kotlin.idea.refactoring.toPsiFile
import org.jetbrains.kotlin.idea.refactoring.toVirtualFile
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.plugins.gradle.service.project.data.ExternalProjectDataCache
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase
import java.io.File

class GradleKotlinVersionInfoProvider : KotlinVersionInfoProvider {
    private fun getGradleFile(module: Module): GroovyFileBase? {
        val rootDir = ExternalSystemApiUtil.getExternalProjectPath(module) ?: return null
        val externalProjectDataCache = ExternalProjectDataCache.getInstance(module.project)
        val rootProject = externalProjectDataCache.getRootExternalProject(GradleConstants.SYSTEM_ID, File(rootDir)) ?: return null
        val buildFile = rootProject.buildFile ?: return null
        return buildFile.toVirtualFile()?.toPsiFile(module.project) as? GroovyFileBase ?: return null
    }

    override fun getCompilerVersion(module: Module): String? {
        return runReadAction { getGradleFile(module)?.let { DifferentKotlinGradleVersionInspection.getKotlinPluginVersion(it) } }
    }

    override fun getLibraryVersions(module: Module, targetPlatform: TargetPlatformKind<*>): Collection<String> {
        return runReadAction {
            getGradleFile(module)?.let {
                DifferentStdlibGradleVersionInspection.getKotlinStdlibVersions(it, targetPlatform.mavenLibraryIds)
            }
        } ?: emptyList()
    }
}