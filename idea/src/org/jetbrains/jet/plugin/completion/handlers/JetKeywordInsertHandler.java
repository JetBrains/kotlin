package org.jetbrains.jet.plugin.completion.handlers;

import com.google.common.collect.Sets;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;

import java.util.Set;

/**
 * @author Nikolay Krasko
 */
public class JetKeywordInsertHandler implements InsertHandler<LookupElement> {

    private final static Set<String> NO_SPACE_AFTER = Sets.newHashSet("this", "super", "This", "true", "false", "null");

    @Override
    public void handleInsert(InsertionContext context, LookupElement item) {
        String keyword = item.getLookupString();

        // Add space after keyword
        if (!NO_SPACE_AFTER.contains(keyword)) {
            Editor editor = context.getEditor();
            Document document = editor.getDocument();

            int offset = context.getStartOffset() + keyword.length();
            context.setAddCompletionChar(false);
            document.insertString(offset, " ");
            editor.getCaretModel().moveToOffset(editor.getCaretModel().getOffset() + 1);
        }
    }
}
