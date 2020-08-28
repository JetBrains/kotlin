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
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.io.FileUtilRt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.framework.JSLibraryKind;
import org.jetbrains.kotlin.idea.framework.KotlinSdkType;
import org.jetbrains.kotlin.test.MockLibraryUtil;
import org.jetbrains.kotlin.utils.PathUtil;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;

public class SdkAndMockLibraryProjectDescriptor extends KotlinLightProjectDescriptor {
    public static final String LIBRARY_NAME = "myKotlinLib";

    private final boolean withSources;
    private final boolean withRuntime;
    private final boolean isJsLibrary;
    private final boolean allowKotlinPackage;
    private final String sourcesPath;
    private final List<String> classpath;

    public SdkAndMockLibraryProjectDescriptor(@NotNull String sourcesPath, boolean withSources) {
        this(sourcesPath, withSources, false, false, false);
    }

    public SdkAndMockLibraryProjectDescriptor(
            @NotNull String sourcesPath,
            boolean withSources, boolean withRuntime, boolean isJsLibrary, boolean allowKotlinPackage
    ) {
        this(sourcesPath, withSources, withRuntime, isJsLibrary, allowKotlinPackage, emptyList());
    }

    public SdkAndMockLibraryProjectDescriptor(
            @NotNull String sourcesPath,
            boolean withSources, boolean withRuntime, boolean isJsLibrary, boolean allowKotlinPackage,
            @NotNull List<String> classpath
    ) {
        this.withSources = withSources;
        this.withRuntime = withRuntime;
        this.isJsLibrary = isJsLibrary;
        this.allowKotlinPackage = allowKotlinPackage;
        this.sourcesPath = sourcesPath;
        this.classpath = classpath;
    }

    @Override
    public void configureModule(@NotNull Module module, @NotNull ModifiableRootModel model) {
        @SuppressWarnings("SpellCheckingInspection")
        List<String> extraOptions = allowKotlinPackage ? Collections.singletonList("-Xallow-kotlin-package") : emptyList();
        File libraryJar =
                isJsLibrary
                ? MockLibraryUtil.compileJsLibraryToJar(sourcesPath, LIBRARY_NAME, withSources, Collections.emptyList())
                : MockLibraryUtil.compileJvmLibraryToJar(sourcesPath, LIBRARY_NAME, withSources, true, extraOptions, classpath);
        String jarUrl = getJarUrl(libraryJar);

        Library.ModifiableModel libraryModel =
                model.getModuleLibraryTable().getModifiableModel().createLibrary(LIBRARY_NAME).getModifiableModel();
        libraryModel.addRoot(jarUrl, OrderRootType.CLASSES);
        if (withRuntime && !isJsLibrary) {
            libraryModel.addRoot(getJarUrl(PathUtil.getKotlinPathsForDistDirectory().getStdlibPath()), OrderRootType.CLASSES);
        }
        if (isJsLibrary && libraryModel instanceof LibraryEx.ModifiableModelEx) {
            ((LibraryEx.ModifiableModelEx) libraryModel).setKind(JSLibraryKind.INSTANCE);
        }
        if (withSources) {
            libraryModel.addRoot(jarUrl + "src", OrderRootType.SOURCES);
        }

        libraryModel.commit();

        if (withRuntime && isJsLibrary) {
            KotlinStdJSProjectDescriptor.INSTANCE.configureModule(module, model);
        }
    }

    @Override
    public Sdk getSdk() {
        return isJsLibrary ? KotlinSdkType.INSTANCE.createSdkWithUniqueName(emptyList()) : PluginTestCaseBase.mockJdk();
    }

    @NotNull
    private static String getJarUrl(@NotNull File libraryJar) {
        return "jar://" + FileUtilRt.toSystemIndependentName(libraryJar.getAbsolutePath()) + "!/";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SdkAndMockLibraryProjectDescriptor that = (SdkAndMockLibraryProjectDescriptor) o;
        return withSources == that.withSources &&
               withRuntime == that.withRuntime &&
               isJsLibrary == that.isJsLibrary &&
               allowKotlinPackage == that.allowKotlinPackage &&
               sourcesPath.equals(that.sourcesPath) &&
               classpath.equals(that.classpath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(withSources, withRuntime, isJsLibrary, allowKotlinPackage, sourcesPath, classpath);
    }
}
