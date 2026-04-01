/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.model.methods

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.generators.MethodGenerator
import org.jetbrains.kotlin.generators.model.MethodModel
import org.jetbrains.kotlin.generators.model.TestInfraRevision
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.generators.util.getFilePath
import org.jetbrains.kotlin.utils.Printer
import java.io.File
import java.util.regex.Pattern

/**
 * Default model for the test method.
 */
class SimpleTestMethodModel(
    private val testInfraRevision: TestInfraRevision,
    private val rootDir: File,
    val file: File,
    private val filenamePattern: Pattern,
    override val tags: List<String>,
) : MethodModel<SimpleTestMethodModel>() {
    override val generator: MethodGenerator<SimpleTestMethodModel> get() = Generator

    override val dataString: String
        get() {
            val path = FileUtil.getRelativePath(rootDir, file)!!
            return File(path).getFilePath()
        }

    override val name: String
        get() {
            val matcher = filenamePattern.matcher(file.name)
            val found = matcher.find()
            assert(found) { file.name + " isn't matched by regex " + filenamePattern.pattern() }
            assert(matcher.groupCount() >= 1) { filenamePattern.pattern() }
            val extractedName = try {
                matcher.group(1) ?: error("extractedName should not be null: " + filenamePattern.pattern())
            } catch (e: Throwable) {
                throw IllegalStateException("Error generating test ${file.name}", e)
            }
            val unescapedName = if (rootDir == file.parentFile) {
                extractedName
            } else {
                val relativePath = FileUtil.getRelativePath(rootDir, file.parentFile)
                relativePath + "-" + extractedName.replaceFirstChar(Char::uppercaseChar)
            }
            val nameSuffix = TestGeneratorUtil.escapeForJavaIdentifier(unescapedName).replaceFirstChar(Char::uppercaseChar)
            return "test$nameSuffix"
        }

    private object Generator : MethodGenerator<SimpleTestMethodModel>() {
        override fun generateSignature(method: SimpleTestMethodModel, p: Printer) {
            generateDefaultSignature(method, p)
        }

        override fun generateBody(method: SimpleTestMethodModel, p: Printer) {
            val file = method.file
            when (method.testInfraRevision) {
                TestInfraRevision.StandardJUnit5 if file.isFile -> {
                    p.println(RunTestWithDirectoryPrefixMethodModel.METHOD_NAME, "(\"", file.name, "\");")
                }
                else -> {
                    val filePath = file.getFilePath() + if (file.isDirectory) "/" else ""
                    p.println(DEFAULT_RUN_TEST_METHOD_NAME, "(\"", filePath, "\");")
                }
            }
        }
    }
}
