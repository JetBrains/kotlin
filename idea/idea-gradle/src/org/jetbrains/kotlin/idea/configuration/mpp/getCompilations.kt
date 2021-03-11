/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration.mpp

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.kotlin.gradle.KotlinCompilation
import org.jetbrains.kotlin.gradle.KotlinMPPGradleModel
import org.jetbrains.kotlin.idea.configuration.KotlinMPPGradleProjectResolver
import org.jetbrains.kotlin.idea.configuration.kotlinSourceSet
import org.jetbrains.kotlin.idea.configuration.utils.getKotlinModuleId
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.service.project.ProjectResolverContext

internal fun KotlinMPPGradleProjectResolver.Companion.getCompilations(
    gradleModule: IdeaModule,
    mppModel: KotlinMPPGradleModel,
    ideModule: DataNode<ModuleData>,
    resolverCtx: ProjectResolverContext
): Sequence<Pair<DataNode<GradleSourceSetData>, KotlinCompilation>> {

    val sourceSetsMap = HashMap<String, DataNode<GradleSourceSetData>>()
    for (dataNode in ExternalSystemApiUtil.findAll(ideModule, GradleSourceSetData.KEY)) {
        if (dataNode.kotlinSourceSet != null) {
            sourceSetsMap[dataNode.data.id] = dataNode
        }
    }

    return sequence {
        for (target in mppModel.targets) {
            for (compilation in target.compilations) {
                val moduleId = getKotlinModuleId(gradleModule, compilation, resolverCtx)
                val moduleDataNode = sourceSetsMap[moduleId] ?: continue
                yield(moduleDataNode to compilation)
            }
        }
    }
}


