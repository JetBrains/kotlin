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

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.isError

object ConstModifierChecker : SimpleDeclarationChecker {
    override fun check(
            declaration: KtDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
        if (descriptor !is VariableDescriptor || !declaration.hasModifier(KtTokens.CONST_KEYWORD)) return

        val constModifierPsiElement = declaration.modifierList!!.getModifier(KtTokens.CONST_KEYWORD)!!

        val diagnostic = checkCanBeConst(declaration, constModifierPsiElement, descriptor).diagnostic
        if (diagnostic != null) {
            diagnosticHolder.report(diagnostic)
        }
    }

    fun canBeConst(declaration: KtDeclaration, constModifierPsiElement: PsiElement, descriptor: VariableDescriptor): Boolean =
            checkCanBeConst(declaration, constModifierPsiElement, descriptor).canBeConst

    private fun checkCanBeConst(declaration: KtDeclaration,
                        constModifierPsiElement: PsiElement,
                        descriptor: VariableDescriptor): ConstApplicability {
        if (descriptor.isVar) {
            return Errors.WRONG_MODIFIER_TARGET.on(constModifierPsiElement, KtTokens.CONST_KEYWORD, "vars").nonApplicable()
        }

        val containingDeclaration = descriptor.containingDeclaration
        if (containingDeclaration is ClassDescriptor && containingDeclaration.kind != ClassKind.OBJECT) {
            return Errors.CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT.on(constModifierPsiElement).nonApplicable()
        }

        if (declaration !is KtProperty || descriptor !is PropertyDescriptor) return ConstApplicability.NonApplicable()

        if (declaration.hasDelegate()) {
            return Errors.CONST_VAL_WITH_DELEGATE.on(declaration.delegate!!).nonApplicable()
        }

        if (descriptor is PropertyDescriptor && !descriptor.getter!!.isDefault) {
            return Errors.CONST_VAL_WITH_GETTER.on(declaration.getter!!).nonApplicable()
        }

        if (descriptor.type.isError) return ConstApplicability.NonApplicable()

        // Report errors about const initializer only on property of resolved types
        if (!descriptor.type.canBeUsedForConstVal()) {
            return Errors.TYPE_CANT_BE_USED_FOR_CONST_VAL.on(constModifierPsiElement, descriptor.type).nonApplicable()
        }

        if (declaration.initializer == null) {
            return Errors.CONST_VAL_WITHOUT_INITIALIZER.on(constModifierPsiElement).nonApplicable()
        }

        if (descriptor.compileTimeInitializer == null) {
            return Errors.CONST_VAL_WITH_NON_CONST_INITIALIZER.on(declaration.initializer!!).nonApplicable()
        }

        return ConstApplicability.Applicable
    }
}

sealed class ConstApplicability(val canBeConst: Boolean, val diagnostic: Diagnostic?) {
    object Applicable : ConstApplicability(true, null)
    class NonApplicable(diagnostic: Diagnostic? = null) : ConstApplicability(false, diagnostic)
}

private fun Diagnostic.nonApplicable() = ConstApplicability.NonApplicable(this)
