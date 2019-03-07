/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.compiler.configuration;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
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
import org.jetbrains.kotlin.idea.facet.DescriptionListCellRenderer;
import org.jetbrains.kotlin.idea.facet.KotlinFacet;
import org.jetbrains.kotlin.idea.roots.RootUtilsKt;
import org.jetbrains.kotlin.idea.util.CidrUtil;
import org.jetbrains.kotlin.idea.util.application.ApplicationUtilsKt;
import org.jetbrains.kotlin.platform.IdePlatform;
import org.jetbrains.kotlin.platform.IdePlatformKind;
import org.jetbrains.kotlin.platform.impl.JsIdePlatformUtil;
import org.jetbrains.kotlin.platform.impl.JvmIdePlatformUtil;
import org.jetbrains.kotlin.platform.impl.JvmIdePlatformKind;
import org.jetbrains.kotlin.resolve.JvmTarget;

import javax.swing.*;
import java.util.*;

public class KotlinCompilerConfigurableTab implements SearchableConfigurable, Configurable.NoScroll{
    private static final Map<String, String> moduleKindDescriptions = new LinkedHashMap<>();
    private static final Map<String, String> soruceMapSourceEmbeddingDescriptions = new LinkedHashMap<>();
    private static final List<LanguageFeature.State> languageFeatureStates = Arrays.asList(
            LanguageFeature.State.ENABLED, LanguageFeature.State.ENABLED_WITH_WARNING, LanguageFeature.State.ENABLED_WITH_ERROR
    );
    private static final int MAX_WARNING_SIZE = 75;

    static {
        moduleKindDescriptions.put(K2JsArgumentConstants.MODULE_PLAIN, "Plain (put to global scope)");
        moduleKindDescriptions.put(K2JsArgumentConstants.MODULE_AMD, "AMD");
        moduleKindDescriptions.put(K2JsArgumentConstants.MODULE_COMMONJS, "CommonJS");
        moduleKindDescriptions.put(K2JsArgumentConstants.MODULE_UMD, "UMD (detect AMD or CommonJS if available, fallback to plain)");

        soruceMapSourceEmbeddingDescriptions.put(K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_NEVER, "Never");
        soruceMapSourceEmbeddingDescriptions.put(K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_ALWAYS, "Always");
        soruceMapSourceEmbeddingDescriptions.put(K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_INLINING,
                                                 "When inlining a function from other module with embedded sources");
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
    private JCheckBox enableIncrementalCompilationForJvmCheckBox;
    private JCheckBox enableIncrementalCompilationForJsCheckBox;
    private JComboBox moduleKindComboBox;
    private JTextField scriptTemplatesField;
    private JTextField scriptTemplatesClasspathField;
    private JLabel scriptTemplatesLabel;
    private JLabel scriptTemplatesClasspathLabel;
    private JPanel k2jvmPanel;
    private JPanel k2jsPanel;
    private JComboBox jvmVersionComboBox;
    private JComboBox<VersionView> languageVersionComboBox;
    private JComboBox coroutineSupportComboBox;
    private JComboBox<VersionView> apiVersionComboBox;
    private JPanel scriptPanel;
    private JLabel labelForOutputPrefixFile;
    private JLabel labelForOutputPostfixFile;
    private JLabel warningLabel;
    private JTextField sourceMapPrefix;
    private JLabel labelForSourceMapPrefix;
    private JComboBox sourceMapEmbedSources;
    private JPanel coroutinesPanel;
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

        warningLabel.setIcon(AllIcons.General.WarningDialog);

        if (isProjectSettings) {
            languageVersionComboBox.addActionListener(e -> onLanguageLevelChanged(getSelectedLanguageVersionView()));
        }

        additionalArgsOptionsField.attachLabel(additionalArgsLabel);

        fillLanguageAndAPIVersionList();
        fillCoroutineSupportList();

        if (CidrUtil.isRunningInCidrIde()) {
            keepAliveCheckBox.setVisible(false);
            k2jvmPanel.setVisible(false);
            k2jsPanel.setVisible(false);
        }
        else {
            initializeNonCidrSettings(isMultiEditor);
        }

        reportWarningsCheckBox.setThirdStateEnabled(isMultiEditor);

        if (isProjectSettings) {
            List<String> modulesOverridingProjectSettings = ArraysKt.mapNotNull(
                    ModuleManager.getInstance(project).getModules(),
                    module -> {
                        KotlinFacet facet = KotlinFacet.Companion.get(module);
                        if (facet == null) return null;
                        KotlinFacetSettings facetSettings = facet.getConfiguration().getSettings();
                        if (facetSettings.getUseProjectSettings()) return null;
                        return module.getName();
                    }
            );
            CollectionsKt.sort(modulesOverridingProjectSettings);
            if (!modulesOverridingProjectSettings.isEmpty()) {
                warningLabel.setVisible(true);
                warningLabel.setText(buildOverridingModulesWarning(modulesOverridingProjectSettings));
            }
        }
    }

