package org.jetbrains.jet.plugin.actions;

import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.actions.CreateFromTemplateAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.jet.plugin.JetIconProvider;

/**
 * @author Nikolay Krasko
 */
public class NewKotlinFileAction extends CreateFromTemplateAction {

    public NewKotlinFileAction() {
        super(FileTemplateManager.getInstance().getInternalTemplate("Kotlin File"));
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        e.getPresentation().setIcon(JetIconProvider.KOTLIN_ICON);
    }
}