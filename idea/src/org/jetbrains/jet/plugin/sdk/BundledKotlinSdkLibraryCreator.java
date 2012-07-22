/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.sdk;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.NewLibraryEditor;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.util.PluginPathUtil;

import java.io.File;
import java.util.Arrays;

/**
 * @author Maxim.Manuylov
 *         Date: 23.07.12
 */
public class BundledKotlinSdkLibraryCreator implements ApplicationComponent {
    @Override
    public void initComponent() {
        createBundledSdkLibraryIfNeeded();
    }

    private static void createBundledSdkLibraryIfNeeded() {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                final LibrariesContainer librariesContainer = LibrariesContainerFactory.createContainer((Project) null);
                if (!bundledSdkLibraryExists(librariesContainer)) {
                    final File bundledSDKHome = PluginPathUtil.getBundledSDKHome();
                    if (bundledSDKHome != null) {
                        final String version = KotlinSdkUtil.getSDKVersion(bundledSDKHome);
                        if (version != null) {
                            final NewLibraryEditor editor = new NewLibraryEditor();
                            editor.setName(KotlinSdkUtil.getSDKName(bundledSDKHome, version));
                            KotlinSdkDescription.addSDKRoots(editor, bundledSDKHome);
                            librariesContainer.createLibrary(editor, LibrariesContainer.LibraryLevel.GLOBAL);
                        }
                    }
                }
            }
        });
    }

    private static boolean bundledSdkLibraryExists(@NotNull final LibrariesContainer librariesContainer) {
        final Library[] globalLibraries = librariesContainer.getLibraries(LibrariesContainer.LibraryLevel.GLOBAL);
        for (final Library library : globalLibraries) {
            final File sdkHome = KotlinSdkUtil.detectSDKHome(Arrays.asList(library.getFiles(OrderRootType.CLASSES)));
            if (sdkHome != null && KotlinSdkUtil.isBundledSDK(sdkHome)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void disposeComponent() {}

    @NotNull
    @Override
    public String getComponentName() {
        return getClass().getName();
    }
}
