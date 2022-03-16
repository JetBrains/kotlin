/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.base

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.analyse
import org.jetbrains.kotlin.analysis.api.analyseInDependedAnalysisSession
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractAnalysisApiBasedSingleModuleTest : AbstractAnalysisApiBasedTest() {
    final override fun doTestByFileStructure(ktFiles: List<KtFile>, moduleStructure: TestModuleStructure, testServices: TestServices) {
        val singleModule = moduleStructure.modules.single()
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

    protected fun <R> analyseForTest(contextElement: KtElement, action: KtAnalysisSession.() -> R): R {
        return if (useDependedAnalysisSession) {
            // Depended mode does not support analysing a KtFile.
            // See org.jetbrains.kotlin.analysis.low.level.api.fir.api.LowLevelFirApiFacadeForResolveOnAir#getResolveStateForDependentCopy
            if (contextElement is KtFile) {
                throw SkipDependedModeException()
            }

            require(!contextElement.isPhysical)
            analyseInDependedAnalysisSession(configurator.getOriginalFile(contextElement.containingKtFile), contextElement, action)
        } else {
            analyse(contextElement, action)
        }
    }
}