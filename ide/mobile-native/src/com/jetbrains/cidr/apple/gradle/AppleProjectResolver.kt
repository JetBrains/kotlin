package com.jetbrains.cidr.apple.gradle

import AppleProjectExtension
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.*
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants
import com.intellij.openapi.externalSystem.util.Order
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.PathUtilRt
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.kotlin.gradle.KotlinMPPGradleModel
import org.jetbrains.kotlin.gradle.KotlinPlatform
import org.jetbrains.plugins.gradle.model.DefaultExternalSourceDirectorySet
import org.jetbrains.plugins.gradle.model.DefaultExternalSourceSet
import org.jetbrains.plugins.gradle.model.ExternalProject
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolver
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil
import org.jetbrains.plugins.gradle.service.project.ProjectResolverContext
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File

@Order(ExternalSystemConstants.UNORDERED)
class AppleProjectResolver : AbstractProjectResolverExtension() {
    override fun getExtraProjectModelClasses() = setOf(AppleProjectModel::class.java)
    override fun getToolingExtensionsClasses() =
        setOf(AppleProjectModelBuilder::class.java, AppleProjectExtension::class.java, Unit::class.java)

    override fun createModule(gradleModule: IdeaModule, projectDataNode: DataNode<ProjectData>): DataNode<ModuleData>? =
        super.createModule(gradleModule, projectDataNode)?.also { mainModuleNode ->
            val externalProject = resolverCtx.getExtraProject(gradleModule, ExternalProject::class.java) ?: return@also
            val apple = resolverCtx.getExtraProject(gradleModule, AppleProjectModel::class.java) ?: return@also
            val sourceSetMap = projectDataNode.getUserData(GradleProjectResolver.RESOLVED_SOURCE_SETS)!!

            for (target in apple.targets.values) {
                val moduleId = getAppleModuleId(gradleModule, target.name, resolverCtx)
                val sourceModuleId = moduleId + "Main"
                val testModuleId = moduleId + "Test"

                val targetModel = AppleTargetModelImpl(
                    target.name, target.sourceFolders, target.testFolders, target.editableXcodeProjectDir, target.bridgingHeader
                )

                fun addSourceSet(id: String, suffix: String, type: ExternalSystemSourceType, dirs: Set<File>) {
                    sourceSetMap.computeIfAbsent(id) {
                        val sourceSetData = GradleSourceSetData(
                            id,
                            getExternalName(gradleModule, target.name, suffix),
                            getInternalName(gradleModule, externalProject, target.name, suffix, resolverCtx),
                            mainModuleNode.data.moduleFileDirectoryPath,
                            mainModuleNode.data.linkedExternalProjectPath
                        ).apply {
                            group = externalProject.group
                            version = externalProject.version
                        }
                        val dataNode = mainModuleNode.createChild(GradleSourceSetData.KEY, sourceSetData).apply {
                            appleSourceSet = targetModel
                        }
                        val externalSourceSet = DefaultExternalSourceSet().apply {
                            name = target.name + suffix
                            setSources(linkedMapOf(
                                type to DefaultExternalSourceDirectorySet().apply { srcDirs = dirs }
                            ).toMap())
                        }
                        Pair.create(dataNode, externalSourceSet)
                    }
                }

                addSourceSet(sourceModuleId, "Main", ExternalSystemSourceType.SOURCE, target.sourceFolders)
                addSourceSet(testModuleId, "Test", ExternalSystemSourceType.TEST, target.testFolders)
            }
        }

    override fun populateModuleContentRoots(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        populateContentRoots(gradleModule, ideModule, resolverCtx)
        super.populateModuleContentRoots(gradleModule, ideModule)
    }

    override fun populateModuleDependencies(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>, ideProject: DataNode<ProjectData>) {
        super.populateModuleDependencies(gradleModule, ideModule, ideProject)

        resolverCtx.getExtraProject(gradleModule, AppleProjectModel::class.java) ?: return
        val mppModel = resolverCtx.getExtraProject(gradleModule, KotlinMPPGradleModel::class.java) ?: return

        // WARNING: the following code is incorrect and here only temporarily. It simply finds K/N modules and adds them as dependencies
        // to the Apple iOS module. Gradle model currently does not represent this dependency, and it's required for the project model
        // in order for the language support to work correctly.
        val deps = mppModel.sourceSets.values.filter { it.actualPlatforms.supports(KotlinPlatform.NATIVE) && !it.isTestModule }

        val addDependency = { dataNode: DataNode<GradleSourceSetData>, sourceSetNameToAdd: String ->
            val moduleId = GradleProjectResolverUtil.getModuleId(resolverCtx, gradleModule) + ":" + sourceSetNameToAdd

            @Suppress("UNCHECKED_CAST")
            val depData = (ideModule.children.firstOrNull { (it.data as? ModuleData)?.id == moduleId } as? DataNode<out ModuleData>)?.data

            if (depData != null) {
                dataNode.createChild(ProjectKeys.MODULE_DEPENDENCY, ModuleDependencyData(dataNode.data, depData))
            }
        }

        for (dataNode in ExternalSystemApiUtil.findAll(ideModule, GradleSourceSetData.KEY)) {
            dataNode.appleSourceSet ?: continue
            for (sourceSet in deps) {
                addDependency(dataNode, sourceSet.name)
                sourceSet.dependsOnSourceSets.forEach { addDependency(dataNode, it) }
            }
        }
    }

    companion object {
        @JvmField
        val APPLE_PROJECT: Key<AppleProjectModel> = Key.create(AppleProjectModel::class.java, ProjectKeys.MODULE.processingWeight + 1)

        private fun getAppleModuleId(gradleModule: IdeaModule, target: String, resolverCtx: ProjectResolverContext) =
            "${GradleProjectResolverUtil.getModuleId(resolverCtx, gradleModule)}:$target"

        private fun getExternalName(gradleModule: IdeaModule, target: String, suffix: String) =
            "${gradleModule.name}:${target}$suffix"

        private fun getInternalName(
            gradleModule: IdeaModule,
            externalProject: ExternalProject,
            target: String,
            suffix: String,
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
            moduleName.append(target + suffix)
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

                fun createRoot(dir: File, type: ExternalSystemSourceType) {
                    // When there are multiple source roots, rootPath can be set to dir.path (like in KotlinGradleMPPProjectResolver), 
                    // they will be merged into a single content root in GradleProjectResolver.mergeModuleContentRoots
                    val contentRoot = ContentRootData(GradleConstants.SYSTEM_ID, dir.parentFile.path)
                    contentRoot.storePath(type, dir.path)
                    dataNode.createChild(ProjectKeys.CONTENT_ROOT, contentRoot)
                }

                val moduleId = dataNode.data.id
                if (moduleId.endsWith("Main")) {
                    target.sourceFolders.forEach { createRoot(it, ExternalSystemSourceType.SOURCE) }
                }
                if (moduleId.endsWith("Test")) {
                    target.testFolders.forEach { createRoot(it, ExternalSystemSourceType.TEST) }
                }
            }

            ideModule.createChild(APPLE_PROJECT, AppleProjectModelImpl(targets))
        }
    }
}
