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
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.jetbrains.jet.InTextDirectivesUtils;
import org.jetbrains.jet.plugin.JetLanguage;
import org.jetbrains.jet.plugin.formatter.JetCodeStyleSettings;
import org.junit.Assert;

import java.lang.reflect.Field;

public class FormattingSettingsConfigurator {
    private final String[] settingsToTrue;
    private final String[] settingsToFalse;

    public FormattingSettingsConfigurator(String fileText) {
        settingsToTrue = InTextDirectivesUtils.findArrayWithPrefix(fileText, "// SET_TRUE:");
        settingsToFalse = InTextDirectivesUtils.findArrayWithPrefix(fileText, "// SET_FALSE:");
    }

    public void configureSettings(CodeStyleSettings settings) {
        JetCodeStyleSettings jetSettings = settings.getCustomSettings(JetCodeStyleSettings.class);
        CommonCodeStyleSettings jetCommonSettings = settings.getCommonSettings(JetLanguage.INSTANCE);

        for (String trueSetting : settingsToTrue) {
            configureSetting(jetSettings, jetCommonSettings, trueSetting, true);
        }

        for (String falseSetting : settingsToFalse) {
            configureSetting(jetSettings, jetCommonSettings, falseSetting, false);
        }
    }

    public void configureInvertedSettings(CodeStyleSettings settings) {
        JetCodeStyleSettings jetSettings = settings.getCustomSettings(JetCodeStyleSettings.class);
        CommonCodeStyleSettings jetCommonSettings = settings.getCommonSettings(JetLanguage.INSTANCE);

        for (String trueSetting : settingsToTrue) {
            configureSetting(jetSettings, jetCommonSettings, trueSetting, false);
        }

        for (String falseSetting : settingsToFalse) {
            configureSetting(jetSettings, jetCommonSettings, falseSetting, true);
        }
    }

    private static void configureSetting(JetCodeStyleSettings jetSettings,
            CommonCodeStyleSettings jetCommonSettings,
            String settingName,
            boolean value
    ) {
        try {
            Field field = jetCommonSettings.getClass().getDeclaredField(settingName);
            setSetting(field, jetCommonSettings, value);
        }
        catch (NoSuchFieldException e) {
            try {
                Field field = jetSettings.getClass().getDeclaredField(settingName);
                setSetting(field, jetSettings, value);
            }
            catch (NoSuchFieldException e1) {
                Assert.assertTrue(String.format("There's no property with name '%s' for kotlin language", settingName), false);
            }
        }
    }

    private static void setSetting(Field settingField, Object settings, boolean value) {
        try {
            settingField.setBoolean(settings, value);
        }
        catch (IllegalAccessException e) {
            Assert.assertTrue(String.format("Can't set property with the name %s", settingField.getName()), false);
        }
    }
}
