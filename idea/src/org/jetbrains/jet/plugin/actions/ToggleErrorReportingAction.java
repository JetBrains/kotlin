package org.jetbrains.jet.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.jet.plugin.annotations.JetPsiChecker;

/**
 * @author yole
 */
public class ToggleErrorReportingAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        JetPsiChecker.setErrorReportingEnabled(!JetPsiChecker.isErrorReportingEnabled());
    }
}
