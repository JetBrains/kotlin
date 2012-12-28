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

package org.jetbrains.jet.testing;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import org.jetbrains.jet.plugin.JetWithJdkAndRuntimeLightProjectDescriptor;

/**
 * Helper for configuring kotlin runtime in tested project.
 */
public class ConfigRuntimeUtil {
    private ConfigRuntimeUtil() {
    }

    public static void configureKotlinRuntime(final Module module, final Sdk sdk) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {

                final ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
                final ModifiableRootModel rootModel = rootManager.getModifiableModel();

                rootModel.setSdk(sdk);
                JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE.configureModule(module, rootModel, null);

                rootModel.commit();
            }
        });
    }

    public static void unConfigureKotlinRuntime(final Module module, final Sdk sdk) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {

                final ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
                final ModifiableRootModel rootModel = rootManager.getModifiableModel();

                rootModel.setSdk(sdk);
                JetWithJdkAndRuntimeLightProjectDescriptor.unConfigureModule(rootModel);

                rootModel.commit();
            }
        });
    }
}
