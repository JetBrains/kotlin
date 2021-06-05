/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.fixes

import com.intellij.codeInsight.daemon.QuickFixBundle
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.hint.QuestionAction
import com.intellij.codeInspection.HintAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.fir.api.fixes.diagnosticFixFactory
import org.jetbrains.kotlin.idea.fir.low.level.api.IndexHelper
import org.jetbrains.kotlin.idea.fir.low.level.api.util.createScopeForModuleLibraries
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics.KtFirDiagnostic
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.addImportToFile
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.idea.quickfix.QuickFixActionBase
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.KtPsiUtil.isSelectorInQualified
import org.jetbrains.kotlin.psi.psiUtil.unwrapNullability

internal class ImportQuickFix(
    element: KtElement,
    private val importCandidates: List<FqName>
) : QuickFixActionBase<KtElement>(element), HintAction {

    init {
        require(importCandidates.isNotEmpty())
    }

    override fun getText(): String = KotlinBundle.message("fix.import")

    override fun getFamilyName(): String = KotlinBundle.message("fix.import")

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        if (file !is KtFile) return

        createAddImportAction(project, editor, file).execute()
    }

    private fun createAddImportAction(project: Project, editor: Editor, file: KtFile): QuestionAction {
        return ImportQuestionAction(project, editor, file, importCandidates)
    }

    override fun showHint(editor: Editor): Boolean {
        val element = element ?: return false
        val file = element.containingKtFile
        val project = file.project

        val elementRange = element.textRange
        val autoImportHintText = "Import ${importCandidates.first().asString()}?"

        HintManager.getInstance().showQuestionHint(
            editor,
            autoImportHintText,
            elementRange.startOffset,
            elementRange.endOffset,
            createAddImportAction(project, editor, file)
        )

        return true
    }

    private val modificationCountOnCreate: Long = PsiModificationTracker.SERVICE.getInstance(element.project).modificationCount

    /**
     * This is a safe-guard against showing hint after the quickfix have been applied.
     *
     * Inspired by the org.jetbrains.kotlin.idea.quickfix.ImportFixBase.isOutdated
     */
    private fun isOutdated(project: Project): Boolean {
        return modificationCountOnCreate != PsiModificationTracker.SERVICE.getInstance(project).modificationCount
    }

    override fun isAvailableImpl(project: Project, editor: Editor?, file: PsiFile): Boolean {
        return super.isAvailableImpl(project, editor, file) && !isOutdated(project)
    }

    private class ImportQuestionAction(
        private val project: Project,
        private val editor: Editor,
        private val file: KtFile,
        private val importCandidates: List<FqName>
    ) : QuestionAction {

        init {
            require(importCandidates.isNotEmpty())
        }

        override fun execute(): Boolean {
            val unambiguousImport = importCandidates.singleOrNull()
            if (unambiguousImport != null) {
                addImport(unambiguousImport)
                return true
            }

            createImportSelectorPopup().showInBestPositionFor(editor)

            return true
        }

        private fun createImportSelectorPopup(): JBPopup {
            return JBPopupFactory.getInstance()
                .createPopupChooserBuilder(importCandidates)
                .setTitle(KotlinBundle.message("action.add.import.chooser.title"))
                .setItemChosenCallback { selectedValue: FqName -> addImport(selectedValue) }
                .createPopup()
        }

        private fun addImport(nameToImport: FqName) {
            project.executeWriteCommand(QuickFixBundle.message("add.import")) {
                addImportToFile(project, file, nameToImport)
            }
        }
    }

    internal companion object {
        val FACTORY = diagnosticFixFactory(KtFirDiagnostic.UnresolvedReference::class) { diagnostic ->
            val element = diagnostic.psi

            val indexHelper = IndexHelper(element.project, createSearchScope(element))

            val quickFix = when (element) {
                is KtTypeReference -> createImportTypeFix(indexHelper, element)
                is KtNameReferenceExpression -> createImportNameFix(indexHelper, element)
                else -> null
            }

            listOfNotNull(quickFix)
        }

        private fun createSearchScope(element: PsiElement): GlobalSearchScope {
            val contentScope = element.getModuleInfo().contentScope()
            val librariesScope = element.module?.let(::createScopeForModuleLibraries)
                ?: GlobalSearchScope.EMPTY_SCOPE

            return contentScope.uniteWith(librariesScope)
        }

        private fun KtAnalysisSession.createImportNameFix(
            indexHelper: IndexHelper,
            element: KtNameReferenceExpression
        ): ImportQuickFix? {
            if (isSelectorInQualified(element)) return null

            val firFile = element.containingKtFile.getFileSymbol()
            val unresolvedName = element.getReferencedNameAsName()

            val isVisible: (KtSymbol) -> Boolean =
                { it !is KtSymbolWithVisibility || isVisible(it, firFile, null, element) }

            val callableCandidates = collectCallableCandidates(indexHelper, unresolvedName, isVisible)
            val typeCandidates = collectTypesCandidates(indexHelper, unresolvedName, isVisible)

            val importCandidates = (callableCandidates + typeCandidates).distinct()
            if (importCandidates.isEmpty()) return null

            return ImportQuickFix(element, importCandidates)
        }

        private fun KtAnalysisSession.createImportTypeFix(indexHelper: IndexHelper, element: KtTypeReference): ImportQuickFix? {
            val firFile = element.containingKtFile.getFileSymbol()
            val unresolvedName = element.typeName ?: return null

            val isVisible: (KtNamedClassOrObjectSymbol) -> Boolean =
                { isVisible(it, firFile, null, element) }

            val acceptableClasses = collectTypesCandidates(indexHelper, unresolvedName, isVisible).distinct()
            if (acceptableClasses.isEmpty()) return null

            return ImportQuickFix(element, acceptableClasses)
        }

        private fun KtAnalysisSession.collectCallableCandidates(
            indexHelper: IndexHelper,
            unresolvedName: Name,
            isVisible: (KtCallableSymbol) -> Boolean
        ): List<FqName> {
            val callablesCandidates = indexHelper.getKotlinCallablesByName(unresolvedName)

            return callablesCandidates
                .asSequence()
                .map { it.getSymbol() as KtCallableSymbol }
                .filter(isVisible)
                .mapNotNull { it.callableIdIfNonLocal?.asSingleFqName() }
                .toList()
        }

        private fun KtAnalysisSession.collectTypesCandidates(
            indexHelper: IndexHelper,
            unresolvedName: Name,
            isVisible: (KtNamedClassOrObjectSymbol) -> Boolean
        ): List<FqName> {
            val classesCandidates = indexHelper.getKotlinClassesByName(unresolvedName)

            return classesCandidates.asSequence()
                .mapNotNull { it.getNamedClassOrObjectSymbol() }
                .filter(isVisible)
                .mapNotNull { it.classIdIfNonLocal?.asSingleFqName() }
                .toList()
        }
    }
}

private val KtTypeReference.typeName: Name?
    get() {
        val userType = typeElement?.unwrapNullability() as? KtUserType
        return userType?.referencedName?.let(Name::identifier)
    }