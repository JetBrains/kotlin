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

import com.google.common.io.Files;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.JetDeclaration;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetNamedFunction;
import org.jetbrains.kotlin.psi.JetProperty;
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
    public static String getSanitizedClassNameBase(@NotNull String str) {
        if (str.isEmpty()) return "__EMPTY__";
        str = str.replaceAll("[^\\p{L}\\p{Digit}]", "_");
        str = toUpperAscii(str.charAt(0)) + str.substring(1);
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

    private static final String FACADE_CLASS_NAME_SUFFIX = "Kt";

    @NotNull
    public static FqName getPackagePartFqName(@NotNull FqName facadeFqName, @NotNull VirtualFile file, @Nullable JetFile jetFile) {
        String fileName = file.getName();
        return getStaticFacadeClassFqNameForFile(facadeFqName.parent(), fileName);
    }

    @NotNull
    private static FqName getStaticFacadeClassFqNameForFile(@NotNull FqName packageFqName, String fileName) {
        String nameWithoutExtension = FileUtil.getNameWithoutExtension(PathUtil.getFileName(fileName));
        return packageFqName.child(Name.identifier(getSanitizedClassNameBase(nameWithoutExtension) + FACADE_CLASS_NAME_SUFFIX));
    }

    public static boolean isFacadeClassFqName(@NotNull FqName classFqName) {
        return classFqName.shortName().asString().endsWith(FACADE_CLASS_NAME_SUFFIX);
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
        return getStaticFacadeClassFqNameForFile(file.getPackageFqName(), file.getName());
    }

    @NotNull
    public static FqName getPackagePartFqName(@NotNull DeserializedCallableMemberDescriptor callable) {
        FqName packageFqName = ((PackageFragmentDescriptor) callable.getContainingDeclaration()).getFqName();
        return packageFqName.child(callable.getNameResolver().getName(callable.getProto().getExtension(JvmProtoBuf.implClassName)));
    }

    @NotNull
    public static List<JetFile> getFilesWithCallables(@NotNull Collection<JetFile> packageFiles) {
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

    @NotNull
    public static List<JetFile> getFilesForFacade(@NotNull FqName facadeFqName, @NotNull Collection<JetFile> files) {
        // TODO Naive implementation. Replace it with "smarter" version.
        assert isFacadeClassFqName(facadeFqName);
        String facadeSimpleName = facadeFqName.shortName().asString();
        final String expectedSanitizedName = facadeSimpleName.substring(0, facadeSimpleName.length() - 2);
        return ContainerUtil.filter(getFilesWithCallables(files), new Condition<JetFile>() {
            @Override
            public boolean value(JetFile input) {
                String sanitizedName = getSanitizedClassNameBase(Files.getNameWithoutExtension(input.getName()));
                return expectedSanitizedName.equals(sanitizedName);
            }
        });
    }
}
