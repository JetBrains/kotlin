package org.jetbrains.konan.gradle

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

class KonanProjectResolver : AbstractProjectResolverExtension() {

    override fun getToolingExtensionsClasses(): Set<Class<out Any>> = setOf(KonanModelBuilder::class.java, Unit::class.java)

    override fun getExtraProjectModelClasses(): Set<Class<out Any>> = setOf(KonanModel::class.java)

    override fun populateModuleExtraModels(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        resolverCtx.getExtraProject(gradleModule, KonanModel::class.java)?.let {
            // store a local process copy of the object to get rid of proxy types for further serialization
            ideModule.createChild(KONAN_MODEL_KEY, KonanModelImpl(it))
        }

        nextResolver.populateModuleExtraModels(gradleModule, ideModule)
    }

    companion object {
        val KONAN_MODEL_KEY = Key.create(KonanModel::class.java, ProjectKeys.MODULE.processingWeight + 1)
    }
}