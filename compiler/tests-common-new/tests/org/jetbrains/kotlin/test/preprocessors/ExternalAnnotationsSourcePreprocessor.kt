/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.preprocessors

import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.services.ReversibleSourceFilePreprocessor
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.isExternalAnnotation

/**
 * This preprocessor is required to drop '//' comments from the file to avoid exceptions from XML parser
 */
class ExternalAnnotationsSourcePreprocessor(testServices: TestServices) : ReversibleSourceFilePreprocessor(testServices) {
    override fun process(file: TestFile, content: String): String = if (file.isExternalAnnotation) {
        content.trim().lineSequence().filterNot { it.startsWith('/') }.joinToString(separator = "\n")
    } else {
        content
    }

    override fun revert(file: TestFile, actualContent: String): String {
        return if (file.isExternalAnnotation) file.originalContent.trim() + "\n" else actualContent
    }
}
