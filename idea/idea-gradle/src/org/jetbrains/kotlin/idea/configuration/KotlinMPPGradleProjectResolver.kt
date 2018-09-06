/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.google.common.graph.GraphBuilder
import com.google.common.graph.Graphs
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.*
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants
import com.intellij.openapi.externalSystem.util.Order
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.PathUtilRt
import com.intellij.util.SmartList
import com.intellij.util.containers.MultiMap
import com.intellij.util.text.VersionComparatorUtil
import org.gradle.tooling.model.UnsupportedMethodException
import org.gradle.tooling.model.idea.IdeaContentRoot
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.ManualLanguageFeatureSetting
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.idea.platform.IdePlatformKindTooling
import org.jetbrains.plugins.gradle.model.*
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolver
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolver.CONFIGURATION_ARTIFACTS
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolver.MODULES_OUTPUTS
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil.buildDependencies
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil.getModuleId
import org.jetbrains.plugins.gradle.service.project.ProjectResolverContext
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File

@Order(ExternalSystemConstants.UNORDERED + 1)
open class KotlinMPPGradleProjectResolver : AbstractProjectResolverExtension() {
    override fun getToolingExtensionsClasses(): Set<Class<out Any>> {
        return setOf(KotlinMPPGradleModelBuilder::class.java, Unit::class.java)
    }

    override fun getExtraProjectModelClasses(): Set<Class<out Any>> {
        return setOf(KotlinMPPGradleModel::class.java)
    }

    override fun createModule(gradleModule: IdeaModule, projectDataNode: DataNode<ProjectData>): DataNode<ModuleData> {
        return super.createModule(gradleModule, projectDataNode).also {
            initializeModuleData(gradleModule, it, projectDataNode, resolverCtx)
        }
    }

    override fun populateModuleContentRoots(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        if (resolverCtx.getExtraProject(gradleModule, KotlinMPPGradleModel::class.java) == null) {
            return super.populateModuleContentRoots(gradleModule, ideModule)
        }
        populateContentRoots(gradleModule, ideModule, resolverCtx)
    }

    override fun populateModuleCompileOutputSettings(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        if (resolverCtx.getExtraProject(gradleModule, KotlinMPPGradleModel::class.java) == null) {
            super.populateModuleCompileOutputSettings(gradleModule, ideModule)
        }

        val mppModel = resolverCtx.getExtraProject(gradleModule, KotlinMPPGradleModel::class.java) ?: return
        val ideaOutDir = File(ideModule.data.linkedExternalProjectPath, "out")
        val projectDataNode = ideModule.getDataNode(ProjectKeys.PROJECT)!!
        val moduleOutputsMap = projectDataNode.getUserData(MODULES_OUTPUTS)!!
        val outputDirs = HashSet<String>()
        processCompilations(gradleModule, mppModel, ideModule, resolverCtx) { dataNode, compilation ->
            var gradleOutputMap = dataNode.getUserData(GradleProjectResolver.GRADLE_OUTPUTS)
            if (gradleOutputMap == null) {
                gradleOutputMap = MultiMap.create()
                dataNode.putUserData(GradleProjectResolver.GRADLE_OUTPUTS, gradleOutputMap)
            }

            val moduleData = dataNode.data

            with(compilation.output) {
                effectiveClassesDir?.let {
                    moduleData.isInheritProjectCompileOutputPath = false
                    moduleData.setCompileOutputPath(compilation.sourceType, it.absolutePath)
                    for (gradleOutputDir in classesDirs) {
                        recordOutputDir(gradleOutputDir, it, compilation.sourceType, moduleData, moduleOutputsMap, gradleOutputMap)
                    }
                }
                resourcesDir?.let {
                    moduleData.setCompileOutputPath(compilation.resourceType, it.absolutePath)
                    recordOutputDir(it, it, compilation.resourceType, moduleData, moduleOutputsMap, gradleOutputMap)
                }
            }
        }
        if (outputDirs.any { FileUtil.isAncestor(ideaOutDir, File(it), false) }) {
            excludeOutDir(ideModule, ideaOutDir)
        }
    }

