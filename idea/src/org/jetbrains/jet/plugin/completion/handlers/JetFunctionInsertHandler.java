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

package org.jetbrains.jet.plugin.completion.handlers;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetImportDirective;
import org.jetbrains.jet.lang.psi.JetQualifiedExpression;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.plugin.completion.JetLookupObject;
import org.jetbrains.jet.plugin.formatter.JetCodeStyleSettings;
import org.jetbrains.jet.plugin.quickfix.ImportInsertHelper;

/**
 * Performs number of code modification after insertion jet function:
 * - Insert ()
 * - Add import directive if necessary for top level functions
 */
public class JetFunctionInsertHandler implements InsertHandler<LookupElement> {

    public enum CaretPosition { IN_BRACKETS, AFTER_BRACKETS }
    public enum BracketType { PARENTHESIS, BRACES }

    private final CaretPosition caretPosition;
    private final BracketType bracketType;

    public JetFunctionInsertHandler(CaretPosition caretPosition, BracketType bracketType) {
        if (caretPosition == CaretPosition.AFTER_BRACKETS && bracketType == BracketType.BRACES) {
            throw new IllegalArgumentException("CaretPosition.AFTER_BRACKETS with bracketType == BracketType.BRACES combination is not supported");
        }

        this.caretPosition = caretPosition;
        this.bracketType = bracketType;
    }

    @Override
    public void handleInsert(InsertionContext context, LookupElement item) {
        PsiDocumentManager.getInstance(context.getProject()).commitAllDocuments();

        if (context.getCompletionChar() == '(') {
            context.setAddCompletionChar(false);
        }

        int startOffset = context.getStartOffset();
        PsiElement element = context.getFile().findElementAt(startOffset);
        if (element == null) {
            return;
        }

        if (shouldAddBrackets(element)) {
            addBrackets(context, element);
        }

        addImport(context, item);
    }

    private static boolean shouldAddBrackets(PsiElement element) {
        return PsiTreeUtil.getParentOfType(element, JetImportDirective.class) == null;
    }

    private void addBrackets(InsertionContext context, PsiElement offsetElement) {
        int offset = context.getSelectionEndOffset();
        Document document = context.getDocument();

        String documentText = document.getText();

        char completionChar = context.getCompletionChar();

        boolean braces = bracketType == BracketType.BRACES && completionChar != '(';
        char openingBracket = braces ? '{' : '(';
        char closingBracket = braces ? '}' : ')';

        int openingBracketIndex = indexOfSkippingSpace(documentText, openingBracket, offset);
        int inBracketsShift = 0;

        if (openingBracketIndex == -1) {
            // Insert ()/{} if it's not already exist
            if (braces) {
                if (completionChar == ' ') {
                    context.setAddCompletionChar(false);
                }

                if (isInsertSpacesInOneLineFunctionEnabled(offsetElement.getProject())) {
                    document.insertString(offset, " {  }");
                    inBracketsShift = 1;
                }
                else {
                    document.insertString(offset, " {}");
                }
            }
            else {
                document.insertString(offset, "()");
            }

            PsiDocumentManager.getInstance(context.getProject()).commitDocument(document);
            documentText = document.getText();
        }

        openingBracketIndex = indexOfSkippingSpace(documentText, openingBracket, offset);
        assert openingBracketIndex != -1 : "If there wasn't open bracket it should already have been inserted";

        // CaretPosition.AFTER_BRACKETS mode cannot work when there are some non-empty chars between open and close bracket
        int closeBracketIndex = indexOfSkippingSpace(documentText, closingBracket, openingBracketIndex + 1);

        Editor editor = context.getEditor();

        // Satisfy TypedHandler.handleRParen() algorithm for preventing doubling ')' char if user typed "()" manually.
        boolean forcePlaceCaretIntoParentheses = completionChar == '(';

        if (caretPosition == CaretPosition.IN_BRACKETS || forcePlaceCaretIntoParentheses || closeBracketIndex == -1) {
            editor.getCaretModel().moveToOffset(openingBracketIndex + 1 + inBracketsShift);
            AutoPopupController.getInstance(context.getProject()).autoPopupParameterInfo(editor, offsetElement);
        }
        else {
            editor.getCaretModel().moveToOffset(closeBracketIndex + 1);
        }

        PsiDocumentManager.getInstance(context.getProject()).commitDocument(context.getDocument());
    }

    private static int indexOfSkippingSpace(String str, char ch, int startIndex) {
        for (int i = startIndex; i < str.length(); i++) {
            char currentChar = str.charAt(i);
            if (ch == currentChar) {
                return i;
            }

            if (!Character.isWhitespace(currentChar)) {
                return -1;
            }
        }

        return -1;
    }

    private static boolean isInsertSpacesInOneLineFunctionEnabled(Project project) {
        CodeStyleSettings settings = CodeStyleSettingsManager.getSettings(project);
        JetCodeStyleSettings jetSettings = settings.getCustomSettings(JetCodeStyleSettings.class);

        return jetSettings.INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD;
    }

    private static void addImport(final InsertionContext context, final @NotNull LookupElement item) {
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                int startOffset = context.getStartOffset();
                PsiElement element = context.getFile().findElementAt(startOffset);
                if (element == null) {
                    return;
                }

                if (context.getFile() instanceof JetFile && item.getObject() instanceof JetLookupObject) {
                    DeclarationDescriptor descriptor = ((JetLookupObject) item.getObject()).getDescriptor();
                    if (descriptor instanceof SimpleFunctionDescriptor) {

                        final JetFile file = (JetFile) context.getFile();
                        final SimpleFunctionDescriptor functionDescriptor = (SimpleFunctionDescriptor) descriptor;
                        // Don't insert import for qualified expression if don't try to insert extension function
                        if (PsiTreeUtil.getParentOfType(element, JetQualifiedExpression.class) != null &&
                            functionDescriptor.getReceiverParameter() == null) {

                            return;
                        }

                        if (DescriptorUtils.isTopLevelDeclaration(functionDescriptor)) {
                            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                                @Override
                                public void run() {
                                    FqName fqn = DescriptorUtils.getFQName(functionDescriptor).toSafe();
                                    ImportInsertHelper.addImportDirectiveIfNeeded(fqn, file);
                                }
                            });
                        }
                    }
                }
            }
        });
    }
}
