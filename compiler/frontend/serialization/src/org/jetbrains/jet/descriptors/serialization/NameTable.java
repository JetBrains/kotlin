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

import gnu.trove.TObjectHashingStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.List;

import static org.jetbrains.jet.descriptors.serialization.ProtoBuf.QualifiedNameTable.QualifiedName;

public class NameTable {
    public static final TObjectHashingStrategy<QualifiedName.Builder> QUALIFIED_NAME_BUILDER_HASHING =
            new TObjectHashingStrategy<ProtoBuf.QualifiedNameTable.QualifiedName.Builder>() {
                @Override
                public int computeHashCode(QualifiedName.Builder object) {
                    int result = 13;
                    result = 31 * result + object.getParentQualifiedName();
                    result = 31 * result + object.getShortName();
                    result = 31 * result + object.getKind().hashCode();
                    return result;
                }

                @Override
                public boolean equals(QualifiedName.Builder o1, QualifiedName.Builder o2) {
                    return o1.getParentQualifiedName() == o2.getParentQualifiedName()
                           && o1.getShortName() == o2.getShortName()
                           && o1.getKind() == o2.getKind();
                }
            };

    private final Interner<String> simpleNames = new Interner<String>();
    private final Interner<QualifiedName.Builder> qualifiedNames = new Interner<QualifiedName.Builder>(QUALIFIED_NAME_BUILDER_HASHING);

    public NameTable() {
    }

    @NotNull
    public List<String> getSimpleNames() {
        return simpleNames.getAllInternedObjects();
    }

    @NotNull
    public List<QualifiedName.Builder> getFqNames() {
        return qualifiedNames.getAllInternedObjects();
    }

    public int getSimpleNameIndex(@NotNull Name name) {
        return simpleNames.intern(name.asString());
    }

    public int getFqNameIndex(@NotNull ClassOrNamespaceDescriptor descriptor) {
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

        return qualifiedNames.intern(builder);
    }

    public int getFqNameIndex(@NotNull FqName fqName) {
        int result = -1;
        for (Name segment : fqName.pathSegments()) {
            QualifiedName.Builder builder = QualifiedName.newBuilder();
            builder.setShortName(getSimpleNameIndex(segment));
            if (result != -1) {
                builder.setParentQualifiedName(result);
            }
            result = qualifiedNames.intern(builder);
        }
        return result;
    }
}
