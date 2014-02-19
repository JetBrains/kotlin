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

package org.jetbrains.jet.plugin.libraries;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.JetLanguage;

public class JetClassFileViewProvider extends SingleRootFileViewProvider {
    private final boolean isInternal;

    public JetClassFileViewProvider(@NotNull PsiManager manager, @NotNull VirtualFile file, boolean physical, boolean internal) {
        super(manager, file, physical, JetLanguage.INSTANCE);
        isInternal = internal;
    }

    @NotNull
    @Override
    public CharSequence getContents() {
        return isInternal ? "" : DecompiledUtils.decompile(getVirtualFile());
    }

    @Nullable
    @Override
    protected PsiFile createFile(@NotNull Project project, @NotNull VirtualFile file, @NotNull FileType fileType) {
        return isInternal ? null : new JetDecompiledFile(this);
    }

    @NotNull
    @Override
    public SingleRootFileViewProvider createCopy(@NotNull VirtualFile copy) {
        return new JetClassFileViewProvider(getManager(), copy, false, isInternal);
    }

    private static class JetDecompiledFile extends ClsFileImpl {
        public JetDecompiledFile(FileViewProvider viewProvider) {
            super(viewProvider);
        }

        @Override
        public PsiFile getDecompiledPsiFile() {
            return JetDecompiledData.getDecompiledData(getVirtualFile(), getProject()).getFile();
        }
    }
}
