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

package org.jetbrains.kotlin.renderer

import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.impl.FunctionExpressionDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.resolve.lazy.KotlinTestWithEnvironment
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.JetTestUtils
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.File

abstract public class AbstractFunctionDescriptorInExpressionRendererTest : KotlinTestWithEnvironment() {
    public fun doTest(path: String) {
        val fileText = FileUtil.loadFile(File(path), true)
        val file = JetPsiFactory(getProject()).createFile(fileText)
        val bindingContext = JvmResolveUtil.analyzeOneFileWithJavaIntegration(file).bindingContext

        val descriptors = arrayListOf<DeclarationDescriptor>()

        file.accept(object : JetTreeVisitorVoid() {
            override fun visitNamedFunction(function: JetNamedFunction) {
                function.acceptChildren(this)
                descriptors.addIfNotNull(bindingContext.get(BindingContext.FUNCTION, function) as? FunctionExpressionDescriptor)
            }
            override fun visitFunctionLiteralExpression(expression: JetFunctionLiteralExpression) {
                expression.acceptChildren(this)
                descriptors.add(bindingContext.get(BindingContext.FUNCTION, expression.getFunctionLiteral()))
            }
        })

        val renderer = DescriptorRendererBuilder().setNameShortness(NameShortness.FULLY_QUALIFIED).setVerbose(true).build()
        val renderedDescriptors = descriptors.map { renderer.render(it) }.joinToString(separator = "\n")

        val document = DocumentImpl(file.getText())
        UsefulTestCase.assertSameLines(JetTestUtils.getLastCommentedLines(document), renderedDescriptors.toString())
    }

    override fun createEnvironment(): KotlinCoreEnvironment {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_ONLY)
    }
}