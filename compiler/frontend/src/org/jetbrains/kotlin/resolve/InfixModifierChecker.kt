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
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue

public class InfixModifierChecker : DeclarationChecker {

    override fun check(
            declaration: JetDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
        val functionDescriptor = descriptor as? FunctionDescriptor ?: return
        if (!functionDescriptor.isInfix) return
        val modifier = declaration.modifierList?.getModifier(JetTokens.INFIX_KEYWORD) ?: return

        if (!isApplicable(functionDescriptor)) {
            diagnosticHolder.report(Errors.INAPPLICABLE_INFIX_MODIFIER.on(modifier))
        }
    }

    private fun isApplicable(descriptor: FunctionDescriptor): Boolean {
        if (descriptor.dispatchReceiverParameter == null && descriptor.extensionReceiverParameter == null) return false
        if (descriptor.valueParameters.size != 1) return false

        val singleParameter = descriptor.valueParameters.first()
        return !singleParameter.hasDefaultValue() && singleParameter.varargElementType == null
    }

}