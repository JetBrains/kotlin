/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.utils.sure
import java.io.File

@ObsoleteTestInfrastructure(replacer = "org.jetbrains.kotlin.test.runners.codegen.AbstractBytecodeListingTest")
abstract class AbstractBytecodeListingTest : CodegenTestCase() {
    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>) {
        compile(files)
        val actualTxt = BytecodeListingTextCollectingVisitor.getText(
            classFileFactory,
            withSignatures = isWithSignatures(wholeFile),
            withAnnotations = isWithAnnotations(wholeFile),
            filter = BytecodeListingTextCollectingVisitor.Filter.ForCodegenTests
        )

        val prefixes = when {
            backend.isIR -> listOf("_ir", "")
            else -> listOf("")
        }

        val txtFile =
            prefixes.firstNotNullOfOrNull { File(wholeFile.parentFile, wholeFile.nameWithoutExtension + "$it.txt").takeIf(File::exists) }
                .sure { "No testData file exists: ${wholeFile.nameWithoutExtension}.txt" }

        KotlinTestUtils.assertEqualsToFile(txtFile, actualTxt)

        if (backend.isIR) {
            val jvmGoldenFile = File(wholeFile.parentFile, wholeFile.nameWithoutExtension + ".txt")
            val jvmIrGoldenFile = File(wholeFile.parentFile, wholeFile.nameWithoutExtension + "_ir.txt")
            if (jvmGoldenFile.exists() && jvmIrGoldenFile.exists()) {
                if (jvmGoldenFile.readText() == jvmIrGoldenFile.readText()) {
                    fail("JVM and JVM_IR golden files are identical. Remove $jvmIrGoldenFile.")
                }
            }
        }
    }

    private fun isWithSignatures(wholeFile: File): Boolean =
        WITH_SIGNATURES.containsMatchIn(wholeFile.readText())

    private fun isWithAnnotations(wholeFile: File): Boolean =
        !IGNORE_ANNOTATIONS.containsMatchIn(wholeFile.readText())

    companion object {
        private val WITH_SIGNATURES = Regex.fromLiteral("// WITH_SIGNATURES")
        private val IGNORE_ANNOTATIONS = Regex.fromLiteral("// IGNORE_ANNOTATIONS")
    }
}
