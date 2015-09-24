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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.types.expressions.OperatorConventions.*

public class OperatorModifierChecker : DeclarationChecker {

    private companion object {
        private val GET = Name.identifier("get")
        private val SET = Name.identifier("set")

        private val COMPONENT_REGEX = "component\\d+".toRegex()
    }

    override fun check(
            declaration: JetDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
        val functionDescriptor = descriptor as? FunctionDescriptor ?: return
        if (!functionDescriptor.isOperator) return
        val modifier = declaration.modifierList?.getModifier(JetTokens.OPERATOR_KEYWORD) ?: return

        val name = functionDescriptor.name

        when {
            GET == name -> {}
            SET == name -> {}
            INVOKE == name -> {}
            CONTAINS == name -> {}
            ITERATOR == name -> {}
            NEXT == name -> {}
            HAS_NEXT == name -> {}
            EQUALS == name -> {}
            COMPARE_TO == name -> {}
            UNARY_OPERATION_NAMES.any { it.value == name } && functionDescriptor.valueParameters.isEmpty() -> {}
            BINARY_OPERATION_NAMES.any { it.value == name } && functionDescriptor.valueParameters.size() == 1 -> {}
            ASSIGNMENT_OPERATIONS.any { it.value == name } -> {}
            name.asString().matches(COMPONENT_REGEX) -> {}
            else -> diagnosticHolder.report(Errors.INAPPLICABLE_OPERATOR_MODIFIER.on(modifier))
        }
    }
}