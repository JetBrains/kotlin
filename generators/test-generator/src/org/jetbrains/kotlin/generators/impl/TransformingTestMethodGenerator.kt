/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.impl

import org.jetbrains.kotlin.generators.MethodGenerator
import org.jetbrains.kotlin.generators.model.MethodModel
import org.jetbrains.kotlin.generators.model.RunTestMethodModel
import org.jetbrains.kotlin.generators.model.TransformingTestMethodModel
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.Printer

object TransformingTestMethodGenerator : MethodGenerator<TransformingTestMethodModel>() {

    override val kind: MethodModel.Kind
        get() = TransformingTestMethodModel.Kind

    override fun generateSignature(method: TransformingTestMethodModel, p: Printer) {
        generateDefaultSignature(method, p)
    }

    override fun generateBody(method: TransformingTestMethodModel, p: Printer) {
        with(method) {
            if (registerInConstructor) {
                val lines = transformer.lines()
                val message = "There is a registered source transformer for the testcase"
                if (lines.size > 1) {
                    p.println("/*")
                    p.println("  $message:")
                    lines.forEach { p.println("  $it") }
                    p.println("*/")
                } else {
                    val restOfLine = lines.firstOrNull()?.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""
                    p.println("// $message$restOfLine")
                }
            }
            val filePath = KtTestUtil.getFilePath(source.file) + if (source.file.isDirectory) "/" else ""
            p.println("${RunTestMethodModel.METHOD_NAME}(\"$filePath\"${if (registerInConstructor) "" else ", $transformer"});")
        }
    }
}
