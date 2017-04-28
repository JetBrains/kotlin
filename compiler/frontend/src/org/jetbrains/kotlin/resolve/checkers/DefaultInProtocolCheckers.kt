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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.hasBody
import org.jetbrains.kotlin.resolve.BindingContext

object DefaultInProtocolCheckers : SimpleDeclarationChecker {

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, diagnosticHolder: DiagnosticSink, bindingContext: BindingContext) {
        val containingDeclaration = descriptor.containingDeclaration
        if (containingDeclaration !is ClassDescriptor || !containingDeclaration.isProtocol) {
            return
        }

        if (descriptor !is FunctionDescriptor  || declaration !is KtNamedFunction)  {
            return
        }

        if (declaration.hasBody()) {
            diagnosticHolder.report(Errors.UNSUPPORTED.on(declaration.bodyExpression!! , "Unsupported default methods in protocol interfaces"))
        }

        declaration.valueParameters.filter { it.hasDefaultValue() }.forEach {
            diagnosticHolder.report(Errors.UNSUPPORTED.on(it.defaultValue!!, "Unsupported default arguments in protocol interfaces"))
        }
    }

}
