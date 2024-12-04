/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.impl

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.generators.MethodGenerator
import org.jetbrains.kotlin.generators.model.MethodModel
import org.jetbrains.kotlin.generators.model.SimpleTestClassModel
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.Printer
import java.util.regex.Pattern

object SimpleTestClassModelTestAllFilesPresentMethodGenerator : MethodGenerator<SimpleTestClassModel.TestAllFilesPresentMethodModel>() {
    override val kind: MethodModel.Kind
        get() = SimpleTestClassModel.TestAllFilesPresentMethodKind

    override fun generateSignature(method: SimpleTestClassModel.TestAllFilesPresentMethodModel, p: Printer) {
        generateDefaultSignature(method, p)
    }

    override fun generateBody(method: SimpleTestClassModel.TestAllFilesPresentMethodModel, p: Printer) {
        with(method) {
            val exclude = StringBuilder()
            for (dir in classModel.excludeDirs + classModel.excludeDirsRecursively) {
                exclude.append(", \"")
                exclude.append(StringUtil.escapeStringCharacters(dir))
                exclude.append("\"")
            }
            val excludePattern = classModel.excludePattern
            if (classModel.targetBackend === TargetBackend.ANY) {
                p.print(
                    "KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File(\"",
                    KtTestUtil.getFilePath(classModel.rootFile),
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
            } else {
                p.print(
                    "KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File(\"",
                    KtTestUtil.getFilePath(classModel.rootFile),
                    "\"), Pattern.compile(\"",
                    StringUtil.escapeStringCharacters(classModel.filenamePattern.pattern()),
                    "\"), ",
                )
                p.printExcludePattern(excludePattern)
                p.printlnWithNoIndent(
                    ", ",
                    TargetBackend::class.java.simpleName,
                    ".",
                    classModel.targetBackend.toString(),
                    ", ",
                    classModel.recursive,
                    exclude,
                    ");"
                )
            }
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
