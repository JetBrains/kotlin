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

package org.jetbrains.jet.plugin.refactoring;

import com.intellij.codeInsight.unwrap.ScopeHighlighter;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.popup.JBPopupAdapter;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.NamespaceType;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.plugin.codeInsight.CodeInsightUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.ArrayList;

import static org.jetbrains.jet.plugin.project.AnalyzeSingleFileUtil.getContextForSingleFile;

/**
 * User: Alefas
 * Date: 25.01.12
 */
public class JetRefactoringUtil {

    private JetRefactoringUtil() {
    }

    public interface SelectExpressionCallback {
        void run(@Nullable JetExpression expression);
    }

    public static void selectExpression(@NotNull Editor editor,
                                        @NotNull PsiFile file,
                                        @NotNull SelectExpressionCallback callback) throws IntroduceRefactoringException {
        if (editor.getSelectionModel().hasSelection()) {
            int selectionStart = editor.getSelectionModel().getSelectionStart();
            int selectionEnd = editor.getSelectionModel().getSelectionEnd();
            String text = file.getText();
            while (selectionStart < selectionEnd && Character.isSpaceChar(text.charAt(selectionStart))) ++selectionStart;
            while (selectionStart < selectionEnd && Character.isSpaceChar(text.charAt(selectionEnd - 1))) --selectionEnd;
            callback.run(findExpression(file, selectionStart, selectionEnd));
        }
        else {
            int offset = editor.getCaretModel().getOffset();
            smartSelectExpression(editor, file, offset, callback);
        }
    }

    private static void smartSelectExpression(@NotNull Editor editor, @NotNull PsiFile file, int offset,
                                             @NotNull final SelectExpressionCallback callback)
            throws IntroduceRefactoringException {
        if (offset < 0) throw new IntroduceRefactoringException(JetRefactoringBundle.message("cannot.refactor.not.expression"));
        PsiElement element = file.findElementAt(offset);
        if (element == null) throw new IntroduceRefactoringException(JetRefactoringBundle.message("cannot.refactor.not.expression"));
        if (element instanceof PsiWhiteSpace) {
            smartSelectExpression(editor, file, offset - 1, callback);
            return;
        }
        ArrayList<JetExpression> expressions = new ArrayList<JetExpression>();
        while (element != null && !(element instanceof JetBlockExpression && !(element.getParent() instanceof JetFunctionLiteral)) &&
               !(element instanceof JetNamedFunction)
               && !(element instanceof JetClassBody)) {
            if (element instanceof JetExpression && !(element instanceof JetStatementExpression)) {
                boolean addExpression = true;
                if (element.getParent() instanceof JetQualifiedExpression) {
                    JetQualifiedExpression qualifiedExpression = (JetQualifiedExpression) element.getParent();
                    if (qualifiedExpression.getReceiverExpression() != element) {
                        addExpression = false;
                    }
                }
                else if (element.getParent() instanceof JetCallElement) {
                    addExpression = false;
                }
                else if (element.getParent() instanceof JetOperationExpression) {
                    JetOperationExpression operationExpression = (JetOperationExpression) element.getParent();
                    if (operationExpression.getOperationReference() == element) {
                        addExpression = false;
                    }
                }
                if (addExpression) {
                    JetExpression expression = (JetExpression)element;
                    BindingContext bindingContext = getContextForSingleFile((JetFile)expression.getContainingFile());
                    JetType expressionType = bindingContext.get(BindingContext.EXPRESSION_TYPE, expression);
                    if (expressionType == null || !(expressionType instanceof NamespaceType) &&
                                                  !JetTypeChecker.INSTANCE.equalTypes(KotlinBuiltIns.
                                                          getInstance().getUnitType(), expressionType)) {
                        expressions.add(expression);
                    }
                }
            }
            element = element.getParent();
        }
        if (expressions.size() == 0) throw new IntroduceRefactoringException(JetRefactoringBundle.message("cannot.refactor.not.expression"));

        final DefaultListModel model = new DefaultListModel();
        for (JetExpression expression : expressions) {
            model.addElement(expression);
        }

        final ScopeHighlighter highlighter = new ScopeHighlighter(editor);

        final JList list = new JBList(model);
        
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component rendererComponent = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                JetExpression element = (JetExpression) value;
                if (element.isValid()) {
                    setText(getExpressionShortText(element));
                }
                return rendererComponent;
            }
        });

        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                highlighter.dropHighlight();
                int selectedIndex = list.getSelectedIndex();
                if (selectedIndex < 0) return;
                JetExpression expression = (JetExpression) model.get(selectedIndex);
                ArrayList<PsiElement> toExtract = new ArrayList<PsiElement>();
                toExtract.add(expression);
                highlighter.highlight(expression, toExtract);
            }
        });

        JBPopupFactory.getInstance().createListPopupBuilder(list).
                setTitle(JetRefactoringBundle.message("expressions.title")).setMovable(false).setResizable(false).
                setRequestFocus(true).setItemChoosenCallback(new Runnable() {
            @Override
            public void run() {
                callback.run((JetExpression) list.getSelectedValue());
            }
        }).addListener(new JBPopupAdapter() {
            @Override
            public void onClosed(LightweightWindowEvent event) {
                highlighter.dropHighlight();
            }
        }).createPopup().showInBestPositionFor(editor);
        
    }

    public static String getExpressionShortText(@NotNull JetExpression expression) { //todo: write appropriate implementation
        String expressionText = expression.getText();
        if (expressionText.length() > 20) {
            expressionText = expressionText.substring(0, 17) + "...";
        }
        return expressionText;
    }

    @NotNull
    private static JetExpression findExpression(@NotNull PsiFile file, int startOffset, int endOffset) throws IntroduceRefactoringException {
        JetExpression element = CodeInsightUtils.findExpression(file, startOffset, endOffset);
        if (element == null) {
            //todo: if it's infix expression => add (), then commit document then return new created expression
            throw new IntroduceRefactoringException(JetRefactoringBundle.message("cannot.refactor.not.expression"));
        }
        return element;
    }

    public static class IntroduceRefactoringException extends Exception {
        private String myMessage;

        public IntroduceRefactoringException(String message) {
            myMessage = message;
        }

        public String getMessage() {
            return myMessage;
        }
    }

}
