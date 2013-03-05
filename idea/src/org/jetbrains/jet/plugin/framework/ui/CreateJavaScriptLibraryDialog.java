package org.jetbrains.jet.plugin.framework.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CreateJavaScriptLibraryDialog extends DialogWrapper {
    private final CopyIntoPanel copyJSIntoPanel;
    private final CopyIntoPanel copyLibraryIntoPanel;

    private JPanel contentPane;
    private JCheckBox copyLibraryCheckbox;
    private JCheckBox copyJSRuntimeCheckbox;
    private JPanel compilerSourcePanelPlace;
    private JPanel copyJSIntoPanelPlace;
    private JPanel copyHeadersIntoPanelPlace;

    public CreateJavaScriptLibraryDialog(@Nullable Project project, @NotNull String title, VirtualFile contextDirectory) {
        super(project);

        setTitle(title);

        init();

        ChooseCompilerSourcePanel compilerSourcePanel = new ChooseCompilerSourcePanel();
        compilerSourcePanelPlace.add(compilerSourcePanel.getContentPane(), BorderLayout.CENTER);

        copyJSIntoPanel = new CopyIntoPanel(project, FileUIUtils.createRelativePath(project, contextDirectory, "script"), "&Script directory:");
        copyJSIntoPanel.addValidityListener(new ValidityListener() {
            @Override
            public void validityChanged(boolean isValid) {
                updateComponents();
            }
        });
        copyJSIntoPanelPlace.add(copyJSIntoPanel.getContentPane(), BorderLayout.CENTER);

        copyLibraryIntoPanel = new CopyIntoPanel(project, FileUIUtils.createRelativePath(project, contextDirectory, "lib"), "&Lib directory:");
        copyLibraryIntoPanel.addValidityListener(new ValidityListener() {
            @Override
            public void validityChanged(boolean isValid) {
                updateComponents();
            }
        });
        copyHeadersIntoPanelPlace.add(copyLibraryIntoPanel.getContentPane(), BorderLayout.CENTER);

        ActionListener updateComponentsListener = new ActionListener() {
            @Override
            public void actionPerformed(@NotNull ActionEvent e) {
                updateComponents();
            }
        };

        copyLibraryCheckbox.addActionListener(updateComponentsListener);
        copyJSRuntimeCheckbox.addActionListener(updateComponentsListener);

        updateComponents();
    }

    @Nullable
    public String getCopyJsIntoPath() {
        return copyJSIntoPanel.getPath();
    }

    @Nullable
    public String getCopyLibraryIntoPath() {
        return copyLibraryIntoPanel.getPath();
    }

    public boolean isCopyLibraryFiles() {
        return copyLibraryCheckbox.isSelected();
    }

    public boolean isCopyJS() {
        return copyJSRuntimeCheckbox.isSelected();
    }

    private void updateComponents() {
        copyLibraryIntoPanel.setEnabled(copyLibraryCheckbox.isSelected());
        copyJSIntoPanel.setEnabled(copyJSRuntimeCheckbox.isSelected());

        setOKActionEnabled(!(copyJSIntoPanel.hasErrors() || copyLibraryIntoPanel.hasErrors()));
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }
}
