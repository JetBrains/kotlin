/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import com.intellij.framework.library.LibraryVersionProperties;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProviderImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.config.*;
import org.jetbrains.kotlin.idea.facet.FacetUtilsKt;
import org.jetbrains.kotlin.idea.facet.KotlinFacet;
import org.jetbrains.kotlin.idea.framework.JSLibraryStdPresentationProvider;
import org.jetbrains.kotlin.idea.framework.KotlinLibraryUtilKt;
import org.jetbrains.kotlin.idea.project.PlatformKt;
import org.jetbrains.kotlin.idea.versions.KotlinRuntimeLibraryUtilKt;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class ConfigureKotlinTest extends AbstractConfigureKotlinTest {
    public void testNewLibrary_copyJar() {
        doTestOneJavaModule(KotlinWithLibraryConfigurator.FileState.COPY);
    }

    public void testNewLibrary_doNotCopyJar() {
        doTestOneJavaModule(KotlinWithLibraryConfigurator.FileState.DO_NOT_COPY);
    }

    public void testLibraryWithoutPaths_jarExists() {
        doTestOneJavaModule(KotlinWithLibraryConfigurator.FileState.EXISTS);
    }

    public void testNewLibrary_jarExists() {
        doTestOneJavaModule(KotlinWithLibraryConfigurator.FileState.EXISTS);
    }

    public void testLibraryWithoutPaths_copyJar() {
        doTestOneJavaModule(KotlinWithLibraryConfigurator.FileState.COPY);
    }

    public void testLibraryWithoutPaths_doNotCopyJar() {
        doTestOneJavaModule(KotlinWithLibraryConfigurator.FileState.DO_NOT_COPY);
    }

    @SuppressWarnings("ConstantConditions")
    public void testTwoModules_exists() {
        Module[] modules = getModules();
        for (Module module : modules) {
            if (module.getName().equals("module1")) {
                configure(module, KotlinWithLibraryConfigurator.FileState.DO_NOT_COPY, JAVA_CONFIGURATOR);
                assertConfigured(module, JAVA_CONFIGURATOR);
            }
            else if (module.getName().equals("module2")) {
                assertNotConfigured(module, JAVA_CONFIGURATOR);
                configure(module, KotlinWithLibraryConfigurator.FileState.EXISTS, JAVA_CONFIGURATOR);
                assertConfigured(module, JAVA_CONFIGURATOR);
            }
        }
    }

    public void testLibraryNonDefault_libExistInDefault() throws IOException {
        Module module = getModule();

        // Move fake runtime jar to default library path to pretend library is already configured
        FileUtil.copy(
                new File(getProject().getBasePath() + "/lib/kotlin-runtime.jar"),
                new File(JAVA_CONFIGURATOR.getDefaultPathToJarFile(getProject()) + "/kotlin-runtime.jar"));

        assertNotConfigured(module, JAVA_CONFIGURATOR);
        JAVA_CONFIGURATOR.configure(myProject, Collections.<Module>emptyList());
        assertProperlyConfigured(module, JAVA_CONFIGURATOR);
    }

    public void testTwoModulesWithNonDefaultPath_doNotCopyInDefault() throws IOException {
        doTestConfigureModulesWithNonDefaultSetup(JAVA_CONFIGURATOR);
        assertEmpty(ConfigureKotlinInProjectUtilsKt.getCanBeConfiguredModules(myProject, JS_CONFIGURATOR));
    }

    public void testTwoModulesWithJSNonDefaultPath_doNotCopyInDefault() throws IOException {
        doTestConfigureModulesWithNonDefaultSetup(JS_CONFIGURATOR);
        assertEmpty(ConfigureKotlinInProjectUtilsKt.getCanBeConfiguredModules(myProject, JAVA_CONFIGURATOR));
    }

    public void testNewLibrary_jarExists_js() {
        doTestOneJsModule(KotlinWithLibraryConfigurator.FileState.EXISTS);
    }

    public void testNewLibrary_copyJar_js() {
        doTestOneJsModule(KotlinWithLibraryConfigurator.FileState.COPY);
    }

    public void testNewLibrary_doNotCopyJar_js() {
        doTestOneJsModule(KotlinWithLibraryConfigurator.FileState.DO_NOT_COPY);
    }

    public void testJsLibraryWithoutPaths_jarExists() {
        doTestOneJsModule(KotlinWithLibraryConfigurator.FileState.EXISTS);
    }

    public void testJsLibraryWithoutPaths_copyJar() {
        doTestOneJsModule(KotlinWithLibraryConfigurator.FileState.COPY);
    }

    public void testJsLibraryWithoutPaths_doNotCopyJar() {
        doTestOneJsModule(KotlinWithLibraryConfigurator.FileState.DO_NOT_COPY);
    }

    public void testProjectWithoutFacetWithRuntime106WithoutLanguageLevel() {
        assertEquals(LanguageVersion.KOTLIN_1_0, PlatformKt.getLanguageVersionSettings(myProject, null).getLanguageVersion());
        assertEquals(LanguageVersion.KOTLIN_1_0, PlatformKt.getLanguageVersionSettings(getModule()).getLanguageVersion());
    }

    public void testProjectWithoutFacetWithRuntime11WithoutLanguageLevel() {
        assertEquals(LanguageVersion.KOTLIN_1_1, PlatformKt.getLanguageVersionSettings(myProject, null).getLanguageVersion());
        assertEquals(LanguageVersion.KOTLIN_1_1, PlatformKt.getLanguageVersionSettings(getModule()).getLanguageVersion());
    }

    public void testProjectWithoutFacetWithRuntime11WithLanguageLevel10() {
        assertEquals(LanguageVersion.KOTLIN_1_0, PlatformKt.getLanguageVersionSettings(myProject, null).getLanguageVersion());
        assertEquals(LanguageVersion.KOTLIN_1_0, PlatformKt.getLanguageVersionSettings(getModule()).getLanguageVersion());
    }

    public void testProjectWithFacetWithRuntime11WithLanguageLevel10() {
        assertEquals(LanguageVersion.KOTLIN_1_1, PlatformKt.getLanguageVersionSettings(myProject, null).getLanguageVersion());
        assertEquals(LanguageVersion.KOTLIN_1_0, PlatformKt.getLanguageVersionSettings(getModule()).getLanguageVersion());
    }

    public void testJsLibraryVersion11() {
        Library jsRuntime = KotlinRuntimeLibraryUtilKt.findAllUsedLibraries(myProject).keySet().iterator().next();
        LibraryVersionProperties properties = KotlinLibraryUtilKt.getLibraryProperties(JSLibraryStdPresentationProvider.getInstance(), jsRuntime);
        assertEquals("1.1.0", properties.getVersionString());
    }

    public void testJsLibraryVersion106() {
        Library jsRuntime = KotlinRuntimeLibraryUtilKt.findAllUsedLibraries(myProject).keySet().iterator().next();
        LibraryVersionProperties properties = KotlinLibraryUtilKt.getLibraryProperties(JSLibraryStdPresentationProvider.getInstance(), jsRuntime);
        assertEquals("1.0.6", properties.getVersionString());
    }

    @SuppressWarnings("ConstantConditions")
    public void testJvmProjectWithV1FacetConfig() {
        KotlinFacetSettings settings = KotlinFacetSettingsProvider.Companion.getInstance(myProject).getInitializedSettings(getModule());
        K2JVMCompilerArguments arguments = (K2JVMCompilerArguments) settings.getCompilerArguments();
        assertEquals(false, settings.getUseProjectSettings());
        assertEquals("1.1", settings.getLanguageLevel().getDescription());
        assertEquals("1.0", settings.getApiLevel().getDescription());
        assertEquals(TargetPlatformKind.Jvm.Companion.get(JvmTarget.JVM_1_8), settings.getTargetPlatformKind());
        assertEquals("1.1", arguments.languageVersion);
        assertEquals("1.0", arguments.apiVersion);
        assertEquals(LanguageFeature.State.ENABLED_WITH_WARNING, CoroutineSupport.byCompilerArguments(arguments));
        assertEquals("1.7", arguments.jvmTarget);
        assertEquals("-version -Xallow-kotlin-package -Xskip-metadata-version-check", settings.getCompilerSettings().additionalArguments);
    }

    @SuppressWarnings("ConstantConditions")
    public void testJsProjectWithV1FacetConfig() {
        KotlinFacetSettings settings = KotlinFacetSettingsProvider.Companion.getInstance(myProject).getInitializedSettings(getModule());
        K2JSCompilerArguments arguments = (K2JSCompilerArguments) settings.getCompilerArguments();
        assertEquals(false, settings.getUseProjectSettings());
        assertEquals("1.1", settings.getLanguageLevel().getDescription());
        assertEquals("1.0", settings.getApiLevel().getDescription());
        assertEquals(TargetPlatformKind.JavaScript.INSTANCE, settings.getTargetPlatformKind());
        assertEquals("1.1", arguments.languageVersion);
        assertEquals("1.0", arguments.apiVersion);
        assertEquals(LanguageFeature.State.ENABLED_WITH_WARNING, CoroutineSupport.byCompilerArguments(arguments));
        assertEquals("amd", arguments.moduleKind);
        assertEquals("-version -meta-info", settings.getCompilerSettings().additionalArguments);
    }

    @SuppressWarnings("ConstantConditions")
    public void testJvmProjectWithV2FacetConfig() {
        KotlinFacetSettings settings = KotlinFacetSettingsProvider.Companion.getInstance(myProject).getInitializedSettings(getModule());
        K2JVMCompilerArguments arguments = (K2JVMCompilerArguments) settings.getCompilerArguments();
        assertEquals(false, settings.getUseProjectSettings());
        assertEquals("1.1", settings.getLanguageLevel().getDescription());
        assertEquals("1.0", settings.getApiLevel().getDescription());
        assertEquals(TargetPlatformKind.Jvm.Companion.get(JvmTarget.JVM_1_8), settings.getTargetPlatformKind());
        assertEquals("1.1", arguments.languageVersion);
        assertEquals("1.0", arguments.apiVersion);
        assertEquals(LanguageFeature.State.ENABLED, CoroutineSupport.byCompilerArguments(arguments));
        assertEquals("1.7", arguments.jvmTarget);
        assertEquals("-version -Xallow-kotlin-package -Xskip-metadata-version-check", settings.getCompilerSettings().additionalArguments);
    }

    @SuppressWarnings("ConstantConditions")
    public void testJsProjectWithV2FacetConfig() {
        KotlinFacetSettings settings = KotlinFacetSettingsProvider.Companion.getInstance(myProject).getInitializedSettings(getModule());
        K2JSCompilerArguments arguments = (K2JSCompilerArguments) settings.getCompilerArguments();
        assertEquals(false, settings.getUseProjectSettings());
        assertEquals("1.1", settings.getLanguageLevel().getDescription());
        assertEquals("1.0", settings.getApiLevel().getDescription());
        assertEquals(TargetPlatformKind.JavaScript.INSTANCE, settings.getTargetPlatformKind());
        assertEquals("1.1", arguments.languageVersion);
        assertEquals("1.0", arguments.apiVersion);
        assertEquals(LanguageFeature.State.ENABLED_WITH_ERROR, CoroutineSupport.byCompilerArguments(arguments));
        assertEquals("amd", arguments.moduleKind);
        assertEquals("-version -meta-info", settings.getCompilerSettings().additionalArguments);
    }

    @SuppressWarnings("ConstantConditions")
    public void testJvmProjectWithV3FacetConfig() {
        KotlinFacetSettings settings = KotlinFacetSettingsProvider.Companion.getInstance(myProject).getInitializedSettings(getModule());
        K2JVMCompilerArguments arguments = (K2JVMCompilerArguments) settings.getCompilerArguments();
        assertEquals(false, settings.getUseProjectSettings());
        assertEquals("1.1", settings.getLanguageLevel().getDescription());
        assertEquals("1.0", settings.getApiLevel().getDescription());
        assertEquals(TargetPlatformKind.Jvm.Companion.get(JvmTarget.JVM_1_8), settings.getTargetPlatformKind());
        assertEquals("1.1", arguments.languageVersion);
        assertEquals("1.0", arguments.apiVersion);
        assertEquals(LanguageFeature.State.ENABLED, CoroutineSupport.byCompilerArguments(arguments));
        assertEquals("1.7", arguments.jvmTarget);
        assertEquals("-version -Xallow-kotlin-package -Xskip-metadata-version-check", settings.getCompilerSettings().additionalArguments);
    }

    private void configureFacetAndCheckJvm(JvmTarget jvmTarget) {
        IdeModifiableModelsProviderImpl modelsProvider = new IdeModifiableModelsProviderImpl(getProject());
        try {
            KotlinFacet facet = FacetUtilsKt.getOrCreateFacet(getModule(), modelsProvider, false, false);
            TargetPlatformKind.Jvm platformKind = TargetPlatformKind.Jvm.Companion.get(jvmTarget);
            FacetUtilsKt.configureFacet(
                    facet,
                    "1.1",
                    LanguageFeature.State.ENABLED,
                    platformKind,
                    modelsProvider
            );
            assertEquals(platformKind, facet.getConfiguration().getSettings().getTargetPlatformKind());
            assertEquals(jvmTarget.getDescription(), ((K2JVMCompilerArguments) facet.getConfiguration().getSettings().getCompilerArguments()).jvmTarget);
        }
        finally {
            modelsProvider.dispose();
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void testJvm8InProjectJvm6InModule() {
        configureFacetAndCheckJvm(JvmTarget.JVM_1_6);
    }

    @SuppressWarnings("ConstantConditions")
    public void testJvm6InProjectJvm8InModule() {
        configureFacetAndCheckJvm(JvmTarget.JVM_1_8);
    }

    public void testProjectWithoutFacetWithJvmTarget18() {
        assertEquals(TargetPlatformKind.Jvm.Companion.get(JvmTarget.JVM_1_8), PlatformKt.getTargetPlatform(getModule()));
    }
}
