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

package org.jetbrains.jet.lang.resolve.kotlin.header;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.ClassReader;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.io.IOException;

import static org.jetbrains.asm4.ClassReader.*;
import static org.jetbrains.jet.lang.resolve.java.AbiVersionUtil.isAbiVersionCompatible;

public abstract class KotlinClassFileHeader {
    @Nullable
    public static KotlinClassFileHeader readKotlinHeaderFromClassFile(@NotNull VirtualFile virtualFile) {
        try {
            ClassReader reader = new ClassReader(virtualFile.contentsToByteArray());
            ReadDataFromAnnotationVisitor visitor = new ReadDataFromAnnotationVisitor();
            reader.accept(visitor, SKIP_CODE | SKIP_FRAMES | SKIP_DEBUG);
            return visitor.createHeader(virtualFile);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final int version;
    private final FqName fqName;

    protected KotlinClassFileHeader(int version, @NotNull FqName fqName) {
        this.version = version;
        this.fqName = fqName;
    }

    public int getVersion() {
        return version;
    }

    /**
     * @return FQ name for class header or package class FQ name for package header (e.g. <code>test.TestPackage</code>)
     */
    @NotNull
    public FqName getFqName() {
        return fqName;
    }

    /**
     * @return true if this is a header for compiled Kotlin file with correct abi version which can be processed by compiler or the IDE
     */
    public boolean isCompatibleKotlinCompiledFile() {
        return isAbiVersionCompatible(version);
    }
}
