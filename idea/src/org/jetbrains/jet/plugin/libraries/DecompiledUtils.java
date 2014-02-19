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
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.kotlin.VirtualFileKotlinClass;
import org.jetbrains.jet.lang.resolve.kotlin.header.KotlinClassHeader;
import org.jetbrains.jet.storage.LockBasedStorageManager;

public final class DecompiledUtils {
    private static final String PACKAGE_FRAGMENT_SIGNATURE = PackageClassUtils.PACKAGE_CLASS_NAME_SUFFIX + "-";

    public static boolean isKotlinCompiledFile(@NotNull VirtualFile file) {
        if (!StdFileTypes.CLASS.getDefaultExtension().equals(file.getExtension())) {
            return false;
        }

        return isKotlinInternalClass(file) || checkFile(file);
    }

    public static boolean isKotlinInternalClass(@NotNull VirtualFile file) {
        // FIXME: not sure if this is a good heuristic
        String name = file.getName();
        int pos = name.indexOf('$');
        if (pos > 0) {
            name = name.substring(0, pos) + ".class";
            VirtualFile supposedHost = file.getParent().findChild(name);
            if (supposedHost != null) {
                return checkFile(supposedHost);
            }
        }

        if (name.contains(PACKAGE_FRAGMENT_SIGNATURE)) {
            KotlinClassHeader header = new VirtualFileKotlinClass(LockBasedStorageManager.NO_LOCKS, file).getClassHeader();
            if (header != null && header.getKind() == KotlinClassHeader.Kind.PACKAGE_FRAGMENT) {
                return true;
            }
        }

        return false;
    }

    private static boolean checkFile(@NotNull VirtualFile file) {
        KotlinClassHeader header = new VirtualFileKotlinClass(LockBasedStorageManager.NO_LOCKS, file).getClassHeader();
        return header != null && header.getAnnotationData() != null;
    }

    public static CharSequence decompile(@NotNull VirtualFile file) {
        Project project = ProjectManager.getInstance().getOpenProjects()[0];  // FIXME: get rid of project usage here
        return JetDecompiledData.getDecompiledData(file, project).getFileText();
    }

    private DecompiledUtils() {
    }
}
