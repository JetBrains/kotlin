/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.findUsages

import com.intellij.ide.IdeBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.core.util.showYesNoCancelDialog
import org.jetbrains.kotlin.idea.frontend.api.analyseInModalWindow
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithKind
import org.jetbrains.kotlin.idea.refactoring.CHECK_SUPER_METHODS_YES_NO_DIALOG
import org.jetbrains.kotlin.idea.refactoring.formatPsiClass
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext

class KotlinFindUsagesSupportFirImpl : KotlinFindUsagesSupport {
    override fun isCallReceiverRefersToCompanionObject(element: KtElement, companionObject: KtObjectDeclaration): Boolean {
        return false
    }

    override fun isDataClassComponentFunction(element: KtParameter): Boolean {
        return false
    }

    override fun getTopMostOverriddenElementsToHighlight(target: PsiElement): List<PsiElement> {
        return emptyList()
    }

    override fun tryRenderDeclarationCompactStyle(declaration: KtDeclaration): String? {
        return (declaration as? KtNamedDeclaration)?.name ?: "SUPPORT FOR FIR"
    }

    override fun isConstructorUsage(psiReference: PsiReference, ktClassOrObject: KtClassOrObject): Boolean {
        return false
    }

    override fun checkSuperMethods(declaration: KtDeclaration, ignore: Collection<PsiElement>?, actionString: String): List<PsiElement> {

        if (!declaration.hasModifier(KtTokens.OVERRIDE_KEYWORD)) return listOf(declaration)

        data class AnalyzedModel(
            val declaredClassRender: String,
            val overriddenDeclarationsAndRenders: Map<PsiElement, String>
        )

        fun getClassDescription(overriddenElement: PsiElement, containingSymbol: KtSymbolWithKind?): String =
            when (overriddenElement) {
                is KtNamedFunction, is KtProperty, is KtParameter -> (containingSymbol as? KtNamedSymbol)?.name?.asString() ?: "Unknown"  //TODO render symbols
                is PsiMethod -> {
                    val psiClass = overriddenElement.containingClass ?: error("Invalid element: ${overriddenElement.text}")
                    formatPsiClass(psiClass, markAsJava = true, inCode = false)
                }
                else -> error("Unexpected element: ${overriddenElement.getElementTextWithContext()}")
            }.let { "    $it\n" }


        val analyzeResult = analyseInModalWindow(declaration, KotlinBundle.message("find.usages.progress.text.declaration.superMethods")) {
            (declaration.getSymbol() as? KtCallableSymbol)?.let { callableSymbol ->
                ((callableSymbol as? KtSymbolWithKind)?.getContainingSymbol() as? KtClassOrObjectSymbol)?.let { containingClass ->
                    val overriddenSymbols = callableSymbol.getOverriddenSymbols(containingClass)

                    val renderToPsi = overriddenSymbols.mapNotNull {
                        it.psi?.let { psi ->
                            psi to getClassDescription(psi, (it as? KtSymbolWithKind)?.getContainingSymbol())
                        }
                    }

                    val filteredDeclarations =
                        if (ignore != null) renderToPsi.filter { ignore.contains(it.first) } else renderToPsi

                    val renderedClass = containingClass.name.asString() //TODO render class

                    AnalyzedModel(renderedClass, filteredDeclarations.toMap())
                }
            }
        } ?: return listOf(declaration)

        if (analyzeResult.overriddenDeclarationsAndRenders.isEmpty()) return listOf(declaration)

        val message = KotlinBundle.message(
            "override.declaration.x.overrides.y.in.class.list",
            analyzeResult.declaredClassRender,
            "\n${analyzeResult.overriddenDeclarationsAndRenders.values.joinToString(separator = "")}",
            actionString
        )

        val exitCode = showYesNoCancelDialog(
            CHECK_SUPER_METHODS_YES_NO_DIALOG,
            declaration.project, message, IdeBundle.message("title.warning"), Messages.getQuestionIcon(), Messages.YES
        )

        return when (exitCode) {
            Messages.YES -> listOf(declaration) + analyzeResult.overriddenDeclarationsAndRenders.keys
            Messages.NO -> listOf(declaration)
            else -> emptyList()
        }
    }

    override fun sourcesAndLibraries(delegate: GlobalSearchScope, project: Project): GlobalSearchScope {
        return delegate
    }
}