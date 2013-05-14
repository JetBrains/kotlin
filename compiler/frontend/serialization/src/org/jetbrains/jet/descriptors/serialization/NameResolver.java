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
import org.jetbrains.jet.lang.resolve.name.Name;

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
        return Name.identifier(name);
    }

    @Nullable
    public ClassDescriptor getClassDescriptor(int fqNameIndex) {
        if (classDescriptors[fqNameIndex] != null) {
            return classDescriptors[fqNameIndex];
        }

        ProtoBuf.QualifiedNameTable.QualifiedName fqNameProto = qualifiedNames.getQualifiedNames(fqNameIndex);
        assert fqNameProto.getKind() == ProtoBuf.QualifiedNameTable.QualifiedName.Kind.CLASS : "Not a class fqName: " + getFqName(fqNameIndex);

        if (fqNameProto.hasParentQualifiedName()) {
            int parentFqNameIndex = fqNameProto.getParentQualifiedName();
            ProtoBuf.QualifiedNameTable.QualifiedName parentFqNameProto = qualifiedNames.getQualifiedNames(parentFqNameIndex);
            switch (fqNameProto.getKind()) {
                case CLASS:
                    return classResolver.findClass(getFqName(fqNameIndex));
                case PACKAGE:
                    Name name = getName(fqNameProto.getShortName());
                    ClassDescriptor outerClass = getClassDescriptor(parentFqNameIndex);
                    if (outerClass == null) return null;

                    return (ClassDescriptor) outerClass.getUnsubstitutedInnerClassesScope().getClassifier(name);
            }
            throw new IllegalStateException("Unknown kind: " + fqNameProto);
        }
        else {
            FqName fqName = FqName.topLevel(getName(fqNameProto.getShortName()));
            return classResolver.findClass(fqName);
        }
    }

    @NotNull
    public FqName getFqName(int index) {
        ProtoBuf.QualifiedNameTable.QualifiedName fqNameProto = qualifiedNames.getQualifiedNames(index);
        StringBuilder sb = renderFqName(new StringBuilder(), fqNameProto);
        return new FqName(sb.toString());
    }

    private StringBuilder renderFqName(StringBuilder sb, ProtoBuf.QualifiedNameTable.QualifiedName fqNameProto) {
        if (fqNameProto.hasParentQualifiedName()) {
            ProtoBuf.QualifiedNameTable.QualifiedName parentProto = qualifiedNames.getQualifiedNames(fqNameProto.getParentQualifiedName());
            renderFqName(sb, parentProto);
            sb.append(".");
        }
        sb.append(simpleNames.getNames(fqNameProto.getShortName()));
        return sb;
    }
}
