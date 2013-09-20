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

public class VirtualFileKotlinClass implements KotlinJvmBinaryClass {
    private final VirtualFile file;

    public VirtualFileKotlinClass(@NotNull VirtualFile file) {
        this.file = file;
    }

    @NotNull
    @Override
    public VirtualFile getFile() {
        return file;
    }

    @Override
    public int hashCode() {
        return file.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof VirtualFileKotlinClass && ((VirtualFileKotlinClass) obj).file.equals(file);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + file.toString();
    }
}
