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

package org.jetbrains.jet.plugin.framework;

import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.CommonProcessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetFileType;

import java.util.List;

public class JsHeaderLibraryDetectionUtil {
    public static boolean isJsHeaderLibraryDetected(@NotNull List<VirtualFile> classesRoots) {
        if (JavaRuntimeDetectionUtil.getJavaRuntimeVersion(classesRoots) != null) {
            // Prevent clashing with java runtime
            return false;
        }

        for (VirtualFile file : classesRoots) {
            CommonProcessors.FindFirstProcessor<VirtualFile> findKTProcessor = new CommonProcessors.FindFirstProcessor<VirtualFile>() {
                @Override
                protected boolean accept(VirtualFile file) {
                    String extension = file.getExtension();
                    return extension != null && extension.equals(JetFileType.INSTANCE.getDefaultExtension());
                }
            };

            VfsUtilCore.processFilesRecursively(file, findKTProcessor);

            if (findKTProcessor.isFound()) {
                return true;
            }
        }

        return false;

    }
}
