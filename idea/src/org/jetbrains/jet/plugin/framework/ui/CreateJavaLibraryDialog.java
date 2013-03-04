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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CreateJavaLibraryDialog extends DialogWrapper {
    private final CopyIntoPanel copyIntoPanel;

    private JPanel contentPane;
    private JCheckBox copyLibraryCheckbox;
    private JPanel copyIntoPanelPlace;
    private JPanel compilerSourcePanelPlace;

    public CreateJavaLibraryDialog(@Nullable Project project, @NotNull String title, VirtualFile contextDirectory) {
        super(project);

        setTitle(title);

        init();

        ChooseCompilerSourcePanel compilerSourcePanel = new ChooseCompilerSourcePanel();
        compilerSourcePanelPlace.add(compilerSourcePanel.getContentPane(), BorderLayout.CENTER, 0);

        copyIntoPanel = new CopyIntoPanel(project, contextDirectory);
        copyIntoPanel.addValidityListener(new ValidityListener() {
            @Override
            public void validityChanged(boolean isValid) {
                updateComponents();
            }
        });
        copyIntoPanelPlace.add(copyIntoPanel.getContentPane(), BorderLayout.CENTER);

        copyLibraryCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(@NotNull ActionEvent e) {
                updateComponents();
            }
        });

        updateComponents();
    }

    @Nullable
    public String getCopyIntoPath() {
        return copyIntoPanel.getPath();
    }

    private void updateComponents() {
        copyIntoPanel.setEnabled(copyLibraryCheckbox.isSelected());
        setOKActionEnabled(!copyIntoPanel.hasErrors());
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }
}
