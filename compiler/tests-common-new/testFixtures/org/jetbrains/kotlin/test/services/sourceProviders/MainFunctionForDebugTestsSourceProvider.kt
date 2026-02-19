/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.sourceProviders

import org.jetbrains.kotlin.test.services.TestServices

class MainFunctionForDebugTestsSourceProvider(testServices: TestServices) : MainFunctionForBlackBoxTestsSourceProvider(testServices) {

    // Debug tests sometimes throw exceptions that are uncaught to check the behavior
    // of constructs such as try-finally. Therefore, the runner catches exceptions
    // and ignores them.
    override fun generateMainBody(): String {
        return """
            try {
                box()
            } catch (t: Throwable) {
                // Ignore.
            }
        """.trimIndent()
    }
}
