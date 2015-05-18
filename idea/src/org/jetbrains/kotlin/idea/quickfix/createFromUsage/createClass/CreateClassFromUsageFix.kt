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

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass

import com.intellij.ide.util.DirectoryChooserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.JetFileType
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.core.refactoring.canRefactor
import org.jetbrains.kotlin.idea.core.refactoring.getOrCreateKotlinFile
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.CreateFromUsageFixBase
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.*
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass.ClassKind.ENUM_ENTRY
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass.ClassKind.OBJECT
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass.ClassKind.PLAIN_CLASS
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass.ClassKind.TRAIT
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetFile
import java.util.Collections
import java.util.HashMap

enum class ClassKind(val keyword: String, val description: String) {
    PLAIN_CLASS("class", "class"),
    ENUM_CLASS("enum class", "enum"),
    ENUM_ENTRY("", "enum constant"),
    ANNOTATION_CLASS("annotation class", "annotation"),
    TRAIT("interface", "interface"),
    OBJECT("object", "object")
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

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        if (!super.isAvailable(project, editor, file)) return false
        with(classInfo) {
            if (targetParent is PsiClass) {
                if (kind == OBJECT || kind == ENUM_ENTRY) return false
                if (targetParent.isInterface() && inner) return false
            }
        }
        return true
    }

    override fun invoke(project: Project, editor: Editor, file: JetFile) {
        fun createFileByPackage(psiPackage: PsiPackage): JetFile? {
            val directories = psiPackage.getDirectories().filter { it.canRefactor() }
            assert (directories.isNotEmpty()) { "Package '${psiPackage.getQualifiedName()}' must be refactorable" }

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
                        is JetElement, is PsiClass -> targetParent
                        is PsiPackage -> createFileByPackage(targetParent)
                        else -> throw AssertionError("Unexpected element: " + targetParent.getText())
                    } ?: return

            val constructorInfo = PrimaryConstructorInfo(classInfo, expectedTypeInfo)
            val builder = CallableBuilderConfiguration(
                    Collections.singletonList(constructorInfo), element as JetElement, file, editor, false, kind == PLAIN_CLASS || kind == TRAIT
            ).createBuilder()
            builder.placement = CallablePlacement.NoReceiver(targetParent)
            project.executeCommand(getText()) { builder.build() }
        }
    }
}
