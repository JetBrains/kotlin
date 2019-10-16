package com.jetbrains.cidr.apple.gradle

import AppleProjectExtension
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ContentRootData
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants
import com.intellij.openapi.externalSystem.util.Order
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.PathUtilRt
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.model.DefaultExternalSourceDirectorySet
import org.jetbrains.plugins.gradle.model.DefaultExternalSourceSet
import org.jetbrains.plugins.gradle.model.ExternalProject
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolver
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil
import org.jetbrains.plugins.gradle.service.project.ProjectResolverContext
import org.jetbrains.plugins.gradle.util.GradleConstants

@Order(ExternalSystemConstants.UNORDERED)
class AppleProjectResolver : AbstractProjectResolverExtension() {
    override fun getExtraProjectModelClasses() = setOf(AppleProjectModel::class.java)
    override fun getToolingExtensionsClasses() =
        setOf(AppleProjectModelBuilder::class.java, AppleProjectExtension::class.java, Unit::class.java)

    override fun createModule(gradleModule: IdeaModule, projectDataNode: DataNode<ProjectData>): DataNode<ModuleData> =
        super.createModule(gradleModule, projectDataNode).also { mainModuleNode ->
            val externalProject = resolverCtx.getExtraProject(gradleModule, ExternalProject::class.java) ?: return@also
            val apple = resolverCtx.getExtraProject(gradleModule, AppleProjectModel::class.java) ?: return@also
            val sourceSetMap = projectDataNode.getUserData(GradleProjectResolver.RESOLVED_SOURCE_SETS)!!

            for (target in apple.targets.values) {
                val moduleId = getAppleModuleId(gradleModule, target.name, resolverCtx)
                sourceSetMap.computeIfAbsent(moduleId) {
                    val sourceSetData = GradleSourceSetData(
                        moduleId,
                        getExternalName(gradleModule, target.name),
                        getInternalName(gradleModule, externalProject, target.name, resolverCtx),
                        mainModuleNode.data.moduleFileDirectoryPath,
                        mainModuleNode.data.linkedExternalProjectPath
                    ).apply {
                        group = externalProject.group
                        version = externalProject.version
                    }
                    val dataNode = mainModuleNode.createChild(GradleSourceSetData.KEY, sourceSetData).apply {
                        appleSourceSet = AppleTargetModelImpl(target.name, target.sourceFolders, target.bridgingHeader)
                    }
                    val externalSourceSet = DefaultExternalSourceSet().apply {
                        name = "${target.name}Main"
                        setSources(linkedMapOf(
                            ExternalSystemSourceType.SOURCE to DefaultExternalSourceDirectorySet().apply {
                                srcDirs = target.sourceFolders
                            }
                        ).toMap())
                    }
                    Pair.create(dataNode, externalSourceSet)
                }
            }
        }

    override fun populateModuleContentRoots(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        populateContentRoots(gradleModule, ideModule, resolverCtx)
        super.populateModuleContentRoots(gradleModule, ideModule)
    }

    companion object {
        @JvmField
        val APPLE_PROJECT: Key<AppleProjectModel> = Key.create(AppleProjectModel::class.java, ProjectKeys.MODULE.processingWeight + 1)

        private fun getAppleModuleId(gradleModule: IdeaModule, target: String, resolverCtx: ProjectResolverContext) =
            "${GradleProjectResolverUtil.getModuleId(resolverCtx, gradleModule)}:${target}Main"

        private fun getExternalName(gradleModule: IdeaModule, target: String) =
            "${gradleModule.name}:${target}Main"

        private fun getInternalName(
            gradleModule: IdeaModule,
            externalProject: ExternalProject,
            target: String,
            resolverCtx: ProjectResolverContext
        ): String {
            val delimiter: String
            val moduleName = StringBuilder()
            if (resolverCtx.isUseQualifiedModuleNames) {
                delimiter = "."
                if (StringUtil.isNotEmpty(externalProject.group)) {
                    moduleName.append(externalProject.group).append(delimiter)
                }
                moduleName.append(externalProject.name)
            } else {
                delimiter = "_"
                moduleName.append(gradleModule.name)
            }
            moduleName.append(delimiter)
            moduleName.append("${target}Main")
            return PathUtilRt.suggestFileName(moduleName.toString(), true, false)
        }

        private fun populateContentRoots(
            gradleModule: IdeaModule,
            ideModule: DataNode<ModuleData>,
            resolverCtx: ProjectResolverContext
        ) {
            if (resolverCtx.getExtraProject(gradleModule, ExternalProject::class.java) == null) return

            val targets = mutableMapOf<String, AppleTargetModel>()
            for (dataNode in ExternalSystemApiUtil.findAll(ideModule, GradleSourceSetData.KEY)) {
                val target = (dataNode.appleSourceSet ?: continue).also { targets[it.name] = it }
                for (sourceFolder in target.sourceFolders) {
                    val contentRoot = ContentRootData(GradleConstants.SYSTEM_ID, sourceFolder.path)
                    contentRoot.storePath(ExternalSystemSourceType.SOURCE, contentRoot.rootPath)
                    dataNode.createChild(ProjectKeys.CONTENT_ROOT, contentRoot)
                }
            }

            ideModule.createChild(APPLE_PROJECT, AppleProjectModelImpl(targets))
        }
    }
}
