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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*

public object ConstModifierChecker : DeclarationChecker {
    override fun check(
            declaration: JetDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
        if (descriptor !is VariableDescriptor || !declaration.hasModifier(JetTokens.CONST_KEYWORD)) return

        val containingDeclaration = descriptor.containingDeclaration
        val constModifierPsiElement = declaration.modifierList!!.getModifier(JetTokens.CONST_KEYWORD)!!

        if (descriptor.isVar) {
            diagnosticHolder.report(Errors.WRONG_MODIFIER_TARGET.on(constModifierPsiElement, JetTokens.CONST_KEYWORD, "vars"))
            return
        }

        if (containingDeclaration is ClassDescriptor && containingDeclaration.kind != ClassKind.OBJECT) {
            diagnosticHolder.report(Errors.CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT.on(constModifierPsiElement))
            return
        }

        if (declaration !is JetProperty || descriptor !is PropertyDescriptor) return

        if (declaration.hasDelegate()) {
            diagnosticHolder.report(Errors.CONST_VAL_WITH_DELEGATE.on(declaration.delegate!!))
            return
        }

        if (descriptor is PropertyDescriptor && !descriptor.getter!!.isDefault) {
            diagnosticHolder.report(Errors.CONST_VAL_WITH_GETTER.on(declaration.getter!!))
            return
        }

        if (!descriptor.type.canBeUsedForConstVal()) {
            diagnosticHolder.report(Errors.TYPE_CANT_BE_USED_FOR_CONST_VAL.on(constModifierPsiElement, descriptor.type))
            return
        }

        if (declaration.initializer == null) {
            diagnosticHolder.report(Errors.CONST_VAL_WITHOUT_INITIALIZER.on(constModifierPsiElement))
            return
        }

        if (descriptor.compileTimeInitializer == null) {
            diagnosticHolder.report(Errors.CONST_VAL_WITH_NON_CONST_INITIALIZER.on(declaration.initializer!!))
            return
        }
    }
}
