/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.psi.KtClassOrObject

// light class for top level or (inner/nested of top level) source declarations
abstract class KtLightClassImpl @JvmOverloads constructor(
    classOrObject: KtClassOrObject,
    jvmDefaultMode: JvmDefaultMode,
    forceUsingOldLightClasses: Boolean = false
) : KtLightClassForSourceDeclaration(classOrObject, jvmDefaultMode, forceUsingOldLightClasses) {
    override fun getQualifiedName() = classOrObject.fqName?.asString()

    override fun getParent() = if (classOrObject.isTopLevel())
        containingFile
    else
        containingClass

    abstract override fun copy(): PsiElement
}
