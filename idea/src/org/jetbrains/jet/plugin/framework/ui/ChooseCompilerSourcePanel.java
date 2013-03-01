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

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import com.intellij.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.JetPluginUtil;
import org.jetbrains.jet.utils.KotlinPaths;
import org.jetbrains.jet.utils.PathUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class ChooseCompilerSourcePanel {
    private JRadioButton useBundledKotlinRadioButton;
    private JRadioButton useStandaloneKotlinRadioButton;
    private TextFieldWithBrowseButton kotlinStandalonePathField;
    private JPanel contentPane;

    private final EventDispatcher<ValidityListener> validityDispatcher = EventDispatcher.create(ValidityListener.class);

    private boolean hasErrorsState = false;
    private String version = null;
    private final String initialStandaloneLabelText;

    public ChooseCompilerSourcePanel(@Nullable Project project) {
        useBundledKotlinRadioButton.setText(useBundledKotlinRadioButton.getText() + " - " + JetPluginUtil.getPluginVersion());

        initialStandaloneLabelText = useStandaloneKotlinRadioButton.getText();

        kotlinStandalonePathField.setEditable(false);
        kotlinStandalonePathField.addBrowseFolderListener(
                "Kotlin Compiler", "Choose folder with Kotlin compiler installation", project,
                new FileChooserDescriptor(false, true, false, false, false, false) {
                    @Override
                    public boolean isFileSelectable(VirtualFile file) {
                        if (!super.isFileSelectable(file)) {
                            return false;
                        }

                        return PathUtil.KOTLIN_HOME_DIRECTORY_ADAPTER.fun(com.intellij.util.PathUtil.getLocalPath(file)) != null;
                    }
                });

        kotlinStandalonePathField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(final DocumentEvent e) {
                updateStandaloneVersion();
                updateComponents();
            }
        });

        useStandaloneKotlinRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(@NotNull ActionEvent e) {
                updateComponents();
            }
        });
        useBundledKotlinRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(@NotNull ActionEvent e) {
                updateComponents();
            }
        });

        updateStandaloneVersion();
        updateComponents();
    }

    public void addValidityListener(ValidityListener listener) {
        validityDispatcher.addListener(listener);
    }

    public boolean hasErrors() {
        return hasErrorsState;
    }

    @Nullable
    public String getStandaloneCompilerPath() {
        if (useStandaloneKotlinRadioButton.isSelected()) {
            return kotlinStandalonePathField.getText().trim();
        }

        return null;
    }

    @NotNull
    public String getVersion() {
        if (useBundledKotlinRadioButton.isSelected()) {
            return JetPluginUtil.getPluginVersion();
        }
        else {
            assert version != null: "It shouldn't be possible to finish dialog with invalid version";
            return version;
        }
    }

    public JPanel getContentPane() {
        return contentPane;
    }

    private void updateComponents() {
        boolean isError = false;

        kotlinStandalonePathField.setEnabled(useStandaloneKotlinRadioButton.isSelected());

        useStandaloneKotlinRadioButton.setForeground(Color.BLACK);
        useStandaloneKotlinRadioButton.setText(initialStandaloneLabelText);
        if (useStandaloneKotlinRadioButton.isSelected()) {
            if (version != null) {
                useStandaloneKotlinRadioButton.setText(initialStandaloneLabelText + " - " + version);
            }
            else {
                useStandaloneKotlinRadioButton.setForeground(Color.RED);
                useStandaloneKotlinRadioButton.setText(initialStandaloneLabelText + " - " + "Invalid Version");
                isError = true;
            }
        }

        if (hasErrorsState != isError) {
            hasErrorsState = isError;
            validityDispatcher.getMulticaster().validityChanged(isError);
        }
    }

    private void updateStandaloneVersion() {
        if (useStandaloneKotlinRadioButton.isSelected()) {
            KotlinPaths paths = PathUtil.getKotlinStandaloneCompilerPaths(kotlinStandalonePathField.getTextField().getText().trim());
            try {
                version = FileUtilRt.loadFile(paths.getBuildVersionFile());
                return;
            }
            catch (IOException e) {
                // Do nothing. Version will be set to null.
            }
        }

        version = null;
    }
}
