/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.testIntegration

import com.intellij.CommonBundle
import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScopesCore
import com.intellij.testIntegration.createTest.CreateTestAction
import com.intellij.testIntegration.createTest.TestGenerators
import org.jetbrains.kotlin.asJava.KotlinLightClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.actions.JavaToKotlinAction
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.kotlin.idea.core.refactoring.j2k
import org.jetbrains.kotlin.idea.core.refactoring.toPsiDirectory
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.idea.util.runWithAlternativeResolveEnabled
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.runWhenSmart
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.utils.addToStdlib.singletonList
import java.util.*

class KotlinCreateTestIntention : JetSelfTargetingRangeIntention<KtClassOrObject>(KtClassOrObject::class.java, "Create test") {
    override fun applicabilityRange(element: KtClassOrObject): TextRange? {
        if (element.isLocal()) return null
        if (element is KtEnumEntry) return null
        if (element is KtClass && (element.isAnnotation() || element.isInterface())) return null
        if (ModuleUtilCore.findModuleForPsiElement(element) == null) return null
        if (element.resolveToDescriptorIfAny() == null) return null

        return TextRange(
                element.startOffset,
                element.getDelegationSpecifierList()?.startOffset ?: element.getBody()?.startOffset ?: element.endOffset
        )
    }

    override fun applyTo(element: KtClassOrObject, editor: Editor) {
        object : CreateTestAction() {
            // Based on the com.intellij.testIntegration.createTest.JavaTestGenerator.createTestClass()
            private fun findTestClass(targetDirectory: PsiDirectory, className: String): PsiClass? {
                val psiPackage = targetDirectory.getPackage() ?: return null
                val scope = GlobalSearchScopesCore.directoryScope(targetDirectory, false)
                val klass = psiPackage.findClassByShortName(className, scope).firstOrNull() ?: return null
                if (!FileModificationService.getInstance().preparePsiElementForWrite(klass)) return null
                return klass
            }

            private fun getTempJavaClassName(project: Project, kotlinFile: VirtualFile): String {
                val baseName = kotlinFile.nameWithoutExtension
                val psiDir = kotlinFile.parent!!.toPsiDirectory(project)!!
                return sequence(0) { it + 1 }
                        .map { "$baseName$it" }
                        .first { psiDir.findFile("$it.java") == null && findTestClass(psiDir, it) == null }
            }

            // Based on the com.intellij.testIntegration.createTest.CreateTestAction.CreateTestAction.invoke()
            override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
                val srcModule = ModuleUtilCore.findModuleForPsiElement(element) ?: return
                val propertiesComponent = PropertiesComponent.getInstance()
                val testFolders = HashSet<VirtualFile>()
                CreateTestAction.checkForTestRoots(srcModule, testFolders)
                if (testFolders.isEmpty() && !propertiesComponent.getBoolean("create.test.in.the.same.root")) {
                    if (Messages.showOkCancelDialog(
                            project,
                            "Create test in the same source root?",
                            "No Test Roots Found",
                            Messages.getQuestionIcon()) != Messages.OK) return

                    propertiesComponent.setValue("create.test.in.the.same.root", true)
                }

                val srcClass = CreateTestAction.getContainingClass(element) ?: return

                val srcDir = element.containingFile.containingDirectory
                val srcPackage = JavaDirectoryService.getInstance().getPackage(srcDir)

                val dialog = KotlinCreateTestDialog(project, text, srcClass, srcPackage, srcModule)
                if (!dialog.showAndGet()) return

                val existingClass = (findTestClass(dialog.targetDirectory, dialog.className) as? KotlinLightClass)?.getOrigin()
                if (existingClass != null) {
                    // TODO: Override dialog method when it becomes protected
                    val answer = Messages.showYesNoDialog(
                            project,
                            "Kotlin class '${existingClass.name}' already exists. Do you want to update it?",
                            CommonBundle.getErrorTitle(),
                            "Rewrite",
                            "Cancel",
                            Messages.getErrorIcon()
                    )
                    if (answer == Messages.NO) return
                }

                val generatedClass = project.executeCommand(CodeInsightBundle.message("intention.create.test"), this) {
                    val generator = TestGenerators.INSTANCE.forLanguage(dialog.selectedTestFrameworkDescriptor.language)
                    project.runWithAlternativeResolveEnabled {
                        if (existingClass != null) {
                            dialog.explicitClassName = getTempJavaClassName(project, existingClass.containingFile.virtualFile)
                        }
                        generator.generateTest(project, dialog)
                    }
                } as? PsiClass ?: return

                project.runWhenSmart {
                    val generatedFile = generatedClass.containingFile as? PsiJavaFile ?: return@runWhenSmart

                    if (generatedClass.language == JavaLanguage.INSTANCE) {
                        project.executeCommand("Convert class '${generatedClass.name}' to Kotlin", this) {
                            runWriteAction {
                                generatedClass.methods.forEach { it.throwsList.referenceElements.forEach { it.delete() } }
                            }

                            if (existingClass != null) {
                                runWriteAction {
                                    val existingMethodNames = existingClass
                                            .declarations
                                            .filterIsInstance<KtNamedFunction>()
                                            .mapTo(HashSet()) { it.name }
                                    generatedClass
                                            .methods
                                            .filter { it.name !in existingMethodNames }
                                            .forEach { it.j2k()?.let { existingClass.addDeclaration(it) } }
                                    generatedClass.delete()
                                }
                                NavigationUtil.activateFileWithPsiElement(existingClass)
                            }
                            else {
                                JavaToKotlinAction.convertFiles(generatedFile.singletonList(), project, false).singleOrNull()
                            }
                        }
                    }
                }
            }
        }.invoke(element.project, editor, element.toLightClass()!!)
    }
}