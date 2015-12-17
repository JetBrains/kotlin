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

package org.jetbrains.kotlin.idea.test;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VfsUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class ProjectDescriptorWithStdlibSources extends KotlinWithJdkAndRuntimeLightProjectDescriptor {
    public static final ProjectDescriptorWithStdlibSources INSTANCE = new ProjectDescriptorWithStdlibSources();

    @Override
    public void configureModule(@NotNull Module module, @NotNull ModifiableRootModel model) {
        super.configureModule(module, model);

        Library library = model.getModuleLibraryTable().getLibraryByName(LIBRARY_NAME);
        assert library != null;
        Library.ModifiableModel modifiableModel = library.getModifiableModel();
        modifiableModel.addRoot(VfsUtil.getUrlForLibraryRoot(new File("libraries/stdlib/src")), OrderRootType.SOURCES);
        modifiableModel.addRoot(VfsUtil.getUrlForLibraryRoot(new File("core/builtins/src")), OrderRootType.SOURCES);
        modifiableModel.addRoot(VfsUtil.getUrlForLibraryRoot(new File("core/builtins/native")), OrderRootType.SOURCES);
        modifiableModel.commit();
    }
}
