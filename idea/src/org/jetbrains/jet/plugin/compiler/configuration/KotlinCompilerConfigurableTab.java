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

package org.jetbrains.jet.plugin.compiler.configuration;

import com.intellij.compiler.options.ComparingUtils;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableEP;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.RawCommandLineEditor;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.cli.common.arguments.CommonCompilerArguments;
import org.jetbrains.jet.cli.common.arguments.K2JSCompilerArguments;
import org.jetbrains.jet.compiler.CompilerSettings;
import org.jetbrains.jet.plugin.JetBundle;

import javax.swing.*;

public class KotlinCompilerConfigurableTab implements SearchableConfigurable, Configurable.NoScroll{
    private final CommonCompilerArguments commonCompilerArguments;
    private final K2JSCompilerArguments k2jsCompilerArguments;
    private final CompilerSettings compilerSettings;
    private final ConfigurableEP extPoint;
    private JPanel contentPane;
    private JCheckBox generateNoWarningsCheckBox;
    private RawCommandLineEditor additionalArgsOptionsField;
    private JLabel additionalArgsLabel;
    private JCheckBox generateSourceMapsCheckBox;
    private TextFieldWithBrowseButton outputPrefixFile;
    private TextFieldWithBrowseButton outputPostfixFile;
    private JLabel labelForOutputPrefixFile;
    private JLabel labelForOutputPostfixFile;

    public KotlinCompilerConfigurableTab(ConfigurableEP ep) {
        this.extPoint = ep;
        this.commonCompilerArguments = KotlinCommonCompilerArgumentsHolder.getInstance(ep.getProject()).getSettings();
        this.k2jsCompilerArguments = Kotlin2JsCompilerArgumentsHolder.getInstance(ep.getProject()).getSettings();
        this.compilerSettings = KotlinCompilerSettings.getInstance(ep.getProject()).getSettings();

        additionalArgsOptionsField.attachLabel(additionalArgsLabel);
        additionalArgsOptionsField.setDialogCaption(JetBundle.message("kotlin.compiler.option.additional.command.line.parameters.dialog.title"));
        
        setupFileChooser(labelForOutputPrefixFile, outputPrefixFile,
                         JetBundle.message("kotlin.compiler.js.option.output.prefix.browse.title"));
        setupFileChooser(labelForOutputPostfixFile, outputPostfixFile,
                         JetBundle.message("kotlin.compiler.js.option.output.postfix.browse.title"));
    }

    @NotNull
    @Override
    public String getId() {
        return extPoint.id;
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
               ComparingUtils.isModified(generateSourceMapsCheckBox, k2jsCompilerArguments.sourceMap) ||
               isModified(outputPrefixFile, k2jsCompilerArguments.outputPrefix) ||
               isModified(outputPostfixFile, k2jsCompilerArguments.outputPostfix);
    }

    @Override
    public void apply() throws ConfigurationException {
        commonCompilerArguments.suppressWarnings = generateNoWarningsCheckBox.isSelected();
        compilerSettings.setAdditionalArguments(additionalArgsOptionsField.getText());
        k2jsCompilerArguments.sourceMap = generateSourceMapsCheckBox.isSelected();
        k2jsCompilerArguments.outputPrefix = StringUtil.nullize(outputPrefixFile.getText(), true);
        k2jsCompilerArguments.outputPostfix = StringUtil.nullize(outputPostfixFile.getText(), true);
    }

    @Override
    public void reset() {
        generateNoWarningsCheckBox.setSelected(commonCompilerArguments.suppressWarnings);
        additionalArgsOptionsField.setText(compilerSettings.getAdditionalArguments());
        generateSourceMapsCheckBox.setSelected(k2jsCompilerArguments.sourceMap);
        outputPrefixFile.setText(k2jsCompilerArguments.outputPrefix);
        outputPostfixFile.setText(k2jsCompilerArguments.outputPostfix);
    }

    @Override
    public void disposeUIResources() {
    }

    @Nls
    @Override
    public String getDisplayName() {
        return extPoint.displayName;
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
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
