/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi

import com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirLibraryBinaryDecompiledTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractDecompiledTextTest : AbstractAnalysisApiBasedTest() {
    override val configurator: AnalysisApiTestConfigurator
        get() = AnalysisApiFirLibraryBinaryDecompiledTestConfigurator

    override fun doTestByMainModuleAndOptionalMainFile(mainFile: KtFile?, mainModule: KtTestModule, testServices: TestServices) {
        val files = mainModule.ktFiles.sortedBy(KtFile::getName)

        val actual = prettyPrint {
            if (files.isEmpty()) {
                appendLine("NO FILES")
                return@prettyPrint
            }

            val file = files.singleOrNull()
            if (file != null) {
                append(file.text)
            } else {
                printCollection(files, separator = "\n") { file ->
                    appendLine("${file.name}:")
                    withIndent {
                        append(file.text)
                    }
                }
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual, extension = ".decompiledText.txt")

        val validator = object : KtTreeVisitorVoid() {
            override fun visitErrorElement(element: PsiErrorElement) {
                testServices.assertions.fail {
                    val parent = element.parent
                    """
                        Decompiled file should not contain syntax errors!
                        Parent class: ${parent::class.simpleName}
                        Parent text: ${parent.text}
                    """.trimIndent()
                }
            }
        }

        for (file in files) {
            file.accept(validator)
        }
    }
}
