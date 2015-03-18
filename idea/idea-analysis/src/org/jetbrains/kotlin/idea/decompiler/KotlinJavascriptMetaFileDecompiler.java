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

package org.jetbrains.kotlin.idea.decompiler;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiManager;
import com.intellij.psi.compiled.ClassFileDecompilers;
import com.intellij.psi.compiled.ClsStubBuilder;
import com.intellij.psi.stubs.PsiFileStub;
import com.intellij.util.indexing.FileContent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.decompiler.stubBuilder.KotlinClsStubBuilder;

public class KotlinJavascriptMetaFileDecompiler extends ClassFileDecompilers.Full {
    private final ClsStubBuilder stubBuilder = new KotlinClsStubBuilder() {
        @Nullable
        @Override
        public PsiFileStub<?> buildFileStub(@NotNull FileContent fileContent) {
            return null;
        }
    };

    @Override
    public boolean accepts(@NotNull VirtualFile file) {
        return file.getName().endsWith(".meta");
    }

    @NotNull
    @Override
    public ClsStubBuilder getStubBuilder() {
        return stubBuilder;
    }

    @NotNull
    @Override
    public FileViewProvider createFileViewProvider(@NotNull VirtualFile file, @NotNull PsiManager manager, boolean physical) {
        return new KotlinJavascriptMetaFileViewProvider(manager, file, physical, DecompilerPackage.isKotlinInternalCompiledFile(file));
    }
}
