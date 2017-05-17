/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
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
import com.intellij.util.ui.UIUtil;
import kotlin.collections.ArraysKt;
import kotlin.collections.CollectionsKt;
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
import org.jetbrains.kotlin.idea.actions.internal.KotlinInternalMode;
import org.jetbrains.kotlin.idea.facet.DescriptionListCellRenderer;
import org.jetbrains.kotlin.idea.facet.KotlinFacet;
import org.jetbrains.kotlin.idea.util.application.ApplicationUtilsKt;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class KotlinCompilerConfigurableTab implements SearchableConfigurable, Configurable.NoScroll{
    private static final Map<String, String> moduleKindDescriptions = new LinkedHashMap<String, String>();
    private static final List<LanguageFeature.State> languageFeatureStates = Arrays.asList(
            LanguageFeature.State.ENABLED, LanguageFeature.State.ENABLED_WITH_WARNING, LanguageFeature.State.ENABLED_WITH_ERROR
    );
    private static final int MAX_WARNING_SIZE = 75;

    static {
        moduleKindDescriptions.put(K2JsArgumentConstants.MODULE_PLAIN, "Plain (put to global scope)");
        moduleKindDescriptions.put(K2JsArgumentConstants.MODULE_AMD, "AMD");
        moduleKindDescriptions.put(K2JsArgumentConstants.MODULE_COMMONJS, "CommonJS");
        moduleKindDescriptions.put(K2JsArgumentConstants.MODULE_UMD, "UMD (detect AMD or CommonJS if available, fallback to plain)");
    }

    @Nullable
    private final KotlinCompilerWorkspaceSettings compilerWorkspaceSettings;
    private final Project project;
    private final boolean isProjectSettings;
    private CommonCompilerArguments commonCompilerArguments;
    private K2JSCompilerArguments k2jsCompilerArguments;
    private K2JVMCompilerArguments k2jvmCompilerArguments;
    private CompilerSettings compilerSettings;
    private JPanel contentPane;
    private ThreeStateCheckBox reportWarningsCheckBox;
    private RawCommandLineEditor additionalArgsOptionsField;
    private JLabel additionalArgsLabel;
    private ThreeStateCheckBox generateSourceMapsCheckBox;
    private TextFieldWithBrowseButton outputPrefixFile;
    private TextFieldWithBrowseButton outputPostfixFile;
    private JLabel labelForOutputDirectory;
    private TextFieldWithBrowseButton outputDirectory;
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
    private JComboBox coroutineSupportComboBox;
    private JComboBox apiVersionComboBox;
    private JPanel scriptPanel;
    private JLabel labelForOutputPrefixFile;
    private JLabel labelForOutputPostfixFile;
    private JLabel warningLabel;
    private boolean isEnabled = true;

    public KotlinCompilerConfigurableTab(
            Project project,
            @NotNull CommonCompilerArguments commonCompilerArguments,
            @NotNull K2JSCompilerArguments k2jsCompilerArguments,
            @NotNull K2JVMCompilerArguments k2jvmCompilerArguments, CompilerSettings compilerSettings,
            @Nullable KotlinCompilerWorkspaceSettings compilerWorkspaceSettings,
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

        if (isProjectSettings) {
            languageVersionComboBox.addActionListener(
                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            restrictAPIVersions(getSelectedLanguageVersion());
                        }
                    }
            );
        }

        additionalArgsOptionsField.attachLabel(additionalArgsLabel);

        setupFileChooser(labelForOutputPrefixFile, outputPrefixFile,
                         KotlinBundle.message("kotlin.compiler.js.option.output.prefix.browse.title"),
                         true);
        setupFileChooser(labelForOutputPostfixFile, outputPostfixFile,
                         KotlinBundle.message("kotlin.compiler.js.option.output.postfix.browse.title"),
                         true);
        setupFileChooser(labelForOutputDirectory, outputDirectory,
                         "Choose Output Directory",
                         false);

        fillModuleKindList();
        fillJvmVersionList();
        fillLanguageAndAPIVersionList();
        fillCoroutineSupportList();

        if (compilerWorkspaceSettings == null) {
            keepAliveCheckBox.setVisible(false);
            k2jvmPanel.setVisible(false);
        }

        reportWarningsCheckBox.setThirdStateEnabled(isMultiEditor);
        generateSourceMapsCheckBox.setThirdStateEnabled(isMultiEditor);
        copyRuntimeFilesCheckBox.setThirdStateEnabled(isMultiEditor);
        keepAliveCheckBox.setThirdStateEnabled(isMultiEditor);

        if (isProjectSettings) {
            List<String> modulesOverridingProjectSettings = ArraysKt.mapNotNull(
                    ModuleManager.getInstance(project).getModules(),
                    new Function1<Module, String>() {
                        @Override
                        public String invoke(Module module) {
                            KotlinFacet facet = KotlinFacet.Companion.get(module);
                            if (facet == null) return null;
                            KotlinFacetSettings facetSettings = facet.getConfiguration().getSettings();
                            if (facetSettings.getUseProjectSettings()) return null;
                            return module.getName();
                        }
                    }
            );
            CollectionsKt.sort(modulesOverridingProjectSettings);
            if (!modulesOverridingProjectSettings.isEmpty()) {
                warningLabel.setVisible(true);
                warningLabel.setText(buildOverridingModulesWarning(modulesOverridingProjectSettings));
            }
        }

        updateOutputDirEnabled();
    }

    @SuppressWarnings("unused")
    public KotlinCompilerConfigurableTab(Project project) {
        this(project,
             KotlinCommonCompilerArgumentsHolder.Companion.getInstance(project).getSettings(),
             Kotlin2JsCompilerArgumentsHolder.Companion.getInstance(project).getSettings(),
             Kotlin2JvmCompilerArgumentsHolder.Companion.getInstance(project).getSettings(),
             KotlinCompilerSettings.Companion.getInstance(project).getSettings(),
             ServiceManager.getService(project, KotlinCompilerWorkspaceSettings.class),
             true,
             false);
    }

    private static int calculateNameCountToShowInWarning(List<String> allNames) {
        int lengthSoFar = 0;
        int size = allNames.size();
        for (int i = 0; i < size; i++) {
            lengthSoFar = (i > 0 ? lengthSoFar + 2 : 0) + allNames.get(i).length();
            if (lengthSoFar > MAX_WARNING_SIZE) return i;
        }
        return size;
    }

    @NotNull
    private static String buildOverridingModulesWarning(List<String> modulesOverridingProjectSettings) {
        int nameCountToShow = calculateNameCountToShowInWarning(modulesOverridingProjectSettings);
        int allNamesCount = modulesOverridingProjectSettings.size();
        if (nameCountToShow == 0) {
            return String.valueOf(allNamesCount) + " modules override project settings";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("<html>Following modules override project settings: ");
        CollectionsKt.joinTo(
                modulesOverridingProjectSettings.subList(0, nameCountToShow),
                builder,
                ", ",
                "",
                "",
                -1,
                "",
                new Function1<String, CharSequence>() {
                    @Override
                    public CharSequence invoke(String s) {
                        return "<strong>" + s + "</strong>";
                    }
                }
        );
        if (nameCountToShow < allNamesCount) {
            builder.append(" and ").append(allNamesCount - nameCountToShow).append(" other(s)");
        }
        return builder.toString();
    }

    @NotNull
    private static String getModuleKindDescription(@Nullable String moduleKind) {
        if (moduleKind == null) return "";
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

    private static LanguageVersion getLanguageVersionOrDefault(@Nullable String languageVersion) {
        LanguageVersion version = LanguageVersion.fromVersionString(languageVersion);
        return version != null ? version : LanguageVersion.LATEST_STABLE;
    }

    private static ApiVersion getApiVersionOrDefault(@Nullable String apiVersion) {
        return apiVersion != null ? ApiVersion.Companion.parse(apiVersion) : ApiVersion.LATEST_STABLE;
    }

    private static void setupFileChooser(
            @NotNull JLabel label,
            @NotNull TextFieldWithBrowseButton fileChooser,
            @NotNull String title,
            boolean forFiles
    ) {
        label.setLabelFor(fileChooser);

        fileChooser.addBrowseFolderListener(title, null, null,
                                            new FileChooserDescriptor(forFiles, !forFiles, false, false, false, false),
                                            TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT, false);
    }

    private static boolean isModified(@NotNull TextFieldWithBrowseButton chooser, @Nullable String currentValue) {
        return !StringUtil.equals(StringUtil.nullize(chooser.getText(), true), currentValue);
    }

    private void updateOutputDirEnabled() {
        if (isEnabled && copyRuntimeFilesCheckBox != null) {
            outputDirectory.setEnabled(copyRuntimeFilesCheckBox.isSelected());
            labelForOutputDirectory.setEnabled(copyRuntimeFilesCheckBox.isSelected());
        }
    }

    @SuppressWarnings("unchecked")
    public void restrictAPIVersions(LanguageVersion upperBound) {
        ApiVersion selectedAPIVersion = getSelectedAPIVersion();
        List<ApiVersion> permittedAPIVersions = ArraysKt.mapNotNull(
                LanguageVersion.values(),
                version -> VersionComparatorUtil.compare(version.getVersionString(), upperBound.getVersionString()) <= 0 &&
                           VersionComparatorUtil.compare(version.getVersionString(), upperBound.getVersionString()) <= 0
                       ? ApiVersion.createByLanguageVersion(version)
                       : null
        );
        apiVersionComboBox.setModel(
                new DefaultComboBoxModel(permittedAPIVersions.toArray())
        );
        apiVersionComboBox.setSelectedItem(
                VersionComparatorUtil.compare(selectedAPIVersion.getVersionString(), upperBound.getVersionString()) <= 0
                ? selectedAPIVersion
                : upperBound
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
            if (!version.isStable() && !KotlinInternalMode.Instance.getEnabled()) {
                continue;
            }

            languageVersionComboBox.addItem(version);
            apiVersionComboBox.addItem(ApiVersion.createByLanguageVersion(version));
        }
        languageVersionComboBox.setRenderer(new DescriptionListCellRenderer());
        apiVersionComboBox.setRenderer(new DescriptionListCellRenderer());
    }

    @SuppressWarnings("unchecked")
    private void fillCoroutineSupportList() {
        for (LanguageFeature.State coroutineSupport : languageFeatureStates) {
            coroutineSupportComboBox.addItem(coroutineSupport);
        }
        coroutineSupportComboBox.setRenderer(new DescriptionListCellRenderer());
    }

    public void setTargetPlatform(@Nullable TargetPlatformKind<?> targetPlatform) {
        k2jsPanel.setVisible(TargetPlatformKind.JavaScript.INSTANCE.equals(targetPlatform));
        scriptPanel.setVisible(targetPlatform instanceof TargetPlatformKind.Jvm);
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
        return ComparingUtils.isModified(reportWarningsCheckBox, !commonCompilerArguments.suppressWarnings) ||
               !getSelectedLanguageVersion().equals(getLanguageVersionOrDefault(commonCompilerArguments.languageVersion)) ||
               !getSelectedAPIVersion().equals(getApiVersionOrDefault(commonCompilerArguments.apiVersion)) ||
               !coroutineSupportComboBox.getSelectedItem().equals(CoroutineSupport.byCompilerArguments(commonCompilerArguments)) ||
               ComparingUtils.isModified(additionalArgsOptionsField, compilerSettings.additionalArguments) ||
               ComparingUtils.isModified(scriptTemplatesField, compilerSettings.scriptTemplates) ||
               ComparingUtils.isModified(scriptTemplatesClasspathField, compilerSettings.scriptTemplatesClasspath) ||
               ComparingUtils.isModified(copyRuntimeFilesCheckBox, compilerSettings.copyJsLibraryFiles) ||
               isModified(outputDirectory, compilerSettings.outputDirectoryForJsLibraryFiles) ||

               (compilerWorkspaceSettings != null &&
                (ComparingUtils.isModified(enablePreciseIncrementalCheckBox, compilerWorkspaceSettings.getPreciseIncrementalEnabled()) ||
                 ComparingUtils.isModified(keepAliveCheckBox, compilerWorkspaceSettings.getEnableDaemon()))) ||

               ComparingUtils.isModified(generateSourceMapsCheckBox, k2jsCompilerArguments.sourceMap) ||
               ComparingUtils.isModified(outputPrefixFile, k2jsCompilerArguments.outputPrefix) ||
               ComparingUtils.isModified(outputPostfixFile, k2jsCompilerArguments.outputPostfix) ||
               !getSelectedModuleKind().equals(getModuleKindOrDefault(k2jsCompilerArguments.moduleKind)) ||

               !getSelectedJvmVersion().equals(getJvmVersionOrDefault(k2jvmCompilerArguments.jvmTarget));
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
    public LanguageVersion getSelectedLanguageVersion() {
        Object item = languageVersionComboBox.getSelectedItem();
        return item != null ? (LanguageVersion) item : LanguageVersion.LATEST_STABLE;
    }

    @NotNull
    private ApiVersion getSelectedAPIVersion() {
        Object item = apiVersionComboBox.getSelectedItem();
        return item != null ? (ApiVersion) item : ApiVersion.LATEST_STABLE;
    }

    public void applyTo(
            CommonCompilerArguments commonCompilerArguments,
            K2JVMCompilerArguments k2jvmCompilerArguments,
            K2JSCompilerArguments k2jsCompilerArguments,
            CompilerSettings compilerSettings
    ) throws ConfigurationException {
        if (isProjectSettings) {
            boolean shouldInvalidateCaches =
                    commonCompilerArguments.languageVersion != getSelectedLanguageVersion().getVersionString() ||
                    commonCompilerArguments.apiVersion != getSelectedAPIVersion().getVersionString() ||
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

        commonCompilerArguments.suppressWarnings = !reportWarningsCheckBox.isSelected();
        commonCompilerArguments.languageVersion = getSelectedLanguageVersion().getVersionString();
        commonCompilerArguments.apiVersion = getSelectedAPIVersion().getVersionString();

        switch ((LanguageFeature.State) coroutineSupportComboBox.getSelectedItem()) {
            case ENABLED:
                commonCompilerArguments.coroutinesState = CommonCompilerArguments.ENABLE;
                break;
            case ENABLED_WITH_WARNING:
                commonCompilerArguments.coroutinesState = CommonCompilerArguments.WARN;
                break;
            case ENABLED_WITH_ERROR:
            case DISABLED:
                commonCompilerArguments.coroutinesState = CommonCompilerArguments.ERROR;
                break;
        }

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

        k2jvmCompilerArguments.jvmTarget = getSelectedJvmVersion();

        if (isProjectSettings) {
            KotlinCommonCompilerArgumentsHolder.Companion.getInstance(project).setSettings(commonCompilerArguments);
            Kotlin2JvmCompilerArgumentsHolder.Companion.getInstance(project).setSettings(k2jvmCompilerArguments);
            Kotlin2JsCompilerArgumentsHolder.Companion.getInstance(project).setSettings(k2jsCompilerArguments);
            KotlinCompilerSettings.Companion.getInstance(project).setSettings(compilerSettings);
        }

        BuildManager.getInstance().clearState(project);
    }

    @Override
    public void apply() throws ConfigurationException {
        applyTo(commonCompilerArguments, k2jvmCompilerArguments, k2jsCompilerArguments, compilerSettings);
    }

    @Override
    public void reset() {
        reportWarningsCheckBox.setSelected(!commonCompilerArguments.suppressWarnings);
        languageVersionComboBox.setSelectedItem(getLanguageVersionOrDefault(commonCompilerArguments.languageVersion));
        apiVersionComboBox.setSelectedItem(getApiVersionOrDefault(commonCompilerArguments.apiVersion));
        restrictAPIVersions(getSelectedLanguageVersion());
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

        jvmVersionComboBox.setSelectedItem(getJvmVersionOrDefault(k2jvmCompilerArguments.jvmTarget));
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

    public ThreeStateCheckBox getReportWarningsCheckBox() {
        return reportWarningsCheckBox;
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

    public TextFieldWithBrowseButton getOutputDirectory() {
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

    public void setEnabled(boolean value) {
        isEnabled = value;
        UIUtil.setEnabled(getContentPane(), value, true);
    }

    public CommonCompilerArguments getCommonCompilerArguments() {
        return commonCompilerArguments;
    }

    public void setCommonCompilerArguments(CommonCompilerArguments commonCompilerArguments) {
        this.commonCompilerArguments = commonCompilerArguments;
    }

    public K2JSCompilerArguments getK2jsCompilerArguments() {
        return k2jsCompilerArguments;
    }

    public void setK2jsCompilerArguments(K2JSCompilerArguments k2jsCompilerArguments) {
        this.k2jsCompilerArguments = k2jsCompilerArguments;
    }

    public K2JVMCompilerArguments getK2jvmCompilerArguments() {
        return k2jvmCompilerArguments;
    }

    public void setK2jvmCompilerArguments(K2JVMCompilerArguments k2jvmCompilerArguments) {
        this.k2jvmCompilerArguments = k2jvmCompilerArguments;
    }

    public CompilerSettings getCompilerSettings() {
        return compilerSettings;
    }

    public void setCompilerSettings(CompilerSettings compilerSettings) {
        this.compilerSettings = compilerSettings;
    }

    private void createUIComponents() {
        // Workaround: ThreeStateCheckBox doesn't send suitable notification on state change
        // TODO: replace with PropertyChangerListener after fix is available in IDEA
        copyRuntimeFilesCheckBox = new ThreeStateCheckBox() {
            @Override
            public void setState(State state) {
                super.setState(state);
                updateOutputDirEnabled();
            }
        };
    }
}
