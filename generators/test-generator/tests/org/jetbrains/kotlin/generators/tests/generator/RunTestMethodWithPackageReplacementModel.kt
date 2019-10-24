/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.generator

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.utils.Printer

class RunTestMethodWithPackageReplacementModel(
    private val targetBackend: TargetBackend,
    private val testMethodName: String,
    private val testRunnerMethodName: String,
    private val additionalRunnerArguments: List<String>
) : MethodModel {
    override val name = METHOD_NAME
    override val dataString: String? = null

    override fun generateSignature(p: Printer) {
        p.print("private void $name(String testDataFilePath, String packageName) throws Exception")
    }

    override fun generateBody(p: Printer) {
        val className = TargetBackend::class.java.simpleName
        val additionalArguments = if (additionalRunnerArguments.isNotEmpty())
            additionalRunnerArguments.joinToString(separator = ", ", prefix = ", ")
        else ""
        p.println("KotlinTestUtils.$testRunnerMethodName(filePath -> $testMethodName(filePath, packageName), $className.$targetBackend, testDataFilePath$additionalArguments);")
    }

    override fun imports(): Collection<Class<*>> {
        return super.imports() + setOf(TargetBackend::class.java)
    }

    companion object {
        const val METHOD_NAME = "runTestWithPackageReplacement"
    }
}