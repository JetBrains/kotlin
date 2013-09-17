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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.JetPluginUtil;
import org.jetbrains.jet.plugin.framework.JSLibraryCreateOptions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class CreateJavaScriptLibraryDialogBase extends DialogWrapper implements JSLibraryCreateOptions {
    protected final CopyIntoPanel copyJsFilesPanel;
    protected final CopyIntoPanel copyJarPanel;

    protected JPanel contentPane;
    protected JCheckBox copyJarCheckbox;
    protected JPanel copyJsFilesPanelPlace;
    protected JPanel copyJarFilePanelPlace;
    protected JLabel compilerTextLabel;
    protected JPanel chooseModulesPanelPlace;

    public CreateJavaScriptLibraryDialogBase(
            @Nullable Project project,
            @NotNull String defaultPathToJar,
            @NotNull String defaultPathToJsFile,
            boolean showPathToJarPanel,
            boolean showPathToJsFilePanel
    ) {
        super(project);

        setTitle("Create Kotlin JavaScript Library");

        init();

        compilerTextLabel.setText(compilerTextLabel.getText() + " - " + JetPluginUtil.getPluginVersion());

        ValidityListener validityListener = new ValidityListener() {
            @Override
            public void validityChanged(boolean isValid) {
                updateComponents();
            }
        };

        copyJsFilesPanel = new CopyIntoPanel(project, defaultPathToJsFile, "Script directory:");
        copyJsFilesPanel.addValidityListener(validityListener);
        copyJsFilesPanelPlace.add(copyJsFilesPanel.getContentPane(), BorderLayout.CENTER);

        copyJarPanel = new CopyIntoPanel(project, defaultPathToJar, "&Lib directory:");
        copyJarPanel.addValidityListener(validityListener);
        copyJarFilePanelPlace.add(copyJarPanel.getContentPane(), BorderLayout.CENTER);

        if (!showPathToJarPanel) {
            copyJarFilePanelPlace.setVisible(false);
            copyJarCheckbox.setVisible(false);
        }

        if (!showPathToJsFilePanel) {
            copyJsFilesPanelPlace.setVisible(false);
        }

        copyJarCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(@NotNull ActionEvent e) {
                updateComponents();
            }
        });
    }

    @Override
    @Nullable
    public String getCopyJsIntoPath() {
        if (!copyJsFilesPanelPlace.isVisible()) return null;
        return copyJsFilesPanel.getPath();
    }

    @Override
    @Nullable
    public String getCopyLibraryIntoPath() {
        return copyJarCheckbox.isSelected() ? copyJarPanel.getPath() : null;
    }

    protected void updateComponents() {
        copyJarPanel.setEnabled(copyJarCheckbox.isSelected());

        setOKActionEnabled(!copyJsFilesPanel.hasErrors() && !copyJarPanel.hasErrors());
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }
}
