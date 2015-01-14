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
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
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
        // Conversion to system-dependent name seems to be unnecessary, but it's hard to check now:
        // it was introduced when fixing KT-2839, which appeared again (KT-3639).
        // If you try to remove it, run tests on Windows.
        return FileUtil.toSystemDependentName(file.getPath()).hashCode();
    }

    @NotNull
    private static String replaceSpecialSymbols(@NotNull String str) {
        return str.replace('.', '_');
    }

    @NotNull
    public static FqName getPackagePartFqName(@NotNull FqName facadeFqName, @NotNull VirtualFile file) {
        String fileName = FileUtil.getNameWithoutExtension(PathUtil.getFileName(file.getName()));

        // path hashCode to prevent same name / different path collision
        String srcName = String.format(
                "%s$%s$%08x",
                facadeFqName.shortName().asString(),
                replaceSpecialSymbols(fileName),
                getPathHashCode(file)
        );

        return facadeFqName.parent().child(Name.identifier(srcName));
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
        return getPackagePartFqName(getPackageClassFqName(file.getPackageFqName()), file.getVirtualFile());
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
