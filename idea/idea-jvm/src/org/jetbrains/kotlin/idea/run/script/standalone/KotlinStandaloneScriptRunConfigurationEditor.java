/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.run.script.standalone;

import com.intellij.execution.ui.CommonJavaParametersPanel;
import com.intellij.execution.ui.DefaultJreSelector;
import com.intellij.execution.ui.JrePathEditor;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
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
    private JrePathEditor jrePathEditor;
    private TextFieldWithBrowseButton chooseScriptFileTextField;
    private LabeledComponent<TextFieldWithBrowseButton> chooseScriptFileComponent;
    private JComponent anchor;

    public KotlinStandaloneScriptRunConfigurationEditor(Project project) {
        initChooseFileField(project);
        jrePathEditor.setDefaultJreSelector(DefaultJreSelector.projectSdk(project));
        anchor = UIUtil.mergeComponentsWithAnchor(chooseScriptFileComponent, commonProgramParameters, jrePathEditor);
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
        jrePathEditor.setPathOrName(configuration.getAlternativeJrePath(), configuration.isAlternativeJrePathEnabled());
    }

    @Override
    protected void applyEditorTo(KotlinStandaloneScriptRunConfiguration configuration) throws ConfigurationException {
        commonProgramParameters.applyTo(configuration);
        configuration.setAlternativeJrePath(jrePathEditor.getJrePathOrName());
        configuration.setAlternativeJrePathEnabled(jrePathEditor.isAlternativeJreSelected());
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
        jrePathEditor.setAnchor(anchor);
        chooseScriptFileComponent.setAnchor(anchor);
    }

    private void createUIComponents() {
        chooseScriptFileComponent = new LabeledComponent<TextFieldWithBrowseButton>();
        chooseScriptFileTextField = new TextFieldWithBrowseButton();
        chooseScriptFileComponent.setComponent(chooseScriptFileTextField);
    }
}
