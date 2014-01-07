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

package org.jetbrains.jet.lang.resolve.kotlin;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.SLRUCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.storage.LockBasedStorageManager;

public abstract class VirtualFileKotlinClassFinder implements VirtualFileFinder {

    // This cache must be small: we only query the same file a few times in a row (from different places)
    private final SLRUCache<VirtualFile, KotlinJvmBinaryClass> cache = new SLRUCache<VirtualFile, KotlinJvmBinaryClass>(2, 2) {
        @NotNull
        @Override
        public KotlinJvmBinaryClass createValue(VirtualFile virtualFile) {
            // Operations under this lock are not supposed to involve other locks
            return new VirtualFileKotlinClass(new LockBasedStorageManager(), virtualFile);
        }
    };

    @Nullable
    @Override
    public KotlinJvmBinaryClass findKotlinClass(@NotNull FqName fqName) {
        VirtualFile file = findVirtualFile(fqName);
        return file == null ? null : createKotlinClass(file);
    }

    @Override
    @NotNull
    public KotlinJvmBinaryClass createKotlinClass(@NotNull VirtualFile file) {
        synchronized (cache) {
            return cache.get(file);
        }
    }
}
