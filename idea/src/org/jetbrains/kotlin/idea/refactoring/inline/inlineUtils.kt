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

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.codeInsight.highlighting.HighlightManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.refactoring.util.RefactoringMessageDialog
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.analysis.analyzeInContext
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.codeInliner.CodeToInline
import org.jetbrains.kotlin.idea.codeInliner.CodeToInlineBuilder
import org.jetbrains.kotlin.idea.core.copied
import org.jetbrains.kotlin.idea.refactoring.move.ContainerChangeInfo
import org.jetbrains.kotlin.idea.refactoring.move.ContainerInfo
import org.jetbrains.kotlin.idea.refactoring.move.postProcessMoveUsages
import org.jetbrains.kotlin.idea.refactoring.move.processInternalReferencesToUpdateOnPackageNameChange
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import java.util.*

fun highlightElements(project: Project, editor: Editor?, elements: List<PsiElement>) {
    if (editor == null || ApplicationManager.getApplication().isUnitTestMode) return

    val editorColorsManager = EditorColorsManager.getInstance()
    val searchResultsAttributes = editorColorsManager.globalScheme.getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES)
    val highlightManager = HighlightManager.getInstance(project)
    highlightManager.addOccurrenceHighlights(editor, elements.toTypedArray(), searchResultsAttributes, true, null)
}

fun showDialog(
        project: Project,
        name: String,
        title: String,
        declaration: KtNamedDeclaration,
        usages: List<KtElement>,
        helpTopic: String? = null
): Boolean {
    if (ApplicationManager.getApplication().isUnitTestMode) return true

    val kind = when (declaration) {
        is KtProperty -> if (declaration.isLocal) "local variable" else "property"
        is KtTypeAlias -> "type alias"
        else -> return false
    }
    val dialog = RefactoringMessageDialog(
            title,
            "Inline " + kind + " '" + name + "'? " + RefactoringBundle.message("occurences.string", usages.size),
            helpTopic,
            "OptionPane.questionIcon",
            true,
            project
    )
    dialog.show()
    return dialog.isOK
}

internal var KtSimpleNameExpression.internalUsageInfos: MutableMap<FqName, (KtSimpleNameExpression) -> UsageInfo?>?
        by CopyableUserDataProperty(Key.create("INTERNAL_USAGE_INFOS"))

internal fun preProcessInternalUsages(element: KtElement, usages: Collection<KtElement>) {
    val mainFile = element.containingKtFile
    val targetPackages = usages.mapTo(LinkedHashSet()) { it.containingKtFile.packageFqName }
    for (targetPackage in targetPackages) {
        if (targetPackage == mainFile.packageFqName) continue
        val packageNameInfo = ContainerChangeInfo(ContainerInfo.Package(mainFile.packageFqName), ContainerInfo.Package(targetPackage))
        element.processInternalReferencesToUpdateOnPackageNameChange(packageNameInfo) { expr, factory ->
            val infos =
                    expr.internalUsageInfos
                    ?: LinkedHashMap<FqName, (KtSimpleNameExpression) -> UsageInfo?>().apply { expr.internalUsageInfos = this }
            infos[targetPackage] = factory
        }
    }
}

internal fun <E : KtElement> postProcessInternalReferences(inlinedElement: E): E? {
    val pointer = inlinedElement.createSmartPointer()
    val targetPackage = inlinedElement.containingKtFile.packageFqName
    val expressionsToProcess = inlinedElement.collectDescendantsOfType<KtSimpleNameExpression> { it.internalUsageInfos != null }
    val internalUsages = expressionsToProcess.mapNotNull { it.internalUsageInfos!![targetPackage]?.invoke(it) }
    expressionsToProcess.forEach { it.internalUsageInfos = null }
    postProcessMoveUsages(internalUsages)
    return pointer.element
}

internal fun buildCodeToInline(
        declaration: KtDeclaration,
        returnType: KotlinType?,
        isReturnTypeExplicit: Boolean,
        bodyOrInitializer: KtExpression,
        isBlockBody: Boolean,
        editor: Editor?
): CodeToInline? {
    val bodyCopy = bodyOrInitializer.copied()

    val expectedType = if (!isBlockBody && isReturnTypeExplicit)
        returnType ?: TypeUtils.NO_EXPECTED_TYPE
    else
        TypeUtils.NO_EXPECTED_TYPE

    fun analyzeBodyCopy(): BindingContext {
        return bodyCopy.analyzeInContext(bodyOrInitializer.getResolutionScope(),
                                         contextExpression = bodyOrInitializer,
                                         expectedType = expectedType)
    }

    val descriptor = declaration.unsafeResolveToDescriptor()
    val builder = CodeToInlineBuilder(descriptor as CallableDescriptor, declaration.getResolutionFacade())
    if (isBlockBody) {
        bodyCopy as KtBlockExpression
        val statements = bodyCopy.statements

        val returnStatements = bodyCopy.collectDescendantsOfType<KtReturnExpression> {
            it.getLabelName().let { it == null || it == declaration.name }
        }

        val lastReturn = statements.lastOrNull() as? KtReturnExpression
        if (returnStatements.any { it != lastReturn }) {
            val message = RefactoringBundle.getCannotRefactorMessage(
                    if (returnStatements.size > 1)
                        "Inline Function is not supported for functions with multiple return statements."
                    else
                        "Inline Function is not supported for functions with return statements not at the end of the body."
            )
            CommonRefactoringUtil.showErrorHint(declaration.project, editor, message, "Inline Function", null)
            return null
        }

        return builder.prepareCodeToInline(lastReturn?.returnedExpression,
                                           statements.dropLast(returnStatements.size), ::analyzeBodyCopy)
    }
    else {
        return builder.prepareCodeToInline(bodyCopy, emptyList(), ::analyzeBodyCopy)
    }
}
