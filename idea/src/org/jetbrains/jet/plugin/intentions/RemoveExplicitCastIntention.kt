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

import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.psi.JetBinaryExpressionWithTypeRHS
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.resolve.BindingContextUtils
import com.intellij.psi.util.PsiTreeUtil

public class RemoveExplicitCastIntention : JetSelfTargetingIntention<JetSimpleNameExpression>("remove.explicit.cast", javaClass()) {

    override fun isApplicableTo(element: JetSimpleNameExpression): Boolean {
        val bindingContext = AnalyzerFacadeWithCache.getContextForElement(element)
        if (bindingContext[BindingContext.AUTOCAST, element] == null) return false
        val context = bindingContext[BindingContext.AUTOCAST, element]

        if (PsiTreeUtil.getParentOfType(element, javaClass<JetBinaryExpressionWithTypeRHS>()) == null) return false
        val parent = PsiTreeUtil.getParentOfType(element, javaClass<JetBinaryExpressionWithTypeRHS>())

        if (!PsiTreeUtil.isAncestor(parent, element, false)) return false

        if (parent!!.getOperationReference().getReferencedNameElementType() != JetTokens.AS_KEYWORD) return false

        if (parent.getRight()?.getText() != context.toString()) return false

        return true
    }

    override fun applyTo(element: JetSimpleNameExpression, editor: Editor) {
        val parent = PsiTreeUtil.getParentOfType(element, javaClass<JetBinaryExpressionWithTypeRHS>())
        parent!!.replace(parent.getLeft())
    }
}

