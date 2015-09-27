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
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.psi.JetProperty

public object ConstModifierChecker : DeclarationChecker {
    override fun check(
            declaration: JetDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
        if (descriptor !is VariableDescriptor || !declaration.hasModifier(JetTokens.CONST_KEYWORD)) return

        val constModifierPsiElement = declaration.modifierList!!.getModifier(JetTokens.CONST_KEYWORD)!!

        val diagnostic = checkCanBeConst(declaration, constModifierPsiElement, descriptor)
        if (diagnostic != null) {
            diagnosticHolder.report(diagnostic)
        }
    }

    fun checkCanBeConst(declaration: JetDeclaration,
                        constModifierPsiElement: PsiElement,
                        descriptor: VariableDescriptor): Diagnostic? {
        if (descriptor.isVar) {
            return Errors.WRONG_MODIFIER_TARGET.on(constModifierPsiElement, JetTokens.CONST_KEYWORD, "vars")
        }

        val containingDeclaration = descriptor.containingDeclaration
        if (containingDeclaration is ClassDescriptor && containingDeclaration.kind != ClassKind.OBJECT) {
            return Errors.CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT.on(constModifierPsiElement)
        }

        if (declaration !is JetProperty || descriptor !is PropertyDescriptor) return null

        if (declaration.hasDelegate()) {
            return Errors.CONST_VAL_WITH_DELEGATE.on(declaration.delegate!!)
        }

        if (descriptor is PropertyDescriptor && !descriptor.getter!!.isDefault) {
            return Errors.CONST_VAL_WITH_GETTER.on(declaration.getter!!)
        }

        if (!descriptor.type.canBeUsedForConstVal()) {
            return Errors.TYPE_CANT_BE_USED_FOR_CONST_VAL.on(constModifierPsiElement, descriptor.type)
        }

        if (declaration.initializer == null) {
            return Errors.CONST_VAL_WITHOUT_INITIALIZER.on(constModifierPsiElement)
        }

        if (descriptor.compileTimeInitializer == null) {
            return Errors.CONST_VAL_WITH_NON_CONST_INITIALIZER.on(declaration.initializer!!)
        }

        return null
    }
}
