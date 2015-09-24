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

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.psi.JetModifierListOwner
import org.jetbrains.kotlin.psi.JetNamedFunction
import org.jetbrains.kotlin.psi.JetVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.dataClassUtils.isComponentLike
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.expressions.OperatorConventions

public class OperatorModifierInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : JetVisitorVoid() {
            override fun visitNamedFunction(function: JetNamedFunction) {
                val nameIdentifier = function.nameIdentifier
                if (nameIdentifier != null &&
                    function.isMemberOrExtension() &&
                    function.isOperator() &&
                    !function.hasModifier(JetTokens.OPERATOR_KEYWORD)) {

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
             name == OperatorConventions.ITERATOR ||
             isComponentLike(name) ||
             name == OperatorConventions.NEXT ||
             (name == OperatorConventions.HAS_NEXT && isBooleanReturnType()))) {
            return true
        }
        if (arity == 1 && (name in OperatorConventions.BINARY_OPERATION_NAMES.values() ||
                           name in OperatorConventions.ASSIGNMENT_OPERATIONS.values () ||
                           (name == OperatorConventions.CONTAINS && isBooleanReturnType()) ||
                           (name == OperatorConventions.COMPARE_TO && isIntReturnType()))) {
            return true
        }
        if (name == OperatorConventions.INVOKE) {
            return true
        }
        if (arity >= 1 && name == OperatorConventions.GET) {
            return true
        }
        if (arity >= 2 && name == OperatorConventions.SET) {
            return true
        }
        return false
    }

    private fun JetNamedFunction.isMemberOrExtension(): Boolean =
        receiverTypeReference != null || getStrictParentOfType<JetClassOrObject>() != null

    private fun JetNamedFunction.isIntReturnType(): Boolean {
        return KotlinBuiltIns.isInt(analyze(BodyResolveMode.PARTIAL).getType(this) ?: return false)
    }

    private fun JetNamedFunction.isBooleanReturnType(): Boolean {
        return KotlinBuiltIns.isBoolean(analyze(BodyResolveMode.PARTIAL).getType(this) ?: return false)
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
