/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.model.methods

import org.jetbrains.kotlin.generators.MethodGenerator
import org.jetbrains.kotlin.generators.model.MethodModel
import org.jetbrains.kotlin.utils.Printer

/**
 * Decorator method, which wraps the delegating of the actual test method with `KotlinTestUtils.runTest` call.
 * This is needed only for legacy tests on JUnit3/4 for handling suppressions with `wrapWithMuteInDatabase` infra.
 * This method is not generated in JUnit5 mode even if it's added to the class model.
 */
class RunTestWithDirectoryPrefixMethodModel(
    val directory: String,
) : MethodModel<RunTestWithDirectoryPrefixMethodModel>() {
    override val generator: MethodGenerator<RunTestWithDirectoryPrefixMethodModel> get() = Generator
    override val name get() = METHOD_NAME
    override val dataString: String? = null
    override val isTestMethod: Boolean get() = false

    override val tags: List<String>
        get() = emptyList()

    object Generator : MethodGenerator<RunTestWithDirectoryPrefixMethodModel>() {
        override fun generateBody(method: RunTestWithDirectoryPrefixMethodModel, p: Printer) {
            p.println("""${DEFAULT_RUN_TEST_METHOD_NAME}("${method.directory}/" + fileName);""")
        }

        override fun generateSignature(method: RunTestWithDirectoryPrefixMethodModel, p: Printer) {
            p.print("private void $METHOD_NAME(String fileName)")
        }
    }

    companion object {
        const val METHOD_NAME = "run"
    }
}
