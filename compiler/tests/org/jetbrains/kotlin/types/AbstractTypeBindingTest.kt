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

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.resolve.typeBinding.TypeArgumentBinding
import org.jetbrains.kotlin.resolve.typeBinding.TypeBinding
import org.jetbrains.kotlin.resolve.typeBinding.createTypeBindingForReturnType
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils.assertEqualsToFile
import org.jetbrains.kotlin.test.KotlinTestUtils.loadJetFile
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment
import org.jetbrains.kotlin.utils.Printer
import java.io.File

abstract class AbstractTypeBindingTest : KotlinTestWithEnvironment() {
    override fun createEnvironment() = createEnvironmentWithMockJdk(ConfigurationKind.ALL)

    protected fun doTest(path: String) {
        val testFile = File(path)
        val testKtFile = loadJetFile(project, testFile)

        val analyzeResult = JvmResolveUtil.analyze(testKtFile, environment)

        val testDeclaration = testKtFile.declarations.last()!! as KtCallableDeclaration

        val typeBinding = testDeclaration.createTypeBindingForReturnType(analyzeResult.bindingContext)

        assertEqualsToFile(
                testFile,
                buildString {
                    append(removeLastComment(testKtFile))
                    append("/*\n")

                    MyPrinter(this).print(typeBinding)

                    append("*/")
                }
        )
    }

    private fun removeLastComment(file: KtFile): String {
        val fileText = file.text
        val lastIndex = fileText.indexOf("/*")
        return if (lastIndex > 0) {
            fileText.substring(0, lastIndex)
        }
        else fileText
    }

    private class MyPrinter(out: StringBuilder) : Printer(out) {
        private fun KotlinType.render() = DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(this)
        private fun TypeParameterDescriptor?.render() = if (this == null) "null" else DescriptorRenderer.SHORT_NAMES_IN_TYPES.render(this)

        fun print(argument: TypeArgumentBinding<*>?): MyPrinter {
            if (argument == null) {
                println("null")
                return this
            }
            println("typeParameter: ${argument.typeParameter.render()}")

            val projection = argument.projection.projectionKind.label.let {
                if (it.isNotEmpty())
                    "$it "
                else
                    ""
            }

            print("typeProjection: ")
            if (argument.projection.isStarProjection)
                printlnWithNoIndent("*")
            else printlnWithNoIndent("${projection}${argument.projection.type.render()}")
            print(argument.binding)
            return this
        }

        fun print(binding: TypeBinding<*>?): MyPrinter {
            if (binding == null) {
                println("null")
                return this
            }

            println("psi: ${binding.psiElement.text}")
            println("type: ${binding.type.render()}")

            printCollection(binding.arguments) {
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
    }
}
