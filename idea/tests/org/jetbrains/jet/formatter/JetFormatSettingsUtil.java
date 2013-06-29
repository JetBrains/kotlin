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

package org.jetbrains.jet.formatter;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.testFramework.LightPlatformTestCase;
import org.jetbrains.jet.plugin.JetLanguage;
import org.jetbrains.jet.plugin.formatter.JetCodeStyleSettings;
import org.jetbrains.jet.testing.SettingsConfigurator;

public class JetFormatSettingsUtil {
    private JetFormatSettingsUtil() {
    }

    public static CodeStyleSettings getSettings() {
        return CodeStyleSettingsManager.getSettings(LightPlatformTestCase.getProject());
    }

    public static SettingsConfigurator createConfigurator(String fileText, CodeStyleSettings settings) {
        return new SettingsConfigurator(fileText,
                                        settings.getCustomSettings(JetCodeStyleSettings.class),
                                        settings.getCommonSettings(JetLanguage.INSTANCE));
    }

    public static SettingsConfigurator createConfigurator(String fileText) {
        return createConfigurator(fileText, getSettings());
    }
}
