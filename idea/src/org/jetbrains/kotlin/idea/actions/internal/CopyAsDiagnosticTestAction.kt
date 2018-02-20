/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.actions.internal;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.jetbrains.kotlin.checkers.CheckerTestUtil;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.BindingContext;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.List;

public class CopyAsDiagnosticTestAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        assert editor != null && psiFile != null;

        BindingContext bindingContext = ResolutionUtils.analyzeFully((KtFile) psiFile);

        List<CheckerTestUtil.ActualDiagnostic> diagnostics =
                CheckerTestUtil.getDiagnosticsIncludingSyntaxErrors(bindingContext, psiFile, false, null, null, false);
        String result = CheckerTestUtil.addDiagnosticMarkersToText(psiFile, diagnostics).toString();

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(result), new ClipboardOwner() {
            @Override
            public void lostOwnership(Clipboard clipboard, Transferable contents) {}
        });
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setVisible(ApplicationManager.getApplication().isInternal());

        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        e.getPresentation().setEnabled(editor != null && psiFile instanceof KtFile);
    }
}
