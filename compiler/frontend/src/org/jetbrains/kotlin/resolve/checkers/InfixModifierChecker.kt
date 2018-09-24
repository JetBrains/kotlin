/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.util.CheckResult
import org.jetbrains.kotlin.util.InfixChecks

class InfixModifierChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val functionDescriptor = descriptor as? FunctionDescriptor ?: return
        if (!functionDescriptor.isInfix) return
        val modifier = declaration.modifierList?.getModifier(KtTokens.INFIX_KEYWORD) ?: return

        val checkResult = InfixChecks.check(functionDescriptor)
        if (checkResult !is CheckResult.IllegalSignature) return

        context.trace.report(Errors.INAPPLICABLE_INFIX_MODIFIER.on(modifier, checkResult.error))
    }
}
