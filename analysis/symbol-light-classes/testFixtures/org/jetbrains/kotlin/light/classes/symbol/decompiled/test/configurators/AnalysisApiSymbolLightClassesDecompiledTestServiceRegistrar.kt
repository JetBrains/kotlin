/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.decompiled.test.configurators

import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.psi.PsiNameHelper
import com.intellij.psi.impl.PsiNameHelperImpl
import org.jetbrains.kotlin.analysis.decompiled.light.classes.ClsJavaStubByVirtualFileCache
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.test.services.TestServices

object AnalysisApiSymbolLightClassesDecompiledTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    override fun registerProjectModelServices(project: MockProject, disposable: Disposable, testServices: TestServices) {
        project.apply {
            registerService(ClsJavaStubByVirtualFileCache::class.java)
            registerService(PsiNameHelper::class.java, PsiNameHelperImpl::class.java)
        }
    }
}
