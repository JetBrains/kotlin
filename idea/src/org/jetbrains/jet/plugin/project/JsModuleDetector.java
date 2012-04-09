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

package org.jetbrains.jet.plugin.project;

import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Pavel Talanov
 *
 * This class has utility functions to determine whether the project (or module) is js project.
 */
public final class JsModuleDetector {

    public static final String INDICATION_FILE_NAME = ".kotlin-js";

    private JsModuleDetector() {
    }

    public static boolean isJsProject(@NotNull Project project) {
        return findIndicationFileInContextRoots(project) != null;
    }

    @Nullable
    public static VirtualFile findIndicationFileInContextRoots(@NotNull Project project) {
        VirtualFile[] roots = ProjectRootManager.getInstance(project).getContentRoots();
        for (VirtualFile root : roots) {
            for (VirtualFile child : root.getChildren()) {
                if (child.getFileType().equals(FileTypes.PLAIN_TEXT) && child.getName().equals(INDICATION_FILE_NAME)) {
                    return child;
                }
            }
        }
        return null;
    }
}
