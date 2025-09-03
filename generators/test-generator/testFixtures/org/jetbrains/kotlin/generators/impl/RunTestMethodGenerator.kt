/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.impl

import org.jetbrains.kotlin.generators.MethodGenerator
import org.jetbrains.kotlin.generators.model.RunTestMethodModel
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.utils.Printer

object RunTestMethodGenerator : MethodGenerator<RunTestMethodModel>() {
    override fun generateBody(method: RunTestMethodModel, p: Printer) {
        with(method) {
            val modifiedTestMethodName = "this::$testMethodName"
            if (!isWithTargetBackend()) {
                p.println("KotlinTestUtils.$testRunnerMethodName($modifiedTestMethodName, this, testDataFilePath);")
            } else {
                val className = TargetBackend::class.java.simpleName
                p.println("KotlinTestUtils.$testRunnerMethodName($modifiedTestMethodName, $className.$targetBackend, testDataFilePath);")
            }
        }
    }

    override fun generateSignature(method: RunTestMethodModel, p: Printer) {
        p.print("private void ${method.name}(String testDataFilePath)")
    }
}
