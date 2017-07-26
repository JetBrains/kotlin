/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
package org.jetbrains.kotlin.gradle.kdsl.frameworkSupport;

import com.intellij.framework.addSupport.FrameworkSupportInModuleConfigurable;
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.roots.ModifiableModelsProvider;
import com.intellij.openapi.roots.ModifiableRootModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static org.jetbrains.kotlin.gradle.kdsl.GradleModuleBuilder.getBuildScriptData;

public abstract class GradleFrameworkSupportProvider extends FrameworkSupportInModuleProvider {

  public static final ExtensionPointName<GradleFrameworkSupportProvider> EP_NAME =
    ExtensionPointName.create("org.jetbrains.kotlin.gradleFrameworkSupport");

  public abstract void addSupport(@NotNull Module module, @NotNull ModifiableRootModel rootModel,
                                  @NotNull ModifiableModelsProvider modifiableModelsProvider,
                                  @NotNull BuildScriptDataBuilder buildScriptData);

  public JComponent createComponent() {
    return null;
  }

  @NotNull
  @Override
  public FrameworkSupportInModuleConfigurable createConfigurable(@NotNull FrameworkSupportModel model) {
    return new FrameworkSupportInModuleConfigurable() {
      @Nullable
      @Override
      public JComponent createComponent() {
        return GradleFrameworkSupportProvider.this.createComponent();
      }

      @Override
      public void addSupport(@NotNull Module module,
                             @NotNull ModifiableRootModel rootModel,
                             @NotNull ModifiableModelsProvider modifiableModelsProvider) {
        final BuildScriptDataBuilder buildScriptData = getBuildScriptData(module);
        if (buildScriptData != null) {
          GradleFrameworkSupportProvider.this.addSupport(module, rootModel, modifiableModelsProvider, buildScriptData);
        }
      }
    };
  }

  @Override
  public boolean isEnabledForModuleType(@NotNull ModuleType moduleType) {
    return false;
  }
}
