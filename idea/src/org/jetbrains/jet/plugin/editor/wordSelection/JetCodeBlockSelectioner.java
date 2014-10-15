/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.editor.wordSelection;

import com.intellij.codeInsight.editorActions.wordSelection.BasicSelectioner;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.jet.lang.psi.JetBlockExpression;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.psi.JetWhenExpression;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.ArrayList;
import java.util.List;

/**
 * Originally from IDEA platform: CodeBlockOrInitializerSelectioner
 */
public class JetCodeBlockSelectioner extends BasicSelectioner {
    public boolean canSelect(PsiElement e) {
        return e instanceof JetBlockExpression || e instanceof JetWhenExpression;
    }

    public List<TextRange> select(PsiElement e, CharSequence editorText, int cursorOffset, Editor editor) {
        List<TextRange> result = new ArrayList<TextRange>();

        result.add(e.getTextRange());

        ASTNode[] children = e.getNode().getChildren(null);
        if (children.length == 0) {
            return result;
        }

        int start = findOpeningBrace(children);
        int end = findClosingBrace(children, start);

        result.addAll(expandToWholeLine(editorText, new TextRange(start, end)));

        return result;
    }

    public static int findOpeningBrace(ASTNode[] children) {
        int start = children[children.length - 1].getTextRange().getStartOffset();
        for (int i = 0; i < children.length; i++) {
            PsiElement child = children[i].getPsi();

            if (child instanceof LeafPsiElement) {
                if (((LeafPsiElement) child).getElementType() == JetTokens.LBRACE) {
                    int j = i + 1;

                    while (children[j] instanceof PsiWhiteSpace) {
                        j++;
                    }

                    start = children[j].getTextRange().getStartOffset();
                }
            }
        }
        return start;
    }

    public static int findClosingBrace(ASTNode[] children, int startOffset) {
        int end = children[children.length - 1].getTextRange().getEndOffset();
        for (int i = 0; i < children.length; i++) {
            PsiElement child = children[i].getPsi();

            if (child instanceof LeafPsiElement) {
                if (((LeafPsiElement) child).getElementType() == JetTokens.RBRACE) {
                    int j = i - 1;

                    while (children[j] instanceof PsiWhiteSpace && children[j].getTextRange().getStartOffset() > startOffset) {
                        j--;
                    }

                    end = children[j].getTextRange().getEndOffset();
                }
            }
        }
        return end;
    }
}
