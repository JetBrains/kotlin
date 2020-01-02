/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

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

class KotlinAddRequiredModuleFix(module: PsiJavaModule, private val requiredName: String) :
    LocalQuickFixAndIntentionActionOnPsiElement(module) {
    @Suppress("InvalidBundleOrProperty")
    override fun getFamilyName(): String = "Add 'requires' directive to module-info.java"

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