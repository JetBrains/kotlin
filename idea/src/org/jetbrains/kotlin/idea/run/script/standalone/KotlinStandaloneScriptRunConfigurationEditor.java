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

package org.jetbrains.kotlin.idea.run.script.standalone;

import com.intellij.execution.ui.AlternativeJREPanel;
import com.intellij.execution.ui.CommonJavaParametersPanel;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.PanelWithAnchor;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.parsing.KotlinParserDefinition;

import javax.swing.*;

public class KotlinStandaloneScriptRunConfigurationEditor extends SettingsEditor<KotlinStandaloneScriptRunConfiguration> implements PanelWithAnchor {
    private JPanel mainPanel;
    private CommonJavaParametersPanel commonProgramParameters;
    private AlternativeJREPanel alternativeJREPanel;
    private TextFieldWithBrowseButton chooseScriptFileTextField;
    private LabeledComponent<TextFieldWithBrowseButton> chooseScriptFileComponent;
    private JComponent anchor;

    public KotlinStandaloneScriptRunConfigurationEditor(Project project) {
        initChooseFileField(project);
        anchor = UIUtil.mergeComponentsWithAnchor(chooseScriptFileComponent, commonProgramParameters, alternativeJREPanel);
    }

    void initChooseFileField(Project project) {
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
                .withFileFilter(new Condition<VirtualFile>() {
                    @Override
                    public boolean value(VirtualFile file) {
                        return file.isDirectory() || KotlinParserDefinition.STD_SCRIPT_SUFFIX.equals(file.getExtension());
                    }
                })
                .withTreeRootVisible(true);

        chooseScriptFileTextField.addBrowseFolderListener("Choose script file", null, project, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
    }

    @Override
    protected void resetEditorFrom(KotlinStandaloneScriptRunConfiguration configuration) {
        commonProgramParameters.reset(configuration);
        String path = configuration.filePath;
        chooseScriptFileTextField.setText(path != null ? path : "");
        alternativeJREPanel.init(configuration.getAlternativeJrePath(), configuration.isAlternativeJrePathEnabled());
    }

    @Override
    protected void applyEditorTo(KotlinStandaloneScriptRunConfiguration configuration) throws ConfigurationException {
        commonProgramParameters.applyTo(configuration);
        configuration.setAlternativeJrePath(alternativeJREPanel.getPath());
        configuration.setAlternativeJrePathEnabled(alternativeJREPanel.isPathEnabled());
        configuration.filePath = chooseScriptFileTextField.getText();
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        return mainPanel;
    }

    @Override
    public JComponent getAnchor() {
        return anchor;
    }

    @Override
    public void setAnchor(JComponent anchor) {
        this.anchor = anchor;
        commonProgramParameters.setAnchor(anchor);
        alternativeJREPanel.setAnchor(anchor);
        chooseScriptFileComponent.setAnchor(anchor);
    }

    private void createUIComponents() {
        chooseScriptFileComponent = new LabeledComponent<TextFieldWithBrowseButton>();
        chooseScriptFileTextField = new TextFieldWithBrowseButton();
        chooseScriptFileComponent.setComponent(chooseScriptFileTextField);
    }
}
