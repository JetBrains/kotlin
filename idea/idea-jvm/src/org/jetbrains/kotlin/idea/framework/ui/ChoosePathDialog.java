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

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileTextField;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jdesktop.swingx.VerticalLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

class ChoosePathDialog extends DialogWrapper {
    private final Project myProject;
    private final String defaultPath;
    private final String description;
    private TextFieldWithBrowseButton myPathField;

    public ChoosePathDialog(@Nullable Project project, @NotNull String title, @NotNull String defaultPath, @Nullable String description) {
        super(project);
        myProject = project;
        this.defaultPath = defaultPath;
        this.description = description;

        setTitle(title);
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setGap(3);

        JPanel panel = new JPanel(verticalLayout);

        if (description != null) {
            panel.add(new JLabel(description));
        }

        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        FileTextField field = FileChooserFactory.getInstance().createFileTextField(descriptor, myDisposable);
        field.getField().setColumns(25);

        myPathField = new TextFieldWithBrowseButton(field.getField());
        myPathField.addBrowseFolderListener("Choose Destination Folder", "Choose folder", myProject, descriptor);
        myPathField.setText(defaultPath);

        panel.add(myPathField);

        return panel;
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
    }

    @NotNull
    public String getPath() {
        return myPathField.getText().trim();
    }
}
