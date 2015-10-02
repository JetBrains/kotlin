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

package org.jetbrains.kotlin.idea.actions.generate

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.generation.actions.GenerateActionPopupTemplateInjector
import com.intellij.codeInsight.hint.HintManager
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.impl.AllFileTemplatesConfigurable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.PopupChooserBuilder
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiFile
import com.intellij.testIntegration.JavaTestFramework
import com.intellij.testIntegration.TestFramework
import com.intellij.testIntegration.TestIntegrationUtils.MethodKind
import com.intellij.ui.components.JBList
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideMemberChooserObject.BodyType
import org.jetbrains.kotlin.idea.core.overrideImplement.generateUnsupportedOrSuperCall
import org.jetbrains.kotlin.idea.core.refactoring.j2k
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.setupEditorSelection
import org.jetbrains.kotlin.idea.quickfix.generateMember
import org.jetbrains.kotlin.idea.testIntegration.findSuitableFrameworks
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.psi.JetNamedFunction
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.utils.ifEmpty

abstract class KotlinGenerateTestSupportActionBase(
        private val methodKind : MethodKind
) : KotlinGenerateActionBase(), GenerateActionPopupTemplateInjector {
    companion object {
        private fun findTargetClass(editor: Editor, file: PsiFile): JetClassOrObject? {
            val elementAtCaret = file.findElementAt(editor.caretModel.offset) ?: return null
            return elementAtCaret.parentsWithSelf.filterIsInstance<JetClassOrObject>().firstOrNull { !it.isLocal() }
        }

        private fun chooseAndPerform(editor: Editor, frameworks: List<TestFramework>, consumer: (TestFramework) -> Unit) {
            frameworks.ifEmpty { return }
            frameworks.singleOrNull()?.let { return consumer(it) }

            if (ApplicationManager.getApplication().isUnitTestMode) return consumer(frameworks.first())

            val list = JBList(*frameworks.toTypedArray())
            list.cellRenderer = TestFrameworkListCellRenderer()

            PopupChooserBuilder(list)
                    .setFilteringEnabled { (it as TestFramework).name }
                    .setTitle("Choose Framework")
                    .setItemChoosenCallback { consumer(list.selectedValue as TestFramework) }
                    .setMovable(true)
                    .createPopup()
                    .showInBestPositionFor(editor)
        }

        private val BODY_VAR = "\${BODY}"
        private val NAME_VAR = "\${NAME}"

        private val NAME_VALIDATOR = object : InputValidator {
            override fun checkInput(inputString: String) = KotlinNameSuggester.isIdentifier(inputString)
            override fun canClose(inputString: String) = true
        }
    }

    public class SetUp : KotlinGenerateTestSupportActionBase(MethodKind.SET_UP) {
        override fun isApplicableTo(framework: TestFramework, targetClass: JetClassOrObject): Boolean {
            return framework.findSetUpMethod(targetClass.toLightClass()!!) == null
        }
    }

    public class Test : KotlinGenerateTestSupportActionBase(MethodKind.TEST) {
        override fun isApplicableTo(framework: TestFramework, targetClass: JetClassOrObject) = true
    }

    public class Data : KotlinGenerateTestSupportActionBase(MethodKind.DATA) {
        override fun isApplicableTo(framework: TestFramework, targetClass: JetClassOrObject): Boolean {
            if (framework !is JavaTestFramework) return false
            return framework.findParametersMethod(targetClass.toLightClass()) == null
        }
    }

    public class TearDown : KotlinGenerateTestSupportActionBase(MethodKind.TEAR_DOWN) {
        override fun isApplicableTo(framework: TestFramework, targetClass: JetClassOrObject): Boolean {
            return framework.findTearDownMethod(targetClass.toLightClass()!!) == null
        }
    }

    private inner class HandlerImpl : CodeInsightActionHandler {
        override fun startInWriteAction() = false

        override fun invoke(project: Project, editor: Editor, file: PsiFile) {
            val klass = findTargetClass(editor, file) ?: return
            val frameworks = findSuitableFrameworks(klass)
                    .filter { methodKind.getFileTemplateDescriptor(it) != null && isApplicableTo(it, klass) }
            chooseAndPerform(editor, frameworks) { doGenerate(editor, file, klass, it) }
        }

        private fun doGenerate(editor: Editor, file: PsiFile, klass: JetClassOrObject, framework: TestFramework) {
            val project = file.project
            val commandName = "Generate test function"
            project.executeWriteCommand(commandName) {
                try {
                    PsiDocumentManager.getInstance(project).commitAllDocuments()

                    val fileTemplateDescriptor = methodKind.getFileTemplateDescriptor(framework)
                    val fileTemplate = FileTemplateManager.getInstance(project).getCodeTemplate(fileTemplateDescriptor.fileName)
                    var templateText = fileTemplate.text.replace(BODY_VAR, "")
                    if (templateText.contains(NAME_VAR)) {
                        var name = "Name"
                        if (!ApplicationManager.getApplication().isUnitTestMode) {
                            name = Messages.showInputDialog("Choose test name: ", commandName, null, name, NAME_VALIDATOR)
                                   ?: return@executeWriteCommand
                        }

                        templateText = fileTemplate.text.replace(NAME_VAR, name)
                    }
                    val factory = PsiElementFactory.SERVICE.getInstance(project)
                    val psiMethod = factory.createMethodFromText(templateText, null)
                    psiMethod.throwsList.referenceElements.forEach { it.delete() }
                    val function = psiMethod.j2k() as? JetNamedFunction
                    if (function == null) {
                        HintManager.getInstance().showErrorHint(editor, "Couldn't convert Java template to Kotlin")
                        return@executeWriteCommand
                    }
                    val functionInPlace = generateMember(editor, klass, function)

                    val functionDescriptor = functionInPlace.resolveToDescriptor() as FunctionDescriptor
                    val overriddenDescriptors = functionDescriptor.overriddenDescriptors
                    val bodyText = when (overriddenDescriptors.size()) {
                        0 -> generateUnsupportedOrSuperCall(functionDescriptor, BodyType.EMPTY)
                        1 -> generateUnsupportedOrSuperCall(overriddenDescriptors.single(), BodyType.SUPER)
                        else -> generateUnsupportedOrSuperCall(overriddenDescriptors.first(), BodyType.QUALIFIED_SUPER)
                    }
                    functionInPlace.bodyExpression?.delete()
                    functionInPlace.add(JetPsiFactory(project).createBlock(bodyText))

                    if (overriddenDescriptors.isNotEmpty()) {
                        functionInPlace.addModifier(JetTokens.OVERRIDE_KEYWORD)
                    }

                    setupEditorSelection(editor, functionInPlace)
                }
                catch (e: IncorrectOperationException) {
                    HintManager.getInstance().showErrorHint(editor, "Cannot generate method: " + e.getMessage())
                }
            }
        }
    }

    private val handler = HandlerImpl()

    override fun getHandler() = handler

    override fun getTargetClass(editor: Editor, file: PsiFile): JetClassOrObject? {
        return findTargetClass(editor, file)
    }

    override fun isValidForClass(targetClass: JetClassOrObject): Boolean {
        return findSuitableFrameworks(targetClass).any { isApplicableTo(it, targetClass) }
    }

    protected abstract fun isApplicableTo(framework: TestFramework, targetClass: JetClassOrObject): Boolean

    override fun createEditTemplateAction(dataContext: DataContext): AnAction? {
        val project = CommonDataKeys.PROJECT.getData(dataContext) ?: return null
        val editor = CommonDataKeys.EDITOR.getData(dataContext) ?: return null
        val file = CommonDataKeys.PSI_FILE.getData(dataContext) ?: return null

        val targetClass = getTargetClass(editor, file) ?: return null
        val frameworks = findSuitableFrameworks(targetClass).ifEmpty { return null }

        return object : AnAction("Edit Template") {
            override fun actionPerformed(e: AnActionEvent) {
                chooseAndPerform(editor, frameworks) {
                    val descriptor = methodKind.getFileTemplateDescriptor(it)
                    if (descriptor == null) {
                        HintManager.getInstance().showErrorHint(editor, "No template found for ${it.name}:${templatePresentation.text}")
                        return@chooseAndPerform
                    }

                    AllFileTemplatesConfigurable.editCodeTemplate(FileUtil.getNameWithoutExtension(descriptor.fileName), project)
                }
            }
        }
    }
}