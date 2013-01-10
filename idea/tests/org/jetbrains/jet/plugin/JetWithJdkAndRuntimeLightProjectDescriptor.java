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
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.forTestCompile.ForTestCompileRuntime;

public class JetWithJdkAndRuntimeLightProjectDescriptor implements LightProjectDescriptor {
    protected JetWithJdkAndRuntimeLightProjectDescriptor() {
    }

    public static final JetWithJdkAndRuntimeLightProjectDescriptor INSTANCE = new JetWithJdkAndRuntimeLightProjectDescriptor();

    @Override
    public ModuleType getModuleType() {
        return StdModuleTypes.JAVA;
    }

    @Override
    public Sdk getSdk() {
        return PluginTestCaseBase.jdkFromIdeaHome();
    }

    @Override
    public void configureModule(@NotNull Module module, @NotNull ModifiableRootModel model, @Nullable ContentEntry contentEntry) {
        Library library = model.getModuleLibraryTable().createLibrary("ktl");
        Library.ModifiableModel modifiableModel = library.getModifiableModel();
        modifiableModel.addRoot(VfsUtil.getUrlForLibraryRoot(ForTestCompileRuntime.runtimeJarForTests()), OrderRootType.CLASSES);
        modifiableModel.commit();
    }

    public static void unConfigureModule(@NotNull ModifiableRootModel model) {
        for (OrderEntry orderEntry : model.getOrderEntries()) {
            if (orderEntry instanceof LibraryOrderEntry) {
                LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry) orderEntry;

                Library library = libraryOrderEntry.getLibrary();
                if (library != null) {
                    String libraryName = library.getName();
                    if (libraryName != null && libraryName.equals("ktl")) {

                        // Dispose attached roots
                        Library.ModifiableModel modifiableModel = library.getModifiableModel();
                        for (String rootUrl : library.getRootProvider().getUrls(OrderRootType.CLASSES)) {
                            modifiableModel.removeRoot(rootUrl, OrderRootType.CLASSES);
                        }
                        modifiableModel.commit();

                        model.getModuleLibraryTable().removeLibrary(library);

                        break;
                    }
                }
            }
        }
    }
}
