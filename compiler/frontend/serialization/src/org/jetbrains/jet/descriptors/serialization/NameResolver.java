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

package org.jetbrains.jet.descriptors.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;

import static org.jetbrains.jet.descriptors.serialization.ProtoBuf.QualifiedNameTable.*;

public class NameResolver {
    private final ProtoBuf.SimpleNameTable simpleNames;
    private final ProtoBuf.QualifiedNameTable qualifiedNames;
    private final ClassDescriptor[] classDescriptors;
    private final ClassResolver classResolver;

    public NameResolver(
            @NotNull ProtoBuf.SimpleNameTable simpleNames,
            @NotNull ProtoBuf.QualifiedNameTable qualifiedNames,
            @NotNull ClassResolver classResolver
    ) {
        this.simpleNames = simpleNames;
        this.qualifiedNames = qualifiedNames;
        this.classDescriptors = new ClassDescriptor[qualifiedNames.getQualifiedNamesCount()];
        this.classResolver = classResolver;
    }

    @NotNull
    public Name getName(int index) {
        String name = simpleNames.getNames(index);
        return Name.guess(name);
    }

    @Nullable
    public ClassDescriptor getClassDescriptor(int fqNameIndex) {
        if (classDescriptors[fqNameIndex] != null) {
            return classDescriptors[fqNameIndex];
        }

        QualifiedName fqNameProto = qualifiedNames.getQualifiedNames(fqNameIndex);
        assert fqNameProto.getKind() == QualifiedName.Kind.CLASS : "Not a class fqName: " + getClassId(fqNameIndex);


        ClassId classId = getClassId(fqNameIndex);
        return classResolver.findClass(classId);
    }

    @NotNull
    public ClassId getClassId(int index) {
        QualifiedName fqNameProto = qualifiedNames.getQualifiedNames(index);

        StringBuilder relativeClassName = new StringBuilder();
        QualifiedName packageFqNameProto = renderFqName(relativeClassName, fqNameProto, QualifiedName.Kind.CLASS);

        FqName packageFqName;
        if (packageFqNameProto != null) {
            StringBuilder sb = new StringBuilder();
            QualifiedName mustBeNull = renderFqName(sb, packageFqNameProto, QualifiedName.Kind.PACKAGE);
            assert mustBeNull == null : "Prefix of an fqName must be all of kind PACKAGE";

            packageFqName = new FqName(sb.toString());
        }
        else {
            packageFqName = FqName.ROOT;
        }

        return new ClassId(packageFqName, new FqNameUnsafe(relativeClassName.toString()));
    }

    @Nullable
    private QualifiedName renderFqName(
            StringBuilder sb,
            QualifiedName fqNameProto,
            QualifiedName.Kind kind
    ) {
        QualifiedName result = null;
        if (fqNameProto.hasParentQualifiedName()) {
            QualifiedName parentProto = qualifiedNames.getQualifiedNames(fqNameProto.getParentQualifiedName());
            if (kind == null || parentProto.getKind() == kind) {
                result = renderFqName(sb, parentProto, kind);
                sb.append(".");
            }
            else {
                result = parentProto;
            }
        }
        sb.append(simpleNames.getNames(fqNameProto.getShortName()));
        return result;
    }
}
