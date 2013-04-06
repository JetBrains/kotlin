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

package org.jetbrains.jet.plugin;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.libraries.NewLibraryConfiguration;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.NewLibraryEditor;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.jet.plugin.framework.JSLibraryCreateOptions;
import org.jetbrains.jet.plugin.framework.JSLibraryStdDescription;
import org.jetbrains.jet.testing.ConfigLibraryUtil;
import org.jetbrains.jet.utils.PathUtil;

public class JetStdJSProjectDescriptor implements LightProjectDescriptor {
    public static final JetStdJSProjectDescriptor INSTANCE = new JetStdJSProjectDescriptor();

    @Override
    public ModuleType getModuleType() {
        return StdModuleTypes.JAVA;
    }

    @Override
    public Sdk getSdk() {
        return null;
    }

    @Override
    public void configureModule(Module module, ModifiableRootModel model, ContentEntry contentEntry) {
        NewLibraryConfiguration configuration = new JSLibraryStdDescription().createNewLibrary(
                null, PathUtil.getKotlinPathsForDistDirectory(), JSLibraryCreateOptions.DEFAULT);

        assert configuration != null : "Configuration should exist";

        NewLibraryEditor editor = new NewLibraryEditor(configuration.getLibraryType(), configuration.getProperties());
        configuration.addRoots(editor);

        ConfigLibraryUtil.addLibrary(editor, model);
    }
}