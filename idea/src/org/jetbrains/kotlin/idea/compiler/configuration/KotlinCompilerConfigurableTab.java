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
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.RawCommandLineEditor;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments;
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments;
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants;
import org.jetbrains.kotlin.config.CompilerSettings;
import org.jetbrains.kotlin.config.JSPlatform;
import org.jetbrains.kotlin.config.TargetPlatformKind;
import org.jetbrains.kotlin.idea.KotlinBundle;
import org.jetbrains.kotlin.idea.PluginStartupComponent;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.LinkedHashMap;
import java.util.Map;

public class KotlinCompilerConfigurableTab implements SearchableConfigurable, Configurable.NoScroll{
    private static final Map<String, String> moduleKindDescriptions = new LinkedHashMap<String, String>();
    private final CommonCompilerArguments commonCompilerArguments;
    private final K2JSCompilerArguments k2jsCompilerArguments;
    private final CompilerSettings compilerSettings;
    @Nullable
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
    private JPanel k2jvmPanel;
    private JPanel k2jsPanel;

    static {
        moduleKindDescriptions.put(K2JsArgumentConstants.MODULE_PLAIN, "Plain (put to global scope)");
        moduleKindDescriptions.put(K2JsArgumentConstants.MODULE_AMD, "AMD");
        moduleKindDescriptions.put(K2JsArgumentConstants.MODULE_COMMONJS, "CommonJS");
        moduleKindDescriptions.put(K2JsArgumentConstants.MODULE_UMD, "UMD (detect AMD or CommonJS if available, fallback to plain)");
    }

    public KotlinCompilerConfigurableTab(
            Project project,
            CommonCompilerArguments commonCompilerArguments,
            K2JSCompilerArguments k2jsCompilerArguments,
            CompilerSettings compilerSettings,
            @Nullable KotlinCompilerWorkspaceSettings compilerWorkspaceSettings
    ) {
        this.project = project;
        this.commonCompilerArguments = commonCompilerArguments;
        this.k2jsCompilerArguments = k2jsCompilerArguments;
        this.compilerSettings = compilerSettings;
        this.compilerWorkspaceSettings = compilerWorkspaceSettings;

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

        if (compilerWorkspaceSettings == null) {
            keepAliveCheckBox.setVisible(false);
            k2jvmPanel.setVisible(false);
        }
    }

    public void setTargetPlatform(@Nullable TargetPlatformKind<?> targetPlatform) {
        k2jsPanel.setVisible(JSPlatform.INSTANCE.equals(targetPlatform));
    }

    @SuppressWarnings("unused")
    public KotlinCompilerConfigurableTab(Project project) {
        this(project,
             KotlinCommonCompilerArgumentsHolder.getInstance(project).getSettings(),
             Kotlin2JsCompilerArgumentsHolder.getInstance(project).getSettings(),
             KotlinCompilerSettings.getInstance(project).getSettings(),
             ServiceManager.getService(project, KotlinCompilerWorkspaceSettings.class));
    }

    @SuppressWarnings("unchecked")
    private void fillModuleKindList() {
        for (String moduleKind : moduleKindDescriptions.keySet()) {
            moduleKindComboBox.addItem(moduleKind);
        }
        moduleKindComboBox.setRenderer(new ListCellRendererWrapper<String>() {
            @Override
            public void customize(JList list, String value, int index, boolean selected, boolean hasFocus) {
                setText(getModuleKindDescription(value));
            }
        });
    }

    @NotNull
    private static String getModuleKindDescription(@NotNull String moduleKind) {
        String result = moduleKindDescriptions.get(moduleKind);
        assert result != null : "Module kind " + moduleKind + " was not added to combobox, therefore it should not be here";
        return result;
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

               (compilerWorkspaceSettings != null &&
                (ComparingUtils.isModified(enablePreciseIncrementalCheckBox, compilerWorkspaceSettings.getPreciseIncrementalEnabled()) ||
                 ComparingUtils.isModified(keepAliveCheckBox, compilerWorkspaceSettings.getEnableDaemon()))) ||

               ComparingUtils.isModified(generateSourceMapsCheckBox, k2jsCompilerArguments.sourceMap) ||
               isModified(outputPrefixFile, k2jsCompilerArguments.outputPrefix) ||
               isModified(outputPostfixFile, k2jsCompilerArguments.outputPostfix) ||
               !getSelectedModuleKind().equals(getModuleKindOrDefault(k2jsCompilerArguments.moduleKind));
    }

    @NotNull
    private String getSelectedModuleKind() {
        return getModuleKindOrDefault((String) moduleKindComboBox.getSelectedItem());
    }

    @Override
    public void apply() throws ConfigurationException {
        commonCompilerArguments.suppressWarnings = generateNoWarningsCheckBox.isSelected();
        compilerSettings.setAdditionalArguments(additionalArgsOptionsField.getText());
        compilerSettings.setCopyJsLibraryFiles(copyRuntimeFilesCheckBox.isSelected());
        compilerSettings.setOutputDirectoryForJsLibraryFiles(outputDirectory.getText());

        if (compilerWorkspaceSettings != null) {
            compilerWorkspaceSettings.setPreciseIncrementalEnabled(enablePreciseIncrementalCheckBox.isSelected());

            boolean oldEnableDaemon = compilerWorkspaceSettings.getEnableDaemon();
            compilerWorkspaceSettings.setEnableDaemon(keepAliveCheckBox.isSelected());
            if (keepAliveCheckBox.isSelected() != oldEnableDaemon) {
                PluginStartupComponent.getInstance().resetAliveFlag();
            }
        }

        k2jsCompilerArguments.sourceMap = generateSourceMapsCheckBox.isSelected();
        k2jsCompilerArguments.outputPrefix = StringUtil.nullize(outputPrefixFile.getText(), true);
        k2jsCompilerArguments.outputPostfix = StringUtil.nullize(outputPostfixFile.getText(), true);
        k2jsCompilerArguments.moduleKind = getSelectedModuleKind();

        BuildManager.getInstance().clearState(project);
    }

    @NotNull
    private static String getModuleKindOrDefault(@Nullable String moduleKindId) {
        if (moduleKindId == null) {
            moduleKindId = K2JsArgumentConstants.MODULE_PLAIN;
        }
        return moduleKindId;
    }

    @Override
    public void reset() {
        generateNoWarningsCheckBox.setSelected(commonCompilerArguments.suppressWarnings);
        additionalArgsOptionsField.setText(compilerSettings.getAdditionalArguments());
        copyRuntimeFilesCheckBox.setSelected(compilerSettings.getCopyJsLibraryFiles());
        outputDirectory.setText(compilerSettings.getOutputDirectoryForJsLibraryFiles());

        if (compilerWorkspaceSettings != null) {
            enablePreciseIncrementalCheckBox.setSelected(compilerWorkspaceSettings.getPreciseIncrementalEnabled());
            keepAliveCheckBox.setSelected(compilerWorkspaceSettings.getEnableDaemon());
        }

        generateSourceMapsCheckBox.setSelected(k2jsCompilerArguments.sourceMap);
        outputPrefixFile.setText(k2jsCompilerArguments.outputPrefix);
        outputPostfixFile.setText(k2jsCompilerArguments.outputPostfix);

        moduleKindComboBox.setSelectedItem(getModuleKindOrDefault(k2jsCompilerArguments.moduleKind));
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
