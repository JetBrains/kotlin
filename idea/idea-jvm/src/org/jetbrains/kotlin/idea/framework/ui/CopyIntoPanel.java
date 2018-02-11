/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.framework.ui;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;

public class CopyIntoPanel {
    private JPanel contentPane;
    private TextFieldWithBrowseButton copyIntoField;
    private JLabel copyIntoLabel;

    private final EventDispatcher<ValidityListener> validityDispatcher = EventDispatcher.create(ValidityListener.class);

    private boolean hasErrorsState;

    public CopyIntoPanel(@Nullable Project project, @NotNull String defaultPath) {
        this(project, defaultPath, null);
    }

    public CopyIntoPanel(@Nullable Project project, @NotNull String defaultPath, @Nullable String labelText) {
        copyIntoField.addBrowseFolderListener(
                "Copy Into...", "Choose folder where files will be copied", project,
                FileChooserDescriptorFactory.createSingleFolderDescriptor());
        copyIntoField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                updateComponents();
            }
        });

        if (labelText != null) {
            String text = labelText.replace("&", "");
            int mnemonicIndex = labelText.indexOf("&");
            char mnemonicChar = mnemonicIndex != -1 && (mnemonicIndex + 1) < labelText.length() ? labelText.charAt(mnemonicIndex + 1) : 0;

            copyIntoLabel.setText(text);
            copyIntoLabel.setDisplayedMnemonic(mnemonicChar);
            copyIntoLabel.setDisplayedMnemonicIndex(mnemonicIndex);
        }
        else {
            copyIntoLabel.setVisible(false);
        }

        copyIntoLabel.setLabelFor(copyIntoField.getTextField());
        copyIntoField.getTextField().setText(defaultPath);

        copyIntoField.getTextField().setColumns(40);

        updateComponents();
    }

    public JComponent getContentPane() {
        return contentPane;
    }

    public void addValidityListener(ValidityListener listener) {
        validityDispatcher.addListener(listener);
    }

    @Nullable
    public String getPath() {
        return copyIntoField.isEnabled() ? copyIntoField.getText().trim() : null;
    }

    private void updateComponents() {
        boolean isError = false;

        copyIntoLabel.setForeground(JBColor.foreground());
        if (copyIntoField.isEnabled()) {
            if (copyIntoField.getText().trim().isEmpty()) {
                copyIntoLabel.setForeground(JBColor.red);
                isError = true;
            }
        }

        if (isError != hasErrorsState) {
            hasErrorsState = isError;
            validityDispatcher.getMulticaster().validityChanged(isError);
        }
    }

    public void setEnabled(boolean enabled) {
        copyIntoField.setEnabled(enabled);
    }

    public boolean hasErrors() {
        return hasErrorsState;
    }

    public void setLabelWidth(int width) {
        copyIntoLabel.setPreferredSize(new Dimension(width, -1));
    }
}
