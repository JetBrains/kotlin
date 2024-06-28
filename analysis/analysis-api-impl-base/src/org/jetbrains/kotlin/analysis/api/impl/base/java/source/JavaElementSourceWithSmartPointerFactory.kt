/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.java.source

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementPsiSource
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementSourceFactory
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementTypeSource

class JavaElementSourceWithSmartPointerFactory(project: Project) : JavaElementSourceFactory() {
    private val smartTypePointerManager = SmartTypePointerManager.getInstance(project)
    private val smartPsiPointerManager = SmartPointerManager.getInstance(project)

    override fun <PSI : PsiElement> createPsiSource(psi: PSI): JavaElementPsiSource<PSI> {
        return JavaElementPsiSourceWithSmartPointer(smartPsiPointerManager.createSmartPsiElementPointer(psi), this)
    }

    override fun <TYPE : PsiType> createTypeSource(type: TYPE): JavaElementTypeSource<TYPE> {
        return JavaElementTypeSourceWithSmartPointer(smartTypePointerManager.createSmartTypePointer(type), this)
    }

    override fun <TYPE : PsiType> createVariableReturnTypeSource(psiVariableSource: JavaElementPsiSource<out PsiVariable>): JavaElementTypeSource<TYPE> {
        require(psiVariableSource is JavaElementPsiSourceWithSmartPointer)
        return JavaElementDelegatingVariableReturnTypeSourceWithSmartPointer(psiVariableSource.pointer, psiVariableSource.factory)
    }

    override fun <TYPE : PsiType> createMethodReturnTypeSource(psiMethodSource: JavaElementPsiSource<out PsiMethod>): JavaElementTypeSource<TYPE> {
        require(psiMethodSource is JavaElementPsiSourceWithSmartPointer)
        return JavaElementDelegatingMethodReturnTypeSourceWithSmartPointer(psiMethodSource.pointer, psiMethodSource.factory)
    }

    override fun <TYPE : PsiType> createTypeParameterUpperBoundTypeSource(
        psiTypeParameterSource: JavaElementPsiSource<out PsiTypeParameter>,
        boundIndex: Int
    ): JavaElementTypeSource<TYPE> {
        require(psiTypeParameterSource is JavaElementPsiSourceWithSmartPointer)
        return JavaElementDelegatingTypeParameterBoundTypeSourceWithSmartPointer(
            psiTypeParameterSource.pointer,
            boundIndex,
            psiTypeParameterSource.factory
        )
    }

    override fun createSuperTypeSource(
        psiTypeParameterSource: JavaElementPsiSource<out PsiClass>,
        superTypeIndex: Int
    ): JavaElementTypeSource<PsiClassType> {
        require(psiTypeParameterSource is JavaElementPsiSourceWithSmartPointer)
        return JavaElementDelegatingSuperTypeSourceWithSmartPointer(
            psiTypeParameterSource.pointer,
            superTypeIndex,
            psiTypeParameterSource.factory,
        )
    }

    override fun <TYPE : PsiType> createExpressionTypeSource(psiExpressionSource: JavaElementPsiSource<out PsiExpression>): JavaElementTypeSource<TYPE> {
        require(psiExpressionSource is JavaElementPsiSourceWithSmartPointer)
        return JavaElementDelegatingExpressionTypeSourceWithSmartPointer(psiExpressionSource.pointer, psiExpressionSource.factory)
    }

    override fun createPermittedTypeSource(
        psiTypeParameterSource: JavaElementPsiSource<out PsiClass>,
        permittedTypeIndex: Int
    ): JavaElementTypeSource<PsiClassType> {
        require(psiTypeParameterSource is JavaElementPsiSourceWithSmartPointer)
        return JavaElementDelegatingPermittedTypeSourceWithSmartPointer(
            psiTypeParameterSource.pointer,
            permittedTypeIndex,
            psiTypeParameterSource.factory,
        )
    }
}