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
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtTypeParameterListOwner

object UnderscoreChecker : DeclarationChecker {

    fun checkIdentifier(identifier: PsiElement?, diagnosticHolder: DiagnosticSink) {
        if (identifier == null || identifier.text.isEmpty()) return
        if (identifier.text.all { it == '_' }) {
            diagnosticHolder.report(Errors.UNDERSCORE_IS_RESERVED.on(identifier))
        }
    }

    fun checkNamed(declaration: KtNamedDeclaration, diagnosticHolder: DiagnosticSink) {
        checkIdentifier(declaration.nameIdentifier, diagnosticHolder)
    }

    override fun check(
            declaration: KtDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
        if (declaration is KtCallableDeclaration) {
            for (parameter in declaration.valueParameters) {
                checkNamed(parameter, diagnosticHolder)
            }
        }
        if (declaration is KtTypeParameterListOwner) {
            for (typeParameter in declaration.typeParameters) {
                checkNamed(typeParameter, diagnosticHolder)
            }
        }
        if (declaration !is KtNamedDeclaration) return
        checkNamed(declaration, diagnosticHolder)
    }
}
