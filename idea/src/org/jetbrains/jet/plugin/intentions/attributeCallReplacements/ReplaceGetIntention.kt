/*
 * Copyright 2014 JetBrains s.r.o.
 * * Licensed under the Apache License, Version 2.0 (the "License");
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

package org.jetbrains.jet.plugin.intentions.attributeCallReplacements

import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.lang.psi.JetQualifiedExpression
import org.jetbrains.jet.lang.psi.JetExpression
import com.intellij.psi.tree.TokenSet
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.JetNodeTypes
import org.jetbrains.jet.lang.psi.ValueArgument
import org.jetbrains.jet.plugin.quickfix.QuickFixUtil
import org.jetbrains.jet.plugin.intentions.attributeCallReplacements.AttributeCallReplacementIntention.ReplacementException
import java.util.HashMap
import java.util.ArrayList

public class ReplaceGetIntention : AttributeCallReplacementIntention("replace.get.with.index") {
    public class UnnamedArgumentsException(message: String) : Exception(message)

    override fun isApplicableToCall(call: JetCallExpression): Boolean {
        return (call.getFunctionName() == "get" && call.getTypeArgumentList() == null && call.getArgumentCount() > 0)
    }

    override fun replaceCall(element: JetQualifiedExpression, call: JetCallExpression, receiver: JetExpression) {
        val argumentString = if (call.hasNamedArguments()) {
            try {
                call.getValueArgumentsUnnamed().iterator().map{ it?.getText() ?: ""}.makeString(", ")
            } catch (e : UnnamedArgumentsException) {
                throw ReplacementException("Could not convert named arguments into positional arguments: ${e.getMessage() ?: "Unspecified Reason"}")
            }
        } else {
            call.getArgumentsString()
        }
        element.replace("${receiver.getText()}[${argumentString}]")
    }

    // Do this to preserve whitespace, comments, etc.
    private val ARGUMENTS_TOKEN = TokenSet.orSet(JetTokens.WHITE_SPACE_OR_COMMENT_BIT_SET, TokenSet.create(JetTokens.COMMA, JetNodeTypes.VALUE_ARGUMENT))
    protected fun JetCallExpression.getArgumentsString(): String? {
        // This will loose comments/whitespace between dangling function arguments and the function call.
        val valueArguments = getValueArgumentList()?.getNode()?.getChildren(ARGUMENTS_TOKEN)?.iterator()?.map({ it.getText() })?.makeString("") ?: ""
        val functionLiteralArguments = getFunctionLiteralArguments().iterator().map { it.getText() }.filterNotNull().makeString(", ")

        return when {
            valueArguments == ""            -> functionLiteralArguments
            functionLiteralArguments == ""  -> valueArguments
            else                            -> "${valueArguments}, ${functionLiteralArguments}"
        }
    }

    protected fun JetCallExpression.getValueArgumentsUnnamed(): List<JetExpression?> {
        val declaredParameters = QuickFixUtil.getParameterListOfCallee(this)?.getParameters()
            ?: throw UnnamedArgumentsException("No matching function declaration found.")

        val valueArguments = try {
            this.getValueArguments().requireNoNulls()
        } catch (e : NullPointerException) {
            throw UnnamedArgumentsException("Malformed arguments.")
        }

        val functionArguments = this.getFunctionLiteralArguments()

        // Get the offset of the first named argument.
        val offset = valueArguments.withIndices().find { pair ->
            pair.second.isNamed()
        }?.first ?: valueArguments.size

        // Make a map of name -> argument
        val namedArgumentMap = HashMap<String, ValueArgument>()
        for (arg in valueArguments.listIterator(offset)) {
            val argName = arg.getArgumentName()?.getReferenceExpression()?.getReferencedName()
                ?: throw UnnamedArgumentsException("Positional arguments must not follow named arguments.")
            namedArgumentMap[argName] = arg
        }

        // Copy unnamed arguments into result
        val result = ArrayList<JetExpression?>(valueArguments.size)
        valueArguments.iterator().take(offset).mapTo(result) {
            it.getArgumentExpression()
        }

        // Copy named arguments into result
        for (i in offset..(declaredParameters.size - functionArguments.size)) {
            if (namedArgumentMap.empty) break

            val argumentName = declaredParameters[i].getName()
                ?: throw UnnamedArgumentsException("Function declaration malformed.")
            val argumentExpression = namedArgumentMap.remove(argumentName)
                ?: throw UnnamedArgumentsException("Missing arguments.")
            result.add(argumentExpression.getArgumentExpression())
        }

        // Make sure all named arguments copied
        if (!namedArgumentMap.empty) {
            throw UnnamedArgumentsException("Extra named arguments: ${namedArgumentMap.keySet().makeString(", ")}")
        }

        // Add function arguments.
        result.addAll(functionArguments)

        return result
    }
}
