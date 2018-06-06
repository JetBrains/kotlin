/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.maven

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.SourceFolder
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.XmlElementFactory
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlText
import com.intellij.util.xml.GenericDomValue
import org.jetbrains.idea.maven.dom.MavenDomElement
import org.jetbrains.idea.maven.dom.MavenDomUtil
import org.jetbrains.idea.maven.dom.model.*
import org.jetbrains.idea.maven.model.MavenId
import org.jetbrains.idea.maven.model.MavenPlugin
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.idea.maven.utils.MavenArtifactScope
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.kotlin.config.KotlinSourceRootType
import org.jetbrains.kotlin.idea.configuration.RepositoryDescription
import org.jetbrains.kotlin.idea.maven.configuration.KotlinMavenConfigurator
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.utils.SmartList
import java.util.*

fun kotlinPluginId(version: String?) = MavenId(KotlinMavenConfigurator.GROUP_ID, KotlinMavenConfigurator.MAVEN_PLUGIN_ID, version)


class PomFile private constructor(private val xmlFile: XmlFile, val domModel: MavenDomProjectModel) {
    constructor(xmlFile: XmlFile) : this(
        xmlFile,
        MavenDomUtil.getMavenDomProjectModel(xmlFile.project, xmlFile.virtualFile)
                ?: throw IllegalStateException("No DOM model found for pom ${xmlFile.name}")
    )

    private val nodesByName = HashMap<String, XmlTag>()
    private val projectElement: XmlTag

