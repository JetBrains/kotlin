package org.jetbrains.jet.plugin.completion.handlers;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.psi.codeStyle.CodeStyleManager;

/**
 * @author max (originally from objc)
 * @author Evgeny Gerashchenko
 * @since 1/31/12
 */
public class JetTemplateInsertHandler implements InsertHandler<LookupElement> {
    private String myInsertion;

    public JetTemplateInsertHandler(String insertion) {
        myInsertion = insertion;
    }

    @Override
    public void handleInsert(InsertionContext context, LookupElement item) {
        Document document = context.getDocument();
        Editor editor = context.getEditor();

        String insertion = myInsertion;
        final char currentChar = context.getTailOffset() < document.getTextLength() ? document.getCharsSequence().charAt(context.getTailOffset()) : 0;
        if (insertion.endsWith(" ") && context.getCompletionChar() != ' ' && (currentChar == ')' || currentChar == ' ' || currentChar == '\t')) {
            insertion = insertion.trim();
        }

        int tailOffset = context.getStartOffset() + insertion.length();
        int caretOffset = insertion.indexOf('|');
        if (caretOffset != -1) {
            insertion = insertion.replace("|", "");
            tailOffset = context.getStartOffset() + caretOffset;
        }

    document.replaceString(context.getStartOffset(), context.getTailOffset(), insertion);

        context.setTailOffset(tailOffset);

        final CodeFoldingManager foldManager = CodeFoldingManager.getInstance(context.getProject());
        foldManager.updateFoldRegions(editor);

        String text = document.getText();
        int firstParamStart = text.indexOf("<#<", context.getStartOffset());
        int firstParamEnd = text.indexOf(">#>", firstParamStart);

        SelectionModel selectionModel = editor.getSelectionModel();
        if (firstParamStart >= 0 && firstParamEnd >= 0 && firstParamStart < context.getTailOffset()) {
            selectionModel.setSelection(firstParamStart, firstParamEnd + 2);
        }

        if (!insertion.endsWith(" ")) {
            CodeStyleManager.getInstance(context.getProject()).reformatText(context.getFile(), context.getStartOffset(), context.getTailOffset());
        }

        if (firstParamStart >= 0 && firstParamEnd >= 0 && selectionModel.hasSelection()) {
            editor.getCaretModel().moveToOffset(selectionModel.getSelectionStart());
        }
        else {
            editor.getCaretModel().moveToOffset(context.getTailOffset());
        }

        if (context.getCompletionChar() == ' ') {
          context.setAddCompletionChar(false);
        }
    }
    
    public static LookupElementBuilder lookup(String template) {
        String presentation = template.replaceAll("<#<(\\w+)>#>", "...");
        LookupElementBuilder builder = LookupElementBuilder.create(presentation).setBold();
        return builder.setInsertHandler(new JetTemplateInsertHandler(template));
    }
}
