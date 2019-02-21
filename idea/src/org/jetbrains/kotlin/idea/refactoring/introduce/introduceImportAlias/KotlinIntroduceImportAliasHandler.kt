/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.introduce.introduceImportAlias

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.RefactoringActionHandler
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.refactoring.rename.KotlinRenameDispatcherHandler
import org.jetbrains.kotlin.idea.refactoring.rename.findElementForRename
import org.jetbrains.kotlin.idea.refactoring.selectElement
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.references.resolveMainReferenceToDescriptors
import org.jetbrains.kotlin.idea.search.usagesSearch.isImportUsage
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinFunctionShortNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinPropertyShortNameIndex
import org.jetbrains.kotlin.idea.util.ImportInsertHelperImpl
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElement
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.resolve.PropertyImportedFromObject
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.resolve.scopes.utils.findFunction
import org.jetbrains.kotlin.resolve.scopes.utils.findPackage
import org.jetbrains.kotlin.resolve.scopes.utils.findVariable
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor

object KotlinIntroduceImportAliasHandler : RefactoringActionHandler {
    const val REFACTORING_NAME = "Introduce Import Alias"

    fun doRefactoring(project: Project, editor: Editor, element: KtNameReferenceExpression) {
        val fqName = element.resolveMainReferenceToDescriptors().firstOrNull()?.importableFqName ?: return
        val file = element.containingKtFile
        val declarationDescriptors = file.resolveImportReference(fqName)

        @Suppress("UNCHECKED_CAST")
        val usages = declarationDescriptors
            .flatMap { findPsiElements(project, file, it) }
            .flatMap {
                ReferencesSearch.search(it, file.useScope).findAll() as List<KtSimpleNameReference>
            }

        val suggestedName = suggestedName(element.mainReference.value, file.getResolutionScope())
        ImportInsertHelperImpl.addImport(project, file, fqName, false, Name.identifier(suggestedName))
        replaceUsages(usages, suggestedName)
        cleanImport(file, fqName)

        if (!ApplicationManager.getApplication().isUnitTestMode) {
            invokeRename(project, editor, file)
        }
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext?) {
        if (file !is KtFile) return
        selectElement(editor, file, listOf(CodeInsightUtils.ElementKind.EXPRESSION)) {
            doRefactoring(project, editor, it as KtNameReferenceExpression)
        }
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        throw AssertionError("${KotlinIntroduceImportAliasHandler.REFACTORING_NAME} can only be invoked from editor")
    }
}

private fun cleanImport(file: KtFile, fqName: FqName) {
    file.importDirectives.find { it.alias == null && fqName == it.importedFqName }?.delete()
}

private fun findPsiElements(project: Project, file: KtFile, descriptor: DeclarationDescriptor): Collection<PsiElement> {
    descriptor.findPsi()?.let { return listOf(it) }
    val fqName = descriptor.importableFqName ?: return emptyList()
    val resolveScope = file.resolveScope
    return when (descriptor) {
        is DeserializedClassDescriptor -> KotlinFullClassNameIndex.getInstance()[fqName.asString(), project, resolveScope]
        is DeserializedSimpleFunctionDescriptor -> KotlinFunctionShortNameIndex.getInstance()[fqName.shortName().asString(), project, resolveScope]
        is PropertyImportedFromObject -> KotlinPropertyShortNameIndex.getInstance()[fqName.shortName().asString(), project, resolveScope]
        else -> emptyList()
    }.filter { fqName == it.fqName }
}

private fun suggestedName(oldName: String, scope: LexicalScope): String =
    KotlinNameSuggester.suggestNameByName(oldName, fun(name: String): Boolean {
        if (oldName == name) return false
        val identifier = Name.identifier(name)
        return scope.findVariable(identifier, NoLookupLocation.FROM_IDE) == null
                && scope.findFunction(identifier, NoLookupLocation.FROM_IDE) == null
                && scope.findClassifier(identifier, NoLookupLocation.FROM_IDE) == null
                && scope.findPackage(identifier) == null
    })

private fun invokeRename(project: Project, editor: Editor, file: KtFile) {
    val elementToRename = file.findElementForRename<KtSimpleNameExpression>(editor.caretModel.offset) ?: return
    val dataContext = SimpleDataContext.getSimpleContext(
        CommonDataKeys.PSI_ELEMENT.name,
        elementToRename,
        (editor as? EditorEx)?.dataContext
    )

    PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)
    KotlinRenameDispatcherHandler().invoke(project, editor, file, dataContext)
}

private fun replaceUsages(usages: List<KtSimpleNameReference>, newName: String) {
    usages.filter { !it.isImportUsage() }
        .reversed() // case: inner element
        .map {
            val newExpression = it.handleElementRename(newName) as KtNameReferenceExpression
            val qualifiedElement = newExpression.getQualifiedElement()
            if (qualifiedElement != newExpression) {
                val parent = newExpression.parent
                if (parent is KtCallExpression || parent is KtUserType) {
                    newExpression.siblings(forward = false, withItself = false).forEach(PsiElement::delete)
                    qualifiedElement.replace(parent)
                } else qualifiedElement.replace(newExpression)
            }
        }
}