    init {
        var projectElement: XmlTag? = null

        xmlFile.document?.accept(object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)

                if (element is XmlTag && element.localName in recommendedElementsOrder && element.parent === projectElement) {
                    nodesByName[element.localName] = element
                } else if (element is XmlTag && element.localName == "project") {
                    projectElement = element
                    element.acceptChildren(this)
                } else {
                    element.acceptChildren(this)
                }
            }
        })

        require(projectElement != null) { "pom file should have project element" }
        this.projectElement = projectElement!!
    }

    fun addProperty(name: String, value: String) {
        val properties = ensureElement(projectElement, "properties")
        val existing = properties.children.filterIsInstance<XmlTag>().filter { it.localName == name }

        if (existing.isNotEmpty()) {
            for (tag in existing) {
                val textNode = tag.children.filterIsInstance<XmlText>().firstOrNull()
                if (textNode != null) {
                    textNode.value = value
                } else {
                    tag.replace(projectElement.createChildTag(name, value))
                }
            }
        } else {
            properties.add(projectElement.createChildTag(name, value))
        }
    }

    fun findProperty(name: String): XmlTag? {
        val propertiesNode = nodesByName["properties"] ?: return null
        return propertiesNode.findFirstSubTag(name)
    }

    fun addDependency(
        artifact: MavenId,
        scope: MavenArtifactScope? = null,
        classifier: String? = null,
        optional: Boolean = false,
        systemPath: String? = null
    ): MavenDomDependency {
        require(systemPath == null || scope == MavenArtifactScope.SYSTEM) { "systemPath is only applicable for system scope dependency" }
        require(artifact.groupId != null) { "groupId shouldn't be null" }
        require(artifact.artifactId != null) { "artifactId shouldn't be null" }

        ensureDependencies()
        val versionless = artifact.withNoVersion().withoutJDKSpecificSuffix()
        val dependency = domModel.dependencies.dependencies.firstOrNull { it.matches(versionless) } ?: domModel.dependencies.addDependency()
        dependency.groupId.stringValue = artifact.groupId
        dependency.artifactId.stringValue = artifact.artifactId
        dependency.version.stringValue = artifact.version
        dependency.classifier.stringValue = classifier

        if (scope != null && scope != MavenArtifactScope.COMPILE) {
            dependency.scope.stringValue = scope.name.toLowerCase()
        }

        if (optional) {
            dependency.optional.value = optional
        }

        dependency.systemPath.stringValue = systemPath
        dependency.ensureTagExists()

        return dependency
    }

    fun addKotlinPlugin(version: String?) = addPlugin(kotlinPluginId(version))

    fun addPlugin(artifact: MavenId): MavenDomPlugin {
        ensureBuild()

        val groupArtifact = artifact.withNoVersion()
        val plugin = findPlugin(groupArtifact) ?: domModel.build.plugins.addPlugin()
        plugin.groupId.stringValue = artifact.groupId
        plugin.artifactId.stringValue = artifact.artifactId
        if (artifact.version != null) {
            plugin.version.stringValue = artifact.version
        }
        plugin.ensureTagExists()

        return plugin
    }

    fun findPlugin(groupArtifact: MavenId) = domModel.build.plugins.plugins.firstOrNull { it.matches(groupArtifact) }

    fun isPluginAfter(plugin: MavenDomPlugin, referencePlugin: MavenDomPlugin): Boolean {
        require(plugin.parent === referencePlugin.parent) { "Plugins should be siblings" }
        require(plugin !== referencePlugin)

        val referenceElement = referencePlugin.xmlElement!!
        var e: PsiElement = plugin.xmlElement!!

        while (e !== referenceElement) {
            val prev = e.prevSibling ?: return false
            e = prev
        }

        return true
    }

    private fun ensurePluginAfter(plugin: MavenDomPlugin, referencePlugin: MavenDomPlugin): MavenDomPlugin {
        if (!isPluginAfter(plugin, referencePlugin)) {
            // rearrange
            val referenceElement = referencePlugin.xmlElement!!
            val newElement = referenceElement.parent.addAfter(plugin.xmlElement!!, referenceElement)
            plugin.xmlTag.delete()

            return domModel.build.plugins.plugins.single { it.xmlElement == newElement }
        }

        return plugin
    }

    fun findKotlinPlugins() = domModel.build.plugins.plugins.filter { it.isKotlinMavenPlugin() }
    fun findKotlinExecutions(vararg goals: String) = findKotlinExecutions().filter { it.goals.goals.any { it.rawText in goals } }
    fun findKotlinExecutions() = findKotlinPlugins().flatMap { it.executions.executions }

    private fun findExecutions(plugin: MavenDomPlugin) = plugin.executions.executions
    fun findExecutions(plugin: MavenDomPlugin, vararg goals: String) =
        findExecutions(plugin).filter { it.goals.goals.any { it.rawText in goals } }

    fun addExecution(plugin: MavenDomPlugin, executionId: String, phase: String, goals: List<String>): MavenDomPluginExecution {
        require(executionId.isNotEmpty()) { "executionId shouldn't be empty" }
        require(phase.isNotEmpty()) { "phase shouldn't be empty" }

        val execution = plugin.executions.executions.firstOrNull { it.id.stringValue == executionId } ?: plugin.executions.addExecution()
        execution.id.stringValue = executionId
        execution.phase.stringValue = phase

        val existingGoals = execution.goals.goals.mapNotNull { it.rawText }
        for (goal in goals.filter { it !in existingGoals }) {
            val goalTag = execution.goals.ensureTagExists().createChildTag("goal", goal)
            execution.goals.xmlTag.add(goalTag)
        }

        return execution
    }

    fun addKotlinExecution(
        module: Module,
        plugin: MavenDomPlugin,
        executionId: String,
        phase: String,
        isTest: Boolean,
        goals: List<String>
    ) {
        val execution = addExecution(plugin, executionId, phase, goals)

        val sourceDirs = ModuleRootManager.getInstance(module)
            .contentEntries
            .flatMap { it.sourceFolders.filter { it.isRelatedSourceRoot(isTest) } }
            .mapNotNull { it.file }
            .mapNotNull { VfsUtilCore.getRelativePath(it, xmlFile.virtualFile.parent, '/') }

        executionSourceDirs(execution, sourceDirs)
    }

    fun isPluginExecutionMissing(plugin: MavenPlugin?, excludedExecutionId: String, goal: String) =
        plugin == null || plugin.executions.none { it.executionId != excludedExecutionId && goal in it.goals }

    fun addJavacExecutions(module: Module, kotlinPlugin: MavenDomPlugin) {
        val javacPlugin = ensurePluginAfter(addPlugin(MavenId("org.apache.maven.plugins", "maven-compiler-plugin", null)), kotlinPlugin)

        val project: MavenProject =
            MavenProjectsManager.getInstance(module.project).findProject(module) ?: run {
                if (ApplicationManager.getApplication().isUnitTestMode) {
                    LOG.warn("WARNING: Bad project configuration in tests. Javac execution configuration was skipped.")
                    return
                }
                error("Can't find maven project for $module")
            }

        val plugin = project.findPlugin("org.apache.maven.plugins", "maven-compiler-plugin")

        if (isExecutionEnabled(plugin, "default-compile")) {
            addExecution(javacPlugin, "default-compile", "none", emptyList())
        }

        if (isExecutionEnabled(plugin, "default-testCompile")) {
            addExecution(javacPlugin, "default-testCompile", "none", emptyList())
        }

        if (isPluginExecutionMissing(plugin, "default-compile", "compile")) {
            addExecution(javacPlugin, "compile", PomFile.DefaultPhases.Compile, listOf("compile"))
        }

        if (isPluginExecutionMissing(plugin, "default-testCompile", "testCompile")) {
            addExecution(javacPlugin, "testCompile", PomFile.DefaultPhases.TestCompile, listOf("testCompile"))
        }
    }

    fun isExecutionEnabled(plugin: MavenPlugin?, executionId: String): Boolean {
        if (plugin == null) {
            return true
        }

        if (domModel.build.plugins.plugins.any {
                it.groupId.stringValue == "org.apache.maven.plugins"
                        && it.artifactId.stringValue == "maven-compiler-plugin"
                        && it.executions.executions.any { it.id.stringValue == executionId && it.phase.stringValue == DefaultPhases.None }
            }) {
            return false
        }

        // TODO: getPhase has been added as per https://youtrack.jetbrains.com/issue/IDEA-153582 and available only in latest IDEAs
        return plugin.executions.filter { it.executionId == executionId }.all { execution ->
            execution::class.java.methods.filter { it.name == "getPhase" && it.parameterTypes.isEmpty() }
                .all { it.invoke(execution) == DefaultPhases.None }
        }
    }

    fun executionSourceDirs(execution: MavenDomPluginExecution, sourceDirs: List<String>, forceSingleSource: Boolean = false) {
        ensureBuild()

        val isTest = execution.goals.goals.any { it.stringValue == KotlinGoals.TestCompile || it.stringValue == KotlinGoals.TestJs }
        val defaultDir = if (isTest) "test" else "main"
        val singleDirectoryElement = if (isTest) {
            domModel.build.testSourceDirectory
        } else {
            domModel.build.sourceDirectory
        }

        if (sourceDirs.isEmpty() || sourceDirs.singleOrNull() == "src/$defaultDir/java") {
            execution.configuration.xmlTag?.findSubTags("sourceDirs")?.forEach { it.deleteCascade() }
            singleDirectoryElement.undefine()
        } else if (sourceDirs.size == 1 && !forceSingleSource) {
            singleDirectoryElement.stringValue = sourceDirs.single()
            execution.configuration.xmlTag?.findSubTags("sourceDirs")?.forEach { it.deleteCascade() }
        } else {
            val sourceDirsTag = executionConfiguration(execution, "sourceDirs")
            val newSourceDirsTag = execution.configuration.createChildTag("sourceDirs")
            for (dir in sourceDirs) {
                newSourceDirsTag.add(newSourceDirsTag.createChildTag("source", dir))
            }
            sourceDirsTag.replace(newSourceDirsTag)
        }
    }

    fun executionSourceDirs(execution: MavenDomPluginExecution): List<String> {
        return execution.configuration.xmlTag
            .getChildrenOfType<XmlTag>().firstOrNull { it.localName == "sourceDirs" }
            ?.getChildrenOfType<XmlTag>()
            ?.map { it.getChildrenOfType<XmlText>().joinToString("") { it.text } }
                ?: emptyList()
    }

    private fun executionConfiguration(execution: MavenDomPluginExecution, name: String): XmlTag {
        val configurationTag = execution.configuration.ensureTagExists()!!

        val existingTag = configurationTag.findSubTags(name).firstOrNull()
        if (existingTag != null) {
            return existingTag
        }

        val newTag = configurationTag.createChildTag(name)
        return configurationTag.add(newTag) as XmlTag
    }

    fun addPluginConfiguration(plugin: MavenDomPlugin, optionName: String, optionValue: String): XmlTag {
        val configurationTag = plugin.configuration.ensureTagExists()
        val existingTag = configurationTag.findFirstSubTag(optionName)
        if (existingTag != null) {
            existingTag.value.text = optionValue
        } else {
            configurationTag.add(configurationTag.createChildTag(optionName, optionValue))
        }
        return configurationTag
    }

    private fun addPluginRepository(
        id: String,
        name: String,
        url: String,
        snapshots: Boolean = false,
        releases: Boolean = true
    ): MavenDomRepository {
        ensurePluginRepositories()

        return addRepository(
            id,
            name,
            url,
            snapshots,
            releases,
            { domModel.pluginRepositories.pluginRepositories },
            { domModel.pluginRepositories.addPluginRepository() })
    }

    fun addPluginRepository(description: RepositoryDescription) {
        addPluginRepository(description.id, description.name, description.url, description.isSnapshot, true)
    }

    private fun addLibraryRepository(
        id: String,
        name: String,
        url: String,
        snapshots: Boolean = false,
        releases: Boolean = true
    ): MavenDomRepository {
        ensureRepositories()

        return addRepository(
            id,
            name,
            url,
            snapshots,
            releases,
            { domModel.repositories.repositories },
            { domModel.repositories.addRepository() })
    }

    fun addLibraryRepository(description: RepositoryDescription) {
        addLibraryRepository(description.id, description.name, description.url, description.isSnapshot, true)
    }

    private fun addRepository(
        id: String,
        name: String,
        url: String,
        snapshots: Boolean,
        releases: Boolean,
        existing: () -> List<MavenDomRepository>,
        create: () -> MavenDomRepository
    ): MavenDomRepository {

        val repository =
            existing().firstOrNull { it.id.stringValue == id } ?: existing().firstOrNull { it.url.stringValue == url } ?: create()

        if (repository.id.isEmpty()) {
            repository.id.stringValue = id
        }
        if (repository.name.isEmpty()) {
            repository.name.stringValue = name
        }
        if (repository.url.isEmpty()) {
            repository.url.stringValue = url
        }
        repository.releases.enabled.value = repository.releases.enabled.value?.let { it || releases } ?: releases
        repository.snapshots.enabled.value = repository.snapshots.enabled.value?.let { it || snapshots } ?: snapshots

        repository.ensureTagExists()

        return repository
    }

    fun findDependencies(artifact: MavenId, scope: MavenArtifactScope? = null) =
        domModel.dependencies.findDependencies(artifact, scope)

    fun findDependencies(artifacts: List<MavenId>, scope: MavenArtifactScope? = null): List<MavenDomDependency> {
        return domModel.dependencies.findDependencies(artifacts, scope)
    }

    private fun ensureBuild(): XmlTag = ensureElement(projectElement, "build")

    private fun ensureDependencies(): XmlTag = ensureElement(projectElement, "dependencies")
    private fun ensurePluginRepositories(): XmlTag = ensureElement(projectElement, "pluginRepositories")
    private fun ensureRepositories(): XmlTag = ensureElement(projectElement, "repositories")

    private fun MavenDomPlugin.isKotlinMavenPlugin() = groupId.stringValue == KotlinMavenConfigurator.GROUP_ID
            && artifactId.stringValue == KotlinMavenConfigurator.MAVEN_PLUGIN_ID

    private fun MavenId.withNoVersion() = MavenId(groupId, artifactId, null)
    private fun MavenId.withoutJDKSpecificSuffix() = MavenId(
        groupId,
        artifactId?.substringBeforeLast("-jre")?.substringBeforeLast("-jdk"),
        null
    )

    private fun MavenDomElement.createChildTag(name: String, value: String? = null) = xmlTag.createChildTag(name, value)
    private fun XmlTag.createChildTag(name: String, value: String? = null) = createChildTag(name, namespace, value, false)!!

    private tailrec fun XmlTag.deleteCascade() {
        val oldParent = this.parentTag
        delete()

        if (oldParent != null && oldParent.subTags.isEmpty()) {
            oldParent.deleteCascade()
        }
    }

    private fun ensureElement(projectElement: XmlTag, localName: String): XmlTag {
        require(localName in recommendedElementsOrder) { "You can only ensure presence or the elements from the recommendation list" }

        return nodesByName.getOrPut(localName) {
            val tag = projectElement.createChildTag(localName, projectElement.namespace, null, false)!!
            val newTag = insertTagImpl(projectElement, tag)

            insertEmptyLines(newTag)

            newTag
        }
    }

    private fun insertTagImpl(projectElement: XmlTag, tag: XmlTag): XmlTag {
        val middle = recommendedOrderAsList.indexOf(tag.localName)
        require(middle != -1) { "You can only insert element from the recommendation list" }

        for (idx in middle - 1 downTo 0) {
            val reference = nodesByName[recommendedOrderAsList[idx]]
            if (reference != null) {
                return projectElement.addAfter(tag, reference) as XmlTag
            }
        }

        for (idx in middle + 1..recommendedOrderAsList.lastIndex) {
            val reference = nodesByName[recommendedOrderAsList[idx]]
            if (reference != null) {
                return projectElement.addBefore(tag, reference) as XmlTag
            }
        }

        return projectElement.add(tag) as XmlTag
    }

    private fun insertEmptyLines(node: XmlTag) {
        node.prevSibling?.let { before ->
            if (!(before.hasEmptyLine() || before.lastChild?.hasEmptyLine() == true)) {
                node.parent.addBefore(createEmptyLine(), node)
            }
        }
        node.nextSibling?.let { after ->
            if (!(after.hasEmptyLine() || after.firstChild?.hasEmptyLine() == true)) {
                node.parent.addAfter(createEmptyLine(), node)
            }
        }
    }

    private fun PsiElement.hasEmptyLine() = this is PsiWhiteSpace && text.count { it == '\n' } > 1

    private fun createEmptyLine(): XmlText {
        return XmlElementFactory.getInstance(xmlFile.project).createTagFromText("<s>\n\n</s>").children.first { it is XmlText } as XmlText
    }

    private fun GenericDomValue<String>.isEmpty() = !exists() || stringValue.isNullOrEmpty()

    private fun SourceFolder.isRelatedSourceRoot(isTest: Boolean): Boolean {
        return if (isTest) {
            rootType == JavaSourceRootType.TEST_SOURCE || rootType == KotlinSourceRootType.TestSource
        } else {
            rootType == JavaSourceRootType.SOURCE || rootType == KotlinSourceRootType.Source
        }
    }

    @Suppress("Unused")
    object DefaultPhases {
        const val None = "none"
        const val Validate = "validate"
        const val Initialize = "initialize"
        const val GenerateSources = "generate-sources"
        const val ProcessSources = "process-sources"
        const val GenerateResources = "generate-resources"
        const val ProcessResources = "process-resources"
        const val Compile = "compile"
        const val ProcessClasses = "process-classes"
        const val GenerateTestSources = "generate-test-sources"
        const val ProcessTestSources = "process-test-sources"
        const val GenerateTestResources = "generate-test-resources"
        const val ProcessTestResources = "process-test-resources"
        const val TestCompile = "test-compile"
        const val ProcessTestClasses = "process-test-classes"
        const val Test = "test"
        const val PreparePackage = "prepare-package"
        const val Package = "package"
        const val PreIntegrationTest = "pre-integration-test"
        const val IntegrationTest = "integration-test"
        const val PostIntegrationTest = "post-integration-test"
        const val Verify = "verify"
        const val Install = "install"
        const val Deploy = "deploy"
    }

    object KotlinGoals {
        const val Compile = "compile"
        const val TestCompile = "test-compile"
        const val Js = "js"
        const val TestJs = "test-js"
        const val MetaData = "metadata"

        val JvmGoals = listOf(Compile, TestCompile)
    }

    companion object {
        private val LOG = Logger.getInstance(PomFile::class.java)

        fun forFileOrNull(xmlFile: XmlFile): PomFile? =
            MavenDomUtil.getMavenDomProjectModel(xmlFile.project, xmlFile.virtualFile)?.let { PomFile(xmlFile, it) }

        @Suppress("DeprecatedCallableAddReplaceWith")
        @Deprecated("We shouldn't use phase but additional compiler configuration in most cases")
        fun getPhase(hasJavaFiles: Boolean, isTest: Boolean) = when {
            hasJavaFiles -> when {
                isTest -> DefaultPhases.ProcessTestSources
                else -> DefaultPhases.ProcessSources
            }
            else -> when {
                isTest -> DefaultPhases.TestCompile
                else -> DefaultPhases.Compile
            }
        }

        // from maven code convention: https://maven.apache.org/developers/conventions/code.html
        val recommendedElementsOrder = """
          <modelVersion/>
          <parent/>

          <groupId/>
          <artifactId/>
          <version/>
          <packaging/>

          <name/>
          <description/>
          <url/>
          <inceptionYear/>
          <organization/>
          <licenses/>

          <developers/>
          <contributors/>

          <mailingLists/>

          <prerequisites/>

          <modules/>

          <scm/>
          <issueManagement/>
          <ciManagement/>
          <distributionManagement/>

          <properties/>

          <dependencyManagement/>
          <dependencies/>

          <repositories/>
          <pluginRepositories/>

          <build/>

          <reporting/>

          <profiles/>
        """.lines()
            .map { it.trim().removePrefix("<").removeSuffix("/>").trim() }
            .filter(String::isNotEmpty)
            .toCollection(LinkedHashSet())

        val recommendedOrderAsList = recommendedElementsOrder.toList()
    }
}

