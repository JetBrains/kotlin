/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiled.light.classes

import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.asJava.classes.KtExtensibleLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.psi.KtClassOrObject

abstract class KtLightClassForDecompiledDeclarationBase(
    val clsDelegate: PsiClass,
    clsParent: PsiElement,
    final override val kotlinOrigin: KtClassOrObject?
) : KtLightElementBase(clsParent), PsiClass, KtExtensibleLightClass {
    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitClass(this)
        } else {
            visitor.visitElement(this)
        }
    }
}
