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

package org.jetbrains.jet.plugin.sdk;

import com.intellij.framework.addSupport.FrameworkSupportInModuleConfigurable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableModelsProvider;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.libraries.CustomLibraryDescription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Maxim.Manuylov
 *         Date: 19.05.12
 */
public class KotlinFrameworkSupportConfigurable extends FrameworkSupportInModuleConfigurable {
    @Nullable
    @Override
    public JComponent createComponent() {
        return null;
    }

    @NotNull
    @Override
    public CustomLibraryDescription createLibraryDescription() {
        return new KotlinSdkDescription();
    }

    @Override
    public boolean isOnlyLibraryAdded() {
        return true;
    }

    @Override
    public void addSupport(@NotNull final Module module,
                           @NotNull final ModifiableRootModel rootModel,
                           @NotNull final ModifiableModelsProvider modifiableModelsProvider) {
    }
}
