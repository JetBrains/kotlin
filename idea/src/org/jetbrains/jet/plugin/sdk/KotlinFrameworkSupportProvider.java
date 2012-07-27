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

import com.intellij.framework.FrameworkTypeEx;
import com.intellij.framework.addSupport.FrameworkSupportInModuleConfigurable;
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.ModuleType;
import org.jetbrains.annotations.NotNull;

/**
 * @author Maxim.Manuylov
 *         Date: 19.05.12
 */
public class KotlinFrameworkSupportProvider extends FrameworkSupportInModuleProvider {
    @NotNull private static final String PLUGIN_MODULE_ID = "PLUGIN_MODULE";
    @NotNull private static final String ANDROID_MODULE_ID = "ANDROID_MODULE";

    @NotNull
    @Override
    public FrameworkTypeEx getFrameworkType() {
        return FrameworkTypeEx.EP_NAME.findExtension(KotlinFrameworkType.class);
    }

    @NotNull
    @Override
    public FrameworkSupportInModuleConfigurable createConfigurable(@NotNull FrameworkSupportModel model) {
        return new KotlinFrameworkSupportConfigurable();
    }

    @Override
    public boolean isEnabledForModuleType(@NotNull ModuleType type) {
        return type instanceof JavaModuleType || PLUGIN_MODULE_ID.equals(type.getId()) || ANDROID_MODULE_ID.equals(type.getId());
    }
}
