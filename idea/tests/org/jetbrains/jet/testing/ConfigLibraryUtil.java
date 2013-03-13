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

package org.jetbrains.jet.testing;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetJdkAndLibraryProjectDescriptor;
import org.jetbrains.jet.plugin.JetWithJdkAndRuntimeLightProjectDescriptor;

/**
 * Helper for configuring kotlin runtime in tested project.
 */
public class ConfigLibraryUtil {
    private ConfigLibraryUtil() {
    }

    public static void configureKotlinRuntime(Module module, Sdk sdk) {
        configureLibrary(module, sdk, JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE);
    }

    public static void unConfigureKotlinRuntime(Module module, Sdk sdk) {
        unConfigureLibrary(module, sdk, JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE);
    }

    public static void configureLibrary(
            @NotNull final Module module,
            @NotNull final Sdk sdk,
            @NotNull final LightProjectDescriptor projectDescriptor
    ) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {

                ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
                ModifiableRootModel rootModel = rootManager.getModifiableModel();

                rootModel.setSdk(sdk);
                projectDescriptor.configureModule(module, rootModel, null);
                rootModel.commit();
            }
        });
    }

    public static void unConfigureLibrary(
            @NotNull final Module module,
            @NotNull final Sdk sdk,
            @NotNull final LightProjectDescriptor projectDescriptor
    ) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
                ModifiableRootModel rootModel = rootManager.getModifiableModel();

                rootModel.setSdk(sdk);
                if (projectDescriptor instanceof JetJdkAndLibraryProjectDescriptor) {
                    ((JetJdkAndLibraryProjectDescriptor) projectDescriptor).unConfigureModule(rootModel);
                }
                rootModel.commit();
            }
        });
    }
}
