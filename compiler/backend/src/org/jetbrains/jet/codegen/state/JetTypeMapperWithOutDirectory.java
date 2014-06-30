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

package org.jetbrains.jet.codegen.state;

import com.intellij.openapi.vfs.StandardFileSystems;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.ClassBuilderMode;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaPackageFragmentDescriptor;
import org.jetbrains.jet.lang.resolve.java.lazy.descriptors.LazyPackageFragmentScopeForJavaPackage;
import org.jetbrains.jet.lang.resolve.kotlin.KotlinJvmBinaryClass;
import org.jetbrains.jet.lang.resolve.kotlin.VirtualFileKotlinClass;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import java.io.File;
import java.util.Set;

// TODO Temporary hack until modules infrastructure is implemented.
// This class is necessary for detecting if compiled class is from the same module as callee.
// Module chunks are treated as single module.
public class JetTypeMapperWithOutDirectory extends JetTypeMapper {
    private final File outDirectory;

    public JetTypeMapperWithOutDirectory(
            @NotNull BindingContext bindingContext,
            @NotNull ClassBuilderMode classBuilderMode,
            @Nullable File outDirectory
    ) {
        super(bindingContext, classBuilderMode);
        this.outDirectory = outDirectory;
    }

    @Override
    protected boolean isContainedByCompiledPartOfOurModule(@NotNull DeclarationDescriptor descriptor) {
        if (outDirectory == null) {
            return false;
        }

        if (!(descriptor.getContainingDeclaration() instanceof JavaPackageFragmentDescriptor)) {
            return false;
        }
        JavaPackageFragmentDescriptor packageFragment = (JavaPackageFragmentDescriptor) descriptor.getContainingDeclaration();
        JetScope packageScope = packageFragment.getMemberScope();
        if (!(packageScope instanceof LazyPackageFragmentScopeForJavaPackage)) {
            return false;
        }
        KotlinJvmBinaryClass binaryClass = ((LazyPackageFragmentScopeForJavaPackage) packageScope).getKotlinBinaryClass();

        if (binaryClass instanceof VirtualFileKotlinClass) {
            VirtualFile file = ((VirtualFileKotlinClass) binaryClass).getFile();
            if (file.getFileSystem().getProtocol() == StandardFileSystems.FILE_PROTOCOL) {
                File ioFile = VfsUtilCore.virtualToIoFile(file);
                return ioFile.getAbsolutePath().startsWith(outDirectory.getAbsolutePath() + File.separator);
            }
        }
        return false;
    }


}
