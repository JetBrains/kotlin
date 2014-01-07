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

package org.jetbrains.jet.plugin.stubindex.builder;

import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.impl.compiled.ClsStubBuilderFactory;
import com.intellij.psi.impl.java.stubs.impl.PsiJavaFileStubImpl;
import com.intellij.psi.stubs.PsiFileStub;
import com.intellij.util.cls.ClsFormatException;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.kotlin.KotlinJvmBinaryClass;
import org.jetbrains.jet.lang.resolve.kotlin.VirtualFileKotlinClass;
import org.jetbrains.jet.lang.resolve.kotlin.header.KotlinClassHeader;
import org.jetbrains.jet.lang.resolve.kotlin.header.PackageFragmentClassHeader;

/**
 * This class is needed to build an empty PSI stub for compiled package fragment classes. This results in these classes not showing up
 * in completion, go-to-class, etc.
 */
public class EmptyPackageFragmentClsStubBuilderFactory extends ClsStubBuilderFactory<PsiJavaFile> {

    @Nullable
    @Override
    public PsiFileStub<PsiJavaFile> buildFileStub(VirtualFile file, byte[] bytes) throws ClsFormatException {
        return new PsiJavaFileStubImpl(null, true);
    }

    @Override
    public boolean canBeProcessed(VirtualFile file, byte[] bytes) {
        if (file.getName().contains(PackageClassUtils.PACKAGE_CLASS_NAME_SUFFIX + "-") &&
            StdFileTypes.CLASS.getDefaultExtension().equals(file.getExtension())) {
            KotlinJvmBinaryClass kotlinClass = new VirtualFileKotlinClass(file);
            return KotlinClassHeader.read(kotlinClass) instanceof PackageFragmentClassHeader;
        }
        return false;
    }

    @Override
    public boolean isInnerClass(VirtualFile file) {
        return false;
    }
}
