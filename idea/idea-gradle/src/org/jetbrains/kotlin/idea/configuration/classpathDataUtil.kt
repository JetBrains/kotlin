/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.model.BuildScriptClasspathModel
import org.jetbrains.plugins.gradle.model.data.BuildScriptClasspathData
import org.jetbrains.plugins.gradle.service.project.ProjectResolverContext
import org.jetbrains.plugins.gradle.util.GradleConstants

fun buildClasspathData(
    gradleModule: IdeaModule,
    resolverCtx: ProjectResolverContext
): BuildScriptClasspathData {
    val classpathModel = resolverCtx.getExtraProject(gradleModule, BuildScriptClasspathModel::class.java)
    val classpathEntries = classpathModel?.classpath?.map {
        BuildScriptClasspathData.ClasspathEntry(it.classes, it.sources, it.javadoc)
    } ?: emptyList()
    return BuildScriptClasspathData(GradleConstants.SYSTEM_ID, classpathEntries).also {
        it.gradleHomeDir = classpathModel?.gradleHomeDir
    }
}
