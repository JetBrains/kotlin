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

package org.jetbrains.kotlin.idea.framework;

import com.intellij.framework.library.LibraryVersionProperties;
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinIcons;

import javax.swing.*;
import java.util.List;

public class JSLibraryStdPresentationProvider extends LibraryPresentationProvider<LibraryVersionProperties> {
    public static JSLibraryStdPresentationProvider getInstance() {
        return LibraryPresentationProvider.EP_NAME.findExtension(JSLibraryStdPresentationProvider.class);
    }

    protected JSLibraryStdPresentationProvider() {
        super(JSLibraryStdDescription.Companion.getKOTLIN_JAVASCRIPT_KIND());
    }

    @Nullable
    @Override
    public Icon getIcon(@Nullable LibraryVersionProperties properties) {
        return KotlinIcons.SMALL_LOGO;
    }

    @Nullable
    @Override
    public LibraryVersionProperties detect(@NotNull List<VirtualFile> classesRoots) {
        String version = JsLibraryStdDetectionUtil.INSTANCE.getJsLibraryStdVersion(classesRoots);
        return version == null ? null : new LibraryVersionProperties(version);
    }
}
