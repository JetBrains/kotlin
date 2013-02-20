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

package org.jetbrains.jet.plugin.project;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleComponent;
import com.intellij.openapi.roots.impl.storage.ClasspathStorage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.k2js.config.EcmaVersion;

@Deprecated
@State(
        name = "K2JSModule",
        storages = @Storage(
          id = ClasspathStorage.DEFAULT_STORAGE,
          file = "$MODULE_FILE$"
        )
)
public final class K2JSModuleComponent implements ModuleComponent, PersistentStateComponent<K2JSModuleComponent> {
    @NotNull
    public static K2JSModuleComponent getInstance(@NotNull Module module) {
        return module.getComponent(K2JSModuleComponent.class);
    }

    public K2JSModuleComponent() {}

    @Override
    public void projectOpened() {
        //do nothing
    }

    @Override
    public void projectClosed() {
        //do nothing
    }

    @Override
    public void moduleAdded() {
        //do nothing
    }

    @Override
    public void initComponent() {
        //do nothing
    }

    @Override
    public void disposeComponent() {
        //do nothing
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "Kotlin to JavaScript module configuration";
    }

    @Override
    public K2JSModuleComponent getState() {
        // Don't store starting from this version
        return null;
    }

    @Override
    public void loadState(K2JSModuleComponent state) {
        // Don't restore - there could be another set of fields
    }
}
