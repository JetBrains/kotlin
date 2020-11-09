/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.generators.tests.generator

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.generators.tests.generator.TestGeneratorUtil.escapeForJavaIdentifier
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.utils.Printer
import java.io.File
import java.util.regex.Pattern

open class SimpleTestMethodModel(
    private val rootDir: File,
    val file: File,
    private val filenamePattern: Pattern,
    checkFilenameStartsLowerCase: Boolean?,
    protected val targetBackend: TargetBackend,
    private val skipIgnored: Boolean
) : MethodModel {
    object Kind : MethodModel.Kind()

    override val kind: MethodModel.Kind
        get() = Kind

    override val dataString: String
        get() {
            val path = FileUtil.getRelativePath(rootDir, file)!!
            return KotlinTestUtils.getFilePath(File(path))
        }

    override fun shouldBeGenerated(): Boolean {
        return InTextDirectivesUtils.isCompatibleTarget(targetBackend, file)
    }

    override val name: String
        get() {
            val matcher = filenamePattern.matcher(file.name)
            val found = matcher.find()
            assert(found) { file.name + " isn't matched by regex " + filenamePattern.pattern() }
            assert(matcher.groupCount() >= 1) { filenamePattern.pattern() }
            val extractedName = matcher.group(1) ?: error("extractedName should not be null: " + filenamePattern.pattern())
            val unescapedName = if (rootDir == file.parentFile) {
                extractedName
            } else {
                val relativePath = FileUtil.getRelativePath(rootDir, file.parentFile)
                relativePath + "-" + extractedName.capitalize()
            }
            val ignored = skipIgnored && InTextDirectivesUtils.isIgnoredTarget(targetBackend, file)
            return (if (ignored) "ignore" else "test") + escapeForJavaIdentifier(unescapedName).capitalize()
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
