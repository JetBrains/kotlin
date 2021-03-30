/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration.mpp

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.idea.configuration.KotlinMPPGradleProjectResolver
import org.jetbrains.kotlin.idea.configuration.KotlinMPPGradleProjectResolver.Companion.CompilationWithDependencies
import org.jetbrains.kotlin.idea.configuration.getMppModel
import org.jetbrains.kotlin.idea.configuration.klib.KotlinNativeLibrariesDependencySubstitutor
import org.jetbrains.plugins.gradle.model.ExternalSourceSet
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolver
import org.jetbrains.plugins.gradle.service.project.ProjectResolverContext
import com.intellij.openapi.util.Pair as IntelliJPair


internal typealias ModuleId = String
internal typealias ArtifactPath = String

internal data class PopulateModuleDependenciesContext(
    val resolverCtx: ProjectResolverContext,
    val mppModel: KotlinMPPGradleModel,
    val gradleModule: IdeaModule,
    val ideProject: DataNode<ProjectData>,
    val ideModule: DataNode<ModuleData>,
    val dependenciesPreprocessor: KotlinDependenciesPreprocessor,
    val sourceSetMap: Map<ModuleId, IntelliJPair<DataNode<GradleSourceSetData>, ExternalSourceSet>>,
    val artifactsMap: Map<ArtifactPath, ModuleId>,
    val processedModuleIds: MutableSet<ModuleId> = mutableSetOf()
)

internal fun KotlinMPPGradleProjectResolver.Companion.createPopulateModuleDependenciesContext(
    gradleModule: IdeaModule,
    ideProject: DataNode<ProjectData>,
    ideModule: DataNode<ModuleData>,
    resolverCtx: ProjectResolverContext
): PopulateModuleDependenciesContext? {
    val mppModel = resolverCtx.getMppModel(gradleModule) ?: return null
    mppModel.dependencyMap.values.modifyDependenciesOnMppModules(ideProject, resolverCtx)

    val sourceSetMap = ideProject.getUserData(GradleProjectResolver.RESOLVED_SOURCE_SETS) ?: return null
    val artifactsMap = ideProject.getUserData(GradleProjectResolver.CONFIGURATION_ARTIFACTS) ?: return null
    val dependenciesPreprocessor = KotlinNativeLibrariesDependencySubstitutor(mppModel, gradleModule, resolverCtx)
        .plus(DistinctIdKotlinDependenciesPreprocessor)

    return PopulateModuleDependenciesContext(
        resolverCtx = resolverCtx,
        mppModel = mppModel,
        gradleModule = gradleModule,
        ideProject = ideProject,
        ideModule = ideModule,
        dependenciesPreprocessor = dependenciesPreprocessor,
        sourceSetMap = sourceSetMap,
        artifactsMap = artifactsMap,
    )
}

internal fun PopulateModuleDependenciesContext.getDependencies(module: KotlinModule): List<KotlinDependency> {
    return dependenciesPreprocessor(module.dependencies.mapNotNull { id -> mppModel.dependencyMap[id] })
}

internal fun PopulateModuleDependenciesContext.getCompilationsWithDependencies(
    sourceSet: KotlinSourceSet
): List<CompilationWithDependencies> {
    return mppModel.getCompilations(sourceSet).map { compilation -> CompilationWithDependencies(compilation, getDependencies(compilation)) }
}

internal fun KotlinCompilation.dependsOnSourceSet(mppModel: KotlinMPPGradleModel, sourceSet: KotlinSourceSet): Boolean {
    return declaredSourceSets.any { containedSourceSet -> sourceSet.isOrDependsOnSourceSet(mppModel, containedSourceSet) }
}

internal fun KotlinSourceSet.isOrDependsOnSourceSet(mppModel: KotlinMPPGradleModel, sourceSet: KotlinSourceSet): Boolean {
    if (this == sourceSet) return true
    return this.declaredDependsOnSourceSets
        .map { dependencySourceSetName -> mppModel.sourceSetsByName.getValue(dependencySourceSetName) }
        .any { dependencySourceSet -> dependencySourceSet.isOrDependsOnSourceSet(mppModel, sourceSet) }
}


