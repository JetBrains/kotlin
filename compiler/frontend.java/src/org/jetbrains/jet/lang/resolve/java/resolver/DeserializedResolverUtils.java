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

package org.jetbrains.jet.lang.resolve.java.resolver;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.ClassId;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassOrNamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.ArrayList;
import java.util.List;

public class DeserializedResolverUtils {
    private DeserializedResolverUtils() {
    }

    @NotNull
    public static FqName kotlinFqNameToJavaFqName(@NotNull FqNameUnsafe kotlinFqName) {
        List<String> correctedSegments = new ArrayList<String>();
        for (Name segment : kotlinFqName.pathSegments()) {
            if (segment.asString().startsWith("<class-object-for")) {
                correctedSegments.add(JvmAbi.CLASS_OBJECT_CLASS_NAME);
            }
            else {
                assert !segment.isSpecial();
                correctedSegments.add(segment.asString());
            }
        }
        return FqName.fromSegments(correctedSegments);
    }

    @Nullable
    public static VirtualFile getVirtualFile(
            @NotNull PsiClass psiClass,
            @NotNull FqName classFqName,
            @NotNull ClassOrNamespaceDescriptor containingDeclaration
    ) {
        VirtualFile mostOuterClassVirtualFile = psiClass.getContainingFile().getVirtualFile();
        if (mostOuterClassVirtualFile == null) {
            throw new IllegalStateException("Could not find virtual file for " + classFqName.asString());
        }
        String fileExtension = mostOuterClassVirtualFile.getExtension();
        if (fileExtension == null || !fileExtension.equals("class")) {
            return null;
        }
        ClassId id = ClassId.fromFqNameAndContainingDeclaration(classFqName, containingDeclaration);
        FqNameUnsafe relativeClassName = id.getRelativeClassName();
        assert relativeClassName.isSafe() : "Relative class name " + relativeClassName.asString() + " should be safe at this point";
        String classNameWithBucks = relativeClassName.asString().replace(".", "$") + ".class";
        VirtualFile virtualFile = mostOuterClassVirtualFile.getParent().findChild(classNameWithBucks);
        if (virtualFile == null) {
            throw new IllegalStateException("No virtual file for " + classFqName.asString());
        }
        return virtualFile;
    }

    @NotNull
    public static FqNameUnsafe naiveKotlinFqName(@NotNull ClassDescriptor descriptor) {
        DeclarationDescriptor containing = descriptor.getContainingDeclaration();
        if (containing instanceof ClassDescriptor) {
            return naiveKotlinFqName((ClassDescriptor) containing).child(descriptor.getName());
        }
        else if (containing instanceof NamespaceDescriptor) {
            return DescriptorUtils.getFQName(containing).child(descriptor.getName());
        }
        else {
            throw new IllegalArgumentException("Class doesn't have a FQ name: " + descriptor);
        }
    }
}
