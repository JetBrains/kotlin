/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.impl

import org.jetbrains.kotlin.generators.model.MethodModel
import org.jetbrains.kotlin.generators.MethodGenerator
import org.jetbrains.kotlin.generators.model.RunTestMethodModel
import org.jetbrains.kotlin.generators.model.SimpleTestMethodModel
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.utils.Printer

object SimpleTestMethodGenerator : MethodGenerator<SimpleTestMethodModel>() {
    override val kind: MethodModel.Kind
        get() = SimpleTestMethodModel.Kind

    override fun generateSignature(method: SimpleTestMethodModel, p: Printer) {
        generateDefaultSignature(method, p)
    }

    override fun generateBody(method: SimpleTestMethodModel, p: Printer) {
        with(method) {
            val filePath = KotlinTestUtils.getFilePath(file) + if (file.isDirectory) "/" else ""
            p.println(RunTestMethodModel.METHOD_NAME, "(\"", filePath, "\");")
        }
    }
}
