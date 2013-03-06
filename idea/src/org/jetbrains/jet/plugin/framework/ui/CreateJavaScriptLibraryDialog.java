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

package org.jetbrains.jet.plugin.framework.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.JetPluginUtil;

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
    private JPanel copyJSIntoPanelPlace;
    private JPanel copyHeadersIntoPanelPlace;
    private JLabel compilerTextLabel;

    public CreateJavaScriptLibraryDialog(@Nullable Project project, @NotNull String title, VirtualFile contextDirectory) {
        super(project);

        setTitle(title);

        init();

        compilerTextLabel.setText(compilerTextLabel.getText() + " - " + JetPluginUtil.getPluginVersion());

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
