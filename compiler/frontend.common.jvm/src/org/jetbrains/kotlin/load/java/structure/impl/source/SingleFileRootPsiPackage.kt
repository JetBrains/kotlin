/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.structure.impl.source

import com.intellij.psi.PsiManager
import com.intellij.psi.PsiModifierList
import com.intellij.psi.impl.file.PsiPackageImpl

class SingleFileRootPsiPackage(
    manager: PsiManager,
    qualifiedName: String,
    private val annotationsList: PsiModifierList?,
) : PsiPackageImpl(manager, qualifiedName) {
    // Do not check validness for packages we just made sure are actually present
    // It might be important for source roots that have non-trivial package prefix
    override fun isValid() = true

    override fun getAnnotationList(): PsiModifierList? {
        return annotationsList
    }
}
