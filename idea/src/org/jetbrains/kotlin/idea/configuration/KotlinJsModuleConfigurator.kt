/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.configuration;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.libraries.Library;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.framework.JSLibraryStdDescription;
import org.jetbrains.kotlin.idea.versions.LibraryJarDescriptor;
import org.jetbrains.kotlin.idea.versions.OutdatedKotlinRuntimeCheckerKt;
import org.jetbrains.kotlin.js.JavaScript;
import org.jetbrains.kotlin.js.resolve.JsPlatform;
import org.jetbrains.kotlin.resolve.TargetPlatform;

import java.util.Arrays;
import java.util.List;

public class KotlinJsModuleConfigurator extends KotlinWithLibraryConfigurator {
    public static final String NAME = JavaScript.LOWER_NAME;

    @NotNull
    @Override
    public String getName() {
        return NAME;
    }

    @NotNull
    @Override
    public TargetPlatform getTargetPlatform() {
        return JsPlatform.INSTANCE;
    }

    @NotNull
    @Override
    public String getPresentableText() {
        return JavaScript.FULL_NAME;
    }

    @Override
    public boolean isConfigured(@NotNull Module module) {
        return ConfigureKotlinInProjectUtilsKt.hasKotlinJsRuntimeInScope(module);
    }

    @NotNull
    @Override
    protected String getLibraryName() {
        return JSLibraryStdDescription.LIBRARY_NAME;
    }

    @NotNull
    @Override
    protected String getDialogTitle() {
        return JSLibraryStdDescription.DIALOG_TITLE;
    }

    @NotNull
    @Override
    protected String getLibraryCaption() {
        return JSLibraryStdDescription.LIBRARY_CAPTION;
    }

    @NotNull
    @Override
    protected String getMessageForOverrideDialog() {
        return JSLibraryStdDescription.JAVA_SCRIPT_LIBRARY_CREATION;
    }

    @NotNull
    @Override
    public List<LibraryJarDescriptor> getLibraryJarDescriptors(@Nullable Sdk sdk) {
        return Arrays.asList(LibraryJarDescriptor.JS_STDLIB_JAR,
                             LibraryJarDescriptor.JS_STDLIB_SRC_JAR);
    }

    @NotNull
    @Override
    protected Function1<Library, Boolean> getLibraryMatcher() {
        return OutdatedKotlinRuntimeCheckerKt::isKotlinJsRuntime;
    }

    KotlinJsModuleConfigurator() {
    }
}
