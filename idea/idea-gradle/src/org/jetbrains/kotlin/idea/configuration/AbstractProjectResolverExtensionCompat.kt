/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

// BUNCH: 193
abstract class AbstractProjectResolverExtensionCompat : AbstractProjectResolverExtension() {
    override fun createModule(gradleModule: IdeaModule, projectDataNode: DataNode<ProjectData>): DataNode<ModuleData> {
        return super.createModule(gradleModule, projectDataNode).also {
            initializeModuleNode(gradleModule, it, projectDataNode)
        }
    }

    // Inline after class remove
    abstract fun initializeModuleNode(
        gradleModule: IdeaModule,
        moduleDataNode: DataNode<ModuleData>,
        projectDataNode: DataNode<ProjectData>,
    )
}