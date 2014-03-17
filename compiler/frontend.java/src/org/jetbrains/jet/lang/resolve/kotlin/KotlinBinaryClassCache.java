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

package org.jetbrains.jet.lang.resolve.kotlin;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.SLRUCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class KotlinBinaryClassCache implements Disposable {

    // This cache must be small: we only query the same file a few times in a row (from different places)
    // It's local to each thread: we don't want a single instance to synchronize access on, because VirtualFileKotlinClass.create involves
    // reading files from disk and may take some time
    private final ThreadLocal<SLRUCache<VirtualFile, Ref<VirtualFileKotlinClass>>> cache =
            new ThreadLocal<SLRUCache<VirtualFile, Ref<VirtualFileKotlinClass>>>() {
                @Override
                protected SLRUCache<VirtualFile, Ref<VirtualFileKotlinClass>> initialValue() {
                    return new SLRUCache<VirtualFile, Ref<VirtualFileKotlinClass>>(2, 2) {
                        @NotNull
                        @Override
                        public Ref<VirtualFileKotlinClass> createValue(VirtualFile virtualFile) {
                            return Ref.create(VirtualFileKotlinClass.create(virtualFile));
                        }
                    };
                }
            };

    @Nullable
    public static KotlinJvmBinaryClass getKotlinBinaryClass(@NotNull VirtualFile file) {
        KotlinBinaryClassCache service = ServiceManager.getService(KotlinBinaryClassCache.class);
        return service.cache.get().get(file).get();
    }

    @Override
    public void dispose() {
        // This is only relevant for tests. We create a new instance of Application for each test, and so a new instance of this service is
        // also created for each test. However all tests share the same event dispatch thread, which would collect all instances of this
        // thread-local if they're not removed properly. Each instance would transitively retain VFS resulting in OutOfMemoryError
        cache.remove();
    }
}
