/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.impl

import org.jetbrains.kotlin.generators.MethodGenerator
import org.jetbrains.kotlin.generators.model.MethodModel
import org.jetbrains.kotlin.generators.model.RunTestMethodModel
import org.jetbrains.kotlin.generators.model.WithoutJvmInlineTestMethodModel
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.Printer

object WithoutJvmInlineTestMethodGenerator : MethodGenerator<WithoutJvmInlineTestMethodModel>() {

    override val kind: MethodModel.Kind
        get() = WithoutJvmInlineTestMethodModel.Kind

    override fun generateSignature(method: WithoutJvmInlineTestMethodModel, p: Printer) {
        generateDefaultSignature(method, p)
    }

    override fun generateBody(method: WithoutJvmInlineTestMethodModel, p: Printer) {
        with(method) {
            val filePath = KtTestUtil.getFilePath(source.file) + if (source.file.isDirectory) "/" else ""
            p.println("${RunTestMethodModel.METHOD_NAME}(\"$filePath\", s -> s.replaceAll(\"@(kotlin.jvm.)?JvmInline\", \"\"));")
        }
    }
}
