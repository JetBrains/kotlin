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

package org.jetbrains.kotlin.idea.maven.actions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.util.xml.DomElement
import com.intellij.util.xml.DomUtil
import com.intellij.util.xml.actions.generate.AbstractDomGenerateProvider
import com.intellij.util.xml.ui.actions.generate.GenerateDomElementAction
import org.jetbrains.idea.maven.dom.MavenDomUtil
import org.jetbrains.idea.maven.dom.model.MavenDomDependency
import org.jetbrains.idea.maven.dom.model.MavenDomPlugin
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel
import org.jetbrains.idea.maven.model.MavenId
import org.jetbrains.kotlin.idea.maven.PomFile
import org.jetbrains.kotlin.idea.maven.configuration.KotlinMavenConfigurator
import org.jetbrains.kotlin.idea.versions.MAVEN_STDLIB_ID
import org.jetbrains.kotlin.psi.psiUtil.endOffset

class GenerateMavenCompileExecutionAction : PomFileActionBase(KotlinMavenExecutionProvider(PomFile.KotlinGoals.Compile, PomFile.DefaultPhases.Compile))
class GenerateMavenTestCompileExecutionAction : PomFileActionBase(KotlinMavenExecutionProvider(PomFile.KotlinGoals.TestCompile, PomFile.DefaultPhases.TestCompile))
class GenerateMavenPluginAction : PomFileActionBase(KotlinMavenPluginProvider())

private val DefaultKotlinVersion = "\${kotlin.version}"

open class PomFileActionBase(generateProvider: AbstractDomGenerateProvider<*>) : GenerateDomElementAction(generateProvider) {
    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile): Boolean {
        return MavenDomUtil.isMavenFile(file) && super.isValidForFile(project, editor, file)
    }

    override fun startInWriteAction() = true
}

private class KotlinMavenPluginProvider : AbstractDomGenerateProvider<MavenDomPlugin>("kotlin-maven-plugin-provider", MavenDomPlugin::class.java) {

    override fun generate(parent: DomElement?, editor: Editor?): MavenDomPlugin? {
        if (parent !is MavenDomProjectModel) {
            return null
        }

        val knownVersion = parent.dependencies.dependencies.firstOrNull { it.isKotlinStdlib() }?.version?.rawText
        val version = when {
            knownVersion == null -> DefaultKotlinVersion
            knownVersion.isRangeVersion() -> knownVersion.getRangeClosedEnd() ?: DefaultKotlinVersion
            else -> knownVersion
        }

        val pom = PomFile.forFileOrNull(DomUtil.getFile(parent)) ?: return null
        return pom.addPlugin(MavenId(KotlinMavenConfigurator.GROUP_ID, KotlinMavenConfigurator.MAVEN_PLUGIN_ID, version))
    }

    override fun getElementToNavigate(t: MavenDomPlugin?) = t?.version

    override fun getParentDomElement(project: Project?, editor: Editor?, file: PsiFile?): DomElement? {
        if (project == null || editor == null || file == null) {
            return null
        }

        return DomUtil.getContextElement(editor)?.findProject()
    }

    override fun isAvailableForElement(contextElement: DomElement): Boolean {
        val parent = contextElement.findProject() ?: return false

        return parent.build.plugins.plugins.none(MavenDomPlugin::isKotlinMavenPlugin)
    }
}

private class KotlinMavenExecutionProvider(val goal: String, val phase: String) : AbstractDomGenerateProvider<MavenDomPlugin>("kotlin-maven-execution-provider", MavenDomPlugin::class.java) {

    override fun generate(parent: DomElement?, editor: Editor?): MavenDomPlugin? {
        if (parent !is MavenDomPlugin) {
            return null
        }

        val file = PomFile.forFileOrNull(DomUtil.getFile(parent)) ?: return null
        val execution = file.addExecution(parent, goal, phase, listOf(goal))

        if (editor != null) {
            editor.caretModel.moveToOffset(execution.ensureXmlElementExists().endOffset)
        }

        return parent
    }

    override fun getElementToNavigate(t: MavenDomPlugin?) = null

    override fun getParentDomElement(project: Project?, editor: Editor?, file: PsiFile?): DomElement? {
        if (project == null || editor == null || file == null) {
            return null
        }

        return DomUtil.getContextElement(editor)?.findPlugin()
    }

    override fun isAvailableForElement(contextElement: DomElement): Boolean {
        val plugin = contextElement.findPlugin()
        return plugin != null
               && plugin.isKotlinMavenPlugin()
               && plugin.executions.executions.none { it.goals.goals.any { it.value == goal } }
    }

}

private fun String.getRangeClosedEnd(): String? = when {
    startsWith("[") -> substringBefore(',', "").drop(1).trimEnd()
    endsWith("]") -> substringAfterLast(',', "").dropLast(1).trimStart()
    else -> null
}

private fun Char.isRangeStart() = this == '[' || this == '('
private fun Char.isRangeEnd() = this == ']' || this == ')'

private fun String.isRangeVersion() = length > 2 && this[0].isRangeStart() && last().isRangeEnd()

private fun DomElement.findProject(): MavenDomProjectModel? = this as? MavenDomProjectModel ?: DomUtil.getParentOfType(this, MavenDomProjectModel::class.java, true)
private fun DomElement.findPlugin(): MavenDomPlugin? = this as? MavenDomPlugin ?: DomUtil.getParentOfType(this, MavenDomPlugin::class.java, true)

private fun MavenDomPlugin.isKotlinMavenPlugin() = groupId.stringValue == KotlinMavenConfigurator.GROUP_ID
                                                   && artifactId.stringValue == KotlinMavenConfigurator.MAVEN_PLUGIN_ID

private fun MavenDomDependency.isKotlinStdlib() = groupId.stringValue == KotlinMavenConfigurator.GROUP_ID
                                                  && artifactId.stringValue == MAVEN_STDLIB_ID
