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
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableEP;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.cli.common.arguments.CommonCompilerArguments;

import javax.swing.*;

import static org.jetbrains.jet.cli.common.arguments.CommonArgumentConstants.SUPPRESS_WARNINGS;

public class KotlinCompilerConfigurableTab implements SearchableConfigurable, Configurable.NoScroll{
    private final CommonCompilerArguments commonCompilerSettings;
    private final ConfigurableEP extPoint;
    private JPanel contentPane;
    private JCheckBox generateNoWarningsCheckBox;

    public KotlinCompilerConfigurableTab(ConfigurableEP ep) {
        this.extPoint = ep;
        this.commonCompilerSettings = KotlinCommonCompilerSettings.getInstance(ep.getProject()).getSettings();
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
        return ComparingUtils.isModified(generateNoWarningsCheckBox, isGenerateNoWarnings());
    }

    @Override
    public void apply() throws ConfigurationException {
        setGenerateNoWarnings(generateNoWarningsCheckBox.isSelected());
    }

    @Override
    public void reset() {
        generateNoWarningsCheckBox.setSelected(isGenerateNoWarnings());
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

    private boolean isGenerateNoWarnings() {
        return commonCompilerSettings.suppressAllWarnings();
    }

    private void setGenerateNoWarnings(boolean selected) {
        commonCompilerSettings.suppress = selected ? SUPPRESS_WARNINGS : "";
    }
}
