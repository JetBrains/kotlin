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
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.idea.maven.utils.MavenArtifactScope
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.kotlin.idea.configuration.RepositoryDescription
import org.jetbrains.kotlin.idea.maven.configuration.KotlinMavenConfigurator
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import java.util.*

fun kotlinPluginId(version: String?) = MavenId(KotlinMavenConfigurator.GROUP_ID, KotlinMavenConfigurator.MAVEN_PLUGIN_ID, version)


class PomFile private constructor(val xmlFile: XmlFile, val domModel: MavenDomProjectModel) {
    constructor(xmlFile: XmlFile) : this(xmlFile, MavenDomUtil.getMavenDomProjectModel(xmlFile.project, xmlFile.virtualFile) ?: throw IllegalStateException("No DOM model found for pom ${xmlFile.name}"))

    private val nodesByName = HashMap<String, XmlTag>()
    private val projectElement: XmlTag

    init {
        var projectElement: XmlTag? = null

        xmlFile.document?.accept(object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)

                if (element is XmlTag && element.localName in recommendedElementsOrder && element.parent === projectElement) {
                    nodesByName[element.localName] = element
                }
                else if (element is XmlTag && element.localName == "project") {
                    projectElement = element
                    element.acceptChildren(this)
                }
                else {
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
                }
                else {
                    tag.replace(projectElement.createChildTag(name, value))
                }
            }
        }
        else {
            properties.add(projectElement.createChildTag(name, value))
        }
    }

    fun addDependency(artifact: MavenId, scope: MavenArtifactScope? = null, classifier: String? = null, optional: Boolean = false, systemPath: String? = null): MavenDomDependency {
        require(systemPath == null || scope == MavenArtifactScope.SYSTEM) { "systemPath is only applicable for system scope dependency" }
        require(artifact.groupId != null) { "groupId shouldn't be null" }
        require(artifact.artifactId != null) { "artifactId shouldn't be null" }

        ensureDependencies()
        val versionless = artifact.withNoVersion().withoutJreSuffix()
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

    fun ensurePluginAfter(plugin: MavenDomPlugin, referencePlugin: MavenDomPlugin): MavenDomPlugin {
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

    fun findExecutions(plugin: MavenDomPlugin) = plugin.executions.executions
    fun findExecutions(plugin: MavenDomPlugin, vararg goals: String) = findExecutions(plugin).filter { it.goals.goals.any { it.rawText in goals } }

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

    fun addKotlinExecution(module: Module, plugin: MavenDomPlugin, executionId: String, phase: String, isTest: Boolean, goals: List<String>) {
        val execution = addExecution(plugin, executionId, phase, goals)

        val sourceDirs = ModuleRootManager.getInstance(module)
                .contentEntries
                .flatMap { it.sourceFolders.filter { it.isRelatedSourceRoot(isTest) } }
                .mapNotNull { it.file }
                .mapNotNull { VfsUtilCore.getRelativePath(it, xmlFile.virtualFile.parent, '/') }

        executionSourceDirs(execution, sourceDirs)
    }

    fun isPluginExecutionMissing(plugin: MavenPlugin?, excludedExecutionId: String, goal: String) = plugin == null || plugin.executions.none { it.executionId != excludedExecutionId && goal in it.goals }

    fun addJavacExecutions(module: Module, kotlinPlugin: MavenDomPlugin) {
        val javacPlugin = ensurePluginAfter(addPlugin(MavenId("org.apache.maven.plugins", "maven-compiler-plugin", null)), kotlinPlugin)

        val project = MavenProjectsManager.getInstance(module.project).findProject(module)!!
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
            execution::class.java.methods.filter { it.name == "getPhase" && it.parameterTypes.isEmpty() }.all { it.invoke(execution) == DefaultPhases.None }
        }
    }

    fun executionSourceDirs(execution: MavenDomPluginExecution, sourceDirs: List<String>, forceSingleSource: Boolean = false) {
        ensureBuild()

        val isTest = execution.goals.goals.any { it.stringValue == KotlinGoals.TestCompile || it.stringValue == KotlinGoals.TestJs }
        val defaultDir = if (isTest) "test" else "main"
        val singleDirectoryElement = if (isTest) {
            domModel.build.testSourceDirectory
        }
        else {
            domModel.build.sourceDirectory
        }

        if (sourceDirs.isEmpty() || sourceDirs.singleOrNull() == "src/$defaultDir/java") {
            execution.configuration.xmlTag?.findSubTags("sourceDirs")?.forEach { it.deleteCascade() }
            singleDirectoryElement.undefine()
        }
        else if (sourceDirs.size == 1 && !forceSingleSource) {
            singleDirectoryElement.stringValue = sourceDirs.single()
            execution.configuration.xmlTag?.findSubTags("sourceDirs")?.forEach { it.deleteCascade() }
        }
        else {
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

    fun executionConfiguration(execution: MavenDomPluginExecution, name: String): XmlTag {
        val configurationTag = execution.configuration.ensureTagExists()!!

        val existingTag = configurationTag.findSubTags(name).firstOrNull()
        if (existingTag != null) {
            return existingTag
        }

        val newTag = configurationTag.createChildTag(name)
        return configurationTag.add(newTag) as XmlTag
    }

    fun addPluginConfiguration(plugin: MavenDomPlugin, optionName: String, optionValue: String) {
        val configurationTag = plugin.configuration.ensureTagExists()
        val existingTag = configurationTag.findFirstSubTag(optionName)
        if (existingTag != null) {
            existingTag.value.text = optionValue
        }
        else {
            configurationTag.add(configurationTag.createChildTag(optionName, optionValue))
        }
    }

    fun addPluginRepository(id: String, name: String, url: String, snapshots: Boolean = false, releases: Boolean = true): MavenDomRepository {
        ensurePluginRepositories()

        return addRepository(id, name, url, snapshots, releases, { domModel.pluginRepositories.pluginRepositories }, { domModel.pluginRepositories.addPluginRepository() })
    }

    fun addPluginRepository(description: RepositoryDescription) {
        addPluginRepository(description.id, description.name, description.url, description.isSnapshot, true)
    }

    fun addLibraryRepository(id: String, name: String, url: String, snapshots: Boolean = false, releases: Boolean = true): MavenDomRepository {
        ensureRepositories()

        return addRepository(id, name, url, snapshots, releases, { domModel.repositories.repositories }, { domModel.repositories.addRepository() })
    }

    fun addLibraryRepository(description: RepositoryDescription) {
        addLibraryRepository(description.id, description.name, description.url, description.isSnapshot, true)
    }

    private fun addRepository(id: String, name: String, url: String, snapshots: Boolean, releases: Boolean, existing: () -> List<MavenDomRepository>, create: () -> MavenDomRepository): MavenDomRepository {

        val repository =
                existing().firstOrNull { it.id.stringValue == id } ?:
                existing().firstOrNull { it.url.stringValue == url } ?:
                create()

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

    fun hasPlugin(artifact: MavenId) = domModel.build.plugins.plugins.any { it.matches(artifact) }

    fun hasDependency(artifact: MavenId, scope: MavenArtifactScope? = null) =
            domModel.dependencies.dependencies.any { it.matches(artifact, scope) }

    fun findDependencies(artifact: MavenId, scope: MavenArtifactScope? = null) =
            domModel.dependencies.dependencies.filter { it.matches(artifact, scope) }

    fun ensureBuild(): XmlTag = ensureElement(projectElement, "build")

    fun ensureDependencies(): XmlTag = ensureElement(projectElement, "dependencies")
    fun ensurePluginRepositories(): XmlTag = ensureElement(projectElement, "pluginRepositories")
    fun ensureRepositories(): XmlTag = ensureElement(projectElement, "repositories")

    private fun MavenDomPlugin.isKotlinMavenPlugin() = groupId.stringValue == KotlinMavenConfigurator.GROUP_ID
                                                       && artifactId.stringValue == KotlinMavenConfigurator.MAVEN_PLUGIN_ID

    private fun MavenDomDependency.matches(artifact: MavenId, scope: MavenArtifactScope?) =
            this.matches(artifact) && (this.scope.stringValue == scope?.name?.toLowerCase() || scope == null && this.scope.stringValue == "compile")

    private fun MavenDomArtifactCoordinates.matches(artifact: MavenId) =
            (artifact.groupId == null || groupId.stringValue == artifact.groupId)
            && (artifact.artifactId == null || artifactId.stringValue == artifact.artifactId)
            && (artifact.version == null || version.stringValue == artifact.version)

    private fun MavenId.withNoVersion() = MavenId(groupId, artifactId, null)
    private fun MavenId.withoutJreSuffix() = MavenId(groupId, artifactId?.substringBeforeLast("-jre"), null)

    private fun MavenDomElement.createChildTag(name: String, value: String? = null) = xmlTag.createChildTag(name, value)
    private fun XmlTag.createChildTag(name: String, value: String? = null) = createChildTag(name, namespace, value, false)!!

    tailrec
    private fun XmlTag.deleteCascade() {
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
            if (!(before.hasEmptyLine() || before.lastChild?.hasEmptyLine() ?: false)) {
                node.parent.addBefore(createEmptyLine(), node)
            }
        }
        node.nextSibling?.let { after ->
            if (!(after.hasEmptyLine() || after.firstChild?.hasEmptyLine() ?: false)) {
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
        val relevantRootType = when {
            isTest -> JavaSourceRootType.TEST_SOURCE
            else -> JavaSourceRootType.SOURCE
        }

        return rootType === relevantRootType
    }

    @Suppress("Unused")
    object DefaultPhases {
        val None = "none"
        val Validate = "validate"
        val Initialize = "initialize"
        val GenerateSources = "generate-sources"
        val ProcessSources = "process-sources"
        val GenerateResources = "generate-resources"
        val ProcessResources = "process-resources"
        val Compile = "compile"
        val ProcessClasses = "process-classes"
        val GenerateTestSources = "generate-test-sources"
        val ProcessTestSources = "process-test-sources"
        val GenerateTestResources = "generate-test-resources"
        val ProcessTestResources = "process-test-resources"
        val TestCompile = "test-compile"
        val ProcessTestClasses = "process-test-classes"
        val Test = "test"
        val PreparePackage = "prepare-package"
        val Package = "package"
        val PreIntegrationTest = "pre-integration-test"
        val IntegrationTest = "integration-test"
        val PostIntegrationTest = "post-integration-test"
        val Verify = "verify"
        val Install = "install"
        val Deploy = "deploy"
    }

    object KotlinGoals {
        val Compile = "compile"
        val TestCompile = "test-compile"
        val Js = "js"
        val TestJs = "test-js"
        val MetaData = "metadata"
    }

    companion object {
        fun forFileOrNull(xmlFile: XmlFile): PomFile? = MavenDomUtil.getMavenDomProjectModel(xmlFile.project, xmlFile.virtualFile)?.let { PomFile(xmlFile, it) }

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