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

package org.jetbrains.jet.plugin.libraries;

import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.kotlin.KotlinBinaryClassCache;
import org.jetbrains.jet.lang.resolve.kotlin.header.KotlinClassHeader;

public final class DecompiledUtils {

    public static boolean isKotlinCompiledFile(@NotNull VirtualFile file) {
        if (!StdFileTypes.CLASS.getDefaultExtension().equals(file.getExtension())) {
            return false;
        }

        KotlinClassHeader header = KotlinBinaryClassCache.getKotlinBinaryClass(file).getClassHeader();
        return header != null;
    }

    public static boolean isKotlinInternalCompiledFile(@NotNull VirtualFile file) {
        KotlinClassHeader header = KotlinBinaryClassCache.getKotlinBinaryClass(file).getClassHeader();
        return header != null && header.getKind() == KotlinClassHeader.Kind.PACKAGE_FRAGMENT;
    }

    public static CharSequence decompile(@NotNull VirtualFile file) {
        Project project = ProjectManager.getInstance().getOpenProjects()[0];  // FIXME: get rid of project usage here
        return JetDecompiledData.getDecompiledData(file, project).getFileText();
    }

    private DecompiledUtils() {
    }
}
