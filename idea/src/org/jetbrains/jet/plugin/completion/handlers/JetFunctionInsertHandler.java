package org.jetbrains.jet.plugin.completion.handlers;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.editor.Editor;

/**
 * Inserts '()' after function proposal insert.
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
        int startOffset = context.getStartOffset();
        int lookupStringLength = item.getLookupString().length();
        int endOffset = startOffset + lookupStringLength;

        context.getDocument().insertString(endOffset, "()");

        Editor editor = context.getEditor();
        if (caretPosition == CaretPosition.IN_BRACKETS) {
            editor.getCaretModel().moveToOffset(editor.getCaretModel().getOffset() + 1);
        } else {
            editor.getCaretModel().moveToOffset(editor.getCaretModel().getOffset() + 2);
        }
    }
}
