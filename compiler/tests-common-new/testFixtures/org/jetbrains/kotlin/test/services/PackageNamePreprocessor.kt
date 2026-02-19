/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.test.model.TestFile
import java.util.regex.Pattern

/**
 * In the original K1 tests helper file with placeholder <?PACKAGE?> inside is only included in several tests.
 * As the whole code-spec infrastructure is not brought into K2 yet, the file is attached to all the tests.
 * In the tests where it shouldn't be added, it affects nothing independent of its package name.
 * In the tests where it is actually required, there is only one package in the main test file.
 * The package magic is necessary to allow import of the functions in the file without the explicit import statements.
 * It is used in K1 tests for the purpose.
 **/
class PackageNamePreprocessor(testServices: TestServices) : SourceFilePreprocessor(testServices) {
    private var packageName: String? = null
    private val packageNamePlaceholder = "<?PACKAGE?>"
    private val packagePattern: Pattern = Pattern.compile("""(?:^|\n)package (?<packageName>.*?)(?:;|\n)""")

    override fun process(file: TestFile, content: String): String {
        val packageName = packagePattern.matcher(content).let {
            if (it.find()) it.group("packageName") else null
        }
        if (packageName != null) {
            require(packageNamePlaceholder !in content) {
                "The actual package directive $packageName is already present in the file"
            }
            this.packageName = packageName
        }
        return content.replace(packageNamePlaceholder, if (this.packageName == null) "" else "package ${this.packageName}")
    }
}
