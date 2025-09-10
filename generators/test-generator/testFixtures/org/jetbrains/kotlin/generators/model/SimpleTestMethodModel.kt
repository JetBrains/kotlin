/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.generators.model

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.generators.MethodGenerator
import org.jetbrains.kotlin.generators.impl.SimpleTestMethodGenerator
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil.escapeForJavaIdentifier
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File
import java.util.regex.Pattern

class SimpleTestMethodModel(
    private val rootDir: File,
    val file: File,
    private val filenamePattern: Pattern,
    checkFilenameStartsLowerCase: Boolean?,
    internal val targetBackend: TargetBackend,
    override val tags: List<String>,
) : MethodModel<SimpleTestMethodModel>() {
    override val generator: MethodGenerator<SimpleTestMethodModel>
        get() = SimpleTestMethodGenerator

    override val dataString: String
        get() {
            val path = FileUtil.getRelativePath(rootDir, file)!!
            return KtTestUtil.getFilePath(File(path))
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
            val nameSuffix = escapeForJavaIdentifier(unescapedName).replaceFirstChar(Char::uppercaseChar)
            return "test$nameSuffix"
        }

    init {
        if (checkFilenameStartsLowerCase != null) {
            val c = file.name[0]
            if (checkFilenameStartsLowerCase) {
                assert(Character.isLowerCase(c)) { "Invalid file name '$file', file name should start with lower-case letter" }
            } else {
                assert(Character.isUpperCase(c)) { "Invalid file name '$file', file name should start with upper-case letter" }
            }
        }
    }
}
