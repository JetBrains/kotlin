/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.ResolveState
import com.intellij.psi.scope.PsiScopeProcessor
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.getOutermostClassOrObject
import org.jetbrains.kotlin.asJava.elements.FakeFileForLightClass
import org.jetbrains.kotlin.psi.KtClassOrObject

class FirFakeFileImpl(private val classOrObject: KtClassOrObject, ktClass: KtLightClass) : FakeFileForLightClass(
    classOrObject.containingKtFile,
    { if (classOrObject.isTopLevel()) ktClass else FirLightClassForSymbol.create(getOutermostClassOrObject(classOrObject))!! },
    { null }
) {

    override fun findReferenceAt(offset: Int) = ktFile.findReferenceAt(offset)

    override fun processDeclarations(
        processor: PsiScopeProcessor,
        state: ResolveState,
        lastParent: PsiElement?,
        place: PsiElement
    ): Boolean {
        if (!super.processDeclarations(processor, state, lastParent, place)) return false

        // We have to explicitly process package declarations if current file belongs to default package
        // so that Java resolve can find classes located in that package
        val packageName = packageName
        if (packageName.isNotEmpty()) return true

        val aPackage = JavaPsiFacade.getInstance(classOrObject.project).findPackage(packageName)
        if (aPackage != null && !aPackage.processDeclarations(processor, state, null, place)) return false

        return true
    }
}