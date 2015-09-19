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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.utils.Interner;
import org.jetbrains.kotlin.utils.UtilsPackage;

import java.io.IOException;
import java.io.OutputStream;

import static org.jetbrains.kotlin.serialization.ProtoBuf.QualifiedNameTable.QualifiedName;

public class StringTableImpl implements StringTable {
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

    public StringTableImpl(@NotNull SerializerExtension extension) {
        this.extension = extension;
    }

    public int getSimpleNameIndex(@NotNull Name name) {
        return getStringIndex(name.asString());
    }

    @Override
    public int getStringIndex(@NotNull String string) {
        return strings.intern(string);
    }

    @Override
    public int getFqNameIndex(@NotNull ClassDescriptor descriptor) {
        if (ErrorUtils.isError(descriptor)) {
            throw new IllegalStateException("Cannot get FQ name of error class: " + descriptor);
        }

        QualifiedName.Builder builder = QualifiedName.newBuilder();
        builder.setKind(QualifiedName.Kind.CLASS);

        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        String shortName;
        if (containingDeclaration instanceof PackageFragmentDescriptor) {
            shortName = descriptor.getName().asString();
            FqName packageFqName = ((PackageFragmentDescriptor) containingDeclaration).getFqName();
            if (!packageFqName.isRoot()) {
                builder.setParentQualifiedName(getPackageFqNameIndex(packageFqName));
            }
        }
        else if (containingDeclaration instanceof ClassDescriptor) {
            shortName = descriptor.getName().asString();
            ClassDescriptor outerClass = (ClassDescriptor) containingDeclaration;
            builder.setParentQualifiedName(getFqNameIndex(outerClass));
        }
        else {
            builder.setKind(QualifiedName.Kind.LOCAL);
            shortName = extension.getLocalClassName(descriptor);
        }

        builder.setShortName(getStringIndex(shortName));

        return qualifiedNames.intern(new FqNameProto(builder));
    }

    private int getPackageFqNameIndex(@NotNull FqName fqName) {
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

    @Override
    public void serializeTo(@NotNull OutputStream output) {
        try {
            ProtoBuf.StringTable.Builder stringTable = ProtoBuf.StringTable.newBuilder();
            for (String simpleName : strings.getAllInternedObjects()) {
                stringTable.addString(simpleName);
            }
            stringTable.build().writeDelimitedTo(output);

            ProtoBuf.QualifiedNameTable.Builder qualifiedNameTable = ProtoBuf.QualifiedNameTable.newBuilder();
            for (FqNameProto fqName : qualifiedNames.getAllInternedObjects()) {
                qualifiedNameTable.addQualifiedName(fqName.fqName);
            }
            qualifiedNameTable.build().writeDelimitedTo(output);
        }
        catch (IOException e) {
            throw UtilsPackage.rethrow(e);
        }
    }
}
