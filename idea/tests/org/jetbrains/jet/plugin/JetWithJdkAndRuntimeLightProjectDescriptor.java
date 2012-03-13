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

package org.jetbrains.jet.plugin;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightProjectDescriptor;
import org.apache.commons.lang.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.ForTestCompileStdlib;

/**
* @author Evgeny Gerashchenko
* @since 2/13/12
*/
public class JetWithJdkAndRuntimeLightProjectDescriptor implements LightProjectDescriptor {
    private JetWithJdkAndRuntimeLightProjectDescriptor() {
    }

    public static final JetWithJdkAndRuntimeLightProjectDescriptor INSTANCE = new JetWithJdkAndRuntimeLightProjectDescriptor();

    @Override
    public ModuleType getModuleType() {
        return StdModuleTypes.JAVA;
    }

    @Override
    public Sdk getSdk() {
        return JavaSdk.getInstance().createJdk("JDK", SystemUtils.getJavaHome().getAbsolutePath());
    }

    @Override
    public void configureModule(@NotNull Module module, @NotNull ModifiableRootModel model, @Nullable ContentEntry contentEntry) {
        Library.ModifiableModel modifiableModel = model.getModuleLibraryTable().createLibrary("ktl").getModifiableModel();
        VirtualFile cd = JarFileSystem.getInstance().findFileByPath(ForTestCompileStdlib.stdlibJarForTests() + "!/");
        assert cd != null;
        modifiableModel.addRoot(cd, OrderRootType.CLASSES);
        modifiableModel.commit();
    }

    public static void unConfigureModule(@NotNull ModifiableRootModel model) {
        for (OrderEntry orderEntry : model.getOrderEntries()) {
            if (orderEntry instanceof LibraryOrderEntry) {
                LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry) orderEntry;

                Library library = libraryOrderEntry.getLibrary();
                if (library != null && library.getName().equals("ktl")) {
                    model.getModuleLibraryTable().removeLibrary(library);
                    break;
                }
            }
        }
    }
}
