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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.name.FqName;

import javax.inject.Inject;

public class VirtualFileKotlinClassFinder implements KotlinClassFinder {
    private VirtualFileFinder virtualFileFinder;

    @Inject
    public void setVirtualFileFinder(@NotNull VirtualFileFinder virtualFileFinder) {
        this.virtualFileFinder = virtualFileFinder;
    }

    @Nullable
    @Override
    public KotlinJvmBinaryClass findKotlinClass(@NotNull FqName fqName) {
        VirtualFile file = virtualFileFinder.findVirtualFile(fqName);
        return file == null ? null : new VirtualFileKotlinClass(file);
    }
}
