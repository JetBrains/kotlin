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

package org.jetbrains.kotlin.idea.debugger.stepping

import com.intellij.debugger.engine.SimplePropertyGetterProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.*

class KotlinSimpleGetterProvider : SimplePropertyGetterProvider {
    override fun isInsideSimpleGetter(element: PsiElement): Boolean {
        // class A(val a: Int)
        if (element is KtParameter) {
            return true
        }

        val accessor = PsiTreeUtil.getParentOfType(element, KtPropertyAccessor::class.java)
        if (accessor != null && accessor.isGetter) {
            val body = accessor.bodyExpression
            return when (body) {
                // val a: Int get() { return field }
                is KtBlockExpression -> {
                    val returnedExpression = (body.statements.singleOrNull() as? KtReturnExpression)?.returnedExpression ?: return false
                    returnedExpression.textMatches("field")
                }
                // val a: Int get() = field
                is KtExpression -> body.textMatches("field")
                else -> false
            }
        }

        val property = PsiTreeUtil.getParentOfType(element, KtProperty::class.java)
        // val a = foo()
        if (property != null) {
            return property.getter == null && !property.isLocal
        }

        return false
    }
}
