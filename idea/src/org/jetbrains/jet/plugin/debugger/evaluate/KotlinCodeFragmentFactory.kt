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

package org.jetbrains.jet.plugin.debugger.evaluate

import com.intellij.debugger.engine.evaluation.CodeFragmentFactory
import com.intellij.debugger.engine.evaluation.TextWithImports
import com.intellij.psi.PsiElement
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaCodeFragment
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.debugger.engine.evaluation.expression.EvaluatorBuilder
import org.jetbrains.jet.plugin.JetFileType
import org.jetbrains.jet.lang.psi.JetCodeFragmentImpl
import com.intellij.psi.tree.IElementType
import org.jetbrains.jet.lexer.JetToken
import org.jetbrains.jet.JetNodeType
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.psi.JetExpressionCodeFragmentImpl

class KotlinCodeFragmentFactory: CodeFragmentFactory() {
    override fun createCodeFragment(item: TextWithImports, context: PsiElement?, project: Project): JavaCodeFragment {
        return JetExpressionCodeFragmentImpl(project, "fragment.kt", item.getText(), context)
    }

    override fun createPresentationCodeFragment(item: TextWithImports, context: PsiElement?, project: Project): JavaCodeFragment {
        return createCodeFragment(item, context, project)
    }

    override fun isContextAccepted(contextElement: PsiElement?): Boolean {
        return contextElement?.getLanguage() == JetFileType.INSTANCE.getLanguage()
    }

    override fun getFileType() = JetFileType.INSTANCE

    override fun getEvaluatorBuilder() = KotlinEvaluationBuilder
}
