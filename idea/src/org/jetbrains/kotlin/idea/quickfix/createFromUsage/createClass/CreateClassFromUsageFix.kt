/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.ide.util.DirectoryChooserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.*
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.core.getFqNameWithImplicitPrefix
import org.jetbrains.kotlin.idea.core.quoteIfNeeded
import org.jetbrains.kotlin.idea.core.util.CodeInsightUtils
import org.jetbrains.kotlin.idea.quickfix.IntentionActionPriority
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.CreateFromUsageFixBase
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.*
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass.ClassKind.*
import org.jetbrains.kotlin.idea.refactoring.SeparateFileWrapper
import org.jetbrains.kotlin.idea.refactoring.canRefactor
import org.jetbrains.kotlin.idea.refactoring.chooseContainerElementIfNecessary
import org.jetbrains.kotlin.idea.refactoring.getOrCreateKotlinFile
import org.jetbrains.kotlin.idea.refactoring.ui.CreateKotlinClassDialog
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.projectStructure.module
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.utils.SmartList
import java.util.*
import com.intellij.codeInsight.daemon.impl.quickfix.ClassKind as IdeaClassKind

enum class ClassKind(val keyword: String, val description: String) {
    PLAIN_CLASS("class", "class"),
    ENUM_CLASS("enum class", "enum"),
    ENUM_ENTRY("", "enum constant"),
    ANNOTATION_CLASS("annotation class", "annotation"),
    INTERFACE("interface", "interface"),
    OBJECT("object", "object"),
    DEFAULT("", "") // Used as a placeholder and must be replaced with one of the kinds above
}

fun ClassKind.toIdeaClassKind() = IdeaClassKind { this@toIdeaClassKind.description.capitalize() }

val ClassKind.actionPriority: IntentionActionPriority
    get() = if (this == ANNOTATION_CLASS) IntentionActionPriority.LOW else IntentionActionPriority.NORMAL

data class ClassInfo(
    val kind: ClassKind = DEFAULT,
    val name: String,
    private val targetParents: List<PsiElement>,
    val expectedTypeInfo: TypeInfo,
    val inner: Boolean = false,
    val open: Boolean = false,
    val typeArguments: List<TypeInfo> = Collections.emptyList(),
    val parameterInfos: List<ParameterInfo> = Collections.emptyList()
) {
    val applicableParents by lazy {
        targetParents.filter {
            if (kind == OBJECT && it is KtClass && (it.isInner() || it.isLocal)) return@filter false
            true
        }
    }
}

