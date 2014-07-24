/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.resolve.calls

import org.jetbrains.jet.ConfigurationKind
import org.jetbrains.jet.JetLiteFixture
import org.jetbrains.jet.JetTestUtils
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall
import org.jetbrains.jet.lang.resolve.scopes.receivers.AbstractReceiverValue
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue

import java.io.File
import org.jetbrains.jet.lang.resolve.lazy.JvmResolveUtil
import org.jetbrains.jet.lang.resolve.calls.model.ArgumentMapping
import org.jetbrains.jet.lang.resolve.calls.model.ArgumentMatch
import org.jetbrains.jet.lang.resolve.calls.util.getAllValueArguments
import org.jetbrains.jet.renderer.DescriptorRenderer
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.jet.lang.psi.ValueArgument
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetExpression
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.jet.lang.resolve.bindingContextUtil.getParentResolvedCall

public abstract class AbstractResolvedCallsTest() : JetLiteFixture() {
    override fun createEnvironment(): JetCoreEnvironment = createEnvironmentWithMockJdk(ConfigurationKind.JDK_ONLY)

    public fun doTest(filePath: String) {
        val text = JetTestUtils.doLoadFile(File(filePath))!!

        val jetFile = JetPsiFactory(getProject()).createFile(text.replace("<caret>", ""))
        val bindingContext = JvmResolveUtil.analyzeOneFileWithJavaIntegration(jetFile).getBindingContext()

        val element = jetFile.findElementAt(text.indexOf("<caret>"))
        val expression = PsiTreeUtil.getParentOfType(element, javaClass<JetExpression>())

        val cachedCall = expression?.getParentResolvedCall(bindingContext, strict = false)

        val resolvedCall = if (cachedCall !is VariableAsFunctionResolvedCall) cachedCall
            else if ("(" == element?.getText()) cachedCall.functionCall
            else cachedCall.variableCall

        val resolvedCallInfoFileName = FileUtil.getNameWithoutExtension(filePath) + ".txt"
        JetTestUtils.assertEqualsToFile(File(resolvedCallInfoFileName), "$text\n\n\n${resolvedCall?.renderToText()}")
    }
}

private fun ReceiverValue.getText() = when (this) {
    is ExpressionReceiver -> getExpression().getText()
    is AbstractReceiverValue -> getType().toString()
    else -> toString()
}

private fun ValueArgument.getText() = this.getArgumentExpression()?.getText()?.replace("\n", " ") ?: ""

private fun ArgumentMapping.getText() = when (this) {
    is ArgumentMatch -> {
        val parameterType = DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(valueParameter.getType())
        "${status.name()}  ${valueParameter.getName()} : ${parameterType} ="
    }
    else -> "ARGUMENT UNMAPPED: "
}

private fun ResolvedCall<*>.renderToText(): String {
    val result = StringBuilder()
    fun addLine(line: String) = result.append(line).append("\n")

    addLine("Resolved call:\n")
    addLine("Explicit receiver kind = ${getExplicitReceiverKind()}")
    addLine("This object = ${getThisObject().getText()}")
    addLine("Receiver argument = ${getReceiverArgument().getText()}")

    val valueArguments = getCall().getAllValueArguments()
    if (valueArguments.isEmpty()) return result.toString()

    addLine("\nValue arguments mapping:\n")

    for (valueArgument in valueArguments) {
        val argumentText = valueArgument.getText()
        val argumentMappingText = getArgumentMapping(valueArgument).getText()

        addLine("$argumentMappingText $argumentText")
    }
    return result.toString()
}