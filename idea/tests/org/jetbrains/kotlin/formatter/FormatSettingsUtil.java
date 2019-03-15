/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formatter;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.testFramework.LightPlatformTestCase;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings;
import org.jetbrains.kotlin.test.SettingsConfigurator;

public class FormatSettingsUtil {
    private FormatSettingsUtil() {
    }

    public static CodeStyleSettings getSettings() {
        return CodeStyleSettingsManager.getSettings(LightPlatformTestCase.getProject());
    }

    public static SettingsConfigurator createConfigurator(String fileText, CodeStyleSettings settings) {
        return new SettingsConfigurator(fileText,
                                        settings.getCustomSettings(KotlinCodeStyleSettings.class),
                                        settings.getCommonSettings(KotlinLanguage.INSTANCE));
    }

    public static SettingsConfigurator createConfigurator(String fileText) {
        return createConfigurator(fileText, getSettings());
    }
}
