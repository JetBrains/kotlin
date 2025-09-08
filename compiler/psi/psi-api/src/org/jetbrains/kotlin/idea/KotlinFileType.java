/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.NotNullLazyValue;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class KotlinFileType extends LanguageFileType {
    public static final String EXTENSION = "kt";
    public static final String DOT_DEFAULT_EXTENSION = "." + EXTENSION;

    public static final String SCRIPT_EXTENSION = "kts";
    public static final String DOT_SCRIPT_EXTENSION = "." + SCRIPT_EXTENSION;

    public static final KotlinFileType INSTANCE = new KotlinFileType();

    private final NotNullLazyValue<Icon> myIcon = NotNullLazyValue.lazy(() -> KotlinIconProviderService.getInstance().getFileIcon());

    private KotlinFileType() {
        super(KotlinLanguage.INSTANCE);
    }

    @Override
    @NotNull
    public String getName() {
        return KotlinLanguage.NAME;
    }

    @Override
    @NotNull
    public String getDescription() {
        return getName();
    }

    @Override
    @NotNull
    public String getDefaultExtension() {
        return EXTENSION;
    }

    @Override
    public Icon getIcon() {
        return myIcon.getValue();
    }
}
