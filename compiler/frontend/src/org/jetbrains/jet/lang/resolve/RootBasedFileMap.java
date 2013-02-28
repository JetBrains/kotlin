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

package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Maps;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class RootBasedFileMap<D> {
    private final Map<VirtualFile, D> data;

    public RootBasedFileMap() {
        this(Maps.<VirtualFile, D>newHashMap());
    }

    public RootBasedFileMap(@NotNull Map<VirtualFile, D> data) {
        this.data = data;
    }

    @Nullable
    public D getDataForFile(@NotNull VirtualFile file) {
        VirtualFile currentFile = file;
        while (currentFile != null) {
            D rootData = data.get(currentFile);
            if (rootData != null) return rootData;

            currentFile = currentFile.getParent();
        }
        return null;
    }

    @Nullable
    public D getDataForRoot(@NotNull VirtualFile root) {
        return data.get(root);
    }

    public D putDataForRoot(@NotNull VirtualFile root, D data) {
        return this.data.put(root, data);
    }
}
