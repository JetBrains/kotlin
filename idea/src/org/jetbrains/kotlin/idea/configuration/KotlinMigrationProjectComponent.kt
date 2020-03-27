/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class KotlinMigrationProjectComponent : StartupActivity {

    override fun runActivity(project: Project) {
        val connection = project.messageBus.connect(project)
        connection.subscribe(ProjectDataImportListener.TOPIC, ProjectDataImportListener {
            KotlinMigrationProjectService.getInstanceIfNotDisposed(project)?.onImportFinished()
        })
    }

}