open class CreateClassFromUsageFix<E : KtElement> protected constructor(
    element: E,
    private val classInfo: ClassInfo
) : CreateFromUsageFixBase<E>(element) {
    override fun getText() = "Create ${classInfo.kind.description} '${classInfo.name}'"

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
        with(classInfo) {
            if (kind == DEFAULT) return false
            if (applicableParents.isEmpty()) return false
            applicableParents.forEach {
                if (it is PsiClass) {
                    if (kind == OBJECT || kind == ENUM_ENTRY) return false
                    if (it.isInterface && inner) return false
                }
            }

        }
        return true
    }

    override fun startInWriteAction() = false

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        if (editor == null) return

        val applicableParents = SmartList<PsiElement>().also { parents ->
            classInfo.applicableParents.filterNotTo(parents) { element ->
                element is KtClassOrObject && element.superTypeListEntries.any {
                    when (it) {
                        is KtDelegatedSuperTypeEntry, is KtSuperTypeEntry -> it.typeAsUserType == this.element
                        is KtSuperTypeCallEntry -> it == this.element
                        else -> false
                    }
                }
            }

            if (classInfo.kind != ENUM_ENTRY && parents.find { it is PsiPackage } == null) {
                parents += SeparateFileWrapper(PsiManager.getInstance(project))
            }
        }

        if (ApplicationManager.getApplication().isUnitTestMode) {
            val targetParent = applicableParents.firstOrNull { element ->
                if (element is PsiPackage) false else element.allChildren.any { it is PsiComment && it.text == "// TARGET_PARENT:" }
            } ?: classInfo.applicableParents.last()
            return doInvoke(targetParent, editor, file)
        }

        chooseContainerElementIfNecessary(applicableParents.reversed(), editor, "Choose class container", true, { it }) {
            doInvoke(it, editor, file)
        }
    }

    private fun createFileByPackage(
        psiPackage: PsiPackage,
        editor: Editor,
        originalFile: KtFile
    ): KtFile? {
        val directories = psiPackage.directories.filter { it.canRefactor() }
        assert(directories.isNotEmpty()) { "Package '${psiPackage.qualifiedName}' must be refactorable" }

        val currentModule = ModuleUtilCore.findModuleForPsiElement(originalFile)
        val preferredDirectory =
            directories.firstOrNull { ModuleUtilCore.findModuleForPsiElement(it) == currentModule }
                ?: directories.firstOrNull()

        val targetDirectory = if (directories.size > 1 && !ApplicationManager.getApplication().isUnitTestMode) {
            DirectoryChooserUtil.chooseDirectory(directories.toTypedArray(), preferredDirectory, originalFile.project, HashMap())
        } else {
            preferredDirectory
        } ?: return null

        val fileName = "${classInfo.name}.${KotlinFileType.INSTANCE.defaultExtension}"
        val targetFile = getOrCreateKotlinFile(fileName, targetDirectory)
        if (targetFile == null) {
            val filePath = "${targetDirectory.virtualFile.path}/$fileName"
            CodeInsightUtils.showErrorHint(
                targetDirectory.project,
                editor,
                "File $filePath already exists but does not correspond to Kotlin file",
                "Create file",
                null
            )
        }
        return targetFile
    }

    private fun doInvoke(selectedParent: PsiElement, editor: Editor, file: KtFile, startCommand: Boolean = true) {
        val className = classInfo.name

        if (selectedParent is SeparateFileWrapper) {
            if (ApplicationManager.getApplication().isUnitTestMode) {
                return doInvoke(file, editor, file)
            }

            val ideaClassKind = classInfo.kind.toIdeaClassKind()
            val defaultPackageFqName = file.packageFqName
            val dialog = object : CreateKotlinClassDialog(
                file.project,
                "Create ${ideaClassKind.description.capitalize()}",
                className,
                defaultPackageFqName.asString(),
                ideaClassKind,
                false,
                file.module
            ) {
                override fun reportBaseInSourceSelectionInTest() = true
            }
            dialog.show()
            if (dialog.exitCode != DialogWrapper.OK_EXIT_CODE) return

            val targetDirectory = dialog.targetDirectory ?: return
            val fileName = "$className.${KotlinFileType.EXTENSION}"
            val packageFqName = targetDirectory.getFqNameWithImplicitPrefix()?.quoteIfNeeded()

            file.project.executeWriteCommand(text) {
                val targetFile = getOrCreateKotlinFile(fileName, targetDirectory, (packageFqName ?: defaultPackageFqName).asString())
                if (targetFile != null) {
                    doInvoke(targetFile, editor, file, false)
                }
            }
            return
        }

        val element = element ?: return

        runWriteAction<Unit> {
            with(classInfo) {
                val targetParent =
                    when (selectedParent) {
                        is KtElement, is PsiClass -> selectedParent
                        is PsiPackage -> createFileByPackage(selectedParent, editor, file)
                        else -> throw AssertionError("Unexpected element: " + selectedParent.text)
                    } ?: return@runWriteAction
                val constructorInfo = ClassWithPrimaryConstructorInfo(
                    classInfo,
                    // Need for #KT-22137
                    if (expectedTypeInfo.isUnit) TypeInfo.Empty else expectedTypeInfo
                )
                val builder = CallableBuilderConfiguration(
                    Collections.singletonList(constructorInfo),
                    element,
                    file,
                    editor,
                    false,
                    kind == PLAIN_CLASS || kind == INTERFACE
                ).createBuilder()
                builder.placement = CallablePlacement.NoReceiver(targetParent)

                fun buildClass() {
                    builder.build {
                        if (targetParent !is KtFile || targetParent == file) return@build
                        val targetPackageFqName = targetParent.packageFqName
                        if (targetPackageFqName == file.packageFqName) return@build
                        val reference = (element.getQualifiedElementSelector() as? KtSimpleNameExpression)?.mainReference ?: return@build
                        reference.bindToFqName(
                            targetPackageFqName.child(Name.identifier(className)),
                            KtSimpleNameReference.ShorteningMode.FORCED_SHORTENING
                        )
                    }
                }

                if (startCommand) {
                    file.project.executeCommand(text, command = ::buildClass)
                } else {
                    buildClass()
                }
            }
        }
    }

    private class LowPriorityCreateClassFromUsageFix<E : KtElement>(
        element: E,
        classInfo: ClassInfo
    ) : CreateClassFromUsageFix<E>(element, classInfo), LowPriorityAction

    private class HighPriorityCreateClassFromUsageFix<E : KtElement>(
        element: E,
        classInfo: ClassInfo
    ) : CreateClassFromUsageFix<E>(element, classInfo), HighPriorityAction

    companion object {
        fun <E : KtElement> create(element: E, classInfo: ClassInfo): CreateClassFromUsageFix<E> {
            return when (classInfo.kind.actionPriority) {
                IntentionActionPriority.NORMAL -> CreateClassFromUsageFix(element, classInfo)
                IntentionActionPriority.LOW -> LowPriorityCreateClassFromUsageFix(element, classInfo)
                IntentionActionPriority.HIGH -> HighPriorityCreateClassFromUsageFix(element, classInfo)
            }
        }
    }
}

private val TypeInfo.isUnit: Boolean
    get() = ((this as? TypeInfo.DelegatingTypeInfo)?.delegate as? TypeInfo.ByType)?.theType?.isUnit() == true
