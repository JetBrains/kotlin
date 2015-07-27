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

package org.jetbrains.kotlin.load.kotlin;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.jvm.JvmClassName;
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor;
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.Collection;
import java.util.List;

import static org.jetbrains.kotlin.load.kotlin.PackageClassUtils.getPackageClassFqName;

public class PackagePartClassUtils {
    public static int getPathHashCode(@NotNull VirtualFile file) {
        return file.getPath().toLowerCase().hashCode();
    }

    @NotNull
    private static String getSanitizedIdentifier(@NotNull String str) {
        str = str.replaceAll("[^\\p{L}\\p{Digit}]", "_");
        if (!Character.isJavaIdentifierStart(str.charAt(0))) {
            str = "_" + str;
        }
        return str;
    }

    @NotNull
    @TestOnly
    public static FqName getPackagePartFqName(@NotNull FqName facadeFqName, @NotNull VirtualFile file) {
        return getPackagePartFqName(facadeFqName, file, null);
    }

    private static char toUpperAscii(char ch) {
        if ('a' <= ch && ch <= 'z') {
            return (char) (ch - 32);
        }
        else {
            return ch;
        }
    }

    @NotNull
    public static FqName getPackagePartFqName(@NotNull FqName facadeFqName, @NotNull VirtualFile file, @Nullable JetFile jetFile) {
        String fileName = FileUtil.getNameWithoutExtension(PathUtil.getFileName(file.getName()));

        if (!fileName.isEmpty()) {
            char c = fileName.charAt(0);
            // Character.isUpperCase also handles non-latin characters - this is not what we want in some locales.
            // Use "ASCII capitalization".
            if ('a' <= c && c <= 'z') {
                fileName = toUpperAscii(c) + fileName.substring(1);
            }
            fileName += "Kt";
        }

        return facadeFqName.parent().child(Name.identifier(getSanitizedIdentifier(fileName)));
    }

    @NotNull
    public static Type getPackagePartType(@NotNull JetFile file) {
        return Type.getObjectType(getPackagePartInternalName(file));
    }

    @NotNull
    public static String getPackagePartInternalName(@NotNull JetFile file) {
        FqName fqName = getPackagePartFqName(file);
        return JvmClassName.byFqNameWithoutInnerClasses(fqName).getInternalName();
    }

    @NotNull
    public static FqName getPackagePartFqName(@NotNull JetFile file) {
        return getPackagePartFqName(getPackageClassFqName(file.getPackageFqName()), file.getVirtualFile(), file);
    }

    @NotNull
    public static FqName getPackagePartFqName(@NotNull DeserializedCallableMemberDescriptor callable) {
        FqName packageFqName = ((PackageFragmentDescriptor) callable.getContainingDeclaration()).getFqName();
        return packageFqName.child(callable.getNameResolver().getName(callable.getProto().getExtension(JvmProtoBuf.implClassName)));
    }

    @NotNull
    public static List<JetFile> getPackageFilesWithCallables(@NotNull Collection<JetFile> packageFiles) {
        return ContainerUtil.filter(packageFiles, new Condition<JetFile>() {
            @Override
            public boolean value(JetFile packageFile) {
                return fileHasCallables(packageFile);
            }
        });
    }

    public static boolean fileHasCallables(@NotNull JetFile file) {
        for (JetDeclaration declaration : file.getDeclarations()) {
            if (declaration instanceof JetProperty || declaration instanceof JetNamedFunction) {
                return true;
            }
        }
        return false;
    }

}
