/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.test.testFramework.mock;

import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class KtMockLanguageFileType extends LanguageFileType {
    private final String myExtension;

    public KtMockLanguageFileType(@NotNull Language language, String extension) {
        super(language);
        this.myExtension = extension;
    }

    @Override
    @NotNull
    public String getName() {
        return this.getLanguage().getID();
    }

    @Override
    @NotNull
    public String getDescription() {
        return "";
    }

    @Override
    @NotNull
    public String getDefaultExtension() {
        String var10000 = this.myExtension;
        if(this.myExtension == null) {
            throw new IllegalStateException(String.format("@NotNull method %s.%s must not return null", new Object[]{"com/intellij/mock/KtMockLanguageFileType", "getDefaultExtension"}));
        } else {
            return var10000;
        }
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    public boolean equals(Object obj) {
        return obj instanceof LanguageFileType && this.getLanguage().equals(((LanguageFileType) obj).getLanguage());
    }
}