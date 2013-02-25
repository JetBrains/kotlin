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

package org.jetbrains.jet.plugin.framework;

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
import org.jetbrains.jet.plugin.framework.ui.FrameworkSourcePanel;

import javax.swing.*;

public class JavaFrameworkSupportProvider extends FrameworkSupportInModuleProvider {
    @NotNull
    @Override
    public FrameworkTypeEx getFrameworkType() {
        return JavaFrameworkType.getInstance();
    }

    @NotNull
    @Override
    public FrameworkSupportInModuleConfigurable createConfigurable(@NotNull FrameworkSupportModel model) {
        return new FrameworkSupportInModuleConfigurable() {
            public FrameworkSourcePanel panel = null;

            @Nullable
            @Override
            public CustomLibraryDescription createLibraryDescription() {
                JavaRuntimeLibraryDescription description = new JavaRuntimeLibraryDescription();
                description.setFrameworkSourcePanel(getConfigurationPanel());
                return description;
            }

            @Nullable
            @Override
            public JComponent createComponent() {
                return getConfigurationPanel().getPanel();
            }

            @Override
            public boolean isOnlyLibraryAdded() {
                return true;
            }

            @Override
            public void onFrameworkSelectionChanged(boolean selected) {
                getConfigurationPanel().onFrameworkSelectionChanged(selected);
            }

            @Override
            public void addSupport(
                    @NotNull Module module,
                    @NotNull ModifiableRootModel rootModel,
                    @NotNull ModifiableModelsProvider modifiableModelsProvider) {
                FrameworksCompatibilityUtils.suggestRemoveIncompatibleFramework(
                        rootModel,
                        new JSLibraryDescription(),
                        JSFrameworkType.getInstance());
            }

            private FrameworkSourcePanel getConfigurationPanel() {
                if (panel == null) {
                    panel = new FrameworkSourcePanel();
                }
                return panel;
            }
        };
    }

    @Override
    public boolean isEnabledForModuleType(@NotNull ModuleType moduleType) {
        return moduleType instanceof JavaModuleType;
    }
}
