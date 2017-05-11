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

package org.jetbrains.kotlin.android.configure;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.projectRoots.Sdk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinPluginUtil;
import org.jetbrains.kotlin.idea.configuration.KotlinWithGradleConfigurator;
import org.jetbrains.kotlin.idea.versions.KotlinRuntimeLibraryUtilKt;
import org.jetbrains.kotlin.resolve.TargetPlatform;
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;

public class KotlinAndroidGradleModuleConfigurator extends KotlinWithGradleConfigurator {
    public static final String NAME = "android-gradle";

    private static final String APPLY_KOTLIN_ANDROID = "apply plugin: 'kotlin-android'";

    @NotNull
    @Override
    public String getName() {
        return NAME;
    }

    @NotNull
    @Override
    public TargetPlatform getTargetPlatform() {
        return JvmPlatform.INSTANCE;
    }

    @NotNull
    @Override
    public String getPresentableText() {
        return "Android with Gradle";
    }

    @Override
    public boolean isApplicable(@NotNull Module module) {
        return KotlinPluginUtil.isAndroidGradleModule(module);
    }

    @Override
    protected String getApplyPluginDirective() {
        return APPLY_KOTLIN_ANDROID;
    }

    @Override
    protected boolean addElementsToFile(@NotNull GroovyFile groovyFile, boolean isTopLevelProjectFile, @NotNull String version) {
        if (isTopLevelProjectFile) {
            return Companion.addElementsToProjectFile(groovyFile, version);
        }
        else {
            return addElementsToModuleFile(groovyFile, version);
        }
    }

    @NotNull
    @Override
    public String getRuntimeLibrary(@Nullable Sdk sdk, @NotNull String version) {
        if (sdk != null && KotlinRuntimeLibraryUtilKt.hasJreSpecificRuntime(version)) {
            JavaSdkVersion sdkVersion = JavaSdk.getInstance().getVersion(sdk);
            if (sdkVersion != null && sdkVersion.isAtLeast(JavaSdkVersion.JDK_1_8)) {
                // Android dex can't convert our kotlin-stdlib-jre8 artifact, so use jre7 instead (KT-16530)
                return KotlinWithGradleConfigurator.Companion.getDependencySnippet(
                        KotlinRuntimeLibraryUtilKt.getMAVEN_STDLIB_ID_JRE7());
            }
        }

        return super.getRuntimeLibrary(sdk, version);
    }

    KotlinAndroidGradleModuleConfigurator() {
    }
}
