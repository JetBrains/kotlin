/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.framework;

import com.intellij.framework.library.LibraryVersionProperties;
import com.intellij.openapi.roots.libraries.LibraryKind;
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.CommonProcessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.JetIcons;

import javax.swing.*;
import java.util.List;

public class JsHeaderLibraryPresentationProvider extends CachingLibraryPresentationProvider<LibraryVersionProperties> {
    public static final LibraryKind KOTLIN_JAVASCRIPT_HEADER_KIND = LibraryKind.create("kotlin-js-header");

    public static JsHeaderLibraryPresentationProvider getInstance() {
        return LibraryPresentationProvider.EP_NAME.findExtension(JsHeaderLibraryPresentationProvider.class);
    }

    protected JsHeaderLibraryPresentationProvider() {
        super(KOTLIN_JAVASCRIPT_HEADER_KIND);
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return JetIcons.SMALL_LOGO_13;
    }

    @Nullable
    @Override
    public LibraryVersionProperties detect(@NotNull List<VirtualFile> classesRoots) {
        if (JavaRuntimePresentationProvider.getInstance().detect(classesRoots) != null) {
            // Prevent clashing with java runtime
            return null;
        }

        for (VirtualFile file : classesRoots) {
            CommonProcessors.FindFirstProcessor<VirtualFile> findKTProcessor = new CommonProcessors.FindFirstProcessor<VirtualFile>() {
                @Override
                protected boolean accept(VirtualFile file) {
                    String extension = file.getExtension();
                    return extension != null && extension.equals(JetFileType.INSTANCE.getDefaultExtension());
                }
            };

            VfsUtil.processFilesRecursively(file, findKTProcessor);

            if (findKTProcessor.isFound()) {
                return new LibraryVersionProperties(null);
            }
        }

        return null;
    }
}
