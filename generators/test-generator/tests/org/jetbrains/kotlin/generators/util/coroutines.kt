/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.util

import org.jetbrains.kotlin.generators.tests.generator.MethodModel
import org.jetbrains.kotlin.generators.tests.generator.SimpleTestMethodModel
import org.jetbrains.kotlin.test.InTextDirectivesUtils.isIgnoredTarget
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.utils.Printer
import java.io.File
import java.util.regex.Pattern

class CoroutinesTestModel(
    rootDir: File,
    file: File,
    private val doTestMethodName: String,
    filenamePattern: Pattern,
    checkFilenameStartsLowerCase: Boolean?,
    targetBackend: TargetBackend,
    skipIgnored: Boolean,
    private val isLanguageVersion1_3: Boolean
) : SimpleTestMethodModel(
    rootDir,
    file,
    filenamePattern,
    checkFilenameStartsLowerCase,
    targetBackend,
    skipIgnored
) {
    override val name: String
        get() = super.name + if (isLanguageVersion1_3) "_1_3" else "_1_2"

    override fun generateBody(p: Printer) {
        val filePath = KotlinTestUtils.getFilePath(file) + if (file.isDirectory) "/" else ""
        p.println("String fileName = KotlinTestUtils.navigationMetadata(\"", filePath, "\");")

        if (isIgnoredTarget(targetBackend, file)) {
            p.println("try {")
            p.pushIndent()
        }

        val packageName = if (isLanguageVersion1_3) "kotlin.coroutines" else "kotlin.coroutines.experimental"
        p.println(doTestMethodName + "WithCoroutinesPackageReplacement", "(fileName, \"$packageName\");")

        if (isIgnoredTarget(targetBackend, file)) {
            p.popIndent()
            p.println("}")
            p.println("catch (Throwable ignore) {")
            p.pushIndent()
            p.println("return;")
            p.popIndent()
            p.println("}")
            p.println("throw new AssertionError(\"Looks like this test can be unmuted. Remove IGNORE_BACKEND directive for that.\");")
        }
    }
}

fun isCommonCoroutineTest(file: File): Boolean {
    return file.readLines().any { it.startsWith("// COMMON_COROUTINES_TEST") }
}

fun createCommonCoroutinesTestMethodModels(
    rootDir: File,
    file: File,
    doTestMethodName: String,
    filenamePattern: Pattern,
    checkFilenameStartsLowerCase: Boolean?,
    targetBackend: TargetBackend,
    skipIgnored: Boolean
): Collection<MethodModel> {
    return if (targetBackend == TargetBackend.JS || targetBackend == TargetBackend.JS_IR)
        listOf(
            CoroutinesTestModel(
                rootDir,
                file,
                doTestMethodName,
                filenamePattern,
                checkFilenameStartsLowerCase,
                targetBackend,
                skipIgnored,
                false
            )
        )
    else
        listOf(
            CoroutinesTestModel(
                rootDir,
                file,
                doTestMethodName,
                filenamePattern,
                checkFilenameStartsLowerCase,
                targetBackend,
                skipIgnored,
                true
            ),
            CoroutinesTestModel(
                rootDir,
                file,
                doTestMethodName,
                filenamePattern,
                checkFilenameStartsLowerCase,
                targetBackend,
                skipIgnored,
                false
            )
        )
}