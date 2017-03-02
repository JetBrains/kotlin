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

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlElement
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.util.xml.DomManager
import com.intellij.util.xml.GenericDomValue
import org.jetbrains.idea.maven.dom.MavenDomUtil
import org.jetbrains.idea.maven.dom.model.MavenDomBuild
import org.jetbrains.idea.maven.dom.model.MavenDomPluginExecution
import org.jetbrains.kotlin.idea.maven.PomFile
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf

class MavenPluginSourcesMoveToExecutionIntention : PsiElementBaseIntentionAction() {
    override fun getFamilyName() = "Move to compile execution"
    override fun getText() = familyName

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        val file = element.containingFile

        if (file == null || !MavenDomUtil.isMavenFile(file) || element !is XmlElement) {
            return false
        }

        val tag = element.getParentOfType<XmlTag>(false) ?: return false
        val domElement = DomManager.getDomManager(project).getDomElement(tag) ?: return false

        if (domElement !is GenericDomValue<*>) {
            return false
        }

        if (MavenDomUtil.getMavenDomProjectModel(project, file.virtualFile) == null) {
            return false
        }

        val pom = PomFile.forFileOrNull(file as XmlFile) ?: return false
        if (domElement.getParentOfType(MavenDomBuild::class.java, false)?.sourceDirectory === domElement) {
            return pom.findKotlinExecutions(PomFile.KotlinGoals.Compile, PomFile.KotlinGoals.Js).isNotEmpty()
        }
        if (domElement.getParentOfType(MavenDomBuild::class.java, false)?.testSourceDirectory === domElement) {
            return pom.findKotlinExecutions(PomFile.KotlinGoals.TestCompile, PomFile.KotlinGoals.TestJs).isNotEmpty()
        }

        return false
    }

    override fun startInWriteAction() = true

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        val xmlFile = element.containingFile as? XmlFile ?: return
        val pomFile = PomFile.forFileOrNull(xmlFile) ?: return

        val tag = element.getParentOfType<XmlTag>(false) ?: return
        val domElement = DomManager.getDomManager(project).getDomElement(tag) as? GenericDomValue<*> ?: return
        val dir = domElement.rawText ?: return

        val relevantExecutions = if (domElement.getParentOfType(MavenDomBuild::class.java, false)?.sourceDirectory === domElement) {
            pomFile.findKotlinExecutions(PomFile.KotlinGoals.Compile, PomFile.KotlinGoals.Js)
        } else if (domElement.getParentOfType(MavenDomBuild::class.java, false)?.testSourceDirectory === domElement) {
            pomFile.findKotlinExecutions(PomFile.KotlinGoals.TestCompile, PomFile.KotlinGoals.TestJs)
        } else {
            emptyList()
        }

        if (relevantExecutions.isNotEmpty()) {
            relevantExecutions.forEach { execution ->
                val existingSourceDirs = pomFile.executionSourceDirs(execution)
                pomFile.executionSourceDirs(execution, (existingSourceDirs + dir).distinct(), true)
            }

            domElement.undefine()
        }
    }
}

class MavenPluginSourcesMoveToBuild : PsiElementBaseIntentionAction() {
    override fun getFamilyName() = "Move to build>sourceDirectory tag"
    override fun getText() = familyName

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        return tryInvoke(project, element)
    }

    override fun startInWriteAction() = true

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        tryInvoke(project, element) { pom, dir, execution, build ->
            pom.executionSourceDirs(execution, listOf(dir))
        }
    }

    private fun tryInvoke(project: Project, element: PsiElement, block: (pom: PomFile, dir: String, execution: MavenDomPluginExecution, build: MavenDomBuild) -> Unit = { p, d, e, b -> }): Boolean {
        val file = element.containingFile

        if (file == null || !MavenDomUtil.isMavenFile(file) || (element !is XmlElement && element.parent !is XmlElement)) {
            return false
        }

        val tag = element.getParentOfType<XmlTag>(false) ?: return false
        val domElement = DomManager.getDomManager(project).getDomElement(tag) ?: return false

        val execution = domElement.getParentOfType(MavenDomPluginExecution::class.java, false) ?: return false
        tag.parentsWithSelf
                .takeWhile { it != execution.xmlElement }
                .filterIsInstance<XmlTag>()
                .firstOrNull { it.localName == "sourceDirs" } ?: return false

        val pom = PomFile.forFileOrNull(element.containingFile as XmlFile) ?: return false
        val sourceDirsToMove = pom.executionSourceDirs(execution)

        if (sourceDirsToMove.size != 1) {
            return false
        }

        val build = execution.getParentOfType(MavenDomBuild::class.java, false) ?: return false
        var couldMove = 0
        if (shouldMoveCompileSourceRoot(execution)) {
            if (!build.sourceDirectory.exists() || build.sourceDirectory.stringValue == sourceDirsToMove.single()) {
                couldMove ++
            }
        }
        if (shouldMoveTestSourceRoot(execution)) {
            if (!build.testSourceDirectory.exists() || build.testSourceDirectory.stringValue == sourceDirsToMove.single()) {
                couldMove ++
            }
        }

        if (couldMove == 1) {
            block(pom, sourceDirsToMove.single(), execution, build)
            return true
        } else {
            return false
        }
    }

    private fun shouldMoveCompileSourceRoot(execution: MavenDomPluginExecution) =
        execution.goals.goals.any { it.stringValue == PomFile.KotlinGoals.Compile || it.stringValue == PomFile.KotlinGoals.Js }

    private fun shouldMoveTestSourceRoot(execution: MavenDomPluginExecution) =
            execution.goals.goals.any { it.stringValue == PomFile.KotlinGoals.TestCompile || it.stringValue == PomFile.KotlinGoals.TestJs }
}
