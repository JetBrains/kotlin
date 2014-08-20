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
import com.intellij.ui.SeparatorWithText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.JetPluginUtil;
import org.jetbrains.jet.plugin.framework.JSLibraryCreateOptions;

import javax.swing.*;
import java.awt.*;

public abstract class CreateJavaScriptLibraryDialogBase extends DialogWrapper implements JSLibraryCreateOptions {
    protected final CopyIntoPanel copyJsFilesPanel;
    protected final CopyIntoPanel copyLibraryFilePanel;

    protected JPanel contentPane;

    protected JLabel compilerTextLabel;
    protected JPanel chooseModulesPlace;

    private SeparatorWithText modulesSeparator;

    protected JPanel copyJsFilesPlace;
    protected JPanel copyLibraryPlace;

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

        modulesSeparator.setCaption("Kotlin JavaScript Library");

        ValidityListener validityListener = new ValidityListener() {
            @Override
            public void validityChanged(boolean isValid) {
                updateComponents();
            }
        };

        copyJsFilesPanel = new CopyIntoPanel(project, defaultPathToJsFile, "Copy &runtime files to:");
        copyJsFilesPanel.addValidityListener(validityListener);
        copyJsFilesPanel.setLabelWidth(110);
        copyJsFilesPlace.add(copyJsFilesPanel.getContentPane(), BorderLayout.CENTER);

        copyLibraryFilePanel = new CopyIntoPanel(project, defaultPathToJar, "Copy &headers to:");
        copyLibraryFilePanel.addValidityListener(validityListener);
        copyLibraryFilePanel.setLabelWidth(110);
        copyLibraryPlace.add(copyLibraryFilePanel.getContentPane(), BorderLayout.CENTER);

        if (!showPathToJarPanel) {
            copyLibraryPlace.setVisible(false);
        }

        if (!showPathToJsFilePanel) {
            copyJsFilesPlace.setVisible(false);
        }

        if (!showPathToJarPanel && !showPathToJsFilePanel) {
            modulesSeparator.setVisible(false);
        }
    }

    @Override
    @Nullable
    public String getCopyJsIntoPath() {
        if (!copyJsFilesPlace.isVisible()) return null;
        return copyJsFilesPanel.getPath();
    }

    @Override
    @Nullable
    public String getCopyLibraryIntoPath() {
        if (!copyLibraryPlace.isVisible()) return null;
        return copyLibraryFilePanel.getPath();
    }

    protected void updateComponents() {
        setOKActionEnabled(!copyJsFilesPanel.hasErrors() && !copyLibraryFilePanel.hasErrors());
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }
}
