/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration.mpp

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.util.Pair
import org.jetbrains.kotlin.gradle.KotlinDependency
import org.jetbrains.kotlin.idea.configuration.KotlinMPPGradleProjectResolver
import org.jetbrains.kotlin.idea.configuration.klib.KotlinNativeLibrariesFixer
import org.jetbrains.plugins.gradle.model.ExternalSourceSet
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil
import org.jetbrains.plugins.gradle.service.project.ProjectResolverContext

@Throws(IllegalStateException::class)
internal fun KotlinMPPGradleProjectResolver.Companion.buildDependencies(
    resolverCtx: ProjectResolverContext,
    sourceSetMap: Map<String, Pair<DataNode<GradleSourceSetData>, ExternalSourceSet>>,
    artifactsMap: Map<ArtifactPath, ModuleId>,
    ownerDataNode: DataNode<GradleSourceSetData>,
    dependencies: Collection<KotlinDependency>,
    ideProject: DataNode<ProjectData>
) {
    GradleProjectResolverUtil.buildDependencies(
        resolverCtx, sourceSetMap, artifactsMap, ownerDataNode, dependencies, ideProject
    )
    KotlinNativeLibrariesFixer.applyTo(ownerDataNode, ideProject)
}