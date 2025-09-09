/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.stubs

import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractStubsTest : AbstractAnalysisApiBasedTest() {
    abstract val outputFileExtension: String
    abstract val stubsTestEngine: StubsTestEngine

    override val additionalDirectives: List<DirectivesContainer>
        get() = stubsTestEngine.additionalDirectives

    override fun doTestByMainModuleAndOptionalMainFile(mainFile: KtFile?, mainModule: KtTestModule, testServices: TestServices) {
        val files = mainModule.ktFiles
        val filesAndStubs = files.sortedBy(KtFile::getName).map { it to stubsTestEngine.compute(it) }

        val actual = prettyPrint {
            if (filesAndStubs.isEmpty()) {
                appendLine("NO FILES")
                return@prettyPrint
            }

            val singleElement = filesAndStubs.singleOrNull()
            if (singleElement != null) {
                printStub(singleElement.second)
            } else {
                printCollection(filesAndStubs, separator = "\n\n") { element ->
                    appendLine("${element.first.name}:")
                    withIndent {
                        printStub(element.second)
                    }
                }
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual, extension = outputFileExtension)

        for ((file, stub) in filesAndStubs) {
            stubsTestEngine.validate(testServices, file, stub)
        }
    }

    context(printer: PrettyPrinter)
    private fun printStub(stub: KotlinFileStubImpl) {
        val stubRepresentation = stubsTestEngine.render(stub)
        printer.append(stubRepresentation)
    }
}
