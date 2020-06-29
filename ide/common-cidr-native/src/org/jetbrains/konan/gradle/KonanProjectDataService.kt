/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project

class KonanProjectDataService : AbstractProjectDataService<KonanModel, Module>() {
    override fun getTargetDataKey(): Key<KonanModel> = KonanProjectResolver.KONAN_MODEL_KEY

    override fun postProcess(
        toImport: Collection<DataNode<KonanModel>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModifiableModelsProvider
    ) {
        GradleKonanWorkspace.getInstanceOrNull(project)?.update()
    }
}