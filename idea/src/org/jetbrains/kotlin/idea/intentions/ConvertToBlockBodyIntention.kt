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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.JetType

public class ConvertToBlockBodyIntention : JetSelfTargetingIntention<JetDeclarationWithBody>(
        javaClass(), "Convert to block body", firstElementOfTypeOnly = true
) {
    override fun isApplicableTo(element: JetDeclarationWithBody, caretOffset: Int): Boolean {
        if (element is JetFunctionLiteral || element.hasBlockBody() || !element.hasBody()) return false

        when (element) {
            is JetNamedFunction -> {
                val returnType = element.returnType() ?: return false
                if (!element.hasDeclaredReturnType() && returnType.isError()) return false// do not convert when type is implicit and unknown
                return true
            }

            is JetPropertyAccessor -> return true

            else -> error("Unknown declaration type: $element")
        }
    }

    override fun applyTo(element: JetDeclarationWithBody, editor: Editor) {
        convert(element)
    }

    companion object {
        public fun convert(declaration: JetDeclarationWithBody): JetDeclarationWithBody {
            val body = declaration.getBodyExpression()!!

            fun generateBody(returnsValue: Boolean): JetExpression {
                val bodyType = body.analyze().getType(body)
                val needReturn = returnsValue &&
                                 (bodyType == null || (!KotlinBuiltIns.isUnit(bodyType) && !KotlinBuiltIns.isNothing(bodyType)))

                val factory = JetPsiFactory(declaration)
                val statement = if (needReturn) factory.createExpressionByPattern("return $0", body) else body
                return factory.createSingleStatementBlock(statement)
            }

            val newBody = when (declaration) {
                is JetNamedFunction -> {
                    val returnType = declaration.returnType()!!
                    if (!declaration.hasDeclaredReturnType() && !KotlinBuiltIns.isUnit(returnType)) {
                        declaration.setType(returnType)
                    }
                    generateBody(!KotlinBuiltIns.isUnit(returnType) && !KotlinBuiltIns.isNothing(returnType))
                }

                is JetPropertyAccessor -> generateBody(declaration.isGetter())

                else -> throw RuntimeException("Unknown declaration type: $declaration")
            }

            declaration.getEqualsToken()!!.delete()
            body.replace(newBody)
            return declaration
        }

        private fun JetNamedFunction.returnType(): JetType? {
            val descriptor = analyze()[BindingContext.DECLARATION_TO_DESCRIPTOR, this] ?: return null
            return (descriptor as FunctionDescriptor).getReturnType()
        }
    }
}
