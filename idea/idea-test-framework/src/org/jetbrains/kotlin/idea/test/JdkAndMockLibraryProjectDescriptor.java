/*
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
import com.intellij.openapi.util.io.FileUtilRt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.test.MockLibraryUtil;
import org.jetbrains.kotlin.utils.PathUtil;

import java.io.File;

public class JdkAndMockLibraryProjectDescriptor extends KotlinLightProjectDescriptor {
    public static final String LIBRARY_NAME = "myKotlinLib";

    private final String sourcesPath;
    private final boolean withSources;
    private final boolean withRuntime;
    private final boolean isJsLibrary;
    private final boolean allowKotlinPackage;

    public JdkAndMockLibraryProjectDescriptor(String sourcesPath, boolean withSources) {
        this(sourcesPath, withSources, false, false, false);
    }

    public JdkAndMockLibraryProjectDescriptor(String sourcesPath, boolean withSources, boolean withRuntime, boolean isJsLibrary, boolean allowKotlinPackage) {
        this.sourcesPath = sourcesPath;
        this.withSources = withSources;
        this.withRuntime = withRuntime;
        this.isJsLibrary = isJsLibrary;
        this.allowKotlinPackage = allowKotlinPackage;
    }

    @Override
    public void configureModule(@NotNull Module module, @NotNull ModifiableRootModel model) {
        String libraryHash = sourcesPath + withSources + isJsLibrary + allowKotlinPackage;
        String libraryFileName = LIBRARY_NAME + "_" + Integer.toHexString(libraryHash.hashCode());
        
        File libraryJar = MockLibraryUtil.compileLibraryToJar(sourcesPath, libraryFileName, withSources, isJsLibrary, allowKotlinPackage);
        String jarUrl = getJarUrl(libraryJar);

        Library.ModifiableModel libraryModel = model.getModuleLibraryTable().getModifiableModel().createLibrary(LIBRARY_NAME).getModifiableModel();
        libraryModel.addRoot(jarUrl, OrderRootType.CLASSES);
        if (withRuntime) {
            libraryModel.addRoot(getJarUrl(PathUtil.getKotlinPathsForDistDirectory().getRuntimePath()), OrderRootType.CLASSES);
        }
        if (withSources) {
            libraryModel.addRoot(jarUrl + "src/", OrderRootType.SOURCES);
        }

        libraryModel.commit();
    }

    @NotNull
    private static String getJarUrl(@NotNull File libraryJar) {
        return "jar://" + FileUtilRt.toSystemIndependentName(libraryJar.getAbsolutePath()) + "!/";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JdkAndMockLibraryProjectDescriptor that = (JdkAndMockLibraryProjectDescriptor) o;

        if (withSources != that.withSources) return false;
        if (withRuntime != that.withRuntime) return false;
        if (isJsLibrary != that.isJsLibrary) return false;
        if (!sourcesPath.equals(that.sourcesPath)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = sourcesPath.hashCode();
        result = 31 * result + (withSources ? 1 : 0);
        result = 31 * result + (withRuntime ? 1 : 0);
        result = 31 * result + (isJsLibrary ? 1 : 0);
        return result;
    }
}
