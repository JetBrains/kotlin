/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.preprocessors

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.services.ReversibleSourceFilePreprocessor
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.utils.ReplacingSourceTransformer

/**
 * Selects the appropriate [ReplacingSourceTransformer] based on the target backend.
 */
abstract class BackendDependentSourceFilePreprocessor(testServices: TestServices) : ReversibleSourceFilePreprocessor(testServices) {
    private val contentModifier: ReplacingSourceTransformer by lazy {
        selectTransformer(testServices.defaultsProvider.targetBackend ?: TargetBackend.ANY)
    }

    abstract fun selectTransformer(targetBackend: TargetBackend): ReplacingSourceTransformer

    override fun process(file: TestFile, content: String): String = contentModifier.invokeForTestFile(content)

    override fun revert(file: TestFile, actualContent: String): String = contentModifier.revertForFile(actualContent)
}
