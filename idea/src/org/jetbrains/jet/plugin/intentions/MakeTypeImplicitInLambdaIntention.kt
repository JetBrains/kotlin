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
import org.jetbrains.jet.lang.psi.JetFunctionLiteralExpression
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.types.ErrorUtils
import org.jetbrains.jet.lang.psi.JetPsiFactory

public class MakeTypeImplicitInLambdaIntention : JetSelfTargetingIntention<JetFunctionLiteralExpression>(
        "make.type.implicit.in.lambda", javaClass()) {
    override fun isApplicableTo(element: JetFunctionLiteralExpression): Boolean {
        val params = element.getValueParameters()
        val hasDeclaredParamsType = params.any { it.getTypeReference() != null }
        val hasDeclaredReturnType = element.hasDeclaredReturnType()
        val hasDeclaredReceiverType = element.getFunctionLiteral().getReceiverTypeRef() != null
        if (hasDeclaredParamsType || hasDeclaredReturnType || hasDeclaredReceiverType) return true

        return false
    }

    override fun applyTo(element: JetFunctionLiteralExpression, editor: Editor) {
        val paramList = element.getFunctionLiteral().getValueParameterList()
        val params = paramList?.getParameters()

        if (element.hasDeclaredReturnType()) {
            val childAfterParamList = paramList?.getNextSibling()
            val arrow = element.getFunctionLiteral().getArrowNode()?.getPsi()
            val childBeforeArrow = arrow?.getPrevSibling()
            element.getFunctionLiteral().deleteChildRange(childAfterParamList, childBeforeArrow)
            val whiteSpaceBeforeArrow = JetPsiFactory.createWhiteSpace(element.getProject())
            element.getFunctionLiteral().addBefore(whiteSpaceBeforeArrow, arrow)
        }

        val hasDeclaredReceiverType = element.getFunctionLiteral().getReceiverTypeRef() != null
        if (hasDeclaredReceiverType) {
            val childAfterBrace = element.getFunctionLiteral().getOpenBraceNode().getPsi()?.getNextSibling()
            val childBeforeParamList = paramList?.getPrevSibling()
            element.getFunctionLiteral().deleteChildRange(childAfterBrace, childBeforeParamList)
        }

        if (params != null && params!!.size() == 1 && params[0].getNameIdentifier() != null) {
            paramList!!.replace(params[0].getNameIdentifier()!!)
        }
        else {
            params?.forEach{
                if (it.getNameIdentifier() != null) {
                    it.replace(it.getNameIdentifier()!!)
                }
            }
        }
    }
}