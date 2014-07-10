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

import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.java.structure.impl.JavaClassImpl;
import org.jetbrains.jet.lang.resolve.name.FqName;

public abstract class VirtualFileKotlinClassFinder implements VirtualFileFinder {
    @Nullable
    @Override
    public KotlinJvmBinaryClass findKotlinClass(@NotNull FqName fqName) {
        VirtualFile file = findVirtualFileWithHeader(fqName);
        return file == null ? null : KotlinBinaryClassCache.getKotlinBinaryClass(file);
    }

    @Override
    @Nullable
    public KotlinJvmBinaryClass findKotlinClass(@NotNull JavaClass javaClass) {
        VirtualFile file = ((JavaClassImpl) javaClass).getPsi().getContainingFile().getVirtualFile();
        if (file == null) return null;

        if (javaClass.getOuterClass() != null) {
            // For nested classes we get a file of the containing class, to get the actual class file for A.B.C,
            // we take the file for A, take its parent directory, then in this directory we look for A$B$C.class
            file = file.getParent().findChild(classFileName(javaClass) + ".class");
            assert file != null : "Virtual file not found for " + javaClass;
        }

        if (file.getFileType() != JavaClassFileType.INSTANCE) return null;

        return KotlinBinaryClassCache.getKotlinBinaryClass(file);
    }

    @NotNull
    private static String classFileName(@NotNull JavaClass jClass) {
        JavaClass outerClass = jClass.getOuterClass();
        if (outerClass == null) return jClass.getName().asString();
        return classFileName(outerClass) + "$" + jClass.getName().asString();
    }
}