    @SuppressWarnings("unused")
    public KotlinCompilerConfigurableTab(Project project) {
        this(project,
             (CommonCompilerArguments) KotlinCommonCompilerArgumentsHolder.Companion.getInstance(project).getSettings().unfrozen(),
             (K2JSCompilerArguments) Kotlin2JsCompilerArgumentsHolder.Companion.getInstance(project).getSettings().unfrozen(),
             (K2JVMCompilerArguments) Kotlin2JvmCompilerArgumentsHolder.Companion.getInstance(project).getSettings().unfrozen(),
             (CompilerSettings) KotlinCompilerSettings.Companion.getInstance(project).getSettings().unfrozen(),
             ServiceManager.getService(project, KotlinCompilerWorkspaceSettings.class),
             true,
             false);
    }

    private void initializeNonCidrSettings(boolean isMultiEditor) {
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
        fillSourceMapSourceEmbeddingList();
        fillJvmVersionList();

        generateSourceMapsCheckBox.setThirdStateEnabled(isMultiEditor);
        generateSourceMapsCheckBox.addActionListener(event -> sourceMapPrefix.setEnabled(generateSourceMapsCheckBox.isSelected()));

        copyRuntimeFilesCheckBox.setThirdStateEnabled(isMultiEditor);
        keepAliveCheckBox.setThirdStateEnabled(isMultiEditor);

        if (compilerWorkspaceSettings == null) {
            keepAliveCheckBox.setVisible(false);
            k2jvmPanel.setVisible(false);
            enableIncrementalCompilationForJsCheckBox.setVisible(false);
        }

        updateOutputDirEnabled();
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
    private static String getSourceMapSourceEmbeddingDescription(@Nullable String sourceMapSourceEmbeddingId) {
        if (sourceMapSourceEmbeddingId == null) return "";
        String result = soruceMapSourceEmbeddingDescriptions.get(sourceMapSourceEmbeddingId);
        assert result != null : "Source map source embedding mode " + sourceMapSourceEmbeddingId +
                                " was not added to combobox, therefore it should not be here";
        return result;
    }

    @NotNull
    private static String getModuleKindOrDefault(@Nullable String moduleKindId) {
        if (moduleKindId == null) {
            moduleKindId = K2JsArgumentConstants.MODULE_PLAIN;
        }
        return moduleKindId;
    }

    @NotNull
    private static String getSourceMapSourceEmbeddingOrDefault(@Nullable String sourceMapSourceEmbeddingId) {
        if (sourceMapSourceEmbeddingId == null) {
            sourceMapSourceEmbeddingId = K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_INLINING;
        }
        return sourceMapSourceEmbeddingId;
    }

    private static String getJvmVersionOrDefault(@Nullable String jvmVersion) {
        return jvmVersion != null ? jvmVersion : JvmTarget.DEFAULT.getDescription();
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
                                            TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
    }

    private static boolean isModifiedWithNullize(@NotNull TextFieldWithBrowseButton chooser, @Nullable String currentValue) {
        return !StringUtil.equals(
                StringUtil.nullize(chooser.getText(), true),
                StringUtil.nullize(currentValue, true));
    }

    private static boolean isModified(@NotNull TextFieldWithBrowseButton chooser, @NotNull String currentValue) {
        return !StringUtil.equals(chooser.getText(), currentValue);
    }

    private void updateOutputDirEnabled() {
        if (isEnabled && copyRuntimeFilesCheckBox != null) {
            outputDirectory.setEnabled(copyRuntimeFilesCheckBox.isSelected());
            labelForOutputDirectory.setEnabled(copyRuntimeFilesCheckBox.isSelected());
        }
    }

    private boolean isLessOrEqual(LanguageVersion version, LanguageVersion upperBound) {
        return VersionComparatorUtil.compare(version.getVersionString(), upperBound.getVersionString()) <= 0;
    }

    public void onLanguageLevelChanged(VersionView languageLevel) {
        restrictAPIVersions(languageLevel);
        coroutinesPanel.setVisible(languageLevel.getVersion().compareTo(LanguageVersion.KOTLIN_1_3) < 0);
    }

    @SuppressWarnings("unchecked")
    private void restrictAPIVersions(VersionView upperBoundView) {
        VersionView selectedAPIView = getSelectedAPIVersionView();
        LanguageVersion selectedAPIVersion = selectedAPIView.getVersion();
        LanguageVersion upperBound = upperBoundView.getVersion();
        List<VersionView> permittedAPIVersions = new ArrayList<>(LanguageVersion.values().length + 1);
        if (isLessOrEqual(VersionView.LatestStable.INSTANCE.getVersion(), upperBound)) {
            permittedAPIVersions.add(VersionView.LatestStable.INSTANCE);
        }
        ArraysKt.mapNotNullTo(
                LanguageVersion.values(),
                permittedAPIVersions,
                version -> isLessOrEqual(version, upperBound) ? new VersionView.Specific(version) : null
        );
        apiVersionComboBox.setModel(
                new DefaultComboBoxModel(permittedAPIVersions.toArray())
        );
        apiVersionComboBox.setSelectedItem(
                VersionComparatorUtil.compare(selectedAPIVersion.getVersionString(), upperBound.getVersionString()) <= 0
                ? selectedAPIView
                : upperBoundView
        );
    }

    @SuppressWarnings("unchecked")
    private void fillJvmVersionList() {
        for (IdePlatform<JvmIdePlatformKind, ?> jvm : JvmIdePlatformKind.INSTANCE.getPlatforms()) {
            jvmVersionComboBox.addItem(jvm.getVersion().getDescription());
        }
    }

    @SuppressWarnings("unchecked")
    private void fillLanguageAndAPIVersionList() {
        languageVersionComboBox.addItem(VersionView.LatestStable.INSTANCE);
        apiVersionComboBox.addItem(VersionView.LatestStable.INSTANCE);

        for (LanguageVersion version : LanguageVersion.values()) {
            if (!version.isStable() && !ApplicationManager.getApplication().isInternal()) {
                continue;
            }

            VersionView.Specific specificVersion = new VersionView.Specific(version);
            languageVersionComboBox.addItem(specificVersion);
            apiVersionComboBox.addItem(specificVersion);
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

    public void setTargetPlatform(@Nullable IdePlatformKind<?> targetPlatform) {
        k2jsPanel.setVisible(JsIdePlatformUtil.isJavaScript(targetPlatform));
        scriptPanel.setVisible(JvmIdePlatformUtil.isJvm(targetPlatform));
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

    @SuppressWarnings("unchecked")
    private void fillSourceMapSourceEmbeddingList() {
        for (String moduleKind : soruceMapSourceEmbeddingDescriptions.keySet()) {
            sourceMapEmbedSources.addItem(moduleKind);
        }
        sourceMapEmbedSources.setRenderer(new ListCellRendererWrapper<String>() {
            @Override
            public void customize(JList list, String value, int index, boolean selected, boolean hasFocus) {
                setText(value != null ? getSourceMapSourceEmbeddingDescription(value) : "");
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
        return isModified(reportWarningsCheckBox, !commonCompilerArguments.getSuppressWarnings()) ||
               !getSelectedLanguageVersionView().equals(KotlinFacetSettingsKt.getLanguageVersionView(commonCompilerArguments)) ||
               !getSelectedAPIVersionView().equals(KotlinFacetSettingsKt.getApiVersionView(commonCompilerArguments)) ||
               !getSelectedCoroutineState().equals(commonCompilerArguments.getCoroutinesState()) ||
               !additionalArgsOptionsField.getText().equals(compilerSettings.getAdditionalArguments()) ||
               isModified(scriptTemplatesField, compilerSettings.getScriptTemplates()) ||
               isModified(scriptTemplatesClasspathField, compilerSettings.getScriptTemplatesClasspath()) ||
               isModified(copyRuntimeFilesCheckBox, compilerSettings.getCopyJsLibraryFiles()) ||
               isModified(outputDirectory, compilerSettings.getOutputDirectoryForJsLibraryFiles()) ||

               (compilerWorkspaceSettings != null &&
                (isModified(enableIncrementalCompilationForJvmCheckBox, compilerWorkspaceSettings.getPreciseIncrementalEnabled()) ||
                 isModified(enableIncrementalCompilationForJsCheckBox, compilerWorkspaceSettings.getIncrementalCompilationForJsEnabled()) ||
                 isModified(keepAliveCheckBox, compilerWorkspaceSettings.getEnableDaemon()))) ||

               isModified(generateSourceMapsCheckBox, k2jsCompilerArguments.getSourceMap()) ||
               isModifiedWithNullize(outputPrefixFile, k2jsCompilerArguments.getOutputPrefix()) ||
               isModifiedWithNullize(outputPostfixFile, k2jsCompilerArguments.getOutputPostfix()) ||
               !getSelectedModuleKind().equals(getModuleKindOrDefault(k2jsCompilerArguments.getModuleKind())) ||
               isModified(sourceMapPrefix, StringUtil.notNullize(k2jsCompilerArguments.getSourceMapPrefix())) ||
               !getSelectedSourceMapSourceEmbedding().equals(
                       getSourceMapSourceEmbeddingOrDefault(k2jsCompilerArguments.getSourceMapEmbedSources())) ||
               !getSelectedJvmVersion().equals(getJvmVersionOrDefault(k2jvmCompilerArguments.getJvmTarget()));
    }

    @NotNull
    private String getSelectedModuleKind() {
        return getModuleKindOrDefault((String) moduleKindComboBox.getSelectedItem());
    }

    private String getSelectedSourceMapSourceEmbedding() {
        return getSourceMapSourceEmbeddingOrDefault((String) sourceMapEmbedSources.getSelectedItem());
    }

    @NotNull
    private String getSelectedJvmVersion() {
        return getJvmVersionOrDefault((String) jvmVersionComboBox.getSelectedItem());
    }

    @NotNull
    public VersionView getSelectedLanguageVersionView() {
        Object item = languageVersionComboBox.getSelectedItem();
        return item != null ? (VersionView) item : VersionView.LatestStable.INSTANCE;
    }

    @NotNull
    private VersionView getSelectedAPIVersionView() {
        Object item = apiVersionComboBox.getSelectedItem();
        return item != null ? (VersionView) item : VersionView.LatestStable.INSTANCE;
    }

    @NotNull
    private String getSelectedCoroutineState() {
        if (getSelectedLanguageVersionView().getVersion().compareTo(LanguageVersion.KOTLIN_1_3) >= 0) {
            return CommonCompilerArguments.DEFAULT;
        }

        LanguageFeature.State state = (LanguageFeature.State) coroutineSupportComboBox.getSelectedItem();
        if (state == null) return CommonCompilerArguments.DEFAULT;
        switch (state) {
            case ENABLED: return CommonCompilerArguments.ENABLE;
            case ENABLED_WITH_WARNING: return CommonCompilerArguments.WARN;
            case ENABLED_WITH_ERROR: return CommonCompilerArguments.ERROR;
            default: return CommonCompilerArguments.DEFAULT;
        }
    }

    public void applyTo(
            CommonCompilerArguments commonCompilerArguments,
            K2JVMCompilerArguments k2jvmCompilerArguments,
            K2JSCompilerArguments k2jsCompilerArguments,
            CompilerSettings compilerSettings
    ) throws ConfigurationException {
        if (isProjectSettings) {
            boolean shouldInvalidateCaches =
                    !getSelectedLanguageVersionView().equals(KotlinFacetSettingsKt.getLanguageVersionView(commonCompilerArguments)) ||
                    !getSelectedAPIVersionView().equals(KotlinFacetSettingsKt.getApiVersionView(commonCompilerArguments)) ||
                    !getSelectedCoroutineState().equals(commonCompilerArguments.getCoroutinesState()) ||
                    !additionalArgsOptionsField.getText().equals(compilerSettings.getAdditionalArguments());

            if (shouldInvalidateCaches) {
                ApplicationUtilsKt.runWriteAction(
                        new Function0<Object>() {
                            @Override
                            public Object invoke() {
                                RootUtilsKt.invalidateProjectRoots(project);
                                return null;
                            }
                        }
                );
            }
        }

        commonCompilerArguments.setSuppressWarnings(!reportWarningsCheckBox.isSelected());
        KotlinFacetSettingsKt.setLanguageVersionView(commonCompilerArguments, getSelectedLanguageVersionView());
        KotlinFacetSettingsKt.setApiVersionView(commonCompilerArguments, getSelectedAPIVersionView());

        commonCompilerArguments.setCoroutinesState(getSelectedCoroutineState());

        compilerSettings.setAdditionalArguments(additionalArgsOptionsField.getText());
        compilerSettings.setScriptTemplates(scriptTemplatesField.getText());
        compilerSettings.setScriptTemplatesClasspath(scriptTemplatesClasspathField.getText());
        compilerSettings.setCopyJsLibraryFiles(copyRuntimeFilesCheckBox.isSelected());
        compilerSettings.setOutputDirectoryForJsLibraryFiles(outputDirectory.getText());

        if (compilerWorkspaceSettings != null) {
            compilerWorkspaceSettings.setPreciseIncrementalEnabled(enableIncrementalCompilationForJvmCheckBox.isSelected());
            compilerWorkspaceSettings.setIncrementalCompilationForJsEnabled(enableIncrementalCompilationForJsCheckBox.isSelected());

            boolean oldEnableDaemon = compilerWorkspaceSettings.getEnableDaemon();
            compilerWorkspaceSettings.setEnableDaemon(keepAliveCheckBox.isSelected());
            if (keepAliveCheckBox.isSelected() != oldEnableDaemon) {
                PluginStartupComponent.getInstance().resetAliveFlag();
            }
        }

        k2jsCompilerArguments.setSourceMap(generateSourceMapsCheckBox.isSelected());
        k2jsCompilerArguments.setOutputPrefix(StringUtil.nullize(outputPrefixFile.getText(), true));
        k2jsCompilerArguments.setOutputPostfix(StringUtil.nullize(outputPostfixFile.getText(), true));
        k2jsCompilerArguments.setModuleKind(getSelectedModuleKind());

        k2jsCompilerArguments.setSourceMapPrefix(sourceMapPrefix.getText());
        k2jsCompilerArguments.setSourceMapEmbedSources(generateSourceMapsCheckBox.isSelected() ? getSelectedSourceMapSourceEmbedding() : null);

        k2jvmCompilerArguments.setJvmTarget(getSelectedJvmVersion());

        if (isProjectSettings) {
            KotlinCommonCompilerArgumentsHolder.Companion.getInstance(project).setSettings(commonCompilerArguments);
            Kotlin2JvmCompilerArgumentsHolder.Companion.getInstance(project).setSettings(k2jvmCompilerArguments);
            Kotlin2JsCompilerArgumentsHolder.Companion.getInstance(project).setSettings(k2jsCompilerArguments);
            KotlinCompilerSettings.Companion.getInstance(project).setSettings(compilerSettings);
        }

        for (ClearBuildStateExtension extension : ClearBuildStateExtension.getExtensions()) {
            extension.clearState(project);
        }
    }

    @Override
    public void apply() throws ConfigurationException {
        applyTo(commonCompilerArguments, k2jvmCompilerArguments, k2jsCompilerArguments, compilerSettings);
    }

    @Override
    public void reset() {
        reportWarningsCheckBox.setSelected(!commonCompilerArguments.getSuppressWarnings());
        languageVersionComboBox.setSelectedItem(KotlinFacetSettingsKt.getLanguageVersionView(commonCompilerArguments));
        onLanguageLevelChanged(getSelectedLanguageVersionView());
        apiVersionComboBox.setSelectedItem(KotlinFacetSettingsKt.getApiVersionView(commonCompilerArguments));
        coroutineSupportComboBox.setSelectedItem(CoroutineSupport.byCompilerArguments(commonCompilerArguments));
        additionalArgsOptionsField.setText(compilerSettings.getAdditionalArguments());
        scriptTemplatesField.setText(compilerSettings.getScriptTemplates());
        scriptTemplatesClasspathField.setText(compilerSettings.getScriptTemplatesClasspath());
        copyRuntimeFilesCheckBox.setSelected(compilerSettings.getCopyJsLibraryFiles());
        outputDirectory.setText(compilerSettings.getOutputDirectoryForJsLibraryFiles());

        if (compilerWorkspaceSettings != null) {
            enableIncrementalCompilationForJvmCheckBox.setSelected(compilerWorkspaceSettings.getPreciseIncrementalEnabled());
            enableIncrementalCompilationForJsCheckBox.setSelected(compilerWorkspaceSettings.getIncrementalCompilationForJsEnabled());
            keepAliveCheckBox.setSelected(compilerWorkspaceSettings.getEnableDaemon());
        }

        generateSourceMapsCheckBox.setSelected(k2jsCompilerArguments.getSourceMap());
        outputPrefixFile.setText(k2jsCompilerArguments.getOutputPrefix());
        outputPostfixFile.setText(k2jsCompilerArguments.getOutputPostfix());

        moduleKindComboBox.setSelectedItem(getModuleKindOrDefault(k2jsCompilerArguments.getModuleKind()));
        sourceMapPrefix.setText(k2jsCompilerArguments.getSourceMapPrefix());
        sourceMapPrefix.setEnabled(k2jsCompilerArguments.getSourceMap());
        sourceMapEmbedSources.setSelectedItem(getSourceMapSourceEmbeddingOrDefault(k2jsCompilerArguments.getSourceMapEmbedSources()));

        jvmVersionComboBox.setSelectedItem(getJvmVersionOrDefault(k2jvmCompilerArguments.getJvmTarget()));
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
