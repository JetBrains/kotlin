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
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.LineTokenizer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.jet.lang.psi.JetBlockExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetWhenEntry;
import org.jetbrains.jet.lang.psi.JetWhenExpression;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.ArrayList;
import java.util.List;

/**
 * Originally from IDEA platform: StatementGroupSelectioner
 */
public class JetStatementGroupSelectioner extends BasicSelectioner {
    @Override
    public boolean canSelect(PsiElement e) {
        return e instanceof JetExpression || e instanceof JetWhenEntry;
    }

    @Override
    public List<TextRange> select(PsiElement e, CharSequence editorText, int cursorOffset, Editor editor) {
        List<TextRange> result = new ArrayList<TextRange>();

        PsiElement parent = e.getParent();

        if (!(parent instanceof JetBlockExpression) && !(parent instanceof JetWhenExpression)) {
            return result;
        }


        PsiElement startElement = e;
        PsiElement endElement = e;


        while (startElement.getPrevSibling() != null) {
            PsiElement sibling = startElement.getPrevSibling();

            if (sibling instanceof LeafPsiElement) {
                if (((LeafPsiElement) sibling).getElementType() == JetTokens.LBRACE) {
                    break;
                }
            }

            if (sibling instanceof PsiWhiteSpace) {
                PsiWhiteSpace whiteSpace = (PsiWhiteSpace) sibling;

                String[] strings = LineTokenizer.tokenize(whiteSpace.getText().toCharArray(), false);
                if (strings.length > 2) {
                    break;
                }
            }

            startElement = sibling;
        }

        while (startElement instanceof PsiWhiteSpace) {
            startElement = startElement.getNextSibling();
        }

        while (endElement.getNextSibling() != null) {
            PsiElement sibling = endElement.getNextSibling();

            if (sibling instanceof LeafPsiElement) {
                if (((LeafPsiElement) sibling).getElementType() == JetTokens.RBRACE) {
                    break;
                }
            }

            if (sibling instanceof PsiWhiteSpace) {
                PsiWhiteSpace whiteSpace = (PsiWhiteSpace) sibling;

                String[] strings = LineTokenizer.tokenize(whiteSpace.getText().toCharArray(), false);
                if (strings.length > 2) {
                    break;
                }
            }

            endElement = sibling;
        }

        while (endElement instanceof PsiWhiteSpace) {
            endElement = endElement.getPrevSibling();
        }

        result.addAll(expandToWholeLine(editorText, new TextRange(startElement.getTextRange().getStartOffset(),
                                                                  endElement.getTextRange().getEndOffset())));

        return result;
    }
}
