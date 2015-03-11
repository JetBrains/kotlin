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

package org.jetbrains.kotlin.resolve.calls

import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.JetLiteFixture
import org.jetbrains.kotlin.test.JetTestUtils
import org.jetbrains.kotlin.cli.jvm.compiler.JetCoreEnvironment
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import java.io.File
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMapping
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getParentResolvedCall
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassReceiver
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.psi.JetFile
import com.intellij.psi.PsiElement

public abstract class AbstractResolvedCallsTest : JetLiteFixture() {
    override fun createEnvironment(): JetCoreEnvironment = createEnvironmentWithMockJdk(ConfigurationKind.ALL)

    public fun doTest(filePath: String) {
        val text = JetTestUtils.doLoadFile(File(filePath))!!

        val jetFile = JetPsiFactory(getProject()).createFile(text.replace("<caret>", ""))
        val bindingContext = JvmResolveUtil.analyzeOneFileWithJavaIntegration(jetFile).bindingContext

        val (element, cachedCall) = buildCachedCall(bindingContext, jetFile, text)

        val resolvedCall = if (cachedCall !is VariableAsFunctionResolvedCall) cachedCall
            else if ("(" == element?.getText()) cachedCall.functionCall
            else cachedCall.variableCall

        val resolvedCallInfoFileName = FileUtil.getNameWithoutExtension(filePath) + ".txt"
        JetTestUtils.assertEqualsToFile(File(resolvedCallInfoFileName), "$text\n\n\n${resolvedCall?.renderToText()}")
    }

    open protected fun buildCachedCall(
            bindingContext: BindingContext, jetFile: JetFile, text: String
    ): Pair<PsiElement?, ResolvedCall<out CallableDescriptor>?> {
        val element = jetFile.findElementAt(text.indexOf("<caret>"))
        val expression = element.getStrictParentOfType<JetExpression>()

        val cachedCall = expression?.getParentResolvedCall(bindingContext, strict = false)
        return Pair(element, cachedCall)
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
        appendln("Dispatch receiver = ${getDispatchReceiver().getText()}")
        appendln("Extension receiver = ${getExtensionReceiver().getText()}")

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
