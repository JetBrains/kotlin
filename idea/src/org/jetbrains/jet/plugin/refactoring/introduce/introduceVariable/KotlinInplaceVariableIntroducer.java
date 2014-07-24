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

package org.jetbrains.jet.plugin.refactoring.introduce.introduceVariable;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.refactoring.introduce.inplace.InplaceVariableIntroducer;
import com.intellij.ui.NonFocusableCheckBox;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.intentions.SpecifyTypeExplicitlyAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class KotlinInplaceVariableIntroducer extends InplaceVariableIntroducer<JetExpression> {

    private final boolean myReplaceOccurrence;
    private final JetProperty myProperty;
    private final boolean isVar;
    private final boolean myDoNotChangeVar;
    @Nullable private final JetType myExprType;
    private final boolean noTypeInference;
    private JCheckBox myVarCheckbox;
    private JCheckBox myExprTypeCheckbox;

    public KotlinInplaceVariableIntroducer(
            PsiNamedElement elementToRename, Editor editor, Project project,
            String title, JetExpression[] occurrences,
            @Nullable JetExpression expr, boolean replaceOccurrence,
            JetProperty property, boolean isVar, boolean doNotChangeVar,
            @Nullable JetType exprType, boolean noTypeInference
    ) {
        super(elementToRename, editor, project, title, occurrences, expr);
        this.myReplaceOccurrence = replaceOccurrence;
        myProperty = property;
        this.isVar = isVar;
        myDoNotChangeVar = doNotChangeVar;
        myExprType = exprType;
        this.noTypeInference = noTypeInference;
    }

    @Override
    @Nullable
    protected JComponent getComponent() {
        if (!myDoNotChangeVar) {
            myVarCheckbox = new NonFocusableCheckBox("Declare with var");
            myVarCheckbox.setSelected(isVar);
            myVarCheckbox.setMnemonic('v');
            myVarCheckbox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new WriteCommandAction(myProject, getCommandName(), getCommandName()) {
                        @Override
                        protected void run(Result result) throws Throwable {
                            PsiDocumentManager.getInstance(myProject).commitDocument(myEditor.getDocument());
                            KotlinChangePropertyActions.declareValueOrVariable(myVarCheckbox.isSelected(), myProperty);
                        }
                    }.execute();
                }
            });
        }

        if (myExprType != null && !noTypeInference) {
            myExprTypeCheckbox = new NonFocusableCheckBox("Specify type explicitly");
            myExprTypeCheckbox.setSelected(false);
            myExprTypeCheckbox.setMnemonic('t');
            myExprTypeCheckbox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    final Ref<Boolean> greedyToRight = new Ref<Boolean>();
                    new WriteCommandAction(myProject, getCommandName(), getCommandName()) {
                        @Override
                        protected void run(Result result) throws Throwable {
                            PsiDocumentManager.getInstance(myProject).commitDocument(myEditor.getDocument());
                            if (myExprTypeCheckbox.isSelected()) {
                                ASTNode identifier = myProperty.getNode().findChildByType(JetTokens.IDENTIFIER);
                                if (identifier != null) {
                                    TextRange range = identifier.getTextRange();
                                    RangeHighlighter[] highlighters = myEditor.getMarkupModel().getAllHighlighters();
                                    for (RangeHighlighter highlighter : highlighters) {
                                        if (highlighter.getStartOffset() == range.getStartOffset()) {
                                            if (highlighter.getEndOffset() == range.getEndOffset()) {
                                                greedyToRight.set(highlighter.isGreedyToRight());
                                                highlighter.setGreedyToRight(false);
                                            }
                                        }
                                    }
                                }
                                SpecifyTypeExplicitlyAction.addTypeAnnotation(myProject, myEditor, myProperty, myExprType);
                                PsiDocumentManager.getInstance(myProject).commitDocument(myEditor.getDocument());
                            }
                            else {
                                SpecifyTypeExplicitlyAction.removeTypeAnnotation(myProperty);
                            }
                        }
                    }.execute();
                    ApplicationManager.getApplication().runReadAction(new Runnable() {
                        @Override
                        public void run() {
                            if (myExprTypeCheckbox.isSelected()) {
                                ASTNode identifier = myProperty.getNode().findChildByType(JetTokens.IDENTIFIER);
                                if (identifier != null) {
                                    TextRange range = identifier.getTextRange();
                                    RangeHighlighter[] highlighters = myEditor.getMarkupModel().getAllHighlighters();
                                    for (RangeHighlighter highlighter : highlighters) {
                                        if (highlighter.getStartOffset() == range.getStartOffset()) {
                                            if (highlighter.getEndOffset() == range.getEndOffset()) {
                                                highlighter.setGreedyToRight(greedyToRight.get());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    });

                }
            });
        }

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(null);
        int count = 1;
        if (myVarCheckbox != null) {
            panel.add(myVarCheckbox, new GridBagConstraints(0, count, 1, 1, 1, 0, GridBagConstraints.NORTHWEST,
                                                            GridBagConstraints.HORIZONTAL,
                                                            new Insets(5, 5, 5, 5), 0, 0));
            ++count;
        }

        if (myExprTypeCheckbox != null) {
            panel.add(myExprTypeCheckbox, new GridBagConstraints(0, count, 1, 1, 1, 0, GridBagConstraints.NORTHWEST,
                                                            GridBagConstraints.HORIZONTAL,
                                                            new Insets(5, 5, 5, 5), 0, 0));
            ++count;
        }
        panel.add(Box.createVerticalBox(), new GridBagConstraints(0, count, 1, 1, 1, 1, GridBagConstraints.NORTHWEST,
                                                                  GridBagConstraints.BOTH,
                                                                  new Insets(0, 0, 0, 0), 0, 0));
        return panel;
    }

    @Override
    protected void moveOffsetAfter(boolean success) {
        if (!myReplaceOccurrence || myExprMarker == null) {
            myEditor.getCaretModel().moveToOffset(myProperty.getTextRange().getEndOffset());
        }
        else {
            int startOffset = myExprMarker.getStartOffset();
            PsiFile file = myProperty.getContainingFile();
            PsiElement elementAt = file.findElementAt(startOffset);
            if (elementAt != null) {
                myEditor.getCaretModel().moveToOffset(elementAt.getTextRange().getEndOffset());
            }
            else {
                myEditor.getCaretModel().moveToOffset(myExprMarker.getEndOffset());
            }
        }
    }
}
