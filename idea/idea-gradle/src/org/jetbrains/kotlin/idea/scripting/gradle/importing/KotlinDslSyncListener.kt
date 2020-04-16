/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.importing

import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.idea.framework.GRADLE_SYSTEM_ID
import org.jetbrains.kotlin.idea.scripting.gradle.getJavaHomeForGradleProject
import org.jetbrains.kotlin.psi.NotNullableUserDataProperty

var Project.kotlinDslModels: MutableMap<ExternalTaskId, KotlinDslScriptModelsForGradleProject>
        by NotNullableUserDataProperty<Project, MutableMap<ExternalTaskId, KotlinDslScriptModelsForGradleProject>>(
            Key("Kotlin DSL Scripts Models"), hashMapOf()
        )

data class KotlinDslScriptModelsForGradleProject(
    val gradleProjectPaths: HashSet<String> = hashSetOf(),
    val models: HashSet<KotlinDslScriptModel> = hashSetOf()
) {
    val gradleProjectId: GradleProjectId get() = GradleProjectId(gradleProjectPaths.map { it.hashCode() })
}

class KotlinDslSyncListener : ExternalSystemTaskNotificationListenerAdapter() {
    override fun onEnd(id: ExternalSystemTaskId) {
        if (id.type != ExternalSystemTaskType.RESOLVE_PROJECT || id.projectSystemId != GRADLE_SYSTEM_ID) {
            return
        }

        val project = id.findProject() ?: return

        val externalTaskId = id.toExternalTaskId()
        val modelsForGradleProject = project.kotlinDslModels[externalTaskId] ?: return

        project.kotlinDslModels.remove(externalTaskId)

        if (modelsForGradleProject.models.isEmpty()) return

        val javaHome = getJavaHomeForGradleProject(project)

        saveScriptModels(project, id, javaHome, modelsForGradleProject)
    }
}
