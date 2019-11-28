/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formatter;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings;
import org.jetbrains.kotlin.test.SettingsConfigurator;

public class FormatSettingsUtil {
    private FormatSettingsUtil() {
    }

    public static SettingsConfigurator createConfigurator(String fileText, CodeStyleSettings settings) {
        return new SettingsConfigurator(fileText,
                                        settings.getCustomSettings(KotlinCodeStyleSettings.class),
                                        settings.getCommonSettings(KotlinLanguage.INSTANCE));
    }
}
