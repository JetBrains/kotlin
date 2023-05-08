/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.structure.impl.source

import com.intellij.psi.PsiElement

abstract class JavaElementPsiSource<PSI : PsiElement> {
    abstract val psi: PSI
    abstract val factory: JavaElementSourceFactory
}

class JavaElementPsiSourceWithFixedPsi<PSI : PsiElement>(
    override val psi: PSI
) : JavaElementPsiSource<PSI>() {
    override val factory: JavaElementSourceFactory
        get() = JavaElementSourceFactory.getInstance(psi.project)

    override fun toString(): String {
        return psi.toString()
    }
}