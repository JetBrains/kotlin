/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.services.ReversibleSourceFilePreprocessor
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.testInfo
import java.util.regex.Pattern

class BatchingPackageInserter(testServices: TestServices) : ReversibleSourceFilePreprocessor(testServices) {
    companion object {
        private val packagePattern: Pattern = Pattern.compile("""^(\s*)package\s+(\S+)""", Pattern.MULTILINE)
    }

    override fun process(file: TestFile, content: String): String {
        val additionalPackage = additionalPackage(testServices)
        val matcher = packagePattern.matcher(content)

        return if (matcher.find()) {
            val leadingWhitespace = matcher.group(1)
            val existingPackage = matcher.group(2)
            val newPackage = "$additionalPackage.$existingPackage"
            matcher.replaceFirst("${leadingWhitespace}package $newPackage")
        } else {
            "package $additionalPackage\n\n$content"
        }
    }

    override fun revert(file: TestFile, actualContent: String): String {
        val additionalPackage = additionalPackage(testServices)
        val matcher = packagePattern.matcher(actualContent)

        if (!matcher.find()) {
            return actualContent
        }
        val leadingWhitespace = matcher.group(1)
        val currentPackage = matcher.group(2)
        val additionalPackagePrefix = "$additionalPackage."

        return when {
            currentPackage.startsWith(additionalPackagePrefix) -> {
                val originalPackage = currentPackage.removePrefix(additionalPackagePrefix)
                matcher.replaceFirst("${leadingWhitespace}package $originalPackage")
            }
            currentPackage == additionalPackage -> {
                // The file originally had no package directive
                val packageDirectiveEnd = matcher.end()
                // Remove the package directive and any following blank lines
                actualContent.substring(packageDirectiveEnd).trimStart('\n')
            }
            else -> actualContent
        }
    }

    private fun additionalPackage(testServices: TestServices): String {
        val (className, methodName, _) = testServices.testInfo
        val classPart = className.substringAfter("$").replace("$", ".")
        return "$classPart.$methodName"
    }
}
