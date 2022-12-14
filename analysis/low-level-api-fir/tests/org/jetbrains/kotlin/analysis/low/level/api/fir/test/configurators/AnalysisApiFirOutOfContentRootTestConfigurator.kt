/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators

import com.intellij.openapi.Disposable
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtOutOfContentRootModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.utils.SkipTestException
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

object AnalysisApiFirOutOfContentRootTestConfigurator : AnalysisApiFirSourceLikeTestConfigurator(false) {
    override val testPrefix: String
        get() = "outside"

    override fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable) {
        builder.apply {
            useDirectives(Directives)
            useAdditionalService<KtModuleFactory> { KtOutOfContentRootModuleFactory() }
        }
    }

    override fun prepareFilesInModule(files: List<PsiFile>, module: TestModule, testServices: TestServices) {
        if (Directives.SKIP_WHEN_OUT_OF_CONTENT_ROOT in module.directives) {
            throw SkipWhenOutOfContentRootException()
        }

        super.prepareFilesInModule(files, module, testServices)
    }

    object Directives : SimpleDirectivesContainer() {
        val SKIP_WHEN_OUT_OF_CONTENT_ROOT by directive(
            description = "Skip the test in out-of-content-root mode",
            applicability = DirectiveApplicability.Global
        )
    }
}

private class SkipWhenOutOfContentRootException : SkipTestException()