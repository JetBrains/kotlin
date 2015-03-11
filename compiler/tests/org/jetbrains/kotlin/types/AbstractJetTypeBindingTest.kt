/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.test.JetLiteFixture
import org.jetbrains.kotlin.test.ConfigurationKind
import java.io.File
import org.jetbrains.kotlin.resolve.typeBinding.*
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.psi.JetCallableDeclaration
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.test.JetTestUtils.*

abstract class AbstractJetTypeBindingTest : JetLiteFixture() {
    override fun createEnvironment() = createEnvironmentWithMockJdk(ConfigurationKind.ALL)

    protected fun doTest(path: String) {
        val testFile = File(path)
        val testKtFile = loadJetFile(getProject(), testFile)

        val analyzeResult = JvmResolveUtil.analyzeFilesWithJavaIntegration(getProject(), listOf(testKtFile))

        val testDeclaration = testKtFile.getDeclarations().last!! as JetCallableDeclaration

        val typeBinding = testDeclaration.createTypeBindingForReturnType(analyzeResult.bindingContext)

        assertEqualsToFile(
                testFile,
                StringBuilder {
                    append(removeLastComment(testKtFile))
                    append("/*\n")

                    MyPrinter(this).print(typeBinding)

                    append("*/")
                }.toString()
        )
    }

    private fun removeLastComment(file: JetFile): String {
        val fileText = file.getText()
        val lastIndex = fileText.indexOf("/*")
        return if (lastIndex > 0) {
            fileText.substring(0, lastIndex)
        }
        else fileText
    }

    private class MyPrinter(out: StringBuilder) : Printer(out) {
        private fun JetType.render() = DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(this)
        private fun TypeParameterDescriptor?.render() = if (this == null) "null" else DescriptorRenderer.SHORT_NAMES_IN_TYPES.render(this)

        fun print(argument: TypeArgumentBinding<*>?): MyPrinter {
            if (argument == null) {
                println("null")
                return this
            }
            println("typeParameter: ${argument.typeParameterDescriptor.render()}")

            val projection = argument.typeProjection.getProjectionKind().label.let {
                if (it.isNotEmpty())
                    "$it "
                else
                    ""
            }

            print("typeProjection: ")
            if (argument.typeProjection.isStarProjection())
                printlnWithNoIndent("*")
            else printlnWithNoIndent("${projection}${argument.typeProjection.getType().render()}")
            print(argument.typeBinding)
            return this
        }

        fun print(binding: TypeBinding<*>?): MyPrinter {
            if (binding == null) {
                println("null")
                return this
            }

            println("psi: ${binding.psiElement.getText()}")
            println("type: ${binding.jetType.render()}")

            printCollection(binding.getArgumentBindings()) {
                print(it)
            }
            return this
        }

        private fun <T> printCollection(list: Iterable<T>, f: MyPrinter.(T) -> Unit) {
            pushIndent()
            var first = true
            for (element in list) {
                if (first) first = false
                else println()

                f(element)
            }
            popIndent()
        }

        override fun toString(): String = out.toString()
    }
}
