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

import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;
import org.jetbrains.kotlin.utils.Interner;

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

    public int getSimpleNameIndex(@NotNull Name name) {
        return getStringIndex(name.asString());
    }

    @Override
    public int getStringIndex(@NotNull String string) {
        return strings.intern(string);
    }

    @Override
    public int getFqNameIndex(@NotNull ClassifierDescriptorWithTypeParameters descriptor) {
        if (ErrorUtils.isError(descriptor)) {
            throw new IllegalStateException("Cannot get FQ name of error class: " + descriptor);
        }

        QualifiedName.Builder builder = QualifiedName.newBuilder();
        builder.setKind(QualifiedName.Kind.CLASS);

        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        if (containingDeclaration instanceof PackageFragmentDescriptor) {
            FqName packageFqName = ((PackageFragmentDescriptor) containingDeclaration).getFqName();
            if (!packageFqName.isRoot()) {
                builder.setParentQualifiedName(getPackageFqNameIndex(packageFqName));
            }
        }
        else if (containingDeclaration instanceof ClassDescriptor) {
            ClassDescriptor outerClass = (ClassDescriptor) containingDeclaration;
            builder.setParentQualifiedName(getFqNameIndex(outerClass));
        }
        else {
            throw new IllegalStateException("Cannot get FQ name of local class: " + descriptor);
        }

        builder.setShortName(getStringIndex(descriptor.getName().asString()));

        return qualifiedNames.intern(new FqNameProto(builder));
    }

    public int getPackageFqNameIndex(@NotNull FqName fqName) {
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
    public Pair<ProtoBuf.StringTable, ProtoBuf.QualifiedNameTable> buildProto() {
        ProtoBuf.StringTable.Builder strings = ProtoBuf.StringTable.newBuilder();
        for (String simpleName : this.strings.getAllInternedObjects()) {
            strings.addString(simpleName);
        }

        ProtoBuf.QualifiedNameTable.Builder qualifiedNames = ProtoBuf.QualifiedNameTable.newBuilder();
        for (FqNameProto fqName : this.qualifiedNames.getAllInternedObjects()) {
            qualifiedNames.addQualifiedName(fqName.fqName);
        }

        return new Pair<ProtoBuf.StringTable, ProtoBuf.QualifiedNameTable>(strings.build(), qualifiedNames.build());
    }

    @Override
    public void serializeTo(@NotNull OutputStream output) {
        try {
            Pair<ProtoBuf.StringTable, ProtoBuf.QualifiedNameTable> protos = buildProto();
            protos.getFirst().writeDelimitedTo(output);
            protos.getSecond().writeDelimitedTo(output);
        }
        catch (IOException e) {
            throw ExceptionUtilsKt.rethrow(e);
        }
    }
}
