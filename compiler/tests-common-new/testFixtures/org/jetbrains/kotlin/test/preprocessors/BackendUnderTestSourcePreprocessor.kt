/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.preprocessors

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.utils.ReplacingSourceTransformer

/**
 * Replaces the magic identifier `BACKEND_UNDER_TEST` in the test source file with a string with the actual name of the backend that
 * is being used to compile this test.
 *
 * This is useful when we want to test some behavior that _slightly_ differs between targets, so a separate test file would be an overkill.
 * Instead, this magic identifier allows to write conditions like
 * ```
 * if (BACKEND_UNDER_TEST == "JVM") {
 *   // ...
 * }
 * ```
 */
class BackendUnderTestSourcePreprocessor(testServices: TestServices) : BackendDependentSourceFilePreprocessor(testServices) {
    override fun selectTransformer(targetBackend: TargetBackend): ReplacingSourceTransformer =
        ReplacingSourceTransformer("BACKEND_UNDER_TEST", "\"$targetBackend\"")
}
