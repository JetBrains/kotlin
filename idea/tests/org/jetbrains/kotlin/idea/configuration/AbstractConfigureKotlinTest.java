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

package org.jetbrains.kotlin.idea.configuration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathMacros;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.testFramework.PlatformTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.versions.LibraryJarDescriptor;
import org.jetbrains.kotlin.utils.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.idea.configuration.KotlinWithLibraryConfigurator.FileState;

public abstract class AbstractConfigureKotlinTest extends PlatformTestCase {
    private static final String BASE_PATH = "idea/testData/configuration/";
    private static final String TEMP_DIR_MACRO_KEY = "TEMP_TEST_DIR";
    protected static final KotlinJavaModuleConfigurator JAVA_CONFIGURATOR = new KotlinJavaModuleConfigurator() {
        @Override
        protected String getDefaultPathToJarFile(@NotNull Project project) {
            return getPathRelativeToTemp("default_jvm_lib");
        }
    };
    protected static final KotlinJsModuleConfigurator JS_CONFIGURATOR = new KotlinJsModuleConfigurator() {
        @Override
        protected String getDefaultPathToJarFile(@NotNull Project project) {
            return getPathRelativeToTemp("default_js_lib");
        }
    };

    private static void configure(
            @NotNull List<Module> modules,
            @NotNull FileState runtimeState,
            @NotNull KotlinWithLibraryConfigurator configurator,
            @NotNull String jarFromDist,
            @NotNull String jarFromTemp
    ) {
        Project project = modules.iterator().next().getProject();
        NotificationMessageCollector collector = NotificationMessageCollectorKt.createConfigureKotlinNotificationCollector(project);

        for (Module module : modules) {
            Library library = configurator.getKotlinLibrary(module);
            if (library == null) {
                library = configurator.createNewLibrary(project, collector);
            }
            String pathToJar = getPathToJar(runtimeState, jarFromDist, jarFromTemp);
            Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
            Library.ModifiableModel model = library.getModifiableModel();
            for (LibraryJarDescriptor descriptor : configurator.getLibraryJarDescriptors(sdk)) {
                configurator.configureLibraryJar(model, runtimeState, pathToJar, descriptor, collector);
            }
            ApplicationManager.getApplication().runWriteAction(model::commit);
            configurator.addLibraryToModuleIfNeeded(module, library, collector);
        }
        collector.showNotification();
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

    protected static void configure(@NotNull Module module, @NotNull FileState jarState, @NotNull KotlinProjectConfigurator configurator) {
        if (configurator instanceof KotlinJavaModuleConfigurator) {
            configure(Collections.singletonList(module), jarState,
                      (KotlinWithLibraryConfigurator) configurator,
                      getPathToExistentRuntimeJar(), getPathToNonexistentRuntimeJar());
        }
        if (configurator instanceof KotlinJsModuleConfigurator) {
            configure(Collections.singletonList(module), jarState,
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
        return PathUtil.getKotlinPathsForDistDirectory().getStdlibPath().getParent();
    }

    private static String getPathToExistentJsJar() {
        return PathUtil.getKotlinPathsForDistDirectory().getJsStdLibJarPath().getParent();
    }

    protected static void assertNotConfigured(Module module, KotlinWithLibraryConfigurator configurator) {
        assertFalse(
                String.format("Module %s should not be configured as %s Module", module.getName(), configurator.getPresentableText()),
                configurator.isConfigured(module));
    }

    protected static void assertConfigured(Module module, KotlinWithLibraryConfigurator configurator) {
        assertTrue(String.format("Module %s should be configured with configurator '%s'", module.getName(),
                                 configurator.getPresentableText()),
                   configurator.isConfigured(module));
    }

    protected static void assertProperlyConfigured(Module module, KotlinWithLibraryConfigurator configurator) {
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

    protected void doTestConfigureModulesWithNonDefaultSetup(KotlinWithLibraryConfigurator configurator) {
        assertNoFilesInDefaultPaths();

        Module[] modules = getModules();
        for (Module module : modules) {
            assertNotConfigured(module, configurator);
        }

        configurator.configure(myProject, Collections.<Module>emptyList());

        assertNoFilesInDefaultPaths();

        for (Module module : modules) {
            assertProperlyConfigured(module, configurator);
        }
    }

    protected void doTestOneJavaModule(@NotNull FileState jarState) {
        doTestOneModule(jarState, JAVA_CONFIGURATOR);
    }

    protected void doTestOneJsModule(@NotNull FileState jarState) {
        doTestOneModule(jarState, JS_CONFIGURATOR);
    }

    private void doTestOneModule(@NotNull FileState jarState, @NotNull KotlinWithLibraryConfigurator configurator) {
        Module module = getModule();

        assertNotConfigured(module, configurator);
        configure(module, jarState, configurator);
        assertProperlyConfigured(module, configurator);
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
        String projectFilePath = getProjectRoot() + "/projectFile.ipr";
        assertTrue("Project file should exists " + projectFilePath, new File(projectFilePath).exists());
        return new File(projectFilePath);
    }

    @Override
    protected Project doCreateProject(@NotNull File projectFile) throws Exception {
        return myProjectManager.loadProject(projectFile.getPath());
    }

    private String getProjectName() {
        String testName = getTestName(true);
        if (testName.contains("_")) {
            return testName.substring(0, testName.indexOf("_"));
        }
        return testName;
    }

    protected String getProjectRoot() {
        return BASE_PATH + getProjectName();
    }

    @Override
    protected void setUpModule() {
    }

    private void assertNoFilesInDefaultPaths() {
        assertDoesntExist(new File(JAVA_CONFIGURATOR.getDefaultPathToJarFile(getProject())));
        assertDoesntExist(new File(JS_CONFIGURATOR.getDefaultPathToJarFile(getProject())));
    }
}
