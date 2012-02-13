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
import org.jetbrains.jet.lang.descriptors.NamedFunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetImportDirective;
import org.jetbrains.jet.lang.psi.JetQualifiedExpression;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.plugin.completion.JetLookupObject;
import org.jetbrains.jet.plugin.quickfix.ImportClassHelper;

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
        } else if (endOffset + 1 < documentText.length() && documentText.charAt(endOffset) == ')') {
            bothParentheses = true;
        }

        Editor editor = context.getEditor();
        if (caretPosition == CaretPosition.IN_BRACKETS || !bothParentheses) {
            editor.getCaretModel().moveToOffset(editor.getCaretModel().getOffset() + 1);
            AutoPopupController.getInstance(context.getProject()).autoPopupParameterInfo(editor, offsetElement);
        } else {
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

                // No auto import for qualified expressions
                if (PsiTreeUtil.getParentOfType(element, JetQualifiedExpression.class) != null) {
                    return;
                }

                if (context.getFile() instanceof JetFile && item.getObject() instanceof JetLookupObject) {
                    final DeclarationDescriptor descriptor = ((JetLookupObject) item.getObject()).getDescriptor();
                    if (descriptor instanceof NamedFunctionDescriptor) {

                        final JetFile file = (JetFile) context.getFile();
                        NamedFunctionDescriptor functionDescriptor = (NamedFunctionDescriptor) descriptor;
                        final String fqn = DescriptorUtils.getFQName(functionDescriptor);

                        if (DescriptorUtils.isTopLevelFunction(functionDescriptor)) {
                            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                                @Override
                                public void run() {
                                    ImportClassHelper.addImportDirective(fqn, file);
                                }
                            });
                        }
                    }
                }
            }
        });
    }
}
