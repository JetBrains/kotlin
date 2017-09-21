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

import com.intellij.framework.FrameworkTypeEx;
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableModelsProvider;
import com.intellij.openapi.roots.ModifiableRootModel;
import icons.JetgroovyIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class GradleGroovyFrameworkSupportProvider extends GradleFrameworkSupportProvider {

  public static final String ID = "groovy";

  @NotNull
  @Override
  public FrameworkTypeEx getFrameworkType() {
    return new FrameworkTypeEx(ID) {
      @NotNull
      @Override
      public FrameworkSupportInModuleProvider createProvider() {
        return GradleGroovyFrameworkSupportProvider.this;
      }

      @NotNull
      @Override
      public String getPresentableName() {
        return "Groovy";
      }

      @NotNull
      @Override
      public Icon getIcon() {
        return JetgroovyIcons.Groovy.Groovy_16x16;
      }
    };
  }

  @Override
  public void addSupport(@NotNull Module module,
                         @NotNull ModifiableRootModel rootModel,
                         @NotNull ModifiableModelsProvider modifiableModelsProvider,
                         @NotNull BuildScriptDataBuilder buildScriptData) {
    buildScriptData
      .addPluginDefinition("plugin(\"groovy\")")
      .addRepositoriesDefinition("mavenCentral()")
      .addDependencyNotation("compile(\"org.codehaus.groovy:groovy-all:2.3.11\")")
      .addDependencyNotation("testCompile(\"junit\", \"junit\", \"4.12\")");
  }
}