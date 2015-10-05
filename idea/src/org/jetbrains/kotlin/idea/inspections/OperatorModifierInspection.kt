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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticWithParameters2
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.quickfix.AddModifierFix
import org.jetbrains.kotlin.idea.quickfix.CleanupFix
import org.jetbrains.kotlin.idea.quickfix.JetSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.psi.JetModifierListOwner
import org.jetbrains.kotlin.psi.JetNamedFunction
import org.jetbrains.kotlin.psi.JetVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.OverrideResolver
import org.jetbrains.kotlin.resolve.dataClassUtils.isComponentLike
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.OperatorNameConventions

public class OperatorModifierInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : JetVisitorVoid() {
            override fun visitNamedFunction(function: JetNamedFunction) {
                val nameIdentifier = function.nameIdentifier
                if (nameIdentifier != null &&
                    function.isMemberOrExtension() &&
                    function.isOperator() &&
                    !function.isAnnotatedAsOperator()) {

                    holder.registerProblem(nameIdentifier, "Function defines an operator but isn't annotated as such",
                                           AddModifierLocalQuickFix())
                }
            }

        }
    }

    private fun JetNamedFunction.isOperator(): Boolean {
        val name = nameAsName ?: return false
        val arity = valueParameters.size()
        if (arity == 0 &&
            (name in OperatorConventions.UNARY_OPERATION_NAMES.values() ||
             name == OperatorNameConventions.ITERATOR ||
             isComponentLike(name) ||
             name == OperatorNameConventions.NEXT ||
             (name == OperatorNameConventions.HAS_NEXT && isBooleanReturnType()))) {
            return true
        }
        if (arity == 1 && (name in OperatorConventions.BINARY_OPERATION_NAMES.values() ||
                           name in OperatorConventions.ASSIGNMENT_OPERATIONS.values () ||
                           (name == OperatorNameConventions.CONTAINS && isBooleanReturnType()) ||
                           (name == OperatorNameConventions.COMPARE_TO && isIntReturnType()))) {
            return true
        }
        if (name == OperatorNameConventions.INVOKE) {
            return true
        }
        if (arity >= 1 && name == OperatorNameConventions.GET) {
            return true
        }
        if (arity >= 2 && name == OperatorNameConventions.SET) {
            return true
        }
        return false
    }

    private fun JetNamedFunction.isAnnotatedAsOperator(): Boolean {
        if (hasModifier(JetTokens.OPERATOR_KEYWORD)) return true
        val descriptor = descriptor as? CallableMemberDescriptor ?: return false
        return OverrideResolver.getOverriddenDeclarations(descriptor).any {
            (it as? FunctionDescriptor)?.isOperator == true
        }
    }

    private fun JetNamedFunction.isMemberOrExtension(): Boolean =
        receiverTypeReference != null || getStrictParentOfType<JetClassOrObject>() != null

    private fun JetNamedFunction.isIntReturnType(): Boolean {
        val returnType = (descriptor as? FunctionDescriptor)?.returnType ?: return false
        return KotlinBuiltIns.isInt(returnType)
    }

    private fun JetNamedFunction.isBooleanReturnType(): Boolean {
        val returnType = (descriptor as? FunctionDescriptor)?.returnType ?: return false
        return KotlinBuiltIns.isBoolean(returnType)
    }
}

private class AddModifierLocalQuickFix() : LocalQuickFix {
    override fun getName(): String = "Add 'operator' modifier"
    override fun getFamilyName(): String = getName()

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val modifierListOwner = descriptor.psiElement.getNonStrictParentOfType<JetModifierListOwner>()
        modifierListOwner?.addModifier(JetTokens.OPERATOR_KEYWORD)
    }
}

object OperatorModifierFixFactory : JetSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val functionDescriptor = (diagnostic as? DiagnosticWithParameters2<*, *, *>)?.a as? FunctionDescriptor ?: return null
        val target = DescriptorToSourceUtilsIde.getAnyDeclaration(diagnostic.psiFile.project, functionDescriptor)
                as? JetModifierListOwner ?: return null
        return object : AddModifierFix(target, JetTokens.OPERATOR_KEYWORD), CleanupFix {}
    }
}
