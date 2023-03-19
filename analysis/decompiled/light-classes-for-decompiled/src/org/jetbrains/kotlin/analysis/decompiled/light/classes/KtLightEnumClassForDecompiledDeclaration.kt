/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiled.light.classes

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.psi.KtClassOrObject

internal class KtLightEnumClassForDecompiledDeclaration(
    private val psiConstantInitializer: PsiEnumConstantInitializer,
    private val enumConstant: KtLightEnumEntryForDecompiledDeclaration,
    clsParent: KtLightClass,
    file: KtClsFile,
    kotlinOrigin: KtClassOrObject?
) : KtLightClassForDecompiledDeclaration(
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

    override fun equals(other: Any?): Boolean = this === other || other is KtLightEnumClassForDecompiledDeclaration && super.equals(other)
    override fun hashCode(): Int = super.hashCode()

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitEnumConstantInitializer(this)
        } else {
            visitor.visitElement(this)
        }
    }
}
