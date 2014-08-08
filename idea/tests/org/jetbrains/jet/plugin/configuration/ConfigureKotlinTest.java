/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.configuration;

import com.intellij.openapi.application.PathMacros;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.testFramework.PlatformTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.utils.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.plugin.configuration.KotlinWithLibraryConfigurator.FileState;
import static org.jetbrains.jet.plugin.configuration.KotlinWithLibraryConfigurator.LibraryState;

public class ConfigureKotlinTest extends PlatformTestCase {
    private static final String BASE_PATH = "idea/testData/configuration/";

    private static final KotlinJavaModuleConfigurator JAVA_CONFIGURATOR = new KotlinJavaModuleConfigurator() {
        @Override
        protected String getDefaultPathToJarFile(@NotNull Project project) {
            return getPathRelativeToTemp("default_jvm_lib");
        }
    };

    private static final String TEMP_DIR_MACRO_KEY = "TEMP_TEST_DIR";

    private static final KotlinJsModuleConfigurator JS_CONFIGURATOR = new KotlinJsModuleConfigurator() {
        @Override
        protected String getDefaultPathToJarFile(@NotNull Project project) {
            return getPathRelativeToTemp("default_js_lib");
        }

        @NotNull
        @Override
        protected String getDefaultPathToJsFile(@NotNull Project project) {
            return getPathRelativeToTemp("default_script");
        }
    };

    @Override
    protected void tearDown() throws Exception {
        PathMacros.getInstance().removeMacro(TEMP_DIR_MACRO_KEY);

        super.tearDown();
    }

    @Override
    protected void initApplication() throws Exception {
        super.initApplication();

        File tempLibDir = FileUtil.createTempDirectory("temp", null);
        PathMacros.getInstance().setMacro(TEMP_DIR_MACRO_KEY, FileUtilRt.toSystemDependentName(tempLibDir.getAbsolutePath()));
    }

    public void testNewLibrary_copyJar() {
        doTestOneJavaModule(FileState.COPY, LibraryState.NEW_LIBRARY);
    }

    public void testNewLibrary_doNotCopyJar() {
        doTestOneJavaModule(FileState.DO_NOT_COPY, LibraryState.NEW_LIBRARY);
    }

    public void testLibrary_doNotCopyJar() {
        try {
            doTestOneJavaModule(FileState.DO_NOT_COPY, LibraryState.LIBRARY);
        }
        catch (IllegalStateException e) {
            return;
        }
        fail("Test should throw IllegalStateException");
    }

    public void testLibraryWithoutPaths_jarExists() {
        doTestOneJavaModule(FileState.EXISTS, LibraryState.NON_CONFIGURED_LIBRARY);
    }

    public void testNewLibrary_jarExists() {
        doTestOneJavaModule(FileState.EXISTS, LibraryState.NEW_LIBRARY);
    }

    public void testLibraryWithoutPaths_copyJar() {
        doTestOneJavaModule(FileState.COPY, LibraryState.NON_CONFIGURED_LIBRARY);
    }

    public void testLibraryWithoutPaths_doNotCopyJar() {
        doTestOneJavaModule(FileState.DO_NOT_COPY, LibraryState.NON_CONFIGURED_LIBRARY);
    }