fun PomFile.changeLanguageVersion(languageVersion: String?, apiVersion: String?): PsiElement? {
    val kotlinPlugin = findPlugin(
        MavenId(
            KotlinMavenConfigurator.GROUP_ID,
            KotlinMavenConfigurator.MAVEN_PLUGIN_ID,
            null
        )
    ) ?: return null
    val languageElement = languageVersion?.let {
        changeConfigurationOrProperty(kotlinPlugin, "languageVersion", "kotlin.compiler.languageVersion", it)
    }
    val apiElement = apiVersion?.let {
        changeConfigurationOrProperty(kotlinPlugin, "apiVersion", "kotlin.compiler.apiVersion", it)
    }
    return languageElement ?: apiElement
}

internal fun MavenDomDependencies.findDependencies(artifact: MavenId, scope: MavenArtifactScope? = null) =
    findDependencies(SmartList(artifact), scope)

internal fun MavenDomDependencies.findDependencies(artifacts: List<MavenId>, scope: MavenArtifactScope? = null): List<MavenDomDependency> {
    return dependencies.filter { dependency ->
        artifacts.any { artifact ->
            dependency.matches(artifact, scope)
        }
    }
}

private fun MavenDomDependency.matches(artifact: MavenId, scope: MavenArtifactScope?) =
    this.matches(artifact) && (this.scope.stringValue == scope?.name?.toLowerCase() || scope == null && this.scope.stringValue == "compile")

