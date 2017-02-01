/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import com.intellij.openapi.roots.ex.ProjectRootManagerEx;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.RawCommandLineEditor;
import com.intellij.util.text.VersionComparatorUtil;
import com.intellij.util.ui.ThreeStateCheckBox;
import kotlin.collections.ArraysKt;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments;
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants;
import org.jetbrains.kotlin.config.*;
import org.jetbrains.kotlin.idea.KotlinBundle;
import org.jetbrains.kotlin.idea.PluginStartupComponent;
import org.jetbrains.kotlin.idea.facet.DescriptionListCellRenderer;
import org.jetbrains.kotlin.idea.util.application.ApplicationUtilsKt;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class KotlinCompilerConfigurableTab implements SearchableConfigurable, Configurable.NoScroll{
    private static final Map<String, String> moduleKindDescriptions = new LinkedHashMap<String, String>();

    static {
        moduleKindDescriptions.put(K2JsArgumentConstants.MODULE_PLAIN, "Plain (put to global scope)");
        moduleKindDescriptions.put(K2JsArgumentConstants.MODULE_AMD, "AMD");
        moduleKindDescriptions.put(K2JsArgumentConstants.MODULE_COMMONJS, "CommonJS");
        moduleKindDescriptions.put(K2JsArgumentConstants.MODULE_UMD, "UMD (detect AMD or CommonJS if available, fallback to plain)");
    }

    private final CommonCompilerArguments commonCompilerArguments;
    private final K2JSCompilerArguments k2jsCompilerArguments;
    private final K2JVMCompilerArguments k2jvmCompilerArguments;
    private final CompilerSettings compilerSettings;
    @Nullable
    private final KotlinCompilerWorkspaceSettings compilerWorkspaceSettings;
    private final Project project;
    private final boolean isProjectSettings;
    private JPanel contentPane;
    private ThreeStateCheckBox generateNoWarningsCheckBox;
    private RawCommandLineEditor additionalArgsOptionsField;
    private JLabel additionalArgsLabel;
    private ThreeStateCheckBox generateSourceMapsCheckBox;
    private TextFieldWithBrowseButton outputPrefixFile;
    private TextFieldWithBrowseButton outputPostfixFile;
    private JLabel labelForOutputPrefixFile;
    private JLabel labelForOutputPostfixFile;
    private JLabel labelForOutputDirectory;
    private JTextField outputDirectory;
    private ThreeStateCheckBox copyRuntimeFilesCheckBox;
    private ThreeStateCheckBox keepAliveCheckBox;
    private JCheckBox enablePreciseIncrementalCheckBox;
    private JComboBox moduleKindComboBox;
    private JTextField scriptTemplatesField;
    private JTextField scriptTemplatesClasspathField;
    private JLabel scriptTemplatesLabel;
    private JLabel scriptTemplatesClasspathLabel;
    private JPanel k2jvmPanel;
    private JPanel k2jsPanel;
    private JComboBox jvmVersionComboBox;
    private JComboBox languageVersionComboBox;
    private JPanel languageVersionPanel;
    private JComboBox coroutineSupportComboBox;
    private JPanel apiVersionPanel;
    private JComboBox apiVersionComboBox;

    public KotlinCompilerConfigurableTab(
            Project project,
            CommonCompilerArguments commonCompilerArguments,
            K2JSCompilerArguments k2jsCompilerArguments,
            CompilerSettings compilerSettings,
            @Nullable KotlinCompilerWorkspaceSettings compilerWorkspaceSettings,
            @Nullable K2JVMCompilerArguments k2jvmCompilerArguments,
            boolean isProjectSettings,
            boolean isMultiEditor
    ) {
        this.project = project;
        this.commonCompilerArguments = commonCompilerArguments;
        this.k2jsCompilerArguments = k2jsCompilerArguments;
        this.compilerSettings = compilerSettings;
        this.compilerWorkspaceSettings = compilerWorkspaceSettings;
        this.k2jvmCompilerArguments = k2jvmCompilerArguments;
        this.isProjectSettings = isProjectSettings;

        languageVersionComboBox.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        restrictAPIVersions();
                    }
                }
        );

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
        fillJvmVersionList();
        fillLanguageAndAPIVersionList();
        fillCoroutineSupportList();

        if (compilerWorkspaceSettings == null) {
            keepAliveCheckBox.setVisible(false);
            k2jvmPanel.setVisible(false);
        }

        generateNoWarningsCheckBox.setThirdStateEnabled(isMultiEditor);
        generateSourceMapsCheckBox.setThirdStateEnabled(isMultiEditor);
        copyRuntimeFilesCheckBox.setThirdStateEnabled(isMultiEditor);
        keepAliveCheckBox.setThirdStateEnabled(isMultiEditor);
    }

    @SuppressWarnings("unused")
    public KotlinCompilerConfigurableTab(Project project) {
        this(project,
             KotlinCommonCompilerArgumentsHolder.getInstance(project).getSettings(),
             Kotlin2JsCompilerArgumentsHolder.getInstance(project).getSettings(),
             KotlinCompilerSettings.getInstance(project).getSettings(),
             ServiceManager.getService(project, KotlinCompilerWorkspaceSettings.class),
             Kotlin2JvmCompilerArgumentsHolder.getInstance(project).getSettings(),
             true,
             false);
    }

    @NotNull
    private static String getModuleKindDescription(@NotNull String moduleKind) {
        String result = moduleKindDescriptions.get(moduleKind);
        assert result != null : "Module kind " + moduleKind + " was not added to combobox, therefore it should not be here";
        return result;
    }

    @NotNull
    private static String getModuleKindOrDefault(@Nullable String moduleKindId) {
        if (moduleKindId == null) {
            moduleKindId = K2JsArgumentConstants.MODULE_PLAIN;
        }
        return moduleKindId;
    }

    private static String getJvmVersionOrDefault(@Nullable String jvmVersion) {
        return jvmVersion != null ? jvmVersion : JvmTarget.DEFAULT.getDescription();
    }

    private static String getLanguageVersionOrDefault(@Nullable String languageVersion) {
        return languageVersion != null ? languageVersion : LanguageVersion.LATEST.getVersionString();
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

    @SuppressWarnings("unchecked")
    private void restrictAPIVersions() {
        String selectedAPIVersion = getSelectedAPIVersion();
        final String selectedLanguageVersion = getSelectedLanguageVersion();
        List<String> permittedAPIVersions = ArraysKt.mapNotNull(
                LanguageVersion.values(),
                new Function1<LanguageVersion, String>() {
                    @Override
                    public String invoke(LanguageVersion version) {
                        return VersionComparatorUtil.compare(version.getVersionString(), selectedLanguageVersion) <= 0
                               ? version.getVersionString()
                               : null;
                    }
                }
        );
        apiVersionComboBox.setModel(
                new DefaultComboBoxModel(permittedAPIVersions.toArray())
        );
        apiVersionComboBox.setSelectedItem(
                VersionComparatorUtil.compare(selectedAPIVersion, selectedLanguageVersion) <= 0
                ? selectedAPIVersion
                : selectedLanguageVersion
        );
    }

    @SuppressWarnings("unchecked")
    private void fillJvmVersionList() {
        for (TargetPlatformKind.Jvm jvm : TargetPlatformKind.Jvm.Companion.getJVM_PLATFORMS()) {
            jvmVersionComboBox.addItem(jvm.getVersion().getDescription());
        }
    }

    @SuppressWarnings("unchecked")
    private void fillLanguageAndAPIVersionList() {
        for (LanguageVersion version : LanguageVersion.values()) {
            languageVersionComboBox.addItem(version.getDescription());
            apiVersionComboBox.addItem(version.getDescription());
        }
    }

    @SuppressWarnings("unchecked")
    private void fillCoroutineSupportList() {
        for (CoroutineSupport coroutineSupport : CoroutineSupport.values()) {
            coroutineSupportComboBox.addItem(coroutineSupport);
        }
        coroutineSupportComboBox.setRenderer(new DescriptionListCellRenderer());
    }

    public void setTargetPlatform(@Nullable TargetPlatformKind<?> targetPlatform) {
        k2jsPanel.setVisible(TargetPlatformKind.JavaScript.INSTANCE.equals(targetPlatform));
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
               !getSelectedLanguageVersion().equals(getLanguageVersionOrDefault(commonCompilerArguments.languageVersion)) ||
               !getSelectedAPIVersion().equals(getLanguageVersionOrDefault(commonCompilerArguments.apiVersion)) ||
               !coroutineSupportComboBox.getSelectedItem().equals(CoroutineSupport.byCompilerArguments(commonCompilerArguments)) ||
               ComparingUtils.isModified(additionalArgsOptionsField, compilerSettings.additionalArguments) ||
               ComparingUtils.isModified(scriptTemplatesField, compilerSettings.scriptTemplates) ||
               ComparingUtils.isModified(scriptTemplatesClasspathField, compilerSettings.scriptTemplatesClasspath) ||
               ComparingUtils.isModified(copyRuntimeFilesCheckBox, compilerSettings.copyJsLibraryFiles) ||
               ComparingUtils.isModified(outputDirectory, compilerSettings.outputDirectoryForJsLibraryFiles) ||

               (compilerWorkspaceSettings != null &&
                (ComparingUtils.isModified(enablePreciseIncrementalCheckBox, compilerWorkspaceSettings.getPreciseIncrementalEnabled()) ||
                 ComparingUtils.isModified(keepAliveCheckBox, compilerWorkspaceSettings.getEnableDaemon()))) ||

               ComparingUtils.isModified(generateSourceMapsCheckBox, k2jsCompilerArguments.sourceMap) ||
               isModified(outputPrefixFile, k2jsCompilerArguments.outputPrefix) ||
               isModified(outputPostfixFile, k2jsCompilerArguments.outputPostfix) ||
               !getSelectedModuleKind().equals(getModuleKindOrDefault(k2jsCompilerArguments.moduleKind)) ||
               (k2jvmCompilerArguments != null && !getSelectedJvmVersion().equals(getJvmVersionOrDefault(k2jvmCompilerArguments.jvmTarget)));
    }

    @NotNull
    private String getSelectedModuleKind() {
        return getModuleKindOrDefault((String) moduleKindComboBox.getSelectedItem());
    }

    @NotNull
    private String getSelectedJvmVersion() {
        return getJvmVersionOrDefault((String) jvmVersionComboBox.getSelectedItem());
    }

    @NotNull
    private String getSelectedLanguageVersion() {
        return getLanguageVersionOrDefault((String) languageVersionComboBox.getSelectedItem());
    }

    @NotNull
    private String getSelectedAPIVersion() {
        return getLanguageVersionOrDefault((String) apiVersionComboBox.getSelectedItem());
    }

    @Override
    public void apply() throws ConfigurationException {
        if (isProjectSettings) {
            boolean shouldInvalidateCaches =
                    commonCompilerArguments.languageVersion != getSelectedLanguageVersion() ||
                    commonCompilerArguments.apiVersion != getSelectedAPIVersion() ||
                    !coroutineSupportComboBox.getSelectedItem().equals(CoroutineSupport.byCompilerArguments(commonCompilerArguments));

            if (shouldInvalidateCaches) {
                ApplicationUtilsKt.runWriteAction(
                        new Function0<Object>() {
                            @Override
                            public Object invoke() {
                                ProjectRootManagerEx.getInstanceEx(project).makeRootsChange(EmptyRunnable.INSTANCE, false, true);
                                return null;
                            }
                        }
                );
            }
        }

        commonCompilerArguments.suppressWarnings = generateNoWarningsCheckBox.isSelected();
        commonCompilerArguments.languageVersion = getSelectedLanguageVersion();
        commonCompilerArguments.apiVersion = getSelectedAPIVersion();
        CoroutineSupport coroutineSupport = (CoroutineSupport) coroutineSupportComboBox.getSelectedItem();
        commonCompilerArguments.coroutinesEnable = coroutineSupport == CoroutineSupport.ENABLED;
        commonCompilerArguments.coroutinesWarn = coroutineSupport == CoroutineSupport.ENABLED_WITH_WARNING;
        commonCompilerArguments.coroutinesError = coroutineSupport == CoroutineSupport.DISABLED;
        compilerSettings.additionalArguments = additionalArgsOptionsField.getText();
        compilerSettings.scriptTemplates = scriptTemplatesField.getText();
        compilerSettings.scriptTemplatesClasspath = scriptTemplatesClasspathField.getText();
        compilerSettings.copyJsLibraryFiles = copyRuntimeFilesCheckBox.isSelected();
        compilerSettings.outputDirectoryForJsLibraryFiles = outputDirectory.getText();

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

        if (k2jvmCompilerArguments != null) {
            k2jvmCompilerArguments.jvmTarget = getSelectedJvmVersion();
        }

        BuildManager.getInstance().clearState(project);
    }

    @Override
    public void reset() {
        generateNoWarningsCheckBox.setSelected(commonCompilerArguments.suppressWarnings);
        languageVersionComboBox.setSelectedItem(getLanguageVersionOrDefault(commonCompilerArguments.languageVersion));
        apiVersionComboBox.setSelectedItem(getLanguageVersionOrDefault(commonCompilerArguments.apiVersion));
        restrictAPIVersions();
        coroutineSupportComboBox.setSelectedItem(CoroutineSupport.byCompilerArguments(commonCompilerArguments));
        additionalArgsOptionsField.setText(compilerSettings.additionalArguments);
        scriptTemplatesField.setText(compilerSettings.scriptTemplates);
        scriptTemplatesClasspathField.setText(compilerSettings.scriptTemplatesClasspath);
        copyRuntimeFilesCheckBox.setSelected(compilerSettings.copyJsLibraryFiles);
        outputDirectory.setText(compilerSettings.outputDirectoryForJsLibraryFiles);

        if (compilerWorkspaceSettings != null) {
            enablePreciseIncrementalCheckBox.setSelected(compilerWorkspaceSettings.getPreciseIncrementalEnabled());
            keepAliveCheckBox.setSelected(compilerWorkspaceSettings.getEnableDaemon());
        }

        generateSourceMapsCheckBox.setSelected(k2jsCompilerArguments.sourceMap);
        outputPrefixFile.setText(k2jsCompilerArguments.outputPrefix);
        outputPostfixFile.setText(k2jsCompilerArguments.outputPostfix);

        moduleKindComboBox.setSelectedItem(getModuleKindOrDefault(k2jsCompilerArguments.moduleKind));

        if (k2jvmCompilerArguments != null) {
            jvmVersionComboBox.setSelectedItem(getJvmVersionOrDefault(k2jvmCompilerArguments.jvmTarget));
        }
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

    public JPanel getContentPane() {
        return contentPane;
    }

    public ThreeStateCheckBox getGenerateNoWarningsCheckBox() {
        return generateNoWarningsCheckBox;
    }

    public RawCommandLineEditor getAdditionalArgsOptionsField() {
        return additionalArgsOptionsField;
    }

    public ThreeStateCheckBox getGenerateSourceMapsCheckBox() {
        return generateSourceMapsCheckBox;
    }

    public TextFieldWithBrowseButton getOutputPrefixFile() {
        return outputPrefixFile;
    }

    public TextFieldWithBrowseButton getOutputPostfixFile() {
        return outputPostfixFile;
    }

    public JTextField getOutputDirectory() {
        return outputDirectory;
    }

    public ThreeStateCheckBox getCopyRuntimeFilesCheckBox() {
        return copyRuntimeFilesCheckBox;
    }

    public ThreeStateCheckBox getKeepAliveCheckBox() {
        return keepAliveCheckBox;
    }

    public JComboBox getModuleKindComboBox() {
        return moduleKindComboBox;
    }

    public JTextField getScriptTemplatesField() {
        return scriptTemplatesField;
    }

    public JTextField getScriptTemplatesClasspathField() {
        return scriptTemplatesClasspathField;
    }

    public JComboBox getLanguageVersionComboBox() {
        return languageVersionComboBox;
    }

    public JComboBox getApiVersionComboBox() {
        return apiVersionComboBox;
    }

    public JComboBox getCoroutineSupportComboBox() {
        return coroutineSupportComboBox;
    }
}
