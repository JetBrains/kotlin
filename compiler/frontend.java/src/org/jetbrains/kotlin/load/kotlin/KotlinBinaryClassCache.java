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

package org.jetbrains.kotlin.load.kotlin;

import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class KotlinBinaryClassCache implements Disposable {
    private static class RequestCache {
        VirtualFile virtualFile;
        long modificationStamp;
        VirtualFileKotlinClass virtualFileKotlinClass;

        public VirtualFileKotlinClass cache(VirtualFile file, VirtualFileKotlinClass aClass) {
            virtualFile = file;
            virtualFileKotlinClass = aClass;
            modificationStamp = file.getModificationStamp();

            return aClass;
        }
    }

    private final ThreadLocal<RequestCache> cache =
            new ThreadLocal<RequestCache>() {
                @Override
                protected RequestCache initialValue() {
                    return new RequestCache();
                }
            };

    @Nullable
    public static KotlinJvmBinaryClass getKotlinBinaryClass(@NotNull final VirtualFile file) {
        if (file.getFileType() != JavaClassFileType.INSTANCE) return null;

        KotlinBinaryClassCache service = ServiceManager.getService(KotlinBinaryClassCache.class);
        RequestCache requestCache = service.cache.get();

        if (file.getModificationStamp() == requestCache.modificationStamp && file.equals(requestCache.virtualFile)) {
            return requestCache.virtualFileKotlinClass;
        }
        else {
            VirtualFileKotlinClass aClass = ApplicationManager.getApplication().runReadAction(new Computable<VirtualFileKotlinClass>() {
                @Override
                public VirtualFileKotlinClass compute() {
                    //noinspection deprecation
                    return VirtualFileKotlinClass.OBJECT$.create(file);
                }
            });

            return requestCache.cache(file, aClass);
        }
    }

    @Override
    public void dispose() {
        // This is only relevant for tests. We create a new instance of Application for each test, and so a new instance of this service is
        // also created for each test. However all tests share the same event dispatch thread, which would collect all instances of this
        // thread-local if they're not removed properly. Each instance would transitively retain VFS resulting in OutOfMemoryError
        cache.remove();
    }
}
