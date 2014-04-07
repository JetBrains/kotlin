/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.inspections

import com.intellij.codeInsight.daemon.GroupNames
import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.jet.plugin.JetBundle
import org.jetbrains.jet.lang.psi.JetTreeVisitorVoid
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.psi.JetClass
import org.jetbrains.jet.lang.psi.JetSuperExpression
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.psi.JetElement

public class UnnecessaryTypeParameterInSuperInspection() : BaseJavaLocalInspectionTool() {
    private val myFix = RemoveUnnecessaryTypeParameterFromSuperFix()

    fun checkExpressionForProblems(holder: ProblemsHolder, superExpression: JetSuperExpression) {
        val typeReference = superExpression.getSuperTypeQualifier()

        if (typeReference == null) {
            return
        }

        val classDescriptor = getReferredClass(AnalyzerFacadeWithCache.getContextForElement(superExpression), superExpression)
        val numSupertypes = classDescriptor?.getTypeConstructor()?.getSupertypes()?.size()
        if (numSupertypes != 1) {
            return
        }

        val message = JetBundle.message("unnecessary.type.parameter.in.super.errorTemplate", typeReference.getText())
        holder.registerProblem(superExpression, message, myFix)
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val project = holder.getProject()
        return object : JetTreeVisitorVoid() {

            override fun visitJetElement(element: JetElement) {
            }

            override fun visitSuperExpression(expression: JetSuperExpression) {
                checkExpressionForProblems(holder, expression)
                super<JetTreeVisitorVoid>.visitSuperExpression(expression)
            }
        }
    }

    override fun getGroupDisplayName(): String {
        return GroupNames.DECLARATION_REDUNDANCY
    }

    override fun getDisplayName(): String {
        return JetBundle.message("unnecessary.type.parameter.in.super.displayName")
    }

    override fun getShortName(): String {
        return "UnnecessaryTypeParameterInSuper"
    }

    override fun isEnabledByDefault(): Boolean {
        return true
    }

    private fun getReferredClass(bindingContext: BindingContext, superExpr: JetSuperExpression): ClassDescriptor? {
        if (superExpr.getLabelName() == null) {
            val jetClass = PsiTreeUtil.getParentOfType(superExpr, javaClass<JetClass>())
            return bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, jetClass) as ClassDescriptor?
        }
        else {
            val jetClass = bindingContext.get(BindingContext.LABEL_TARGET, superExpr.getTargetLabel())
            return bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, jetClass) as ClassDescriptor?
        }
    }

    private inner class RemoveUnnecessaryTypeParameterFromSuperFix() : LocalQuickFix {
        override fun getName(): String {
            return JetBundle.message("unnecessary.type.parameter.in.super.quickfix")
        }

        override fun getFamilyName(): String {
            return getName()
        }

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val superExpression = descriptor.getPsiElement() as JetSuperExpression
            superExpression.deleteChildRange(superExpression.getLeftAngleBracketNode(), superExpression.getRightAngleBracketNode())
        }
    }

    public class InspectionProvider() : InspectionToolProvider {
        override fun getInspectionClasses(): Array<Class<*>> {
            return array(javaClass<UnnecessaryTypeParameterInSuperInspection>())
        }
    }
}
