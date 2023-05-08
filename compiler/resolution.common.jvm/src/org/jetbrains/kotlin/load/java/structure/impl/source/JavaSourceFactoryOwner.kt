/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.structure.impl.source

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType

interface JavaSourceFactoryOwner {
    val sourceFactory: JavaElementSourceFactory

    fun <PSI : PsiElement> createPsiSource(psi: PSI): JavaElementPsiSource<PSI> {
        return sourceFactory.createPsiSource(psi)
    }

    fun <TYPE : PsiType> createTypeSource(type: TYPE): JavaElementTypeSource<TYPE> {
        return sourceFactory.createTypeSource(type)
    }
}