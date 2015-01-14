/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.actions;

import com.intellij.codeInsight.CodeInsightUtilBase;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.kotlin.idea.JetLanguage;
import org.jetbrains.kotlin.idea.caches.resolve.ResolvePackage;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.types.JetType;

public class ShowExpressionTypeAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        assert editor != null && psiFile != null;
        JetExpression expression;
        BindingContext bindingContext = ResolvePackage.analyzeFully((JetFile) psiFile);
        if (editor.getSelectionModel().hasSelection()) {
            int startOffset = editor.getSelectionModel().getSelectionStart();
            int endOffset = editor.getSelectionModel().getSelectionEnd();
            expression = CodeInsightUtilBase.findElementInRange(psiFile, startOffset, endOffset,
                    JetExpression.class, JetLanguage.INSTANCE);
        }
        else {
            int offset = editor.getCaretModel().getOffset();
            expression = PsiTreeUtil.getParentOfType(psiFile.findElementAt(offset), JetExpression.class);
            while (expression != null && bindingContext.get(BindingContext.EXPRESSION_TYPE, expression) == null) {
                expression = PsiTreeUtil.getParentOfType(expression, JetExpression.class);
            }
            if (expression != null) {
                editor.getSelectionModel().setSelection(expression.getTextRange().getStartOffset(),
                        expression.getTextRange().getEndOffset());
            }
        }
        if (expression != null) {
            JetType type = bindingContext.get(BindingContext.EXPRESSION_TYPE, expression);
            if (type != null) {
                HintManager.getInstance().showInformationHint(editor, "<html>" + DescriptorRenderer.HTML.renderType(type) + "</html>");
            }
        }
    }

    @Override
    public void update(AnActionEvent e) {
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        e.getPresentation().setEnabled(editor != null && psiFile instanceof JetFile);
    }
}
