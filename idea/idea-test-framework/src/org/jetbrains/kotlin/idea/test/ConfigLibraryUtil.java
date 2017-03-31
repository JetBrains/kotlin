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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.NewLibraryEditor;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VfsUtil;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.util.application.ApplicationUtilsKt;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.utils.PathUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Helper for configuring kotlin runtime in tested project.
 */
public class ConfigLibraryUtil {
    private static final String DEFAULT_JAVA_RUNTIME_LIB_NAME = "JAVA_RUNTIME_LIB_NAME";
    private static final String DEFAULT_KOTLIN_TEST_LIB_NAME = "KOTLIN_TEST_LIB_NAME";
    private static final String DEFAULT_KOTLIN_JS_STDLIB_NAME = "KOTLIN_JS_STDLIB_NAME";

    private ConfigLibraryUtil() {
    }

    private static NewLibraryEditor getKotlinRuntimeLibEditor(String libName, File library) {
        NewLibraryEditor editor = new NewLibraryEditor();
        editor.setName(libName);
        editor.addRoot(VfsUtil.getUrlForLibraryRoot(library), OrderRootType.CLASSES);

        return editor;
    }

    public static void configureKotlinRuntimeAndSdk(Module module, Sdk sdk) {
        configureSdk(module, sdk);
        configureKotlinRuntime(module);
    }

    public static void configureKotlinJsRuntimeAndSdk(Module module, Sdk sdk) {
        configureSdk(module, sdk);
        configureKotlinJsRuntime(module);
    }

    public static void configureKotlinRuntime(Module module) {
        addLibrary(getKotlinRuntimeLibEditor(DEFAULT_JAVA_RUNTIME_LIB_NAME, PathUtil.getKotlinPathsForDistDirectory().getStdlibPath()),
                   module);
        addLibrary(getKotlinRuntimeLibEditor(DEFAULT_KOTLIN_TEST_LIB_NAME, PathUtil.getKotlinPathsForDistDirectory().getKotlinTestPath()),
                   module);
    }

    public static void configureKotlinJsRuntime(Module module) {
        addLibrary(getKotlinRuntimeLibEditor(DEFAULT_KOTLIN_JS_STDLIB_NAME,
                                             PathUtil.getKotlinPathsForDistDirectory().getJsStdLibJarPath()), module);
    }

    public static void unConfigureKotlinRuntime(Module module) {
        removeLibrary(module, DEFAULT_JAVA_RUNTIME_LIB_NAME);
        removeLibrary(module, DEFAULT_KOTLIN_TEST_LIB_NAME);
    }

    public static void unConfigureKotlinRuntimeAndSdk(Module module, Sdk sdk) {
        configureSdk(module, sdk);
        unConfigureKotlinRuntime(module);
    }

    public static void unConfigureKotlinJsRuntimeAndSdk(Module module, Sdk sdk) {
        configureSdk(module, sdk);
        removeLibrary(module, DEFAULT_KOTLIN_JS_STDLIB_NAME);
    }

    public static void configureSdk(@NotNull final Module module, @NotNull final Sdk sdk) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
                ModifiableRootModel rootModel = rootManager.getModifiableModel();

