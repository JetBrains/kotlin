/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.impl

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.generators.model.MethodModel
import org.jetbrains.kotlin.generators.MethodGenerator
import org.jetbrains.kotlin.generators.model.SingleClassTestModel
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.utils.Printer

object SingleClassTestModelAllFilesPresentedMethodGenerator : MethodGenerator<SingleClassTestModel.TestAllFilesPresentMethodModel>() {
    override val kind: MethodModel.Kind
        get() = SingleClassTestModel.AllFilesPresentedMethodKind

    override fun generateSignature(method: SingleClassTestModel.TestAllFilesPresentMethodModel, p: Printer) {
        generateDefaultSignature(method, p)
    }

    override fun generateBody(method: SingleClassTestModel.TestAllFilesPresentMethodModel, p: Printer) {
        with(method) {
            with(classModel) {
                val assertTestsPresentStr: String
                val excludedArgument = if (excludePattern != null) {
                    String.format(
                        "Pattern.compile(\"%s\")", StringUtil.escapeStringCharacters(
                            excludePattern.pattern()
                        )
                    )
                } else {
                    null
                }
                assertTestsPresentStr = if (targetBackend !== TargetBackend.ANY) {
                    String.format(
                        "KotlinTestUtils.assertAllTestsPresentInSingleGeneratedClassWithExcluded(this.getClass(), new File(\"%s\"), Pattern.compile(\"%s\"), %s, %s.%s);",
                        KotlinTestUtils.getFilePath(rootFile), StringUtil.escapeStringCharacters(filenamePattern.pattern()),
                        excludedArgument, TargetBackend::class.java.simpleName, targetBackend.toString()
                    )
                } else {
                    String.format(
                        "KotlinTestUtils.assertAllTestsPresentInSingleGeneratedClassWithExcluded(this.getClass(), new File(\"%s\"), Pattern.compile(\"%s\"), %s);",
                        KotlinTestUtils.getFilePath(rootFile),
                        StringUtil.escapeStringCharacters(filenamePattern.pattern()),
                        excludedArgument
                    )
                }
                p.println(assertTestsPresentStr)
            }
        }

    }
}
