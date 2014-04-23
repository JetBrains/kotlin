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

package org.jetbrains.jet.lang.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.jet.JetNodeTypes
import com.intellij.psi.JavaCodeFragment
import com.intellij.psi.PsiType
import com.intellij.psi.PsiClass

class JetExpressionCodeFragmentImpl(
        project: Project,
        name: String,
        text: CharSequence,
        context: PsiElement?
) : JetCodeFragmentImpl(project, name, text, JetNodeTypes.EXPRESSION_CODE_FRAGMENT, context), JetExpressionCodeFragment {

    private var _thisType: PsiType? = null
    private var _superType: PsiType? = null
    private var _exceptionHandler: JavaCodeFragment.ExceptionHandler? = null

    private val myImports: MutableSet<String> = hashSetOf()

    override fun getThisType() = _thisType

    override fun setThisType(psiType: PsiType?) {
        _thisType = psiType
    }

    override fun getSuperType() = _superType

    override fun setSuperType(superType: PsiType?) {
        _superType = superType
    }

    override fun importsToString(): String {
        return myImports.makeString(IMPORT_SEPARATOR)
    }

    override fun addImportsFromString(imports: String?) {
        if (imports == null) return

        myImports.addAll(imports.split(IMPORT_SEPARATOR))
    }

    override fun setVisibilityChecker(checker: JavaCodeFragment.VisibilityChecker?) {
    }

    override fun getVisibilityChecker() = JavaCodeFragment.VisibilityChecker.EVERYTHING_VISIBLE

    override fun setExceptionHandler(checker: JavaCodeFragment.ExceptionHandler?) {
        _exceptionHandler = checker
    }

    override fun getExceptionHandler() = _exceptionHandler

    override fun importClass(aClass: PsiClass?): Boolean {
        return true
    }

    override fun getExpression(): JetExpression? {
        var resultingExpression: JetExpression? = null
        this.accept(object: JetTreeVisitor<Void>() {
            override fun visitExpression(expression: JetExpression, data: Void?): Void? {
                resultingExpression = expression
                return null
            }

            override fun visitElement(element: PsiElement) = element.acceptChildren(this)

        }, null)
        return resultingExpression
    }

    class object {
        val IMPORT_SEPARATOR = ","
    }
}
