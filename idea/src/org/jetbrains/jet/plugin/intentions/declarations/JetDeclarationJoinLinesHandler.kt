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

package org.jetbrains.jet.plugin.intentions.declarations

import com.intellij.codeInsight.editorActions.JoinRawLinesHandlerDelegate
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ArrayUtil
import org.jetbrains.jet.lang.psi.JetPsiUtil
import org.jetbrains.jet.lang.psi.psiUtil.*

public class JetDeclarationJoinLinesHandler : JoinRawLinesHandlerDelegate {

    override fun tryJoinRawLines(document: Document, file: PsiFile, start: Int, end: Int): Int {
        val element = JetPsiUtil.skipSiblingsBackwardByPredicate(file.findElementAt(start), DeclarationUtils.SKIP_DELIMITERS)

        val target = element?.getParentByTypesAndPredicate<PsiElement>(strict = false) {
            DeclarationUtils.checkAndGetPropertyAndInitializer(it) != null
        } ?: return -1

        return DeclarationUtils.joinPropertyDeclarationWithInitializer(target).getTextRange()!!.getStartOffset()
    }

    override fun tryJoinLines(document: Document, file: PsiFile, start: Int, end: Int)
            = -1
}