                rootModel.setSdk(sdk);
                rootModel.commit();
            }
        });
    }

    public static Library addLibrary(final NewLibraryEditor editor, final Module module) {
        return ApplicationManager.getApplication().runWriteAction(new Computable<Library>() {
            @Override
            public Library compute() {
                ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
                ModifiableRootModel model = rootManager.getModifiableModel();

                Library library = addLibrary(editor, model);

                model.commit();

                return library;
            }
        });
    }

    public static Library addLibrary(NewLibraryEditor editor, ModifiableRootModel model) {
        Library library = model.getModuleLibraryTable().createLibrary(editor.getName());

        Library.ModifiableModel libModel = library.getModifiableModel();
        editor.applyTo((LibraryEx.ModifiableModelEx) libModel);

        libModel.commit();

        return library;
    }


    public static boolean removeLibrary(@NotNull final Module module, @NotNull final String libraryName) {
        return ApplicationUtilsKt.runWriteAction(
                new Function0<Boolean>() {
                    @Override
                    public Boolean invoke() {
                        boolean removed = false;

                        ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
                        ModifiableRootModel model = rootManager.getModifiableModel();

                        for (OrderEntry orderEntry : model.getOrderEntries()) {
                            if (orderEntry instanceof LibraryOrderEntry) {
                                LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry) orderEntry;

                                Library library = libraryOrderEntry.getLibrary();
                                if (library != null) {
                                    String name = library.getName();
                                    if (name != null && name.equals(libraryName)) {

                                        // Dispose attached roots
                                        Library.ModifiableModel modifiableModel = library.getModifiableModel();
                                        for (String rootUrl : library.getRootProvider().getUrls(OrderRootType.CLASSES)) {
                                            modifiableModel.removeRoot(rootUrl, OrderRootType.CLASSES);
                                        }
                                        for (String rootUrl : library.getRootProvider().getUrls(OrderRootType.SOURCES)) {
                                            modifiableModel.removeRoot(rootUrl, OrderRootType.SOURCES);
                                        }
                                        modifiableModel.commit();

                                        model.getModuleLibraryTable().removeLibrary(library);

                                        removed = true;
                                        break;
                                    }
                                }
                            }
                        }

                        model.commit();

                        return removed;
                    }
                }
        );
    }

    public static void addLibrary(@NotNull Module module, @NotNull String libraryName, @NotNull String rootPath, @NotNull String[] jarPaths) {
        NewLibraryEditor editor = new NewLibraryEditor();
        editor.setName(libraryName);
        for (String jarPath : jarPaths) {
            editor.addRoot(VfsUtil.getUrlForLibraryRoot(new File(rootPath, jarPath)), OrderRootType.CLASSES);
        }

        addLibrary(editor, module);
    }

    public static void configureLibraries(@NotNull Module module, String rootPath, List<String> libraryInfos) {
        for (String libraryInfo : libraryInfos) {
            int i = libraryInfo.indexOf('@');
            String libraryName = libraryInfo.substring(0, i);
            String[] jarPaths = libraryInfo.substring(i + 1).split(";");
            addLibrary(module, libraryName, rootPath, jarPaths);
        }
    }

    public static void unconfigureLibrariesByName(@NotNull Module module, List<String> libraryNames) {
        for (Iterator<String> iterator = libraryNames.iterator(); iterator.hasNext(); ) {
            String libraryName = iterator.next();
            if (removeLibrary(module, libraryName)) {
                iterator.remove();
            }
        }

        if (!libraryNames.isEmpty()) throw new AssertionError("Couldn't find the following libraries: " + libraryNames);
    }

    public static void unconfigureLibrariesByInfo(@NotNull Module module, List<String> libraryInfos) {
        List<String> libraryNames = new ArrayList<String>();
        for (String libraryInfo : libraryInfos) {
            libraryNames.add(libraryInfo.substring(0, libraryInfo.indexOf('@')));
        }
        unconfigureLibrariesByName(module, libraryNames);
    }

    public static void configureLibrariesByDirective(@NotNull Module module, String rootPath, String fileText) {
        configureLibraries(module, rootPath, InTextDirectivesUtils.findListWithPrefixes(fileText, "// CONFIGURE_LIBRARY: "));
    }

    public static void unconfigureLibrariesByDirective(@NotNull Module module, String fileText) {
        List<String> libraryNames = new ArrayList<String>();
        for (String libInfo : InTextDirectivesUtils.findListWithPrefixes(fileText, "// CONFIGURE_LIBRARY: ")) {
            libraryNames.add(libInfo.substring(0, libInfo.indexOf('@')));
        }
        for (String libraryName : InTextDirectivesUtils.findListWithPrefixes(fileText, "// UNCONFIGURE_LIBRARY: ")) {
            libraryNames.add(libraryName);
        }

        unconfigureLibrariesByName(module, libraryNames);
    }
}
