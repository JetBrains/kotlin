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
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.io.FileUtilRt;
import org.jetbrains.kotlin.test.MockLibraryUtil;

import java.io.File;

public class JdkAndMockLibraryProjectDescriptor extends JetLightProjectDescriptor {
    public static final String LIBRARY_NAME = "myKotlinLib";

    private final String sourcesPath;
    private final boolean withSources;
    private final boolean isJsLibrary;

    public JdkAndMockLibraryProjectDescriptor(String sourcesPath, boolean withSources) {
        this(sourcesPath, withSources, false);
    }

    public JdkAndMockLibraryProjectDescriptor(String sourcesPath, boolean withSources, boolean isJsLibrary) {
        this.sourcesPath = sourcesPath;
        this.withSources = withSources;
        this.isJsLibrary = isJsLibrary;
    }

    @Override
    public void configureModule(Module module, ModifiableRootModel model, ContentEntry contentEntry) {
        File libraryJar = MockLibraryUtil.compileLibraryToJar(sourcesPath, LIBRARY_NAME, withSources, isJsLibrary);
        String jarUrl = "jar://" + FileUtilRt.toSystemIndependentName(libraryJar.getAbsolutePath()) + "!/";

        Library.ModifiableModel libraryModel = model.getModuleLibraryTable().getModifiableModel().createLibrary(LIBRARY_NAME).getModifiableModel();
        libraryModel.addRoot(jarUrl, OrderRootType.CLASSES);
        if (withSources) {
            libraryModel.addRoot(jarUrl + "src/", OrderRootType.SOURCES);
        }
        libraryModel.commit();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JdkAndMockLibraryProjectDescriptor that = (JdkAndMockLibraryProjectDescriptor) o;

        if (withSources != that.withSources) return false;
        if (sourcesPath != null ? !sourcesPath.equals(that.sourcesPath) : that.sourcesPath != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = sourcesPath != null ? sourcesPath.hashCode() : 0;
        result = 31 * result + (withSources ? 1 : 0);
        return result;
    }
}
