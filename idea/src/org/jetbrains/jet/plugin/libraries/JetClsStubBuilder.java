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

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.compiled.ClsStubBuilder;
import com.intellij.psi.impl.compiled.ClassFileStubBuilder;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import com.intellij.psi.stubs.PsiFileStub;
import com.intellij.util.cls.ClsFormatException;
import com.intellij.util.indexing.FileContent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JetClsStubBuilder extends ClsStubBuilder {
    @Override
    public int getStubVersion() {
        return ClassFileStubBuilder.STUB_VERSION + 1;
    }

    @Nullable
    @Override
    public PsiFileStub<?> buildFileStub(@NotNull FileContent content) throws ClsFormatException {
        VirtualFile file = content.getFile();

        if (LibrariesPackage.isKotlinInternalCompiledFile(file)) {
            return null;
        }

        return ClsFileImpl.buildFileStub(file, content.getContent());
    }
}
