/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.quickfix.createFromUsage.createClass

import org.jetbrains.jet.plugin.quickfix.createFromUsage.CreateFromUsageFixBase
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.plugin.JetBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiDirectory
import org.jetbrains.jet.plugin.JetFileType
import org.jetbrains.jet.plugin.codeInsight.CodeInsightUtils
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.ParameterInfo
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.ConstructorInfo
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.CallableBuilderConfiguration
import java.util.Collections
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.createBuilder
import com.intellij.openapi.command.CommandProcessor
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.CallablePlacement
import org.jetbrains.jet.plugin.refactoring.getOrCreateKotlinFile
import org.jetbrains.jet.plugin.quickfix.createFromUsage.createClass.ClassKind.*
import com.intellij.psi.PsiPackage
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.ide.util.DirectoryChooserUtil
import java.util.HashMap
import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.jet.plugin.refactoring.canRefactor

enum class ClassKind(val keyword: String, val description: String) {
    PLAIN_CLASS: ClassKind("class", "class")
    ENUM_CLASS: ClassKind("enum class", "enum")
    ENUM_ENTRY: ClassKind("", "enum constant")
    ANNOTATION_CLASS: ClassKind("annotation class", "annotation")
    TRAIT: ClassKind("trait", "trait")
    OBJECT: ClassKind("object", "object")
}

public class ClassInfo(
        val kind: ClassKind,
        val name: String,
        val targetParent: PsiElement,
        val expectedTypeInfo: TypeInfo,
        val inner: Boolean = false,
        val open: Boolean = false,
        val typeArguments: List<TypeInfo> = Collections.emptyList(),
        val parameterInfos: List<ParameterInfo> = Collections.emptyList()
)

public class CreateClassFromUsageFix(
        element: JetElement,
        val classInfo: ClassInfo
): CreateFromUsageFixBase(element) {
    override fun getText(): String =
            JetBundle.message("create.0.from.usage", "${classInfo.kind.description} '${classInfo.name}'")

    override fun invoke(project: Project, editor: Editor, file: JetFile) {
        fun createFileByPackage(psiPackage: PsiPackage): JetFile? {
            val directories = psiPackage.getDirectories().filter { it.canRefactor() }
            assert (directories.isNotEmpty(), "Package '${psiPackage.getQualifiedName() ?: ""}' must be refactorable")

            val currentModule = ModuleUtilCore.findModuleForPsiElement(file)
            val preferredDirectory =
                    directories.firstOrNull { ModuleUtilCore.findModuleForPsiElement(it) == currentModule }
                    ?: directories.firstOrNull()

            val targetDirectory = if (directories.size > 1 && !ApplicationManager.getApplication().isUnitTestMode()) {
                DirectoryChooserUtil.chooseDirectory(directories.copyToArray(), preferredDirectory, project, HashMap<PsiDirectory, String>())
            }
            else {
                preferredDirectory
            } ?: return null

            val fileName = "${classInfo.name}.${JetFileType.INSTANCE.getDefaultExtension()}"
            val targetFile = getOrCreateKotlinFile(fileName, targetDirectory)
            if (targetFile == null) {
                val filePath = "${targetDirectory.getVirtualFile().getPath()}/$fileName"
                CodeInsightUtils.showErrorHint(
                        targetDirectory.getProject(),
                        editor,
                        "File $filePath already exists but does not correspond to Kotlin file",
                        "Create file",
                        null
                )
            }
            return targetFile
        }

        with (classInfo) {
            val targetParent =
                    when (targetParent) {
                        is JetElement -> targetParent
                        is PsiPackage -> createFileByPackage(targetParent)
                        else -> throw AssertionError("Unexpected element: " + targetParent.getText())
                    } as? JetElement ?: return

            val constructorInfo = ConstructorInfo(classInfo, expectedTypeInfo)
            val builder = CallableBuilderConfiguration(
                    Collections.singletonList(constructorInfo), element as JetElement, file, editor, kind == PLAIN_CLASS || kind == TRAIT
            ).createBuilder()
            builder.placement = CallablePlacement.NoReceiver(targetParent)
            CommandProcessor.getInstance().executeCommand(project, { builder.build() }, getText(), null)
        }
    }
}