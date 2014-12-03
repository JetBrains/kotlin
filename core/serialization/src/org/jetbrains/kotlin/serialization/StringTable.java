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

import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.ClassOrPackageFragmentDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;

import java.util.List;

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

    @NotNull
    public List<String> getStrings() {
        return strings.getAllInternedObjects();
    }

    @NotNull
    public List<QualifiedName.Builder> getFqNames() {
        return KotlinPackage.map(qualifiedNames.getAllInternedObjects(), new Function1<FqNameProto, QualifiedName.Builder>() {
            @Override
            public QualifiedName.Builder invoke(FqNameProto proto) {
                return proto.fqName;
            }
        });
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
        builder.setShortName(getSimpleNameIndex(descriptor.getName()));

        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        if (containingDeclaration instanceof PackageFragmentDescriptor) {
            PackageFragmentDescriptor fragment = (PackageFragmentDescriptor) containingDeclaration;
            if (!fragment.getFqName().isRoot()) {
                builder.setParentQualifiedName(getFqNameIndex(fragment.getFqName()));
            }
        }
        else if (containingDeclaration instanceof ClassDescriptor) {
            ClassDescriptor outerClass = (ClassDescriptor) containingDeclaration;
            builder.setParentQualifiedName(getFqNameIndex(outerClass));
        }
        else {
            throw new IllegalStateException("FQ names are only stored for top-level or inner classes: " + descriptor);
        }

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
}
