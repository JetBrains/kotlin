/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.base

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktModuleProvider
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractAnalysisApiBasedSingleModuleTest : AbstractAnalysisApiBasedTest() {
    final override fun doTestByFileStructure(moduleStructure: TestModuleStructure, testServices: TestServices) {
        val singleModule = moduleStructure.modules.single()
        val ktFiles = testServices.ktModuleProvider.getModuleFiles(singleModule).filterIsInstance<KtFile>()
        doTestByFileStructure(ktFiles, singleModule, testServices)
    }

    protected abstract fun doTestByFileStructure(ktFiles: List<KtFile>, module: TestModule, testServices: TestServices)

    protected inline fun <R> analyseOnPooledThreadInReadAction(context: KtElement, crossinline action: KtAnalysisSession.() -> R): R =
        executeOnPooledThreadInReadAction {
            analyseForTest(context) { action() }
        }

    protected inline fun <T> runReadAction(crossinline runnable: () -> T): T {
        return ApplicationManager.getApplication().runReadAction(Computable { runnable() })
    }

    protected inline fun <R> executeOnPooledThreadInReadAction(crossinline action: () -> R): R =
        ApplicationManager.getApplication().executeOnPooledThread<R> { runReadAction(action) }.get()
}