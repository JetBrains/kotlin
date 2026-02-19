/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.structure.impl.source

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.PsiVariable

interface JavaSourceFactoryOwner {
    val sourceFactory: JavaElementSourceFactory

    fun <PSI : PsiElement> createPsiSource(psi: PSI): JavaElementPsiSource<PSI> {
        return sourceFactory.createPsiSource(psi)
    }

    fun <TYPE : PsiType> createTypeSource(type: TYPE): JavaElementTypeSource<TYPE> {
        return sourceFactory.createTypeSource(type)
    }

    fun <TYPE : PsiType> createVariableReturnTypeSource(psiVariableSource: JavaElementPsiSource<out PsiVariable>): JavaElementTypeSource<TYPE> {
        return sourceFactory.createVariableReturnTypeSource(psiVariableSource)
    }

    fun <TYPE : PsiType> createMethodReturnTypeSource(psiMethodSource: JavaElementPsiSource<out PsiMethod>): JavaElementTypeSource<TYPE> {
        return sourceFactory.createMethodReturnTypeSource(psiMethodSource)
    }

}