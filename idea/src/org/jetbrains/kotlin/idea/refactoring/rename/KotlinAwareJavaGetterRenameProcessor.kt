/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiType
import com.intellij.psi.search.SearchScope
import com.intellij.refactoring.rename.RenameJavaMethodProcessor
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.idea.references.SyntheticPropertyAccessorReference
import org.jetbrains.kotlin.idea.references.SyntheticPropertyAccessorReferenceDescriptorImpl
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.utils.addToStdlib.filterIsInstanceWithChecker
import org.jetbrains.kotlin.utils.ifEmpty

class KotlinAwareJavaGetterRenameProcessor : RenameJavaMethodProcessor() {
    override fun canProcessElement(element: PsiElement) =
        element is PsiMethod && element !is KtLightMethod && JvmAbi.isGetterName(element.name)

    override fun findReferences(
        element: PsiElement,
        searchScope: SearchScope,
        searchInCommentsAndStrings: Boolean
    ): Collection<PsiReference> {
        val getterReferences = super.findReferences(element, searchScope, searchInCommentsAndStrings)
        val getter = element as? PsiMethod ?: return getterReferences
        val propertyName = SyntheticJavaPropertyDescriptor.propertyNameByGetMethodName(Name.identifier(getter.name))
            ?: return getterReferences
        val setterName = JvmAbi.setterName(propertyName.asString())
        val containingClass = getter.containingClass ?: return getterReferences
        val setterReferences = containingClass
            .findMethodsByName(setterName, true)
            .filter { it.parameters.size == 1 && it.returnType == PsiType.VOID }
            .flatMap {
                super.findReferences(it, searchScope, searchInCommentsAndStrings)
                    .filterIsInstanceWithChecker<SyntheticPropertyAccessorReference> { !it.getter }
            }
            .ifEmpty { return getterReferences }
        return ArrayList<PsiReference>(getterReferences.size + setterReferences.size).apply {
            addAll(getterReferences)
            setterReferences.mapTo(this) { SyntheticPropertyAccessorReferenceDescriptorImpl(it.expression, getter = true) }
        }
    }
}