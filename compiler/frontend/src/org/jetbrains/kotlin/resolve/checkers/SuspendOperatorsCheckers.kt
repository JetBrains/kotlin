/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.util.OperatorNameConventions

object SuspendOperatorsCheckers : SimpleDeclarationChecker {
    private val UNSUPPORTED_OPERATOR_NAMES = setOf(
            OperatorNameConventions.CONTAINS,
            OperatorNameConventions.GET, OperatorNameConventions.SET
    )

    override fun check(
            declaration: KtDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
        if (descriptor is FunctionDescriptor && descriptor.isSuspend && descriptor.isOperator &&
            descriptor.name in UNSUPPORTED_OPERATOR_NAMES) {
            declaration.modifierList?.getModifier(KtTokens.OPERATOR_KEYWORD)?.let {
                diagnosticHolder.report(Errors.UNSUPPORTED.on(it, "suspend operator \"${descriptor.name}\""))
            }
        }
    }
}