    override fun populateModuleDependencies(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>, ideProject: DataNode<ProjectData>) {
        if (resolverCtx.getExtraProject(gradleModule, KotlinMPPGradleModel::class.java) == null) {
            super.populateModuleDependencies(gradleModule, ideModule, ideProject)
        }
        populateModuleDependencies(gradleModule, ideProject, ideModule, resolverCtx)
    }

    private fun recordOutputDir(
        gradleOutputDir: File,
        effectiveOutputDir: File,
        sourceType: ExternalSystemSourceType,
        moduleData: GradleSourceSetData,
        moduleOutputsMap: MutableMap<String, Pair<String, ExternalSystemSourceType>>,
        gradleOutputMap: MultiMap<ExternalSystemSourceType, String>
    ) {
        val gradleOutputPath = ExternalSystemApiUtil.toCanonicalPath(gradleOutputDir.absolutePath)
        gradleOutputMap.putValue(sourceType, gradleOutputPath)
        if (gradleOutputDir.path != effectiveOutputDir.path) {
            moduleOutputsMap[gradleOutputPath] = Pair(moduleData.id, sourceType)
        }
    }

    private fun excludeOutDir(ideModule: DataNode<ModuleData>, ideaOutDir: File) {
        val contentRootDataDataNode = ExternalSystemApiUtil.find(ideModule, ProjectKeys.CONTENT_ROOT)

        val excludedContentRootData: ContentRootData
        if (contentRootDataDataNode == null || !FileUtil.isAncestor(File(contentRootDataDataNode.data.rootPath), ideaOutDir, false)) {
            excludedContentRootData = ContentRootData(GradleConstants.SYSTEM_ID, ideaOutDir.absolutePath)
            ideModule.createChild(ProjectKeys.CONTENT_ROOT, excludedContentRootData)
        } else {
            excludedContentRootData = contentRootDataDataNode.data
        }

        excludedContentRootData.storePath(ExternalSystemSourceType.EXCLUDED, ideaOutDir.absolutePath)
    }

