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

import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProviderImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.RootPolicy;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.config.*;
import org.jetbrains.kotlin.idea.facet.FacetUtilsKt;
import org.jetbrains.kotlin.idea.facet.KotlinFacet;
import org.jetbrains.kotlin.idea.framework.JsLibraryStdDetectionUtil;
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
                Companion.configure(module, KotlinWithLibraryConfigurator.FileState.DO_NOT_COPY, Companion.getJAVA_CONFIGURATOR());
                Companion.assertConfigured(module, Companion.getJAVA_CONFIGURATOR());
            }
            else if (module.getName().equals("module2")) {
                Companion.assertNotConfigured(module, Companion.getJAVA_CONFIGURATOR());
                Companion.configure(module, KotlinWithLibraryConfigurator.FileState.EXISTS, Companion.getJAVA_CONFIGURATOR());
                Companion.assertConfigured(module, Companion.getJAVA_CONFIGURATOR());
            }
        }
    }

    public void testLibraryNonDefault_libExistInDefault() throws IOException {
        Module module = getModule();

        // Move fake runtime jar to default library path to pretend library is already configured
        FileUtil.copy(
                new File(getProject().getBasePath() + "/lib/kotlin-runtime.jar"),
                new File(Companion.getJAVA_CONFIGURATOR().getDefaultPathToJarFile(getProject()) + "/kotlin-runtime.jar"));

        Companion.assertNotConfigured(module, Companion.getJAVA_CONFIGURATOR());
        Companion.getJAVA_CONFIGURATOR().configure(myProject, Collections.<Module>emptyList());
        Companion.assertProperlyConfigured(module, Companion.getJAVA_CONFIGURATOR());
    }

    public void testTwoModulesWithNonDefaultPath_doNotCopyInDefault() throws IOException {
        doTestConfigureModulesWithNonDefaultSetup(Companion.getJAVA_CONFIGURATOR());
        assertEmpty(ConfigureKotlinInProjectUtilsKt.getCanBeConfiguredModules(myProject, Companion.getJS_CONFIGURATOR()));
    }

    public void testTwoModulesWithJSNonDefaultPath_doNotCopyInDefault() throws IOException {
        doTestConfigureModulesWithNonDefaultSetup(Companion.getJS_CONFIGURATOR());
        assertEmpty(ConfigureKotlinInProjectUtilsKt.getCanBeConfiguredModules(myProject, Companion.getJAVA_CONFIGURATOR()));
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

    public void testJsLibraryWrongKind() {
        doTestOneJsModule(KotlinWithLibraryConfigurator.FileState.EXISTS);
        assertEquals(1, ModuleRootManager.getInstance(getModule()).orderEntries().process(new LibraryCountingRootPolicy(), 0).intValue());
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
        String version = JsLibraryStdDetectionUtil.INSTANCE.getJsLibraryStdVersion(jsRuntime);
        assertEquals("1.1.0", version);
    }

    public void testJsLibraryVersion106() {
        Library jsRuntime = KotlinRuntimeLibraryUtilKt.findAllUsedLibraries(myProject).keySet().iterator().next();
        String version = JsLibraryStdDetectionUtil.INSTANCE.getJsLibraryStdVersion(jsRuntime);
        assertEquals("1.0.6", version);
    }

    @SuppressWarnings("ConstantConditions")
    public void testJvmProjectWithV1FacetConfig() {
        KotlinFacetSettings settings = KotlinFacetSettingsProvider.Companion.getInstance(myProject).getInitializedSettings(getModule());
        K2JVMCompilerArguments arguments = (K2JVMCompilerArguments) settings.getCompilerArguments();
        assertEquals(false, settings.getUseProjectSettings());
        assertEquals("1.1", settings.getLanguageLevel().getDescription());
        assertEquals("1.0", settings.getApiLevel().getDescription());
        assertEquals(TargetPlatformKind.Jvm.Companion.get(JvmTarget.JVM_1_8), settings.getTargetPlatformKind());
        assertEquals("1.1", arguments.getLanguageVersion());
        assertEquals("1.0", arguments.getApiVersion());
        assertEquals(LanguageFeature.State.ENABLED_WITH_WARNING, CoroutineSupport.byCompilerArguments(arguments));
        assertEquals("1.7", arguments.getJvmTarget());
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
        assertEquals("1.1", arguments.getLanguageVersion());
        assertEquals("1.0", arguments.getApiVersion());
        assertEquals(LanguageFeature.State.ENABLED_WITH_WARNING, CoroutineSupport.byCompilerArguments(arguments));
        assertEquals("amd", arguments.getModuleKind());
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
        assertEquals("1.1", arguments.getLanguageVersion());
        assertEquals("1.0", arguments.getApiVersion());
        assertEquals(LanguageFeature.State.ENABLED, CoroutineSupport.byCompilerArguments(arguments));
        assertEquals("1.7", arguments.getJvmTarget());
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
        assertEquals("1.1", arguments.getLanguageVersion());
        assertEquals("1.0", arguments.getApiVersion());
        assertEquals(LanguageFeature.State.ENABLED_WITH_ERROR, CoroutineSupport.byCompilerArguments(arguments));
        assertEquals("amd", arguments.getModuleKind());
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
        assertEquals("1.1", arguments.getLanguageVersion());
        assertEquals("1.0", arguments.getApiVersion());
        assertEquals(LanguageFeature.State.ENABLED, CoroutineSupport.byCompilerArguments(arguments));
        assertEquals("1.7", arguments.getJvmTarget());
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
            assertEquals(jvmTarget.getDescription(),
                         ((K2JVMCompilerArguments) facet.getConfiguration().getSettings().getCompilerArguments()).getJvmTarget());
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

    private static class LibraryCountingRootPolicy extends RootPolicy<Integer> {
        @Override
        public Integer visitLibraryOrderEntry(LibraryOrderEntry libraryOrderEntry, Integer value) {
            return value + 1;
        }
    }
}
