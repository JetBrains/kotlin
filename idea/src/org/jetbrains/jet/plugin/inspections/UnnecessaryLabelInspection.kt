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
import org.jetbrains.jet.lang.psi.JetLabelQualifiedExpression
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.resolve.BindingContext
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.psi.JetBreakExpression
import org.jetbrains.jet.lang.psi.JetLoopExpression
import org.jetbrains.jet.lang.psi.JetContinueExpression
import org.jetbrains.jet.lang.psi.JetThisExpression
import org.jetbrains.jet.lang.psi.JetClass
import org.jetbrains.jet.lang.psi.JetObjectLiteralExpression
import org.jetbrains.jet.lang.psi.JetSuperExpression
import org.jetbrains.jet.lang.psi.JetElement

// Maps what kind of block expression an label-qualified expression refers to. E.g. 'break' is always associated with a loop block
public class LabeledExpressionAssociatedBlockMapping(val labeledExpressionType: Class<out JetLabelQualifiedExpression?>,
                                                     val associatedBlockType: Array<Class<out PsiElement?>?>) {
    public class object {
        val MAPPINGS = array(
                LabeledExpressionAssociatedBlockMapping(javaClass<JetBreakExpression>(), array(javaClass<JetLoopExpression>())),
                LabeledExpressionAssociatedBlockMapping(javaClass<JetContinueExpression>(), array(javaClass<JetLoopExpression>())),
                LabeledExpressionAssociatedBlockMapping(javaClass<JetThisExpression>(),
                                                        array(javaClass<JetClass>(), javaClass<JetObjectLiteralExpression>())),
                LabeledExpressionAssociatedBlockMapping(javaClass<JetSuperExpression>(),
                                                        array(javaClass<JetClass>(), javaClass<JetObjectLiteralExpression>()))
                // Non-local return not implemented, can't test the following:
                // LabeledExpressionAssociatedBlockMapping(javaClass<JetReturnExpression>(), array(javaClass<JetNamedFunction>()))
        )
    }
}

public class UnnecessaryLabelInspection() : BaseJavaLocalInspectionTool() {
    private val myFix = RemoveUnnecessaryLabelFix()

    fun checkExpressionForProblems(holder: ProblemsHolder, expression: JetLabelQualifiedExpression) {
        if (expression.getLabelName() == null) {
            return
        }

        for (mapping in LabeledExpressionAssociatedBlockMapping.MAPPINGS) {
            if (!mapping.labeledExpressionType.isInstance(expression)) {
                continue
            }

            val bindingContext = AnalyzerFacadeWithCache.getContextForElement(expression)
            val labelTargetBlock = bindingContext.get(BindingContext.LABEL_TARGET, expression.getTargetLabel())

            val associatedBlock = PsiTreeUtil.getParentOfType(expression, *mapping.associatedBlockType)
            if (labelTargetBlock != associatedBlock) {
                return
            }

            val message = JetBundle.message("unnecessary.label.errorTemplate", expression.getTargetLabel()!!.getText())
            holder.registerProblem(expression, message, myFix)
        }
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val project = holder.getProject()
        return object : JetTreeVisitorVoid() {

            override fun visitJetElement(element: JetElement) {
            }

            override fun visitLabelQualifiedExpression(expression: JetLabelQualifiedExpression) {
                checkExpressionForProblems(holder, expression)
                super<JetTreeVisitorVoid>.visitLabelQualifiedExpression(expression)
            }
        }
    }

    override fun getGroupDisplayName(): String {
        return GroupNames.DECLARATION_REDUNDANCY
    }

    override fun getDisplayName(): String {
        return JetBundle.message("unnecessary.label.displayName")
    }

    override fun getShortName(): String {
        return "UnnecessaryLabel"
    }

    override fun isEnabledByDefault(): Boolean {
        return true
    }

    private inner class RemoveUnnecessaryLabelFix() : LocalQuickFix {
        override fun getName(): String {
            return JetBundle.message("unnecessary.label.quickfix")
        }

        override fun getFamilyName(): String {
            return getName()
        }

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val labeledExpression = descriptor.getPsiElement() as JetLabelQualifiedExpression
            labeledExpression.getTargetLabel()!!.delete()
        }
    }

    public class InspectionProvider() : InspectionToolProvider {
        override fun getInspectionClasses(): Array<Class<*>> {
            return array(javaClass<UnnecessaryLabelInspection>())
        }
    }
}
