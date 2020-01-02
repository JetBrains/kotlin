/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.impl.light.LightElement
import com.intellij.psi.search.SearchScope
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.refactoring.project
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor

class RenameJavaSyntheticPropertyHandler : AbstractReferenceSubstitutionRenameHandler() {
    class Processor : RenamePsiElementProcessor() {
        override fun prepareRenaming(element: PsiElement, newName: String, allRenames: MutableMap<PsiElement, String>, scope: SearchScope) {
            val propertyWrapper = element as? SyntheticPropertyWrapper ?: return
            propertyWrapper.getter?.let { allRenames[it] = JvmAbi.getterName(newName) }
            propertyWrapper.setter?.let { allRenames[it] = JvmAbi.setterName(newName) }
        }

        override fun canProcessElement(element: PsiElement) = element is SyntheticPropertyWrapper
    }

    internal class SyntheticPropertyWrapper(
        manager: PsiManager,
        val descriptor: SyntheticJavaPropertyDescriptor
    ) : LightElement(manager, KotlinLanguage.INSTANCE), PsiNamedElement {
        val getter: PsiMethod? get() = descriptor.getMethod.source.getPsi() as? PsiMethod
        val setter: PsiMethod? get() = descriptor.setMethod?.source?.getPsi() as? PsiMethod

        override fun getContainingFile() = getter?.containingFile

        override fun getName() = descriptor.name.asString()

        override fun setName(name: String): PsiElement? {
            getter?.name = JvmAbi.getterName(name)
            setter?.name = JvmAbi.setterName(name)
            return this
        }

        override fun toString(): String {
            val renderer = IdeDescriptorRenderers.SOURCE_CODE
            return "${renderer.render(descriptor.getMethod)}|${descriptor.setMethod?.let { renderer.render(it) }}"
        }
    }

    override fun getElementToRename(dataContext: DataContext): PsiElement? {
        val refExpr = getReferenceExpression(dataContext) ?: return null
        val descriptor = refExpr.resolveToCall()?.resultingDescriptor as? SyntheticJavaPropertyDescriptor ?: return null
        return SyntheticPropertyWrapper(PsiManager.getInstance(dataContext.project), descriptor)
    }
}
