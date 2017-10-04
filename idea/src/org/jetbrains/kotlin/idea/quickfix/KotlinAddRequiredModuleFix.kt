/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.quickfix

/*
import com.intellij.codeInsight.daemon.QuickFixBundle
import com.intellij.codeInsight.daemon.impl.analysis.JavaModuleGraphUtil
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiUtil
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.idea.util.findRequireDirective
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

class KotlinAddRequiredModuleFix(module: PsiJavaModule, private val requiredName: String) : LocalQuickFixAndIntentionActionOnPsiElement(module) {
    @Suppress("InvalidBundleOrProperty")
    override fun getFamilyName(): String = QuickFixBundle.message("module.info.add.requires.family.name")
    @Suppress("InvalidBundleOrProperty")
    override fun getText(): String = QuickFixBundle.message("module.info.add.requires.name", requiredName)

    override fun startInWriteAction() = true

    override fun isAvailable(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement): Boolean {
        return PsiUtil.isLanguageLevel9OrHigher(file) &&
               startElement is PsiJavaModule &&
               startElement.getManager().isInProject(startElement) &&
               getLBrace(startElement) != null
    }

    override fun invoke(project: Project, file: PsiFile, editor: Editor?, startElement: PsiElement, endElement: PsiElement) {
        addModuleRequirement(startElement as PsiJavaModule, requiredName)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val expression = diagnostic.psiElement as? KtExpression ?: return null
            val javaModule = JavaModuleGraphUtil.findDescriptorByElement(expression) ?: return null

            val dependDiagnostic = DiagnosticFactory.cast(diagnostic, ErrorsJvm.JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE)
            val moduleName = dependDiagnostic.a

            return KotlinAddRequiredModuleFix(javaModule, moduleName)
        }

        fun addModuleRequirement(module: PsiJavaModule, requiredName: String): Boolean {
            if (!module.isValid) return false
            if (findRequireDirective(module, requiredName) != null) return false

            val parserFacade = JavaPsiFacade.getInstance(module.project).parserFacade
            val tempModule = parserFacade.createModuleFromText("module TempModuleName { requires $requiredName; }")
            val requiresStatement = tempModule.requires.first()

            val addingPlace = findAddingPlace(module) ?: return false
            addingPlace.parent.addAfter(requiresStatement, addingPlace)

            return true
        }

        private fun getLBrace(module: PsiJavaModule): PsiElement? {
            val nameElement = module.nameIdentifier
            var element: PsiElement? = nameElement.nextSibling
            while (element != null) {
                if (PsiUtil.isJavaToken(element, JavaTokenType.LBRACE)) {
                    return element
                }
                element = element.nextSibling
            }
            return null // module-info is incomplete
        }

        private fun findAddingPlace(module: PsiJavaModule): PsiElement? {
            val addingPlace = module.requires.lastOrNull()
            return addingPlace ?: getLBrace(module)
        }
    }
}

*/

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.psi.PsiJavaModule
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.diagnostics.Diagnostic

class KotlinAddRequiredModuleFix {
    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            return null
        }

        fun addModuleRequirement(module: PsiJavaModule, requiredName: String): Boolean {
            return false
        }
    }
}