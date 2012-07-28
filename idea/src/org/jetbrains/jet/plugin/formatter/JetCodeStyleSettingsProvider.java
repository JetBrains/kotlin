/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.formatter;

import com.intellij.application.options.CodeStyleAbstractConfigurable;
import com.intellij.application.options.CodeStyleAbstractPanel;
import com.intellij.application.options.TabbedLanguageCodeStylePanel;
import com.intellij.lang.Language;
import com.intellij.openapi.options.Configurable;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetLanguage;

public class JetCodeStyleSettingsProvider extends CodeStyleSettingsProvider {

    @Override
    public String getConfigurableDisplayName() {
        return JetLanguage.NAME;
    }

    @Override
    public Language getLanguage() {
        return JetLanguage.INSTANCE;
    }

    @Override
    public CustomCodeStyleSettings createCustomSettings(CodeStyleSettings settings) {
        return new JetCodeStyleSettings(settings);
    }

    @NotNull
    @Override
    public Configurable createSettingsPage(CodeStyleSettings settings, CodeStyleSettings originalSettings) {
        return new CodeStyleAbstractConfigurable(settings, originalSettings, JetLanguage.NAME) {
            @Override
            protected CodeStyleAbstractPanel createPanel(CodeStyleSettings settings) {
                return new TabbedLanguageCodeStylePanel(JetLanguage.INSTANCE, getCurrentSettings(), settings) {
                    @Override
                    protected void initTabs(CodeStyleSettings settings) {
                        // TODO: activate all parent tabs
                        addIndentOptionsTab(settings);
                        addSpacesTab(settings);
                        addWrappingAndBracesTab(settings);
                    }
                };
            }

            @Override
            public String getHelpTopic() {
                return null;
            }
        };
    }
}
