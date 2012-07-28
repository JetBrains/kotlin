package org.jetbrains.jet.plugin.conversion.copy;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.jet.plugin.editor.JetEditorOptions;

import javax.swing.*;
import java.awt.*;

/**
 * @ignatov
 */
@SuppressWarnings("UnusedDeclaration")
public class KotlinPasteFromJavaDialog extends DialogWrapper {
    private JPanel myPanel;
    private JCheckBox donTShowThisCheckBox;
    private JButton buttonOK;

    public KotlinPasteFromJavaDialog(Project project) {
        super(project, true);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setTitle("Convert Code From Java");
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        return myPanel;
    }

    @Override
    public Container getContentPane() {
        return myPanel;
    }

    @Override
    protected Action[] createActions() {
        return new Action[] {getOKAction(), getCancelAction()};
    }

    @Override
    protected void doOKAction() {
        if (donTShowThisCheckBox.isSelected()) {
            JetEditorOptions.getInstance().setDonTShowConversionDialog(true);
        }
        super.doOKAction();
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
