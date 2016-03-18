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

package org.jetbrains.kotlin.idea.configuration

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
import org.jetbrains.idea.maven.utils.MavenArtifactScope
import java.util.*

class PomFile(val xmlFile: XmlFile) {
    private val domModel = MavenDomUtil.getMavenDomProjectModel(xmlFile.project, xmlFile.virtualFile) ?: throw IllegalStateException("No DOM model found for pom ${xmlFile.name}")
    private val nodesByName = HashMap<String, XmlTag>()
    private var projectElement: XmlTag? = null

    init {
        xmlFile.document?.accept(object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)

                if (element is XmlTag && element.localName in recommendedElementsOrder) {
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
    }

    fun addProperty(name: String, value: String) {
        projectElement?.let { project ->
            val properties = ensureElement(project, "properties")
            val existing = properties.children.filterIsInstance<XmlTag>().filter { it.localName == name }

            if (existing.isNotEmpty()) {
                for (tag in existing) {
                    val textNode = tag.children.filterIsInstance<XmlText>().firstOrNull()
                    if (textNode != null) {
                        textNode.value = value
                    }
                    else {
                        tag.replace(project.createChildTag(name, project.namespace, value, false))
                    }
                }
            }
            else {
                properties.add(project.createChildTag(name, project.namespace, value, false))
            }
        }
    }

    fun addDependency(artifact: MavenId, scope: MavenArtifactScope? = null, classifier: String? = null, optional: Boolean = false, systemPath: String? = null): MavenDomDependency {
        require(systemPath == null || scope == MavenArtifactScope.SYSTEM) { "systemPath is only applicable for system scope dependency" }

        ensureDependencies()
        val versionless = artifact.withNoVersion()
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

    fun addPlugin(artifact: MavenId): MavenDomPlugin {
        ensureBuild()

        val groupArtifact = artifact.withNoVersion()
        val plugin = domModel.build.plugins.plugins.firstOrNull { it.matches(groupArtifact) } ?: domModel.build.plugins.addPlugin()
        plugin.groupId.stringValue = artifact.groupId
        plugin.artifactId.stringValue = artifact.artifactId
        plugin.version.stringValue = artifact.version
        plugin.ensureTagExists()

        return plugin
    }

    fun addExecution(plugin: MavenDomPlugin, executionId: String, phase: String, goals: List<String>): MavenDomPluginExecution {
        require(goals.isNotEmpty()) { "Execution $executionId requires at least one goal but empty list has been provided" }
        require(executionId.isNotEmpty()) { "executionId shouldn't be empty" }
        require(phase.isNotEmpty()) { "phase shouldn't be empty" }

        val execution = plugin.executions.executions.firstOrNull { it.id.stringValue == executionId } ?: plugin.executions.addExecution()
        execution.id.stringValue = executionId
        execution.phase.stringValue = phase
        execution.goals.ensureTagExists()

        val existingGoals = execution.goals.goals.mapNotNull { it.rawText }
        for (goal in goals.filter { it !in existingGoals }) {
            val goalTag = execution.goals.xmlTag.createChildTag("goal", plugin.xmlElementNamespace, goal, false)
            execution.goals.xmlTag.add(goalTag)
        }

        return execution
    }

    fun executionSourceDirs(execution: MavenDomPluginExecution, sourceDirs: List<String>) {
        ensureBuild()

        val isTest = execution.goals.goals.any { it.stringValue?.let { "test" in it } ?: false }
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
        else if (sourceDirs.size == 1) {
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

    fun executionConfiguration(execution: MavenDomPluginExecution, name: String): XmlTag {
        val configurationTag = execution.configuration.ensureTagExists()!!

        val existingTag = configurationTag.findSubTags(name).firstOrNull()
        if (existingTag != null) {
            return existingTag
        }

        val newTag = configurationTag.createChildTag(name, configurationTag.namespace, null, false)!!
        return configurationTag.add(newTag) as XmlTag
    }

    fun addPluginRepository(id: String, name: String, url: String, snapshots: Boolean = false, releases: Boolean = true): MavenDomRepository {
        ensurePluginRepositories()

        return addRepository(id, name, url, snapshots, releases, { domModel.pluginRepositories.pluginRepositories }, { domModel.pluginRepositories.addPluginRepository() })
    }

    fun addLibraryRepository(id: String, name: String, url: String, snapshots: Boolean = false, releases: Boolean = true): MavenDomRepository {
        ensureRepositories()

        return addRepository(id, name, url, snapshots, releases, { domModel.repositories.repositories }, { domModel.repositories.addRepository() })
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
            domModel.dependencies.dependencies.any { it.matches(artifact) && (it.scope.stringValue == scope?.name || scope == null && it.scope.stringValue == "compile") }

    fun ensureBuild(): XmlTag = ensureElement(projectElement!!, "build")
    fun ensureDependencies(): XmlTag = ensureElement(projectElement!!, "dependencies")
    fun ensurePluginRepositories(): XmlTag = ensureElement(projectElement!!, "pluginRepositories")
    fun ensureRepositories(): XmlTag = ensureElement(projectElement!!, "repositories")

    private fun MavenDomArtifactCoordinates.matches(artifact: MavenId) =
            (artifact.groupId == null || groupId.stringValue == artifact.groupId)
            && (artifact.artifactId == null || artifactId.stringValue == artifact.artifactId)
            && (artifact.version == null || version.stringValue == artifact.version)

    private fun MavenId.withNoVersion() = MavenId(groupId, artifactId, null)

    private fun MavenDomElement.createChildTag(name: String, value: String? = null) = xmlTag.createChildTag(name, value)
    private fun XmlTag.createChildTag(name: String, value: String? = null) = createChildTag(name, namespace, value, false)

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

    companion object {
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
                .filter { it.isNotEmpty() }
                .toCollection(LinkedHashSet())

        val recommendedOrderAsList = recommendedElementsOrder.toList()
    }
}