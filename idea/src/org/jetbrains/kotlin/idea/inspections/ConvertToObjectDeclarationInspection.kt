/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.refactoring.checkConflictsInteractively
import org.jetbrains.kotlin.idea.refactoring.safeDelete.canDeleteElement
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi.synthetics.findClassDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.utils.collectDescriptorsFiltered

class ConvertToObjectDeclarationInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return classVisitor(fun(clazz: KtClass) {
            if (clazz.isInner() || clazz.isLocal) return
            val declarations = clazz.declarations
            val objDecl = declarations.singleOrNull { it is KtObjectDeclaration } as? KtObjectDeclaration ?: return
            if (!objDecl.isCompanion()) return

            if (clazz.hasClassUsage()) return
            if (clazz.primaryConstructor?.hasUsage() == true) return
            if (declarations.filter { it !is KtObjectDeclaration }.any { it.hasUsage() }) return

            val descriptor = clazz.findClassDescriptor(clazz.analyze())
            if (descriptor.getSuperClassNotAny() != null || descriptor.getSuperInterfaces().isNotEmpty()) return

            val endRange = clazz.nameIdentifier?.endOffset ?: clazz.getDeclarationKeyword()?.endOffset ?: clazz.endOffset
            holder.registerProblem(
                clazz,
                TextRange(0, endRange - clazz.startOffset),
                "Can be converted to object declaration",
                ConvertToObjectDeclarationFix()
            )
        })
    }

    private fun KtClass.hasClassUsage(): Boolean {
        return ReferencesSearch.search(this).any { ref ->
            val element = ref.element
            if (element is KtSimpleNameExpression && element.parent is KtCallExpression) return true
            val possibleImport = ref.element.getParentOfType<KtImportDirective>(false)
            if (possibleImport != null && possibleImport.importPath?.fqName == this.fqName) return true
            if (element.getParentOfType<KtTypeAlias>(false) != null) return true
            return false
        }
    }

    private fun KtDeclaration.hasUsage(): Boolean {
        val psi = this.originalElement ?: return false
        return ReferencesSearch.search(psi).any()
    }

    private class ConvertToObjectDeclarationFix : LocalQuickFix {
        override fun getFamilyName() = "Convert to object declaration"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val clazz = descriptor.psiElement as? KtClass ?: return
            val clazzDescriptor = clazz.resolveToDescriptorIfAny() ?: return
            val obj = clazz.declarations.singleOrNull { it is KtObjectDeclaration } as? KtObjectDeclaration ?: return
            val objName = clazz.name ?: return
            val currentCompanionReferences = ReferencesSearch.search(obj).toList()

            val conflicts = findConflicts(currentCompanionReferences, clazz.nameAsSafeName, clazzDescriptor)
            project.checkConflictsInteractively(conflicts) {
                project.executeWriteCommand(name) {
                    clazz.declarations.forEach { declaration ->
                        if (declaration.canDeleteElement() && declaration != obj) declaration.delete()
                    }
                    removeCompanionReference(objName, clazzDescriptor, currentCompanionReferences)
                    obj.setName(objName)
                    obj.removeModifier(KtTokens.COMPANION_KEYWORD)
                    clazz.replace(obj)
                }
            }
        }

        private fun findConflicts(
            references: List<PsiReference>,
            conflictingName: Name,
            ignoreDescriptor: ClassDescriptor
        ): MultiMap<PsiElement, String> {
            return MultiMap.create<PsiElement, String>().also { conflicts ->
                for (ref in references) {
                    val element = ref.element
                    if (element is KtElement) {
                        val scope = element.getResolutionScope(element.analyze())
                        if (scope == null) {
                            conflicts.putValue(element, "Can't replace reference: " + StringUtil.htmlEmphasize(element.text))
                            continue
                        }
                        val descriptorsByName = scope.collectDescriptorsFiltered(DescriptorKindFilter.ALL, { it == conflictingName })
                        if (descriptorsByName.any { it != ignoreDescriptor }) {
                            conflicts.putValue(element, "${element.name} already exists")
                            continue
                        }
                    } else {
                        conflicts.putValue(element, "Unrecognized reference will be skipped: " + StringUtil.htmlEmphasize(element.text))
                    }
                }
            }
        }

        private fun removeCompanionReference(objName: String, descriptor: ClassDescriptor, references: Collection<PsiReference>) {
            references.forEach { ref ->
                val element = ref.element as? KtElement ?: return@forEach
                val parent = element.parent
                if (parent is KtDotQualifiedExpression && parent.selectorExpression == element) {
                    parent.replace(parent.receiverExpression)
                } else {
                    ImportInsertHelper.getInstance(ref.element.project).importDescriptor(element.containingKtFile, descriptor)
                    element.containingKtFile.importList?.imports?.find { it.importedFqName == descriptor.fqNameSafe }?.delete()
                    element.replace(KtPsiFactory(element).createNameIdentifier(objName))
                }
            }
        }
    }
}
