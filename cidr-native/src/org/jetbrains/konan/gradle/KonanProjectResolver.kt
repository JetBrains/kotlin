/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension
import java.io.File

class KonanProjectResolver : AbstractProjectResolverExtension() {
    override fun getToolingExtensionsClasses(): Set<Class<out Any>> {
        return setOf(KonanModelBuilder::class.java, Unit::class.java)
    }

    override fun getExtraProjectModelClasses(): Set<Class<out Any>> {
        return setOf(KonanModel::class.java)
    }

    override fun populateModuleExtraModels(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        resolverCtx.getExtraProject(gradleModule, KonanModel::class.java)?.let {
            // store a local process copy of the object to get rid of proxy types for further serialization
            ideModule.createChild(KONAN_MODEL_KEY, MyKonanModel(it))
        }

        nextResolver.populateModuleExtraModels(gradleModule, ideModule)
    }

    private class MyKonanModel(konanModel: KonanModel) : KonanModel {
        override val artifacts: List<KonanModelArtifact> = konanModel.artifacts.map { MyKonanArtifactEx(it) }
        override val buildTaskPath: String = konanModel.buildTaskPath
        override val cleanTaskPath: String = konanModel.cleanTaskPath
        override val kotlinNativeHome: String = konanModel.kotlinNativeHome

        private class MyKonanArtifactEx(artifact: KonanModelArtifact) : KonanModelArtifact {
            override val name: String = artifact.name
            override val type: CompilerOutputKind = artifact.type
            override val targetPlatform: String = artifact.targetPlatform
            override val file: File = artifact.file
            override val buildTaskPath: String = artifact.buildTaskPath
            override val isTests: Boolean = artifact.isTests
        }
    }

    companion object {
        val KONAN_MODEL_KEY = Key.create(KonanModel::class.java, ProjectKeys.MODULE.processingWeight + 1)
    }
}