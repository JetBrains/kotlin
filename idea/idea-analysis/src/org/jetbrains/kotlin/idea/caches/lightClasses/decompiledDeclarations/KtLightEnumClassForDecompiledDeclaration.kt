/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.lightClasses.decompiledDeclarations

import com.intellij.psi.*
import org.jetbrains.kotlin.idea.caches.lightClasses.KtLightClassForDecompiledDeclaration
import org.jetbrains.kotlin.idea.decompiler.classFile.KtClsFile
import org.jetbrains.kotlin.psi.KtClassOrObject

internal class KtLightEnumClassForDecompiledDeclaration(
    private val psiConstantInitializer: PsiEnumConstantInitializer,
    private val enumConstant: KtLightEnumEntryForDecompiledDeclaration,
    clsParent: KtLightClassForDecompiledDeclaration,
    file: KtClsFile,
    kotlinOrigin: KtClassOrObject?
) :
    KtLightClassForDecompiledDeclaration(
        clsDelegate = psiConstantInitializer,
        clsParent = clsParent,
        file = file,
        kotlinOrigin = kotlinOrigin
    ), PsiEnumConstantInitializer {

    override fun getBaseClassType(): PsiClassType = psiConstantInitializer.baseClassType

    override fun getArgumentList(): PsiExpressionList? = psiConstantInitializer.argumentList

    override fun getEnumConstant(): PsiEnumConstant = enumConstant

    override fun getBaseClassReference(): PsiJavaCodeReferenceElement = psiConstantInitializer.baseClassReference

    override fun isInQualifiedNew(): Boolean = psiConstantInitializer.isInQualifiedNew

    override fun equals(other: Any?): Boolean = other is KtLightEnumClassForDecompiledDeclaration && super.equals(other)
    override fun hashCode(): Int = super.hashCode()
}