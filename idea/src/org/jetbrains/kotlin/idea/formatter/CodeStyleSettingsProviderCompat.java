/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter;

import com.intellij.openapi.options.Configurable;
import com.intellij.psi.codeStyle.CodeStyleConfigurable;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider;
import org.jetbrains.annotations.NotNull;

// Additional method is introduced in 183 instead of deprecated one
// BUNCH: 182
@SuppressWarnings({"IncompatibleAPI", "MissingRecentApi"})
public abstract class CodeStyleSettingsProviderCompat extends CodeStyleSettingsProvider {
    // Can't use @Override because it's going to be an error in 182
    @SuppressWarnings("override")
    @NotNull
    public abstract CodeStyleConfigurable createConfigurable(@NotNull CodeStyleSettings settings, @NotNull CodeStyleSettings modelSettings);

    // Should be implemented or it's going to be error in 182
    @SuppressWarnings("deprecation")
    @Override
    @NotNull
    public Configurable createSettingsPage(@NotNull CodeStyleSettings settings, @NotNull  CodeStyleSettings modelSettings) {
        return createConfigurable(settings, modelSettings);
    }
}
