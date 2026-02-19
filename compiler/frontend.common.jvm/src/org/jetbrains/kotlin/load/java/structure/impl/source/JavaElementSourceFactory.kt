/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.structure.impl.source

import com.intellij.openapi.project.Project
import com.intellij.psi.*

abstract class JavaElementSourceFactory {
    abstract fun <PSI : PsiElement> createPsiSource(psi: PSI): JavaElementPsiSource<PSI>
    abstract fun <TYPE : PsiType> createTypeSource(type: TYPE): JavaElementTypeSource<TYPE>
    abstract fun <TYPE : PsiType> createVariableReturnTypeSource(psiVariableSource: JavaElementPsiSource<out PsiVariable>): JavaElementTypeSource<TYPE>
    abstract fun <TYPE : PsiType> createMethodReturnTypeSource(psiMethodSource: JavaElementPsiSource<out PsiMethod>): JavaElementTypeSource<TYPE>

    abstract fun <TYPE : PsiType> createTypeParameterUpperBoundTypeSource(
        psiTypeParameterSource: JavaElementPsiSource<out PsiTypeParameter>,
        boundIndex: Int,
    ): JavaElementTypeSource<TYPE>

    abstract fun createSuperTypeSource(
        psiTypeParameterSource: JavaElementPsiSource<out PsiClass>,
        superTypeIndex: Int,
    ): JavaElementTypeSource<PsiClassType>

    abstract fun <TYPE : PsiType> createExpressionTypeSource(psiExpressionSource: JavaElementPsiSource<out PsiExpression>): JavaElementTypeSource<TYPE>

    /**
     * @see com.intellij.psi.PsiClass.getPermitsListTypes
     */
    abstract fun createPermittedTypeSource(
        psiTypeParameterSource: JavaElementPsiSource<out PsiClass>,
        permittedTypeIndex: Int,
    ): JavaElementTypeSource<PsiClassType>

    companion object {
        @JvmStatic
        fun getInstance(project: Project): JavaElementSourceFactory {
            return project.getService(JavaElementSourceFactory::class.java)
        }
    }
}

class JavaFixedElementSourceFactory : JavaElementSourceFactory() {
    override fun <PSI : PsiElement> createPsiSource(psi: PSI): JavaElementPsiSource<PSI> {
        return JavaElementPsiSourceWithFixedPsi(psi)
    }

    override fun <TYPE : PsiType> createTypeSource(type: TYPE): JavaElementTypeSource<TYPE> {
        return JavaElementTypeSourceWithFixedType(type, this)
    }

    override fun <TYPE : PsiType> createVariableReturnTypeSource(psiVariableSource: JavaElementPsiSource<out PsiVariable>): JavaElementTypeSource<TYPE> {
        @Suppress("UNCHECKED_CAST")
        return createTypeSource(psiVariableSource.psi.type as TYPE)
    }

    override fun <TYPE : PsiType> createTypeParameterUpperBoundTypeSource(
        psiTypeParameterSource: JavaElementPsiSource<out PsiTypeParameter>,
        boundIndex: Int
    ): JavaElementTypeSource<TYPE> {
        @Suppress("UNCHECKED_CAST")
        return createTypeSource(psiTypeParameterSource.psi.bounds[boundIndex] as TYPE)
    }

    override fun createSuperTypeSource(
        psiTypeParameterSource: JavaElementPsiSource<out PsiClass>,
        superTypeIndex: Int,
    ): JavaElementTypeSource<PsiClassType> {
        return createTypeSource(psiTypeParameterSource.psi.superTypes[superTypeIndex])
    }

    override fun createPermittedTypeSource(
        psiTypeParameterSource: JavaElementPsiSource<out PsiClass>,
        permittedTypeIndex: Int
    ): JavaElementTypeSource<PsiClassType> {
        return createTypeSource(psiTypeParameterSource.psi.permitsListTypes[permittedTypeIndex])
    }

    override fun <TYPE : PsiType> createExpressionTypeSource(psiExpressionSource: JavaElementPsiSource<out PsiExpression>): JavaElementTypeSource<TYPE> {
        @Suppress("UNCHECKED_CAST")
        return createTypeSource(psiExpressionSource.psi.type as TYPE)
    }

    override fun <TYPE : PsiType> createMethodReturnTypeSource(psiMethodSource: JavaElementPsiSource<out PsiMethod>): JavaElementTypeSource<TYPE> {
        val psiType: PsiType = psiMethodSource.psi.returnType
            ?: error("Method is not a constructor and has no return type: " + psiMethodSource.psi.name)
        @Suppress("UNCHECKED_CAST")
        return createTypeSource(psiType as TYPE)
    }
}
