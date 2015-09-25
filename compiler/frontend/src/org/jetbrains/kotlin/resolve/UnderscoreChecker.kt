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

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.JetCallableDeclaration
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.psi.JetNamedDeclaration
import org.jetbrains.kotlin.psi.JetTypeParameterListOwner

object UnderscoreChecker : DeclarationChecker {

    fun checkIdentifier(identifier: PsiElement?, diagnosticHolder: DiagnosticSink) {
        if (identifier == null || identifier.text.isEmpty()) return
        if (identifier.text.all { it == '_' }) {
            diagnosticHolder.report(Errors.UNDESCORE_IS_DEPRECATED.on(identifier))
        }
    }

    fun checkNamed(declaration: JetNamedDeclaration, diagnosticHolder: DiagnosticSink) {
        checkIdentifier(declaration.nameIdentifier, diagnosticHolder)
    }

    override fun check(
            declaration: JetDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
        if (declaration is JetCallableDeclaration) {
            for (parameter in declaration.valueParameters) {
                checkNamed(parameter, diagnosticHolder)
            }
        }
        if (declaration is JetTypeParameterListOwner) {
            for (typeParameter in declaration.typeParameters) {
                checkNamed(typeParameter, diagnosticHolder)
            }
        }
        if (declaration !is JetNamedDeclaration) return
        checkNamed(declaration, diagnosticHolder)
    }
}
