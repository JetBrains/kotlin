/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.impl

import org.jetbrains.kotlin.generators.model.MethodModel
import org.jetbrains.kotlin.generators.MethodGenerator
import org.jetbrains.kotlin.generators.model.RunTestMethodModel
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.utils.Printer

object RunTestMethodGenerator : MethodGenerator<RunTestMethodModel>() {
    override val kind: MethodModel.Kind
        get() = RunTestMethodModel.Kind

    override fun generateBody(method: RunTestMethodModel, p: Printer) {
        with(method) {
            if (!isWithTargetBackend()) {
                p.println("KotlinTestUtils.${method.testRunnerMethodName}(this::$testMethodName, this, testDataFilePath);")
            } else {
                val className = TargetBackend::class.java.simpleName
                val additionalArguments = if (additionalRunnerArguments.isNotEmpty())
                    additionalRunnerArguments.joinToString(separator = ", ", prefix = ", ")
                else ""
                p.println("KotlinTestUtils.$testRunnerMethodName(this::$testMethodName, $className.$targetBackend, testDataFilePath$additionalArguments);")
            }
        }
    }

    override fun generateSignature(method: RunTestMethodModel, p: Printer) {
        p.print("private void ${method.name}(String testDataFilePath) throws Exception")
    }
}
