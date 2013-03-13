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

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.psi.codeStyle.CodeStyleManager;

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
        char currentChar = context.getTailOffset() < document.getTextLength() ? document.getCharsSequence().charAt(context.getTailOffset()) : 0;
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

        CodeFoldingManager foldManager = CodeFoldingManager.getInstance(context.getProject());
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
        String presentation = template.replaceAll("<#<(\\w+)>#>", "...").replace("\n", "");
        LookupElementBuilder builder = LookupElementBuilder.create(presentation).setBold();
        return builder.setInsertHandler(new JetTemplateInsertHandler(template));
    }
}
