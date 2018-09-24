/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.backend.common.AbstractClosureAnnotator
import org.jetbrains.kotlin.backend.common.Closure
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

abstract class AbstractClosureAnnotatorTestCase : AbstractIrGeneratorTestCase() {
    override fun doTest(wholeFile: File, testFiles: List<TestFile>) {
        val dir = wholeFile.parentFile
        val ignoreErrors = shouldIgnoreErrors(wholeFile)
        for ((testFile, irFile) in generateIrFilesAsSingleModule(testFiles, ignoreErrors)) {
            doTestIrFileAgainstExpectations(dir, testFile, irFile)
        }
    }

    private fun doTestIrFileAgainstExpectations(dir: File, testFile: TestFile, irFile: IrFile) {
        val expectedFile = File(dir, testFile.name.replace(".kt", ".closure"))
        val actualClosures = renderClosures(irFile)
        KotlinTestUtils.assertEqualsToFile(expectedFile, actualClosures)
    }

    private fun renderClosures(irFile: IrFile): String {
        val actualStringWriter = StringWriter()
        val actualOut = PrintWriter(actualStringWriter)

        irFile.acceptChildrenVoid(object : AbstractClosureAnnotator() {
            override fun recordClassClosure(classDescriptor: ClassDescriptor, closure: Closure) {
                actualOut.println("Closure for class ${classDescriptor.name}:")
                printClosure(closure)
                actualOut.println()
            }

            override fun recordFunctionClosure(functionDescriptor: FunctionDescriptor, closure: Closure) {
                if (functionDescriptor is ConstructorDescriptor) {
                    actualOut.println("Closure for constructor ${functionDescriptor.containingDeclaration.name}:")
                } else {
                    actualOut.println("Closure for function ${functionDescriptor.name}:")
                }
                printClosure(closure)
                actualOut.println()
            }

            private fun printClosure(closure: Closure) {
                closure.capturedValues.forEach {
                    actualOut.println("  variable ${it.name}")
                }
            }
        })

        return actualStringWriter.toString()
    }
}
