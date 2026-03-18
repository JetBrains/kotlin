/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.model.methods

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.generators.MethodGenerator
import org.jetbrains.kotlin.generators.model.MethodModel
import org.jetbrains.kotlin.generators.model.SimpleTestClassModel
import org.jetbrains.kotlin.generators.util.getFilePath
import org.jetbrains.kotlin.utils.Printer
import java.util.regex.Pattern

/**
 * Test method which ensures that there is a generated test for each testdata file in the
 *   corresponding directory. Used to validate that generated test is up-to-date.
 */
class TestAllFilesPresentMethodModel(val classModel: SimpleTestClassModel) : MethodModel<TestAllFilesPresentMethodModel>() {
    override val generator: MethodGenerator<TestAllFilesPresentMethodModel> get() = Generator

    override val name: String
        get() = "testAllFilesPresentIn${classModel.testClassName}"

    override val dataString: String?
        get() = null

    override val tags: List<String>
        get() = emptyList()

    private object Generator : MethodGenerator<TestAllFilesPresentMethodModel>() {
        override fun generateSignature(method: TestAllFilesPresentMethodModel, p: Printer) {
            generateDefaultSignature(method, p)
        }

        override fun generateBody(method: TestAllFilesPresentMethodModel, p: Printer) {
            with(method) {
                val exclude = StringBuilder()
                for (dir in classModel.allExcludedDirs) {
                    exclude.append(", \"")
                    exclude.append(StringUtil.escapeStringCharacters(dir))
                    exclude.append("\"")
                }
                val excludePattern = classModel.excludePattern
                p.print(
                    "KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File(\"",
                    classModel.rootFile.getFilePath(),
                    "\"), Pattern.compile(\"",
                    StringUtil.escapeStringCharacters(classModel.filenamePattern.pattern()),
                    "\"), ",
                )
                p.printExcludePattern(excludePattern)
                p.printlnWithNoIndent(
                    ", ",
                    classModel.recursive,
                    exclude,
                    ");"
                )

            }
        }

        private fun Printer.printExcludePattern(excludePattern: Pattern?) {
            if (excludePattern != null) {
                printWithNoIndent(
                    "Pattern.compile(\"",
                    StringUtil.escapeStringCharacters(excludePattern.pattern()),
                    "\")"
                )
            } else {
                printWithNoIndent("null")
            }
        }
    }
}
