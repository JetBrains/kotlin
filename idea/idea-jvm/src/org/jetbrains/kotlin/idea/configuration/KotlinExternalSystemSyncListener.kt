/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.configuration.ui.KotlinConfigurationCheckerComponent

class KotlinExternalSystemSyncListener : ExternalSystemTaskNotificationListenerAdapter() {
    override fun onStart(id: ExternalSystemTaskId, workingDir: String) {
        val project = id.findResolvedProject() ?: return
        KotlinMigrationProjectComponent.getInstanceIfNotDisposed(project)?.onImportAboutToStart()
        KotlinConfigurationCheckerComponent.getInstanceIfNotDisposed(project)?.syncStarted()
    }

    override fun onEnd(id: ExternalSystemTaskId) {
        // At this point changes might be still not applied to project structure yet.
        val project = id.findResolvedProject() ?: return
        KotlinConfigurationCheckerComponent.getInstanceIfNotDisposed(project)?.syncDone()
    }
}

internal fun ExternalSystemTaskId.findResolvedProject(): Project? {
    if (type != ExternalSystemTaskType.RESOLVE_PROJECT) return null
    return findProject()
}
