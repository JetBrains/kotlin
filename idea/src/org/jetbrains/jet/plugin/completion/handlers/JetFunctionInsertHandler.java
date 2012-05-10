/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetImportDirective;
import org.jetbrains.jet.lang.psi.JetQualifiedExpression;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.FqName;
import org.jetbrains.jet.plugin.completion.JetLookupObject;
import org.jetbrains.jet.plugin.quickfix.ImportInsertHelper;

/**
 * Performs number of code modification after insertion jet function:
 * - Insert ()
 * - Add import directive if necessary for top level functions
 *
 * @author Nikolay Krasko
 */
public class JetFunctionInsertHandler implements InsertHandler<LookupElement> {

    public enum CaretPosition { IN_BRACKETS, AFTER_BRACKETS }

    private final CaretPosition caretPosition;

    public JetFunctionInsertHandler(CaretPosition caretPosition) {
        this.caretPosition = caretPosition;
    }

    @Override
    public void handleInsert(InsertionContext context, LookupElement item) {
        if (context.getCompletionChar() == '(') {
            context.setAddCompletionChar(false);
        }

        int startOffset = context.getStartOffset();
        PsiElement element = context.getFile().findElementAt(startOffset);
        if (element == null) {
            return;
        }

        if (shouldAddParenthesis(element)) {
            addParenthesis(context, item, element);
        }

        addImport(context, item);
    }

    private static boolean shouldAddParenthesis(PsiElement element) {
        return PsiTreeUtil.getParentOfType(element, JetImportDirective.class) == null;
    }

    private void addParenthesis(InsertionContext context, LookupElement item, PsiElement offsetElement) {
        int startOffset = context.getStartOffset();

        int lookupStringLength = item.getLookupString().length();
        int endOffset = startOffset + lookupStringLength;
        Document document = context.getDocument();

        boolean bothParentheses = false;
        String documentText = document.getText();

        if (!(endOffset < documentText.length() && documentText.charAt(endOffset) == '(')) {
            // Insert () if it's not already exist
            document.insertString(endOffset, "()");
            bothParentheses = true;
        }
        else if (endOffset + 1 < documentText.length() && documentText.charAt(endOffset + 1) == ')') {
            bothParentheses = true;
        }

        Editor editor = context.getEditor();
        if (caretPosition == CaretPosition.IN_BRACKETS || !bothParentheses) {
            editor.getCaretModel().moveToOffset(editor.getCaretModel().getOffset() + 1);
            AutoPopupController.getInstance(context.getProject()).autoPopupParameterInfo(editor, offsetElement);
        }
        else {
            editor.getCaretModel().moveToOffset(editor.getCaretModel().getOffset() + 2);
        }

        PsiDocumentManager.getInstance(context.getProject()).commitDocument(context.getDocument());
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
                    final DeclarationDescriptor descriptor = ((JetLookupObject) item.getObject()).getDescriptor();
                    if (descriptor instanceof SimpleFunctionDescriptor) {

                        final JetFile file = (JetFile) context.getFile();
                        final SimpleFunctionDescriptor functionDescriptor = (SimpleFunctionDescriptor) descriptor;
                        // Don't insert import for qualified expression if don't try to insert extension function
                        if (PsiTreeUtil.getParentOfType(element, JetQualifiedExpression.class) != null &&
                                !functionDescriptor.getReceiverParameter().exists()) {

                            return;
                        }

                        if (DescriptorUtils.isTopLevelFunction(functionDescriptor)) {
                            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                                @Override
                                public void run() {
                                    final FqName fqn = DescriptorUtils.getFQName(functionDescriptor).toSafe();
                                    ImportInsertHelper.addImportDirective(fqn, file);
                                }
                            });
                        }
                    }
                }
            }
        });
    }
}
