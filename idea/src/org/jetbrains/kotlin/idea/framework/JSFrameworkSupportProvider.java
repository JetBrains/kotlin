/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.framework;

import com.intellij.framework.FrameworkTypeEx;
import com.intellij.framework.addSupport.FrameworkSupportInModuleConfigurable;
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.roots.ModifiableModelsProvider;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.libraries.CustomLibraryDescription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class JSFrameworkSupportProvider extends FrameworkSupportInModuleProvider {
    @NotNull
    @Override
    public FrameworkTypeEx getFrameworkType() {
        return JSFrameworkType.getInstance();
    }

    @NotNull
    @Override
    public FrameworkSupportInModuleConfigurable createConfigurable(@NotNull final FrameworkSupportModel model) {
        return new FrameworkSupportInModuleConfigurable() {
            public JSLibraryStdDescription description;

            @Nullable
            @Override
            public CustomLibraryDescription createLibraryDescription() {
                description = new JSLibraryStdDescription(model.getProject());
                return description;
            }

            @Nullable
            @Override
            public JComponent createComponent() {
                return null;
            }

            @Override
            public boolean isOnlyLibraryAdded() {
                return true;
            }

            @Override
            public void addSupport(
                    @NotNull Module module,
                    @NotNull ModifiableRootModel rootModel,
                    @NotNull ModifiableModelsProvider modifiableModelsProvider) {
                FrameworksCompatibilityUtils.suggestRemoveIncompatibleFramework(
                        rootModel,
                        JavaRuntimeLibraryDescription.SUITABLE_LIBRARY_KINDS,
                        JavaFrameworkType.getInstance());

                FrameworksCompatibilityUtils.suggestRemoveOldJsLibrary(rootModel);

                description.finishLibConfiguration(module, rootModel);
            }

            @Override
            public void onFrameworkSelectionChanged(boolean selected) {
                if (selected) {
                    String providerId = JavaFrameworkType.getInstance().getId();
                    if (model.isFrameworkSelected(providerId)) {
                        model.setFrameworkComponentEnabled(providerId, false);
                    }
                }
            }
        };
    }

    @Override
    public boolean isEnabledForModuleType(@NotNull ModuleType moduleType) {
        return moduleType instanceof JavaModuleType;
    }
}