    @SuppressWarnings("ConstantConditions")
    public void testTwoModules_exists() {
        Module[] modules = getModules();
        for (Module module : modules) {
            if (module.getName().equals("module1")) {
                configure(module, KotlinWithLibraryConfigurator.FileState.DO_NOT_COPY, LibraryState.NEW_LIBRARY, JAVA_CONFIGURATOR);
                assertConfigured(module, JAVA_CONFIGURATOR);
            }
            else if (module.getName().equals("module2")) {
                assertNotConfigured(module, JAVA_CONFIGURATOR);
                configure(module, FileState.EXISTS, LibraryState.LIBRARY, JAVA_CONFIGURATOR);
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
        JAVA_CONFIGURATOR.configure(myProject);
        assertProperlyConfigured(module, JAVA_CONFIGURATOR);
    }

    public void testTwoModulesWithNonDefaultPath_doNotCopyInDefault() throws IOException {
        doTestConfigureModulesWithNonDefaultSetup(JAVA_CONFIGURATOR);
    }

    public void testTwoModulesWithJSNonDefaultPath_doNotCopyInDefault() throws IOException {
        doTestConfigureModulesWithNonDefaultSetup(JS_CONFIGURATOR);
    }

    public void testNewLibrary_jarExists_js() {
        doTestOneJsModule(FileState.EXISTS, LibraryState.NEW_LIBRARY);
    }

    public void testNewLibrary_copyJar_js() {
        doTestOneJsModule(FileState.COPY, LibraryState.NEW_LIBRARY);
    }

    public void testNewLibrary_doNotCopyJar_js() {
        doTestOneJsModule(FileState.DO_NOT_COPY, LibraryState.NEW_LIBRARY);
    }

    public void testJsLibrary_doNotCopyJar() {
        try {
            doTestOneJsModule(FileState.DO_NOT_COPY, LibraryState.LIBRARY);
        }
        catch (IllegalStateException e) {
            return;
        }
        fail("Test should throw IllegalStateException");
    }

    public void testJsLibraryWithoutPaths_jarExists() {
        doTestOneJsModule(FileState.EXISTS, LibraryState.NON_CONFIGURED_LIBRARY);
    }

    public void testJsLibraryWithoutPaths_copyJar() {
        doTestOneJsModule(FileState.COPY, LibraryState.NON_CONFIGURED_LIBRARY);
    }

    public void testJsLibraryWithoutPaths_doNotCopyJar() {
        doTestOneJsModule(FileState.DO_NOT_COPY, LibraryState.NON_CONFIGURED_LIBRARY);
    }

    private void doTestConfigureModulesWithNonDefaultSetup(KotlinWithLibraryConfigurator configurator) {
        assertNoFilesInDefaultPaths(true);

        Module[] modules = getModules();
        for (Module module : modules) {
            assertNotConfigured(module, configurator);
        }

        configurator.configure(myProject);

        assertNoFilesInDefaultPaths(false);

        for (Module module : modules) {
            assertProperlyConfigured(module, configurator);
        }
    }

    private void doTestOneJavaModule(@NotNull FileState jarState, @NotNull LibraryState libraryState) {
        doTestOneModule(jarState, libraryState, JAVA_CONFIGURATOR);
    }

    private void doTestOneJsModule(@NotNull FileState jarState, @NotNull LibraryState libraryState) {
        doTestOneModule(jarState, libraryState, JS_CONFIGURATOR);
    }

    private void doTestOneModule(@NotNull FileState jarState, @NotNull LibraryState libraryState, @NotNull KotlinWithLibraryConfigurator configurator) {
        Module module = getModule();
        assertEquals("Library state loaded from project files should be " + libraryState, libraryState, configurator.getLibraryState(module.getProject()));

        assertNotConfigured(module, configurator);
        configure(module, jarState, libraryState, configurator);
        assertProperlyConfigured(module, configurator);
    }

    private static void configure(
            @NotNull List<Module> modules,
            @NotNull FileState runtimeState,
            @NotNull LibraryState libraryState,
            @NotNull KotlinWithLibraryConfigurator configurator,
            @NotNull String jarFromDist,
            @NotNull String jarFromTemp
    ) {
        for (Module module : modules) {
            String pathToJar = getPathToJar(runtimeState, jarFromDist, jarFromTemp);
            configurator.configureModuleWithLibraryClasses(module, libraryState, runtimeState, pathToJar);
        }
    }

    @NotNull
    private static String getPathToJar(@NotNull FileState runtimeState, @NotNull String jarFromDist, @NotNull String jarFromTemp) {
        switch (runtimeState) {
            case EXISTS:
                return jarFromDist;
            case COPY:
                return jarFromTemp;
            case DO_NOT_COPY:
                return jarFromDist;
        }
        return jarFromDist;
    }

    private static void configure(@NotNull Module module, @NotNull FileState jarState, @NotNull LibraryState libraryState, @NotNull KotlinProjectConfigurator configurator) {
        if (configurator instanceof KotlinJavaModuleConfigurator) {
            configure(Collections.singletonList(module), jarState, libraryState,
                      (KotlinWithLibraryConfigurator) configurator,
                      getPathToExistentRuntimeJar(), getPathToNonexistentRuntimeJar());
        }
        if (configurator instanceof KotlinJsModuleConfigurator) {
            configure(Collections.singletonList(module), jarState, libraryState,
                      (KotlinWithLibraryConfigurator) configurator,
                      getPathToExistentJsJar(), getPathToNonexistentJsJar());
        }
    }

    private static String getPathToNonexistentRuntimeJar() {
        String pathToTempKotlinRuntimeJar = FileUtil.getTempDirectory() + "/" + PathUtil.KOTLIN_JAVA_RUNTIME_JAR;
        myFilesToDelete.add(new File(pathToTempKotlinRuntimeJar));
        return pathToTempKotlinRuntimeJar;
    }

    private static String getPathToNonexistentJsJar() {
        String pathToTempKotlinRuntimeJar = FileUtil.getTempDirectory() + "/" + PathUtil.JS_LIB_JAR_NAME;
        myFilesToDelete.add(new File(pathToTempKotlinRuntimeJar));
        return pathToTempKotlinRuntimeJar;
    }

    private static String getPathToExistentRuntimeJar() {
        return PathUtil.getKotlinPathsForDistDirectory().getRuntimePath().getParent();
    }

    private static String getPathToExistentJsJar() {
        return PathUtil.getKotlinPathsForDistDirectory().getJsLibJarPath().getParent();
    }

    @Override
    public Module getModule() {
        Module[] modules = ModuleManager.getInstance(myProject).getModules();
        assert modules.length == 1 : "One module should be loaded " + modules.length;
        myModule = modules[0];
        return super.getModule();
    }

    public Module[] getModules() {
        return ModuleManager.getInstance(myProject).getModules();
    }

    @Override
    protected File getIprFile() throws IOException {
        String projectName = getProjectName();
        String projectFilePath = BASE_PATH + projectName + "/projectFile.ipr";
        assertTrue("Project file should exists " + projectFilePath, new File(projectFilePath).exists());
        return new File(projectFilePath);
    }

    @Override
    protected Project doCreateProject(File projectFile) throws Exception {
        return myProjectManager.loadProject(projectFile.getPath());
    }

    private String getProjectName() {
        String testName = getTestName(true);
        if (testName.contains("_")) {
            return testName.substring(0, testName.indexOf("_"));
        }
        return testName;
    }

    @Override
    protected void setUpModule() {
    }

    private void assertNoFilesInDefaultPaths(boolean checkJsFiles) {
        assertDoesntExist(new File(JAVA_CONFIGURATOR.getDefaultPathToJarFile(getProject())));
        assertDoesntExist(new File(JS_CONFIGURATOR.getDefaultPathToJarFile(getProject())));

        if (checkJsFiles) {
            assertDoesntExist(new File(JS_CONFIGURATOR.getDefaultPathToJsFile(getProject())));
        }
    }

    private static void assertNotConfigured(Module module, KotlinWithLibraryConfigurator configurator) {
        assertFalse(
                String.format("Module %s should not be configured as %s Module", module.getName(), configurator.getPresentableText()),
                configurator.isConfigured(module));
    }

    private static void assertConfigured(Module module, KotlinWithLibraryConfigurator configurator) {
        assertTrue(String.format("Module %s should be configured with configurator '%s'", module.getName(),
                                 configurator.getPresentableText()),
                   configurator.isConfigured(module));
    }

    private static void assertProperlyConfigured(Module module, KotlinWithLibraryConfigurator configurator) {
        assertConfigured(module, configurator);
        assertNotConfigured(module, getOppositeConfigurator(configurator));
    }

    private static KotlinWithLibraryConfigurator getOppositeConfigurator(KotlinWithLibraryConfigurator configurator) {
        if (configurator == JAVA_CONFIGURATOR) return JS_CONFIGURATOR;
        if (configurator == JS_CONFIGURATOR) return JAVA_CONFIGURATOR;

        throw new IllegalArgumentException("Only JS_CONFIGURATOR and JAVA_CONFIGURATOR are supported");
    }

    private static String getPathRelativeToTemp(String relativePath) {
        String tempPath = PathMacros.getInstance().getValue(TEMP_DIR_MACRO_KEY);
        return tempPath + '/' + relativePath;
    }
}