private fun MavenDomArtifactCoordinates.matches(artifact: MavenId) =
    (artifact.groupId == null || groupId.stringValue == artifact.groupId)
            && (artifact.artifactId == null || artifactId.stringValue == artifact.artifactId)
            && (artifact.version == null || version.stringValue == artifact.version)

private fun PomFile.changeConfigurationOrProperty(
    kotlinPlugin: MavenDomPlugin,
    configurationTagName: String,
    propertyName: String, value: String
): XmlTag? {
    val configuration = kotlinPlugin.configuration
    if (configuration.exists()) {
        val subTag = configuration.xmlTag.findFirstSubTag(configurationTagName)
        if (subTag != null) {
            subTag.value.text = value
            return subTag
        }
    }

    val propertyTag = findProperty(propertyName)
    if (propertyTag != null) {
        val textNode = propertyTag.children.filterIsInstance<XmlText>().firstOrNull()
        if (textNode != null) {
            textNode.value = value
            return propertyTag
        }
    }

    return addPluginConfiguration(kotlinPlugin, configurationTagName, value)
}

fun PomFile.changeCoroutineConfiguration(value: String): PsiElement? {
    val kotlinPlugin = findPlugin(
        MavenId(
            KotlinMavenConfigurator.GROUP_ID,
            KotlinMavenConfigurator.MAVEN_PLUGIN_ID,
            null
        )
    ) ?: return null
    return changeConfigurationOrProperty(kotlinPlugin, "experimentalCoroutines", "kotlin.compiler.experimental.coroutines", value)
}
