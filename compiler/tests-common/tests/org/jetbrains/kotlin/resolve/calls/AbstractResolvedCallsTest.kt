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

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getParentResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMapping
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.Receiver
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment
import java.io.File

abstract class AbstractResolvedCallsTest : KotlinTestWithEnvironment() {
    override fun createEnvironment(): KotlinCoreEnvironment = createEnvironmentWithMockJdk(ConfigurationKind.ALL)

    fun doTest(filePath: String) {
        val originalText = KotlinTestUtils.doLoadFile(File(filePath))!!
        val (text, carets) = extractCarets(originalText)

        val ktFile = KtPsiFactory(project).createFile(text)
        val bindingContext = JvmResolveUtil.analyze(ktFile, environment).bindingContext

        val resolvedCallsAt = carets.map { caret -> caret to run {
            val (element, cachedCall) = buildCachedCallAtIndex(bindingContext, ktFile, caret)

            val resolvedCall = when {
                cachedCall !is VariableAsFunctionResolvedCall -> cachedCall
                "(" == element?.text -> cachedCall.functionCall
                else -> cachedCall.variableCall
            }

            resolvedCall
        }}

        val output = renderOutput(originalText, text, resolvedCallsAt)

        val resolvedCallInfoFileName = FileUtil.getNameWithoutExtension(filePath) + ".txt"
        KotlinTestUtils.assertEqualsToFile(File(resolvedCallInfoFileName), output)
    }

    protected open fun renderOutput(originalText: String, text: String, resolvedCallsAt: List<Pair<Int, ResolvedCall<*>?>>): String =
            resolvedCallsAt.joinToString("\n\n", prefix = "$originalText\n\n\n") { (_, resolvedCall) ->
                resolvedCall?.renderToText().toString()
            }

    protected fun extractCarets(text: String): Pair<String, List<Int>> {
        val parts = text.split("<caret>")
        if (parts.size < 2) return text to emptyList()
        // possible to rewrite using 'scan' function to get partial sums of parts lengths
        val indices = mutableListOf<Int>()
        val resultText = buildString {
            parts.dropLast(1).forEach { part ->
                append(part)
                indices.add(this.length)
            }
            append(parts.last())
        }
        return resultText to indices
    }

    protected open fun buildCachedCallAtIndex(
            bindingContext: BindingContext, jetFile: KtFile, index: Int
    ): Pair<PsiElement?, ResolvedCall<out CallableDescriptor>?> {
        val element = jetFile.findElementAt(index)!!
        val expression = element.getStrictParentOfType<KtExpression>()

        val cachedCall = expression?.getParentResolvedCall(bindingContext, strict = false)
        return Pair(element, cachedCall)
    }
}

internal fun Receiver?.getText() = when (this) {
    is ExpressionReceiver -> "${expression.text} {${type}}"
    is ImplicitClassReceiver -> "Class{${type}}"
    is ExtensionReceiver -> "${type}Ext{${declarationDescriptor.getText()}}"
    null -> "NO_RECEIVER"
    else -> toString()
}

internal fun ValueArgument.getText() = this.getArgumentExpression()?.text?.replace("\n", " ") ?: ""

internal fun ArgumentMapping.getText() = when (this) {
    is ArgumentMatch -> {
        val parameterType = DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(valueParameter.type)
        "${status.name}  ${valueParameter.name} : ${parameterType} ="
    }
    else -> "ARGUMENT UNMAPPED: "
}

internal fun DeclarationDescriptor.getText(): String = when (this) {
    is ReceiverParameterDescriptor -> "${value.getText()}::this"
    else -> DescriptorRenderer.COMPACT_WITH_SHORT_TYPES.render(this)
}

internal fun ResolvedCall<*>.renderToText(): String {
    return buildString {
        appendln("Resolved call:")
        appendln()

        if (candidateDescriptor != resultingDescriptor) {
            appendln("Candidate descriptor: ${candidateDescriptor!!.getText()}")
        }
        appendln("Resulting descriptor: ${resultingDescriptor!!.getText()}")
        appendln()

        appendln("Explicit receiver kind = ${explicitReceiverKind}")
        appendln("Dispatch receiver = ${dispatchReceiver.getText()}")
        appendln("Extension receiver = ${extensionReceiver.getText()}")

        val valueArguments = call.valueArguments
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
    }
}
