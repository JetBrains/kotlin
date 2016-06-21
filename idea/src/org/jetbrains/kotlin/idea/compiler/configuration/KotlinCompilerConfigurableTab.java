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

package org.jetbrains.kotlin.idea.compiler.configuration;

import com.intellij.compiler.options.ComparingUtils;
import com.intellij.compiler.server.BuildManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.RawCommandLineEditor;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments;
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments;
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants;
import org.jetbrains.kotlin.config.CompilerSettings;
import org.jetbrains.kotlin.idea.KotlinBundle;
import org.jetbrains.kotlin.idea.PluginStartupComponent;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.List;

import static java.util.Arrays.asList;

public class KotlinCompilerConfigurableTab implements SearchableConfigurable, Configurable.NoScroll{
    private static final List<String> moduleKindDescriptions = asList(
            "Plain (put to global scope)",
            "AMD",
            "CommonJS/node.js",
            "UMD (detect AMD or CommonJS if available, fallback to plain)"
    );
    private static final List<String> moduleKindIds = asList(K2JsArgumentConstants.MODULE_PLAIN, K2JsArgumentConstants.MODULE_AMD,
                                                             K2JsArgumentConstants.MODULE_COMMONJS, K2JsArgumentConstants.MODULE_UMD);
    private final CommonCompilerArguments commonCompilerArguments;
    private final K2JSCompilerArguments k2jsCompilerArguments;
    private final CompilerSettings compilerSettings;
    private final KotlinCompilerWorkspaceSettings compilerWorkspaceSettings;
    private final Project project;
    private JPanel contentPane;
    private JCheckBox generateNoWarningsCheckBox;
    private RawCommandLineEditor additionalArgsOptionsField;
    private JLabel additionalArgsLabel;
    private JCheckBox generateSourceMapsCheckBox;
    private TextFieldWithBrowseButton outputPrefixFile;
    private TextFieldWithBrowseButton outputPostfixFile;
    private JLabel labelForOutputPrefixFile;
    private JLabel labelForOutputPostfixFile;
    private JLabel labelForOutputDirectory;
    private JTextField outputDirectory;
    private JCheckBox copyRuntimeFilesCheckBox;
    private JCheckBox keepAliveCheckBox;
    private JCheckBox enablePreciseIncrementalCheckBox;
    private JComboBox moduleKindComboBox;

