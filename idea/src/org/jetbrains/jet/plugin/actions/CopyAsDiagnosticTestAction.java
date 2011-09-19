package org.jetbrains.jet.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.jetbrains.jet.checkers.CheckerTestUtil;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.AnalyzerFacade;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

/**
 * @author abreslav
 */
public class CopyAsDiagnosticTestAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        assert editor != null && psiFile != null;

        BindingContext bindingContext = AnalyzerFacade.analyzeFileWithCache((JetFile) psiFile);

        String result = CheckerTestUtil.addDiagnosticMarkersToText(psiFile, bindingContext).toString();

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(result), new ClipboardOwner() {
            @Override
            public void lostOwnership(Clipboard clipboard, Transferable contents) {}
        });
    }


    @Override
    public void update(AnActionEvent e) {
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        e.getPresentation().setEnabled(editor != null && psiFile instanceof JetFile && ApplicationManager.getApplication().isInternal());
    }

}
