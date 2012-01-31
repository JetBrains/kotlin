package org.jetbrains.jet.plugin.completion.handlers;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.psi.codeStyle.CodeStyleManager;

/**
 * @author Evgeny Gerashchenko
 * @author max (originally from objc)
 * @since 1/31/12
 */
public class JetTemplateInsertHandler implements InsertHandler<LookupElement> {
    @Override
    public void handleInsert(InsertionContext context, LookupElement item) {
        Editor editor = context.getEditor();

        final CodeFoldingManager foldManager = CodeFoldingManager.getInstance(context.getProject());
        foldManager.updateFoldRegions(editor);

        String text = context.getDocument().getText();
        int firstParamStart = text.indexOf("<#<", context.getStartOffset());
        int firstParamEnd = text.indexOf(">#>", firstParamStart);

        SelectionModel selectionModel = editor.getSelectionModel();
        if (firstParamStart >= 0 && firstParamEnd >= 0 && firstParamStart < context.getTailOffset()) {
            selectionModel.setSelection(firstParamStart, firstParamEnd + 2);
        }

        CodeStyleManager.getInstance(context.getProject()).reformatText(context.getFile(), context.getStartOffset(), context.getTailOffset());

        if (firstParamStart >= 0 && firstParamEnd >= 0 && selectionModel.hasSelection()) {
            editor.getCaretModel().moveToOffset(selectionModel.getSelectionStart());
        }
        else {
            editor.getCaretModel().moveToOffset(context.getTailOffset());
        }
    }
}