    public KotlinCompilerConfigurableTab(Project project) {
        this.commonCompilerArguments = KotlinCommonCompilerArgumentsHolder.getInstance(project).getSettings();
        this.k2jsCompilerArguments = Kotlin2JsCompilerArgumentsHolder.getInstance(project).getSettings();
        this.compilerSettings = KotlinCompilerSettings.getInstance(project).getSettings();
        this.compilerWorkspaceSettings = ServiceManager.getService(project, KotlinCompilerWorkspaceSettings.class);
        this.project = project;

        additionalArgsOptionsField.attachLabel(additionalArgsLabel);

        setupFileChooser(labelForOutputPrefixFile, outputPrefixFile,
                         KotlinBundle.message("kotlin.compiler.js.option.output.prefix.browse.title"));
        setupFileChooser(labelForOutputPostfixFile, outputPostfixFile,
                         KotlinBundle.message("kotlin.compiler.js.option.output.postfix.browse.title"));

        labelForOutputDirectory.setLabelFor(outputDirectory);
        copyRuntimeFilesCheckBox.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(@NotNull ChangeEvent e) {
                outputDirectory.setEnabled(copyRuntimeFilesCheckBox.isSelected());
                labelForOutputDirectory.setEnabled(copyRuntimeFilesCheckBox.isSelected());
            }
        });

        fillModuleKindList();
    }

    @SuppressWarnings("unchecked")
    private void fillModuleKindList() {
        for (String description : moduleKindDescriptions) {
            // TODO: not sure if it's a right way to add items to a combo box
            moduleKindComboBox.addItem(description);
        }
    }

    @NotNull
    @Override
    public String getId() {
        return "project.kotlinCompiler";
    }

    @Nullable
    @Override
    public Runnable enableSearch(String option) {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return contentPane;
    }

    @Override
    public boolean isModified() {
        return ComparingUtils.isModified(generateNoWarningsCheckBox, commonCompilerArguments.suppressWarnings) ||
               ComparingUtils.isModified(additionalArgsOptionsField, compilerSettings.getAdditionalArguments()) ||
               ComparingUtils.isModified(copyRuntimeFilesCheckBox, compilerSettings.getCopyJsLibraryFiles()) ||
               ComparingUtils.isModified(outputDirectory, compilerSettings.getOutputDirectoryForJsLibraryFiles()) ||

               ComparingUtils.isModified(enablePreciseIncrementalCheckBox, compilerWorkspaceSettings.getPreciseIncrementalEnabled()) ||
               ComparingUtils.isModified(keepAliveCheckBox, compilerWorkspaceSettings.getEnableDaemon()) ||

               ComparingUtils.isModified(generateSourceMapsCheckBox, k2jsCompilerArguments.sourceMap) ||
               isModified(outputPrefixFile, k2jsCompilerArguments.outputPrefix) ||
               isModified(outputPostfixFile, k2jsCompilerArguments.outputPostfix) ||
               !getSelectedModuleKind().equals(k2jsCompilerArguments.moduleKind);
    }

    private String getSelectedModuleKind() {
        return moduleKindIds.get(moduleKindComboBox.getSelectedIndex());
    }

    @Override
    public void apply() throws ConfigurationException {
        commonCompilerArguments.suppressWarnings = generateNoWarningsCheckBox.isSelected();
        compilerSettings.setAdditionalArguments(additionalArgsOptionsField.getText());
        compilerSettings.setCopyJsLibraryFiles(copyRuntimeFilesCheckBox.isSelected());
        compilerSettings.setOutputDirectoryForJsLibraryFiles(outputDirectory.getText());

        compilerWorkspaceSettings.setPreciseIncrementalEnabled(enablePreciseIncrementalCheckBox.isSelected());

        boolean oldEnableDaemon = compilerWorkspaceSettings.getEnableDaemon();
        compilerWorkspaceSettings.setEnableDaemon(keepAliveCheckBox.isSelected());
        if (keepAliveCheckBox.isSelected() != oldEnableDaemon) {
            PluginStartupComponent.getInstance().resetAliveFlag();
        }

        k2jsCompilerArguments.sourceMap = generateSourceMapsCheckBox.isSelected();
        k2jsCompilerArguments.outputPrefix = StringUtil.nullize(outputPrefixFile.getText(), true);
        k2jsCompilerArguments.outputPostfix = StringUtil.nullize(outputPostfixFile.getText(), true);
        k2jsCompilerArguments.moduleKind = getSelectedModuleKind();

        BuildManager.getInstance().clearState(project);
    }

    @Override
    public void reset() {
        generateNoWarningsCheckBox.setSelected(commonCompilerArguments.suppressWarnings);
        additionalArgsOptionsField.setText(compilerSettings.getAdditionalArguments());
        copyRuntimeFilesCheckBox.setSelected(compilerSettings.getCopyJsLibraryFiles());
        outputDirectory.setText(compilerSettings.getOutputDirectoryForJsLibraryFiles());

        enablePreciseIncrementalCheckBox.setSelected(compilerWorkspaceSettings.getPreciseIncrementalEnabled());
        keepAliveCheckBox.setSelected(compilerWorkspaceSettings.getEnableDaemon());

        generateSourceMapsCheckBox.setSelected(k2jsCompilerArguments.sourceMap);
        outputPrefixFile.setText(k2jsCompilerArguments.outputPrefix);
        outputPostfixFile.setText(k2jsCompilerArguments.outputPostfix);

        String moduleKind = k2jsCompilerArguments.moduleKind;
        int index = moduleKind != null ? moduleKindIds.indexOf(moduleKind) : 0;
        if (index < 0) {
            index = 0;
        }
        moduleKindComboBox.setSelectedIndex(index);
    }

    @Override
    public void disposeUIResources() {
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Kotlin Compiler";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return "reference.compiler.kotlin";
    }

    private static void setupFileChooser(
            @NotNull JLabel label,
            @NotNull TextFieldWithBrowseButton fileChooser,
            @NotNull String title
    ) {
        label.setLabelFor(fileChooser);

        fileChooser.addBrowseFolderListener(title, null, null,
                                            new FileChooserDescriptor(true, false, false, false, false, false),
                                            TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT, false);
    }

    private static boolean isModified(@NotNull TextFieldWithBrowseButton chooser, @Nullable String currentValue) {
        return !StringUtil.equals(StringUtil.nullize(chooser.getText(), true), currentValue);
    }
}
