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

package org.jetbrains.jet.plugin.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.plugin.JetBundle
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache

// Maps what kind of block expression an label-qualified expression refers to. E.g. 'break' is always associated with a loop block
public class LabeledExpressionAssociatedBlockMapping(val labeledExpressionType: Class<out JetLabelQualifiedExpression?>,
                                                     val associatedBlockTypes: Array<Class<out PsiElement?>?>) {
    public class object {
        val MAPPINGS = array(
                LabeledExpressionAssociatedBlockMapping(javaClass<JetBreakExpression>(), array(javaClass<JetLoopExpression>())),
                LabeledExpressionAssociatedBlockMapping(javaClass<JetContinueExpression>(), array(javaClass<JetLoopExpression>())),
                LabeledExpressionAssociatedBlockMapping(javaClass<JetThisExpression>(),
                                                        array(javaClass<JetClass>(), javaClass<JetObjectLiteralExpression>())),
                LabeledExpressionAssociatedBlockMapping(javaClass<JetSuperExpression>(),
                                                        array(javaClass<JetClass>(), javaClass<JetObjectLiteralExpression>()))
                // Non-local return not implemented, can't test the following:
                // LabeledExpression(javaClass<JetReturnExpression>(), array(javaClass<JetNamedFunction>()))
        )
    }
}

public class RemoveUnnecessaryLabelIntention() : JetSelfTargetingIntention<JetElement>("unnecessary.label", javaClass()) {

    override fun applyTo(element: JetElement, editor: Editor) {
        for (mapping in LabeledExpressionAssociatedBlockMapping.MAPPINGS) {
            val labeledExpression = PsiTreeUtil.getNonStrictParentOfType(element, mapping.labeledExpressionType)
            if (labeledExpression != null) {
                labeledExpression.getTargetLabel()!!.delete()
                return
            }
        }
        assert(false, "isAvailable should have returned false")
    }

    override fun isApplicableTo(element: JetElement): Boolean {
        if (!JetPsiUtil.isLabelIdentifierExpression(element)) {
            return false
        }
        for (mapping in LabeledExpressionAssociatedBlockMapping.MAPPINGS) {
            val labeledExpression = PsiTreeUtil.getNonStrictParentOfType(element, mapping.labeledExpressionType)
            if (labeledExpression == null || labeledExpression.getLabelName() == null) {
                continue
            }

            val bindingContext = AnalyzerFacadeWithCache.getContextForElement(labeledExpression)
            val labelTargetBlock = bindingContext[BindingContext.LABEL_TARGET, labeledExpression.getTargetLabel()]

            val associatedBlock = PsiTreeUtil.getParentOfType(labeledExpression, *mapping.associatedBlockTypes)
            if (labelTargetBlock != associatedBlock) {
                return false
            }

            setText(JetBundle.message(key, labeledExpression.getTargetLabel()!!.getText()));
            return true
        }
        return false
    }
}
