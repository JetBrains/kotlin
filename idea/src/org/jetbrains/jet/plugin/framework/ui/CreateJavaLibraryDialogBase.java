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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class CreateJavaLibraryDialogBase extends DialogWrapper {
    protected JPanel contentPane;
    protected JLabel compilerTextLabel;
    protected JPanel chooseModulesPanelPlace;
    protected SeparatorWithText modulesSeparator;
    protected JPanel chooseLibraryPathPlace;

    protected final ChooseLibraryPathPanel pathPanel;

    public CreateJavaLibraryDialogBase(
            @Nullable Project project,
            @NotNull String defaultPath
    ) {
        super(project);

        setTitle("Create Kotlin Java Runtime Library");

        init();

        compilerTextLabel.setText(compilerTextLabel.getText() + " - " + JetPluginUtil.getPluginVersion());

        pathPanel = new ChooseLibraryPathPanel(defaultPath);
        pathPanel.addValidityListener(new ValidityListener() {
            @Override
            public void validityChanged(boolean isValid) {
                updateComponents();
            }
        });
        pathPanel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateComponents();
            }
        });
        chooseLibraryPathPlace.add(pathPanel.getContentPane(), BorderLayout.CENTER);

        modulesSeparator.setCaption("Kotlin Java Runtime Library");
    }

    protected void updateComponents() {
        setOKActionEnabled(!pathPanel.hasErrors());
    }

    @Nullable
    public String getCopyIntoPath() {
        return chooseLibraryPathPlace.isVisible() ? pathPanel.getPath() : null;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

}
