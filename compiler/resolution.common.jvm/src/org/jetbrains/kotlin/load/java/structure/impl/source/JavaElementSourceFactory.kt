/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.structure.impl.source

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType

abstract class JavaElementSourceFactory {
    abstract fun <PSI : PsiElement> createPsiSource(psi: PSI): JavaElementPsiSource<PSI>
    abstract fun <TYPE : PsiType> createTypeSource(type: TYPE): JavaElementTypeSource<TYPE>

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
}
