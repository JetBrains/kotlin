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

package org.jetbrains.kotlin.codegen.intrinsics

import com.intellij.psi.PsiElement
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lexer.JetTokens

import org.jetbrains.kotlin.codegen.AsmUtil.genEqualsForExpressionsOnStack
import org.jetbrains.jet.lang.resolve.java.AsmTypes.OBJECT_TYPE

public class Equals : LazyIntrinsicMethod() {
    override fun generateImpl(
            codegen: ExpressionCodegen,
            returnType: Type,
            element: PsiElement?,
            arguments: List<JetExpression>,
            receiver: StackValue
    ): StackValue {
        val leftExpr: StackValue
        val rightExpr: JetExpression
        if (element is JetCallExpression) {
            leftExpr = StackValue.coercion(receiver, OBJECT_TYPE)
            rightExpr = arguments.get(0)
        }
        else {
            leftExpr = codegen.genLazy(arguments.get(0), OBJECT_TYPE)
            rightExpr = arguments.get(1)
        }

        return genEqualsForExpressionsOnStack(JetTokens.EQEQ, leftExpr, codegen.genLazy(rightExpr, OBJECT_TYPE))
    }
}
