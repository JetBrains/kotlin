/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.codeInsight.navigation.actions.GotoSuperAction
import com.intellij.featureStatistics.FeatureUsageTracker
import com.intellij.ide.util.EditSourceUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.isAny
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.core.getDirectlyOverriddenDeclarations
import org.jetbrains.kotlin.idea.highlighter.markers.expectedDeclarationIfAny
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class GotoSuperActionHandler : CodeInsightActionHandler {
    internal fun allSuperDeclarationsAndDescriptor(editor: Editor, file: PsiFile): Pair<List<PsiElement>, DeclarationDescriptor?> {
        val element = file.findElementAt(editor.caretModel.offset) ?: return emptyList<PsiElement>() to null
        val declaration =
            PsiTreeUtil.getParentOfType<KtDeclaration>(
                element,
                KtNamedFunction::class.java,
                KtClass::class.java,
                KtProperty::class.java,
                KtObjectDeclaration::class.java
            ) ?: return emptyList<PsiElement>() to null

        val expectDeclaration = if (declaration.hasActualModifier()) declaration.expectedDeclarationIfAny() else null

        val descriptor = declaration.unsafeResolveToDescriptor(BodyResolveMode.PARTIAL)
        val superDeclarations = findSuperDeclarations(file.project, descriptor)
        return ((superDeclarations ?: emptyList()) + listOfNotNull(expectDeclaration)) to descriptor
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        FeatureUsageTracker.getInstance().triggerFeatureUsed(GotoSuperAction.FEATURE_ID)

        val (allDeclarations, descriptor) = allSuperDeclarationsAndDescriptor(editor, file)
        if (allDeclarations.isEmpty()) return
        if (allDeclarations.size == 1) {
            val navigatable = EditSourceUtil.getDescriptor(allDeclarations[0])
            if (navigatable != null && navigatable.canNavigate()) {
                navigatable.navigate(true)
            }
        } else {
            val message = getTitle(descriptor!!)
            val superDeclarationsArray = PsiUtilCore.toPsiElementArray(allDeclarations)
            val popup = if (descriptor is ClassDescriptor)
                NavigationUtil.getPsiElementPopup(superDeclarationsArray, message)
            else
                NavigationUtil.getPsiElementPopup(
                    superDeclarationsArray,
                    KtFunctionPsiElementCellRenderer(), message
                )
            popup.showInBestPositionFor(editor)
        }
    }

    private fun getTitle(descriptor: DeclarationDescriptor): String? =
        when (descriptor) {
            is ClassDescriptor -> KotlinBundle.message("goto.super.class.chooser.title")
            is PropertyDescriptor -> KotlinBundle.message("goto.super.property.chooser.title")
            is SimpleFunctionDescriptor -> KotlinBundle.message("goto.super.function.chooser.title")
            else -> null
        }

    private fun findSuperDeclarations(project: Project, descriptor: DeclarationDescriptor): List<PsiElement>? {
        val superDescriptors: Collection<DeclarationDescriptor> = when (descriptor) {
            is ClassDescriptor -> {
                val supertypes = descriptor.typeConstructor.supertypes
                val superclasses = supertypes.mapNotNull { type ->
                    type.constructor.declarationDescriptor as? ClassDescriptor
                }
                ContainerUtil.removeDuplicates(superclasses)
                superclasses
            }
            is CallableMemberDescriptor -> descriptor.getDirectlyOverriddenDeclarations()
            else -> return null
        }

        return superDescriptors.mapNotNull { descriptor ->
            if (descriptor is ClassDescriptor && isAny(descriptor)) {
                null
            } else
                DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor)
        }
    }

    override fun startInWriteAction() = false
}
