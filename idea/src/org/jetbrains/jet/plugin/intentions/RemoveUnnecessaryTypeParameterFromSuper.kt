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
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.plugin.JetBundle
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.descriptors.ClassDescriptor

public class RemoveUnnecessaryTypeParameterFromSuperIntention() : JetSelfTargetingIntention<JetTypeReference>("unnecessary.type.parameter.in.super", javaClass()) {
    override fun isApplicableTo(typeReference: JetTypeReference): Boolean {
        val superExpression = PsiTreeUtil.getParentOfType(typeReference, javaClass<JetSuperExpression>())

        if (superExpression == null) {
            return false
        }

        val classDescriptor = getReferredClass(AnalyzerFacadeWithCache.getContextForElement(superExpression), superExpression)
        val numSupertypes = classDescriptor?.getTypeConstructor()?.getSupertypes()?.size()
        if (numSupertypes != 1) {
            return false
        }

        setText(JetBundle.message(key, typeReference.getText()))
        return true
    }

    override fun applyTo(element: JetTypeReference, editor: Editor) {
        val superExpression = PsiTreeUtil.getParentOfType(element, javaClass<JetSuperExpression>())
        superExpression!!.deleteChildRange(superExpression.getLeftAngleBracketNode(), superExpression.getRightAngleBracketNode())
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
}
