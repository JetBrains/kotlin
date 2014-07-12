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
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue

import java.io.File
import org.jetbrains.jet.lang.resolve.lazy.JvmResolveUtil
import org.jetbrains.jet.lang.resolve.calls.model.ArgumentMapping
import org.jetbrains.jet.lang.resolve.calls.model.ArgumentMatch
import org.jetbrains.jet.renderer.DescriptorRenderer
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.jet.lang.psi.ValueArgument
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetExpression
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.jet.lang.resolve.calls.callUtil.getParentResolvedCall
import org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.resolve.scopes.receivers.ClassReceiver

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
    is ExpressionReceiver -> "${getExpression().getText()} {${getType()}}"
    is ClassReceiver -> "Class{${getType()}}"
    is ExtensionReceiver -> "${getType()}Ext{${getDeclarationDescriptor().getText()}}"
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

private fun DeclarationDescriptor.getText(): String = when (this) {
    is ReceiverParameterDescriptor -> "${getValue().getText()}::this"
    else -> DescriptorRenderer.COMPACT_WITH_SHORT_TYPES.render(this)
}

private fun ResolvedCall<*>.renderToText(): String {
    return StringBuilder {
        appendln("Resolved call:")
        appendln()

        if (getCandidateDescriptor() != getResultingDescriptor()) {
            appendln("Candidate descriptor: ${getCandidateDescriptor()!!.getText()}")
        }
        appendln("Resulting descriptor: ${getResultingDescriptor()!!.getText()}")
        appendln()

        appendln("Explicit receiver kind = ${getExplicitReceiverKind()}")
        appendln("This object = ${getThisObject().getText()}")
        appendln("Receiver argument = ${getReceiverArgument().getText()}")

        val valueArguments = getCall().getValueArguments()
        if (!valueArguments.isEmpty()) {
            appendln()
            appendln("Value arguments mapping:")
            appendln()

            for (valueArgument in valueArguments) {
                val argumentText = valueArgument!!.getText()
                val argumentMappingText = getArgumentMapping(valueArgument).getText()

                appendln("$argumentMappingText $argumentText")
            }
        }
    }.toString()
}