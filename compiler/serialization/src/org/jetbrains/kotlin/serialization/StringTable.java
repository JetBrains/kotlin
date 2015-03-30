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

package org.jetbrains.kotlin.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.ClassOrPackageFragmentDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;

import static org.jetbrains.kotlin.serialization.ProtoBuf.QualifiedNameTable.QualifiedName;

public class StringTable {
    private static final class FqNameProto {
        public final QualifiedName.Builder fqName;

        public FqNameProto(@NotNull QualifiedName.Builder fqName) {
            this.fqName = fqName;
        }

        @Override
        public int hashCode() {
            int result = 13;
            result = 31 * result + fqName.getParentQualifiedName();
            result = 31 * result + fqName.getShortName();
            result = 31 * result + fqName.getKind().hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || getClass() != obj.getClass()) return false;

            QualifiedName.Builder other = ((FqNameProto) obj).fqName;
            return fqName.getParentQualifiedName() == other.getParentQualifiedName()
                   && fqName.getShortName() == other.getShortName()
                   && fqName.getKind() == other.getKind();
        }
    }

    private final Interner<String> strings = new Interner<String>();
    private final Interner<FqNameProto> qualifiedNames = new Interner<FqNameProto>();
    private final SerializerExtension extension;

    public StringTable(@NotNull SerializerExtension extension) {
        this.extension = extension;
    }

    public int getSimpleNameIndex(@NotNull Name name) {
        return getStringIndex(name.asString());
    }

    public int getStringIndex(@NotNull String string) {
        return strings.intern(string);
    }

    public int getFqNameIndex(@NotNull ClassOrPackageFragmentDescriptor descriptor) {
        QualifiedName.Builder builder = QualifiedName.newBuilder();
        if (descriptor instanceof ClassDescriptor) {
            builder.setKind(QualifiedName.Kind.CLASS);
        }

        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        int shortName;
        if (containingDeclaration instanceof PackageFragmentDescriptor) {
            shortName = getSimpleNameIndex(descriptor.getName());
            PackageFragmentDescriptor fragment = (PackageFragmentDescriptor) containingDeclaration;
            if (!fragment.getFqName().isRoot()) {
                builder.setParentQualifiedName(getFqNameIndex(fragment.getFqName()));
            }
        }
        else if (containingDeclaration instanceof ClassDescriptor) {
            shortName = getSimpleNameIndex(descriptor.getName());
            ClassDescriptor outerClass = (ClassDescriptor) containingDeclaration;
            builder.setParentQualifiedName(getFqNameIndex(outerClass));
        }
        else {
            if (descriptor instanceof ClassDescriptor) {
                builder.setKind(QualifiedName.Kind.LOCAL);
                shortName = getStringIndex(extension.getLocalClassName((ClassDescriptor) descriptor));
            }
            else {
                throw new IllegalStateException("Package container should be a package: " + descriptor);
            }
        }

        builder.setShortName(shortName);

        return qualifiedNames.intern(new FqNameProto(builder));
    }

    public int getFqNameIndex(@NotNull FqName fqName) {
        int result = -1;
        for (Name segment : fqName.pathSegments()) {
            QualifiedName.Builder builder = QualifiedName.newBuilder();
            builder.setShortName(getSimpleNameIndex(segment));
            if (result != -1) {
                builder.setParentQualifiedName(result);
            }
            result = qualifiedNames.intern(new FqNameProto(builder));
        }
        return result;
    }

    @NotNull
    public ProtoBuf.StringTable serializeSimpleNames() {
        ProtoBuf.StringTable.Builder builder = ProtoBuf.StringTable.newBuilder();
        for (String simpleName : strings.getAllInternedObjects()) {
            builder.addString(simpleName);
        }
        return builder.build();
    }

    @NotNull
    public ProtoBuf.QualifiedNameTable serializeQualifiedNames() {
        ProtoBuf.QualifiedNameTable.Builder builder = ProtoBuf.QualifiedNameTable.newBuilder();
        for (FqNameProto fqName : qualifiedNames.getAllInternedObjects()) {
            builder.addQualifiedName(fqName.fqName);
        }
        return builder.build();
    }
}
