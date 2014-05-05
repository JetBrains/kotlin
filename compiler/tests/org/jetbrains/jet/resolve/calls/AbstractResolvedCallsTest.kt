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
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall
import org.jetbrains.jet.lang.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.jet.lang.resolve.scopes.receivers.AbstractReceiverValue
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.jet.lang.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.jet.lang.resolve.lazy.JvmResolveUtil
import java.util.ArrayList
import org.jetbrains.jet.lang.resolve.calls.model.ArgumentMapping
import org.jetbrains.jet.lang.resolve.calls.model.ArgumentMatch
import org.jetbrains.jet.lang.diagnostics.rendering.Renderers
import org.jetbrains.jet.lang.resolve.calls.util.getAllValueArguments
import org.jetbrains.jet.renderer.DescriptorRenderer

public abstract class AbstractResolvedCallsTest() : JetLiteFixture() {
    override fun createEnvironment(): JetCoreEnvironment = createEnvironmentWithMockJdk(ConfigurationKind.JDK_ONLY)

    public fun doTest(filePath: String) {
        val file = File(filePath)
        val text = JetTestUtils.doLoadFile(file)
        val directives = JetTestUtils.parseDirectives(text)

        val onlyArguments = directives.onlyArguments()
        val (callName, thisObject, receiverArgument) = with (directives) {
            Triple(get("CALL"), get("THIS_OBJECT"), get("RECEIVER_ARGUMENT"))
        }
        val explicitReceiverKind = directives.getExplicitReceiverKind()

        fun analyzeFileAndGetResolvedCallEntries(): Map<JetElement, ResolvedCall<*>> {
            val psiFile = JetTestUtils.loadJetFile(getProject(), file)!!
            val analyzeExhaust = JvmResolveUtil.analyzeOneFileWithJavaIntegration(psiFile)
            val bindingContext = analyzeExhaust.getBindingContext()
            return bindingContext.getSliceContents(BindingContext.RESOLVED_CALL)
        }

        fun checkResolvedCall(resolvedCall: ResolvedCall<*>, element: JetElement) {
            if (onlyArguments) return

            val lineAndColumn = DiagnosticUtils.getLineAndColumnInPsiFile(element.getContainingFile(), element.getTextRange())

            val (actualThisObject, actualReceiverArgument, actualExplicitReceiverKind) = with(resolvedCall) {
                Triple(getThisObject().getText(), getReceiverArgument().getText(), getExplicitReceiverKind())
            }
            val actualDataMessage = "Actual data:\nThis object: $actualThisObject. Receiver argument: $actualReceiverArgument. " +
                                    "Explicit receiver kind: $actualExplicitReceiverKind.\n"

            assertEquals(thisObject, actualThisObject, "${actualDataMessage}This object mismatch: ")
            assertEquals(receiverArgument, actualReceiverArgument, "${actualDataMessage}Receiver argument mismatch: ")
            assertEquals(explicitReceiverKind, actualExplicitReceiverKind,
                         "$actualDataMessage" +
                         "Explicit receiver kind for resolved call for '${element.getText()}'$lineAndColumn in not as expected")
        }

        val argumentsExpectedData = directives.getArgumentsExpectedData()
        fun checkArguments(resolvedCall: ResolvedCall<*>) {
            if (argumentsExpectedData.isEmpty()) return

            for ((index, valueArgument) in resolvedCall.getCall().getAllValueArguments().withIndices()) {
                val (argText, argumentMapping) = argumentsExpectedData[index]
                if (argText != null) {
                    assertEquals(argText, valueArgument.getArgumentExpression()!!.getText(),
                                 "An argument expression is incorrect")
                }
                assertEquals(argumentMapping, resolvedCall.getArgumentMapping(valueArgument).print(),
                             "Argument mapping is incorrect")
            }
        }

        var callFound = false
        for ((element, resolvedCall) in analyzeFileAndGetResolvedCallEntries()) {

            fun tryCall(resolvedCall: ResolvedCall<*>, actualName: String? = element.getText()) {
                if (callName == null || callName != actualName) return
                callFound = true
                checkResolvedCall(resolvedCall, element)
                checkArguments(resolvedCall)
            }

            if (resolvedCall is VariableAsFunctionResolvedCall) {
                tryCall(resolvedCall.functionCall, "invoke")
                tryCall(resolvedCall.variableCall)
            }
            else {
                tryCall(resolvedCall)
            }
        }
        assertTrue(callFound, "Resolved call for $callName was not found")
    }
}

private fun ReceiverValue.getText() =
        if (this is ExpressionReceiver) {
            this.getExpression().getText()
        }
        else if (this is AbstractReceiverValue) {
            this.getType().toString()
        }
        else toString()

private val EXPLICIT_RECEIVER_KIND_DIRECTIVE: String = "EXPLICIT_RECEIVER_KIND"
private fun Map<String, String>.getExplicitReceiverKind(): ExplicitReceiverKind? {
    if (onlyArguments()) return null

    val explicitReceiverKind = get(EXPLICIT_RECEIVER_KIND_DIRECTIVE)
    assert(explicitReceiverKind != null) { "$EXPLICIT_RECEIVER_KIND_DIRECTIVE should be present." }
    try {
        return ExplicitReceiverKind.valueOf(explicitReceiverKind!!)
    }
    catch (e: IllegalArgumentException) {
        val message = StringBuilder()
        message.append("$EXPLICIT_RECEIVER_KIND_DIRECTIVE must be one of the following: ")
        for (kind in ExplicitReceiverKind.values()) {
            message.append("$kind, ")
        }
        message.append("\nnot $explicitReceiverKind.")
        throw AssertionError(message)
    }
}

private fun Map<String, String>.getArgumentsExpectedData(): List<Pair<String?, String>> {
    val result = ArrayList<Pair<String?, String>>()
    for (i in 1..7) {
        val argData = get("ARG_$i")
        if (argData == null) continue

        if (argData.contains("=")) {
            val strings = argData.split("=")
            assert(strings.size == 2, "Incorrect input data for an argument ARG_$i : $argData")
            result.add(Pair(strings[0].trim(), strings[1].trim()))
        }
        else {
            result.add(Pair(null, argData))
        }
    }
    return result
}

private fun Map<String, String>.onlyArguments() = containsKey("ONLY_ARGUMENTS")

fun ArgumentMapping.print() = when (this) {
    is ArgumentMatch -> {
        val parameterType = DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(valueParameter.getType())
        "ArgumentMatch(${valueParameter.getName()} : ${parameterType}, ${status.name()})"
    }
    else -> "ArgumentUnmapped"
}