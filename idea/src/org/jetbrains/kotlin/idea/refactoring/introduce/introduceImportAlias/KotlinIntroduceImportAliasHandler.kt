/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.introduce.introduceImportAlias

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.refactoring.rename.inplace.VariableInplaceRenamer
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.refactoring.selectElement
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.references.resolveMainReferenceToDescriptors
import org.jetbrains.kotlin.idea.search.fileScope
import org.jetbrains.kotlin.idea.search.usagesSearch.isImportUsage
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinFunctionShortNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinPropertyShortNameIndex
import org.jetbrains.kotlin.idea.util.ImportInsertHelperImpl
import org.jetbrains.kotlin.idea.util.getAllAccessibleFunctions
import org.jetbrains.kotlin.idea.util.getAllAccessibleVariables
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElement
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.resolve.PropertyImportedFromObject
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.resolve.scopes.utils.findPackage
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor

object KotlinIntroduceImportAliasHandler : RefactoringActionHandler {
    const val REFACTORING_NAME = "Introduce Import Alias"

    fun doRefactoring(project: Project, editor: Editor, element: KtNameReferenceExpression) {
        val fqName = element.resolveMainReferenceToDescriptors().firstOrNull()?.importableFqName ?: return
        val file = element.containingKtFile
        val declarationDescriptors = file.resolveImportReference(fqName)

        val fileSearchScope = file.fileScope()
        val resolveScope = file.resolveScope
        val usages = declarationDescriptors.flatMap { descriptor ->
            val isExtension = descriptor.isExtension
            findPsiElements(project, resolveScope, descriptor).flatMap {
                ReferencesSearch.search(it, fileSearchScope)
                    .findAll()
                    .map { reference -> UsageContext(reference as KtSimpleNameReference, isExtension = isExtension) }
            }
        }

        val oldName = element.mainReference.value
        val scopes = usages.mapNotNull {
            val expression = it.reference.element
            expression.getResolutionScope(expression.analyze(BodyResolveMode.PARTIAL_FOR_COMPLETION))
        }.distinct()

        val validator = fun(name: String): Boolean {
            if (oldName == name) return false
            val identifier = Name.identifier(name)

            return scopes.all { scope ->
                scope.getAllAccessibleFunctions(identifier).isEmpty()
                        && scope.getAllAccessibleVariables(identifier).isEmpty()
                        && scope.findClassifier(identifier, NoLookupLocation.FROM_IDE) == null
                        && scope.findPackage(identifier) == null
            }
        }

        val suggestionsName = KotlinNameSuggester.suggestNamesByFqName(fqName, validator = validator)
        val newName = suggestionsName.first()
        val newDirective = ImportInsertHelperImpl.addImport(project, file, fqName, false, Name.identifier(newName))

        replaceUsages(usages, newName)
        cleanImport(file, fqName)

        if (!ApplicationManager.getApplication().isUnitTestMode) {
            invokeRename(project, editor, newDirective.alias, suggestionsName)
        }
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext?) {
        if (file !is KtFile) return
        selectElement(editor, file, listOf(CodeInsightUtils.ElementKind.EXPRESSION)) {
            doRefactoring(project, editor, it as KtNameReferenceExpression)
        }
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        throw AssertionError("$REFACTORING_NAME can only be invoked from editor")
    }
}

private data class UsageContext(val reference: KtSimpleNameReference, val isExtension: Boolean)

private fun cleanImport(file: KtFile, fqName: FqName) {
    file.importDirectives.find { it.alias == null && fqName == it.importedFqName }?.delete()
}

private fun findPsiElements(project: Project, resolveScope: GlobalSearchScope, descriptor: DeclarationDescriptor): Collection<PsiElement> {
    descriptor.findPsi()?.let { return listOf(it) }
    val fqName = descriptor.importableFqName ?: return emptyList()
    return when (descriptor) {
        is DeserializedClassDescriptor -> KotlinFullClassNameIndex.getInstance()[fqName.asString(), project, resolveScope]
        is DeserializedSimpleFunctionDescriptor -> KotlinFunctionShortNameIndex.getInstance()[fqName.shortName().asString(), project, resolveScope]
        is PropertyImportedFromObject -> KotlinPropertyShortNameIndex.getInstance()[fqName.shortName().asString(), project, resolveScope]
        else -> emptyList()
    }.filter { fqName == it.fqName }
}

private fun invokeRename(
    project: Project,
    editor: Editor,
    elementToRename: PsiNamedElement?,
    suggestionsName: Collection<String>
) {
    PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)
    val rename = VariableInplaceRenamer(elementToRename, editor, project)
    rename.performInplaceRefactoring(LinkedHashSet(suggestionsName))
}

private fun replaceUsages(usages: List<UsageContext>, newName: String) {
    usages.filter { !it.reference.isImportUsage() }
        .reversed() // case: inner element
        .forEach {
            val newExpression = it.reference.handleElementRename(newName) as KtNameReferenceExpression
            if (it.isExtension) {
                newExpression.getQualifiedElementSelector()?.replace(newExpression)
                return@forEach
            }

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