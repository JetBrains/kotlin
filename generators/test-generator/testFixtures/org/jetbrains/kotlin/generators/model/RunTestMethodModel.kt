/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.model

import org.jetbrains.kotlin.generators.MethodGenerator
import org.jetbrains.kotlin.generators.impl.RunTestMethodGenerator
import org.jetbrains.kotlin.test.TargetBackend

/**
 * Decorator method, which wraps the delegating of the actual test method with `KotlinTestUtils.runTest` call.
 * This is needed only for legacy tests on JUnit3/4 for handling suppressions with `wrapWithMuteInDatabase` infra.
 * This method is not generated in JUnit5 mode even if it's added to the class model.
 */
class RunTestMethodModel(
    val targetBackend: TargetBackend?,
    val testMethodName: String,
    val testRunnerMethodName: String,
) : MethodModel<RunTestMethodModel>() {
    override val generator: MethodGenerator<RunTestMethodModel> get() = RunTestMethodGenerator
    override val name = MethodGenerator.DEFAULT_RUN_TEST_METHOD_NAME
    override val dataString: String? = null
    override val isTestMethod: Boolean get() = false

    override val tags: List<String>
        get() = emptyList()

    override fun imports(): Collection<Class<*>> {
        return super.imports() + if (isWithTargetBackend()) setOf(TargetBackend::class.java) else emptySet()
    }

    fun isWithTargetBackend(): Boolean {
        return !(targetBackend == null && testRunnerMethodName == MethodGenerator.DEFAULT_RUN_TEST_METHOD_NAME)
    }
}
