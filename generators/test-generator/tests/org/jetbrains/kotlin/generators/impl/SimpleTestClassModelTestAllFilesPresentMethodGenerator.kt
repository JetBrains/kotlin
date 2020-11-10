/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.impl

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.generators.model.MethodModel
import org.jetbrains.kotlin.generators.MethodGenerator
import org.jetbrains.kotlin.generators.model.SimpleTestClassModel
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addToStdlib.runIf

object SimpleTestClassModelTestAllFilesPresentMethodGenerator : MethodGenerator<SimpleTestClassModel.TestAllFilesPresentMethodModel>() {
    override val kind: MethodModel.Kind
        get() = SimpleTestClassModel.TestAllFilesPresentMethodKind

    override fun generateSignature(method: SimpleTestClassModel.TestAllFilesPresentMethodModel, p: Printer) {
        generateDefaultSignature(method, p)
    }

    override fun generateBody(method: SimpleTestClassModel.TestAllFilesPresentMethodModel, p: Printer) {
        with(method) {
            val exclude = StringBuilder()
            for (dir in classModel.excludeDirs) {
                exclude.append(", \"")
                exclude.append(StringUtil.escapeStringCharacters(dir))
                exclude.append("\"")
            }
            val excludedArgument = runIf(classModel.excludePattern != null) {
                String.format("Pattern.compile(\"%s\")", StringUtil.escapeStringCharacters(classModel.excludePattern!!.pattern()))
            }
            val assertTestsPresentStr = if (classModel.targetBackend === TargetBackend.ANY) {
                String.format(
                    "KotlinTestUtils.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File(\"%s\"), Pattern.compile(\"%s\"), %s, %s%s);",
                    KotlinTestUtils.getFilePath(classModel.rootFile),
                    StringUtil.escapeStringCharacters(classModel.filenamePattern.pattern()),
                    excludedArgument,
                    classModel.recursive,
                    exclude
                )
            } else {
                String.format(
                    "KotlinTestUtils.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File(\"%s\"), Pattern.compile(\"%s\"), %s, %s.%s, %s%s);",
                    KotlinTestUtils.getFilePath(classModel.rootFile),
                    StringUtil.escapeStringCharacters(classModel.filenamePattern.pattern()),
                    excludedArgument, TargetBackend::class.java.simpleName, classModel.targetBackend.toString(), classModel.recursive, exclude
                )
            }
            p.println(assertTestsPresentStr)
        }
    }
}