    companion object {
        fun initializeModuleData(
            gradleModule: IdeaModule,
            mainModuleNode: DataNode<ModuleData>,
            projectDataNode: DataNode<ProjectData>,
            resolverCtx: ProjectResolverContext
        ) {
            val mainModuleData = mainModuleNode.data
            val mainModuleConfigPath = mainModuleData.linkedExternalProjectPath
            val mainModuleFileDirectoryPath = mainModuleData.moduleFileDirectoryPath

            val externalProject = resolverCtx.getExtraProject(gradleModule, ExternalProject::class.java)
            val mppModel = resolverCtx.getExtraProject(gradleModule, KotlinMPPGradleModel::class.java)
            if (mppModel == null || externalProject == null) return

            val jdkName = gradleModule.jdkNameIfAny

            val moduleGroup: Array<String>? = if (!resolverCtx.isUseQualifiedModuleNames) {
                val gradlePath = gradleModule.gradleProject.path
                val isRootModule = gradlePath.isEmpty() || gradlePath == ":"
                if (isRootModule) {
                    arrayOf(mainModuleData.internalName)
                } else {
                    gradlePath.split(":").drop(1).toTypedArray()
                }
            } else null

            val sourceSetMap = projectDataNode.getUserData(GradleProjectResolver.RESOLVED_SOURCE_SETS)!!

            val sourceSetToCompilationData = LinkedHashMap<KotlinSourceSet, MutableSet<GradleSourceSetData>>()
            for (target in mppModel.targets) {
                if (target.platform == KotlinPlatform.ANDROID) continue
                if (target.name == KotlinTarget.METADATA_TARGET_NAME) continue
                val targetData = KotlinTargetData(target.name).also {
                    it.archiveFile = target.jar?.archiveFile
                }
                val targetDataNode = mainModuleNode.createChild<KotlinTargetData>(KotlinTargetData.KEY, targetData)

                val compilationIds = LinkedHashSet<String>()
                for (compilation in target.compilations) {
                    val moduleId = getKotlinModuleId(gradleModule, compilation, resolverCtx)
                    val existingSourceSetDataNode = sourceSetMap[moduleId]?.first
                    if (existingSourceSetDataNode?.kotlinSourceSet != null) continue

                    compilationIds += moduleId

                    val moduleExternalName = getExternalModuleName(gradleModule, compilation)
                    val moduleInternalName = getInternalModuleName(gradleModule, externalProject, compilation, resolverCtx)

                    val compilationData = existingSourceSetDataNode?.data ?: GradleSourceSetData(
                        moduleId, moduleExternalName, moduleInternalName, mainModuleFileDirectoryPath, mainModuleConfigPath
                    ).also {
                        it.group = externalProject.group
                        it.version = externalProject.version

                        when (compilation.name) {
                            KotlinCompilation.MAIN_COMPILATION_NAME -> {
                                it.publication = ProjectId(externalProject.group, externalProject.name, externalProject.version)
                            }
                            KotlinCompilation.TEST_COMPILATION_NAME -> {
                                it.productionModuleId = getInternalModuleName(
                                    gradleModule,
                                    externalProject,
                                    compilation,
                                    resolverCtx,
                                    KotlinCompilation.MAIN_COMPILATION_NAME
                                )
                            }
                        }

                        it.ideModuleGroup = moduleGroup
                        it.sdkName = jdkName
                    }

                    val kotlinSourceSet = createSourceSetInfo(compilation, gradleModule, resolverCtx)

                    if (compilation.platform == KotlinPlatform.JVM || compilation.platform == KotlinPlatform.ANDROID) {
                        compilationData.targetCompatibility = (kotlinSourceSet.compilerArguments as? K2JVMCompilerArguments)?.jvmTarget
                    }

                    for (sourceSet in compilation.sourceSets) {
                        sourceSetToCompilationData.getOrPut(sourceSet) { LinkedHashSet() } += compilationData
                    }

                    val compilationDataNode =
                        (existingSourceSetDataNode ?: mainModuleNode.createChild(GradleSourceSetData.KEY, compilationData)).also {
                            it.kotlinSourceSet = kotlinSourceSet
                            it.kotlinTargetDataNode = targetDataNode
                        }
                    if (existingSourceSetDataNode == null) {
                        sourceSetMap[moduleId] = Pair(compilationDataNode, createExternalSourceSet(compilation, compilationData))
                    }
                }

                targetData.moduleIds = compilationIds
            }

            for (sourceSet in mppModel.sourceSets.values) {
                if (sourceSet.platform == KotlinPlatform.ANDROID) continue
                val moduleId = getKotlinModuleId(gradleModule, sourceSet, resolverCtx)
                val existingSourceSetDataNode = sourceSetMap[moduleId]?.first
                if (existingSourceSetDataNode?.kotlinSourceSet != null) continue

                val moduleExternalName = getExternalModuleName(gradleModule, sourceSet)
                val moduleInternalName = getInternalModuleName(gradleModule, externalProject, sourceSet, resolverCtx)

                val sourceSetData = existingSourceSetDataNode?.data ?: GradleSourceSetData(
                    moduleId, moduleExternalName, moduleInternalName, mainModuleFileDirectoryPath, mainModuleConfigPath
                ).also {
                    it.group = externalProject.group
                    it.version = externalProject.version

                    val name = sourceSet.name
                    val baseName = name.removeSuffix("Test")
                    if (baseName != name) {
                        it.productionModuleId = getInternalModuleName(
                            gradleModule,
                            externalProject,
                            sourceSet,
                            resolverCtx,
                            baseName + "Main"
                        )
                    }

                    it.ideModuleGroup = moduleGroup
                    it.sdkName = jdkName
                    it.targetCompatibility = sourceSetToCompilationData[sourceSet]
                        ?.mapNotNull { it.targetCompatibility }
                        ?.minWith(VersionComparatorUtil.COMPARATOR)
                }

                val kotlinSourceSet = createSourceSetInfo(sourceSet, gradleModule, resolverCtx)

                val sourceSetDataNode =
                    (existingSourceSetDataNode ?: mainModuleNode.createChild(GradleSourceSetData.KEY, sourceSetData)).also {
                        it.kotlinSourceSet = kotlinSourceSet
                    }
                if (existingSourceSetDataNode == null) {
                    sourceSetMap[moduleId] = Pair(sourceSetDataNode, createExternalSourceSet(sourceSet, sourceSetData))
                }
            }

            with(projectDataNode.data) {
                if (mainModuleData.linkedExternalProjectPath == linkedExternalProjectPath) {
                    group = mainModuleData.group
                    version = mainModuleData.version
                }
            }

            mainModuleNode.coroutines = mppModel.extraFeatures.coroutinesState
        }

        fun populateContentRoots(
            gradleModule: IdeaModule,
            ideModule: DataNode<ModuleData>,
            resolverCtx: ProjectResolverContext
        ) {
            val mppModel = resolverCtx.getExtraProject(gradleModule, KotlinMPPGradleModel::class.java) ?: return
            processSourceSets(gradleModule, mppModel, ideModule, resolverCtx) { dataNode, sourceSet ->
                createContentRootData(sourceSet.sourceDirs, sourceSet.sourceType, dataNode)
                createContentRootData(sourceSet.resourceDirs, sourceSet.resourceType, dataNode)
            }

            for (gradleContentRoot in gradleModule.contentRoots ?: emptySet<IdeaContentRoot?>()) {
                if (gradleContentRoot == null) continue

                val rootDirectory = gradleContentRoot.rootDirectory ?: continue
                val ideContentRoot = ContentRootData(GradleConstants.SYSTEM_ID, rootDirectory.absolutePath).also { ideContentRoot ->
                    (gradleContentRoot.excludeDirectories ?: emptySet()).forEach { file ->
                        ideContentRoot.storePath(ExternalSystemSourceType.EXCLUDED, file.absolutePath)
                    }
                }
                ideModule.createChild(ProjectKeys.CONTENT_ROOT, ideContentRoot)
            }
        }

        fun populateModuleDependencies(
            gradleModule: IdeaModule,
            ideProject: DataNode<ProjectData>,
            ideModule: DataNode<ModuleData>,
            resolverCtx: ProjectResolverContext
        ) {
            val mppModel = resolverCtx.getExtraProject(gradleModule, KotlinMPPGradleModel::class.java) ?: return
            val sourceSetMap = ideProject.getUserData(GradleProjectResolver.RESOLVED_SOURCE_SETS) ?: return
            val artifactsMap = ideProject.getUserData(CONFIGURATION_ARTIFACTS) ?: return
            val processedModuleIds = HashSet<String>()
            processCompilations(gradleModule, mppModel, ideModule, resolverCtx) { dataNode, compilation ->
                if (processedModuleIds.add(getKotlinModuleId(gradleModule, compilation, resolverCtx))) {
                    buildDependencies(
                        resolverCtx,
                        sourceSetMap,
                        artifactsMap,
                        dataNode,
                        preprocessDependencies(compilation.dependencies, ideProject),
                        ideProject
                    )
                    for (sourceSet in compilation.sourceSets) {
                        if (sourceSet.fullName() == compilation.fullName()) continue
                        val targetDataNode = getSiblingKotlinModuleData(sourceSet, gradleModule, ideModule, resolverCtx) ?: continue
                        addDependency(dataNode, targetDataNode)
                    }
                }
            }
            val sourceSetGraph = GraphBuilder.directed().build<KotlinModule>()
            processSourceSets(gradleModule, mppModel, ideModule, resolverCtx) { dataNode, sourceSet ->
                sourceSetGraph.addNode(sourceSet)
                val productionSourceSet = dataNode.data.productionModuleId
                    ?.let { ideModule.findChildModuleByInternalName(it) }
                    ?.kotlinSourceSet
                    ?.kotlinModule
                    ?.toSourceSet(mppModel)
                if (productionSourceSet != null) {
                    sourceSetGraph.putEdge(sourceSet, productionSourceSet)
                }
                for (targetSourceSetName in sourceSet.dependsOnSourceSets) {
                    val targetSourceSet = mppModel.sourceSets[targetSourceSetName] ?: continue
                    sourceSetGraph.putEdge(sourceSet, targetSourceSet)
                }
            }
            val closedSourceSetGraph = Graphs.transitiveClosure(sourceSetGraph)
            for (sourceSet in closedSourceSetGraph.nodes()) {
                val fromDataNode = getSiblingKotlinModuleData(sourceSet, gradleModule, ideModule, resolverCtx) ?: continue
                val dependeeSourceSets = closedSourceSetGraph.successors(sourceSet)
                for (dependeeSourceSet in dependeeSourceSets) {
                    val toDataNode = getSiblingKotlinModuleData(dependeeSourceSet, gradleModule, ideModule, resolverCtx) ?: continue
                    addDependency(fromDataNode, toDataNode)
                }
                val sourceSetInfo = fromDataNode.kotlinSourceSet
                if (sourceSetInfo != null && sourceSetInfo.kotlinModule is KotlinCompilation) {
                    val selfName = sourceSetInfo.kotlinModule.fullName()
                    sourceSetInfo.sourceSetIdsByName = dependeeSourceSets
                        .asSequence()
                        .filter { it.fullName() != selfName }
                        .map { it.name to getKotlinModuleId(gradleModule, it, resolverCtx) }
                        .toMap()
                }
                if (processedModuleIds.add(getKotlinModuleId(gradleModule, sourceSet, resolverCtx))) {
                    val mergedDependencies = LinkedHashSet<KotlinDependency>().apply {
                        addAll(sourceSet.dependencies)
                        dependeeSourceSets.flatMapTo(this) { it.dependencies }
                    }
                    buildDependencies(
                        resolverCtx,
                        sourceSetMap,
                        artifactsMap,
                        fromDataNode,
                        preprocessDependencies(mergedDependencies, ideProject),
                        ideProject
                    )
                }
            }
        }

        private fun KotlinModule.toSourceSet(mppModel: KotlinMPPGradleModel) = when (this) {
            is KotlinSourceSet -> this
            is KotlinCompilation -> mppModel.sourceSets[fullName()]
            else -> null
        }

        private fun preprocessDependencies(
            dependencies: Set<KotlinDependency>,
            ideProject: DataNode<ProjectData>
        ): List<ExternalDependency> {
            return dependencies
                .groupBy { it.id }
                .mapValues { it.value.firstOrNull { it.scope == "COMPILE" } ?: it.value.lastOrNull() }
                .values
                .filterNotNull()
                .map { adjustDependency(it, ideProject) }
        }

        private fun adjustDependency(
            dependency: ExternalDependency,
            ideProject: DataNode<ProjectData>
        ): ExternalDependency {
            if (dependency !is ExternalProjectDependency) return dependency
            val projectPath = dependency.projectPath
            val classifier = dependency.classifier ?: return dependency
            val targetModuleNode = ExternalSystemApiUtil.findFirstRecursively(ideProject) {
                (it.data as? ModuleData)?.id == projectPath
            } ?: return dependency
            val targetCompilation = ExternalSystemApiUtil
                .getChildren(targetModuleNode, GradleSourceSetData.KEY)
                .mapNotNull { sourceSetNode ->
                    sourceSetNode.kotlinSourceSet?.kotlinModule as? KotlinCompilation
                }
                .firstOrNull {
                    it.name == KotlinCompilation.MAIN_COMPILATION_NAME && it.target.disambiguationClassifier == classifier
                } ?: return dependency
            return DefaultExternalProjectDependency(dependency).also {
                it.configurationName = targetCompilation.fullName()
            }
        }

        private fun addDependency(fromModule: DataNode<*>, toModule: DataNode<*>) {
            val fromData = fromModule.data as? ModuleData ?: return
            val toData = toModule.data as? ModuleData ?: return
            val moduleDependencyData = ModuleDependencyData(fromData, toData).also {
                it.scope = DependencyScope.COMPILE
                it.isExported = false
            }
            fromModule.createChild(ProjectKeys.MODULE_DEPENDENCY, moduleDependencyData)
        }

        private fun getSiblingKotlinModuleData(
            kotlinModule: KotlinModule,
            gradleModule: IdeaModule,
            ideModule: DataNode<ModuleData>,
            resolverCtx: ProjectResolverContext
        ): DataNode<out ModuleData>? {
            val usedModuleId = getKotlinModuleId(gradleModule, kotlinModule, resolverCtx)
            return ideModule.findChildModuleById(usedModuleId)
        }

        private fun createContentRootData(sourceDirs: Set<File>, sourceType: ExternalSystemSourceType, parentNode: DataNode<*>) {
            for (sourceDir in sourceDirs) {
                val contentRootData = ContentRootData(GradleConstants.SYSTEM_ID, sourceDir.absolutePath)
                contentRootData.storePath(sourceType, sourceDir.absolutePath)
                parentNode.createChild(ProjectKeys.CONTENT_ROOT, contentRootData)
            }
        }

        private fun processSourceSets(
            gradleModule: IdeaModule,
            mppModel: KotlinMPPGradleModel,
            ideModule: DataNode<ModuleData>,
            resolverCtx: ProjectResolverContext,
            processor: (DataNode<GradleSourceSetData>, KotlinSourceSet) -> Unit
        ) {
            val sourceSetsMap = HashMap<String, DataNode<GradleSourceSetData>>()
            for (dataNode in ExternalSystemApiUtil.findAll(ideModule, GradleSourceSetData.KEY)) {
                if (dataNode.kotlinSourceSet != null) {
                    sourceSetsMap[dataNode.data.id] = dataNode
                }
            }
            for (sourceSet in mppModel.sourceSets.values) {
                if (sourceSet.platform == KotlinPlatform.ANDROID) continue
                val moduleId = getKotlinModuleId(gradleModule, sourceSet, resolverCtx)
                val moduleDataNode = sourceSetsMap[moduleId] ?: continue
                processor(moduleDataNode, sourceSet)
            }
        }

        private fun processCompilations(
            gradleModule: IdeaModule,
            mppModel: KotlinMPPGradleModel,
            ideModule: DataNode<ModuleData>,
            resolverCtx: ProjectResolverContext,
            processor: (DataNode<GradleSourceSetData>, KotlinCompilation) -> Unit
        ) {
            val sourceSetsMap = HashMap<String, DataNode<GradleSourceSetData>>()
            for (dataNode in ExternalSystemApiUtil.findAll(ideModule, GradleSourceSetData.KEY)) {
                if (dataNode.kotlinSourceSet != null) {
                    sourceSetsMap[dataNode.data.id] = dataNode
                }
            }
            for (target in mppModel.targets) {
                if (target.platform == KotlinPlatform.ANDROID) continue
                for (compilation in target.compilations) {
                    val moduleId = getKotlinModuleId(gradleModule, compilation, resolverCtx)
                    val moduleDataNode = sourceSetsMap[moduleId] ?: continue
                    processor(moduleDataNode, compilation)
                }
            }
        }

        private val IdeaModule.jdkNameIfAny
            get() = try {
                jdkName
            } catch (e: UnsupportedMethodException) {
                null
            }

        private fun getExternalModuleName(gradleModule: IdeaModule, kotlinModule: KotlinModule) =
            gradleModule.name + ":" + kotlinModule.fullName()

        private fun getInternalModuleName(
            gradleModule: IdeaModule,
            externalProject: ExternalProject,
            kotlinModule: KotlinModule,
            resolverCtx: ProjectResolverContext,
            actualName: String = kotlinModule.name
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
            moduleName.append(kotlinModule.fullName(actualName))
            return PathUtilRt.suggestFileName(moduleName.toString(), true, false)
        }

        private fun createExternalSourceSet(compilation: KotlinCompilation, compilationData: GradleSourceSetData): ExternalSourceSet {
            return DefaultExternalSourceSet().also { sourceSet ->
                val effectiveClassesDir = compilation.output.effectiveClassesDir
                val resourcesDir = compilation.output.resourcesDir

                sourceSet.name = compilation.fullName()
                sourceSet.targetCompatibility = compilationData.targetCompatibility
                sourceSet.dependencies += compilation.dependencies
                val sourcesWithTypes = SmartList<kotlin.Pair<IExternalSystemSourceType, ExternalSourceDirectorySet>>()
                if (effectiveClassesDir != null) {
                    sourcesWithTypes += compilation.sourceType to DefaultExternalSourceDirectorySet().also { dirSet ->
                        dirSet.outputDir = effectiveClassesDir
                        dirSet.srcDirs = compilation.sourceSets.flatMapTo(LinkedHashSet()) { it.sourceDirs }
                        dirSet.gradleOutputDirs += compilation.output.classesDirs
                        dirSet.setInheritedCompilerOutput(false)
                    }
                }
                if (resourcesDir != null) {
                    sourcesWithTypes += compilation.resourceType to DefaultExternalSourceDirectorySet().also { dirSet ->
                        dirSet.outputDir = resourcesDir
                        dirSet.srcDirs = compilation.sourceSets.flatMapTo(LinkedHashSet()) { it.resourceDirs }
                        dirSet.gradleOutputDirs += resourcesDir
                        dirSet.setInheritedCompilerOutput(false)
                    }
                }
                sourceSet.sources = sourcesWithTypes.toMap()
            }
        }

        private fun createExternalSourceSet(ktSourceSet: KotlinSourceSet, ktSourceSetData: GradleSourceSetData): ExternalSourceSet {
            return DefaultExternalSourceSet().also { sourceSet ->
                sourceSet.name = ktSourceSet.name
                sourceSet.targetCompatibility = ktSourceSetData.targetCompatibility
                sourceSet.dependencies += ktSourceSet.dependencies
                sourceSet.sources = linkedMapOf<IExternalSystemSourceType, ExternalSourceDirectorySet>(
                    ktSourceSet.sourceType to DefaultExternalSourceDirectorySet().also { dirSet ->
                        dirSet.srcDirs = ktSourceSet.sourceDirs
                    },
                    ktSourceSet.resourceType to DefaultExternalSourceDirectorySet().also { dirSet ->
                        dirSet.srcDirs = ktSourceSet.resourceDirs
                    }
                )
            }
        }

        private val KotlinModule.sourceType
            get() = if (isTestModule) ExternalSystemSourceType.TEST else ExternalSystemSourceType.SOURCE

        private val KotlinModule.resourceType
            get() = if (isTestModule) ExternalSystemSourceType.TEST_RESOURCE else ExternalSystemSourceType.RESOURCE

        private fun createSourceSetInfo(
            sourceSet: KotlinSourceSet,
            gradleModule: IdeaModule,
            resolverCtx: ProjectResolverContext
        ): KotlinSourceSetInfo {
            return KotlinSourceSetInfo(sourceSet).also { info ->
                val languageSettings = sourceSet.languageSettings
                info.moduleId = getKotlinModuleId(gradleModule, sourceSet, resolverCtx)
                info.platform = sourceSet.platform
                info.isTestModule = sourceSet.isTestModule
                info.compilerArguments = createCompilerArguments(emptyList(), sourceSet.platform).also {
                    it.multiPlatform = true
                    it.languageVersion = languageSettings.languageVersion
                    it.apiVersion = languageSettings.apiVersion
                    it.progressiveMode = languageSettings.isProgressiveMode
                    it.internalArguments = languageSettings.enabledLanguageFeatures.mapNotNull {
                        val feature = LanguageFeature.fromString(it) ?: return@mapNotNull null
                        val arg = "-XXLanguage:+$it"
                        ManualLanguageFeatureSetting(feature, LanguageFeature.State.ENABLED, arg)
                    }
                }
            }
        }

        fun createSourceSetInfo(
            compilation: KotlinCompilation,
            gradleModule: IdeaModule,
            resolverCtx: ProjectResolverContext
        ): KotlinSourceSetInfo {
            return KotlinSourceSetInfo(compilation).also {
                it.moduleId = getKotlinModuleId(gradleModule, compilation, resolverCtx)
                it.platform = compilation.platform
                it.isTestModule = compilation.isTestModule
                it.compilerArguments = createCompilerArguments(compilation.arguments.currentArguments, compilation.platform).also {
                    it.multiPlatform = true
                }
                it.dependencyClasspath = compilation.dependencyClasspath
                it.defaultCompilerArguments = createCompilerArguments(compilation.arguments.defaultArguments, compilation.platform)
            }
        }

        private fun createCompilerArguments(args: List<String>, platform: KotlinPlatform): CommonCompilerArguments {
            val compilerArguments = IdePlatformKindTooling.getTooling(platform).kind.argumentsClass.newInstance()
            parseCommandLineArguments(args, compilerArguments)
            return compilerArguments
        }

        private fun KotlinModule.fullName(simpleName: String = name) = when (this) {
            is KotlinCompilation -> compilationFullName(simpleName, target.disambiguationClassifier)
            else -> simpleName
        }

        private fun getKotlinModuleId(gradleModule: IdeaModule, kotlinModule: KotlinModule, resolverCtx: ProjectResolverContext) =
            getModuleId(resolverCtx, gradleModule) + ":" + kotlinModule.fullName()
    }
}