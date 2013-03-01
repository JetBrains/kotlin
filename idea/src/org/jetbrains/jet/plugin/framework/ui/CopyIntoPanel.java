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

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
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

    public CopyIntoPanel(@Nullable Project project, @NotNull VirtualFile contextDirectory) {
        copyIntoField.addBrowseFolderListener(
                "Copy Into...", "Choose folder where files will be copied", project,
                FileChooserDescriptorFactory.createSingleFolderDescriptor());
        copyIntoField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(final DocumentEvent e) {
                updateComponents();
            }
        });

        copyIntoLabel.setLabelFor(copyIntoField.getTextField());
        copyIntoField.getTextField().setText(FileUIUtils.getDefaultLibraryFolder(project, contextDirectory));

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

        copyIntoLabel.setForeground(Color.BLACK);
        if (copyIntoField.isEnabled()) {
            if (copyIntoField.getText().trim().isEmpty()) {
                copyIntoLabel.setForeground(Color.RED);
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
}
