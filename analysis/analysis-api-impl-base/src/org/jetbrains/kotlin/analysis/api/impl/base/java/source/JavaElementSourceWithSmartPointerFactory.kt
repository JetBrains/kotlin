/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.java.source

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartTypePointerManager
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
}