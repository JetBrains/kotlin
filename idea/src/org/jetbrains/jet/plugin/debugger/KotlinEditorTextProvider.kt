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

package org.jetbrains.jet.plugin.debugger

import com.intellij.debugger.impl.EditorTextProvider
import com.intellij.psi.PsiElement
import com.intellij.debugger.engine.evaluation.TextWithImports
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.Pair
import com.intellij.debugger.engine.evaluation.TextWithImportsImpl
import com.intellij.debugger.engine.evaluation.CodeFragmentKind
import com.intellij.openapi.fileTypes.FileType
import org.jetbrains.jet.plugin.JetFileType
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.psi.JetExpressionCodeFragmentImpl

class KotlinEditorTextProvider : EditorTextProvider {
    override fun getEditorText(elementAtCaret: PsiElement): TextWithImports? {
        return TextWithImportsImpl(CodeFragmentKind.EXPRESSION, elementAtCaret.getText(), getImports(elementAtCaret), JetFileType.INSTANCE)
    }

    override fun findExpression(elementAtCaret: PsiElement, allowMethodCalls: Boolean): Pair<PsiElement, TextRange>? {
        return Pair(elementAtCaret, elementAtCaret.getTextRange())
    }

    class object {
        fun getImports(elementAtCaret: PsiElement): String {
            val containingFile = elementAtCaret.getContainingFile()
            if (containingFile !is JetFile) return ""

            return containingFile.getImportList()?.getImports()
                                    ?.map { it.getText() }
                                    ?.makeString(JetExpressionCodeFragmentImpl.IMPORT_SEPARATOR) ?: ""
        }
    }
}

