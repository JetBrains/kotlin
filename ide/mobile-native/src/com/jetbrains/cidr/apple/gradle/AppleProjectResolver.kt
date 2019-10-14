package com.jetbrains.cidr.apple.gradle

import AppleProjectExtension
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ContentRootData
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants
import com.intellij.openapi.externalSystem.util.Order
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension
import org.jetbrains.plugins.gradle.util.GradleConstants

@Order(ExternalSystemConstants.UNORDERED)
class AppleProjectResolver : AbstractProjectResolverExtension() {
    companion object {
        @JvmField
        val APPLE_PROJECT: Key<AppleProjectModel> = Key.create(AppleProjectModel::class.java, ProjectKeys.MODULE.processingWeight + 1)
    }

    override fun getExtraProjectModelClasses() = setOf(AppleProjectModel::class.java)
    override fun getToolingExtensionsClasses() =
        setOf(AppleProjectModelBuilder::class.java, AppleProjectExtension::class.java, Unit::class.java)

    override fun populateModuleContentRoots(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        resolverCtx.getExtraProject(gradleModule, AppleProjectModel::class.java)?.let { apple ->
            val contentRoots = mutableMapOf<String, ContentRootData>()

            val targets = mutableMapOf<String, AppleTargetModel>()
            for (target in apple.targets.values) {
                for (folder in target.sourceFolders) {
                    val contentRoot = ContentRootData(GradleConstants.SYSTEM_ID, folder.path)
                    if (contentRoots.putIfAbsent(contentRoot.rootPath, contentRoot) == null) {
                        contentRoot.storePath(ExternalSystemSourceType.SOURCE, contentRoot.rootPath)
                    }
                }
                targets[target.name] = AppleTargetModelImpl(target.name, target.sourceFolders, target.bridgingHeader)
            }
            ideModule.createChild(APPLE_PROJECT, AppleProjectModelImpl(targets))

            for (contentRoot in contentRoots.values) {
                ideModule.createChild(ProjectKeys.CONTENT_ROOT, contentRoot)
            }
        }

        super.populateModuleContentRoots(gradleModule, ideModule)
    }
}
