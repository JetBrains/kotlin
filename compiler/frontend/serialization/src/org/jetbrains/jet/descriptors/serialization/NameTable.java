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
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.NamespaceDescriptorParent;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
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
                public boolean equals(
                        QualifiedName.Builder o1, QualifiedName.Builder o2
                ) {
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

    public int getFqNameIndex(@NotNull ClassDescriptor classDescriptor) {
        QualifiedName.Builder builder = QualifiedName.newBuilder();
        builder.setKind(QualifiedName.Kind.CLASS);
        builder.setShortName(getSimpleNameIndex(classDescriptor.getName()));

        DeclarationDescriptor containingDeclaration = classDescriptor.getContainingDeclaration();
        if (containingDeclaration instanceof NamespaceDescriptor) {
            NamespaceDescriptor namespaceDescriptor = (NamespaceDescriptor) containingDeclaration;
            if (DescriptorUtils.isRootNamespace(namespaceDescriptor)) {
                builder.clearParentQualifiedName();
            }
            else {
                builder.setParentQualifiedName(getFqNameIndex(namespaceDescriptor));
            }
        }
        else if (containingDeclaration instanceof ClassDescriptor) {
            ClassDescriptor outerClass = (ClassDescriptor) containingDeclaration;
            builder.setParentQualifiedName(getFqNameIndex(outerClass));
        }
        else {
            throw new IllegalStateException("FQ names are only stored for top-level or inner classes: " + classDescriptor);
        }

        return qualifiedNames.intern(builder);
    }

    public int getFqNameIndex(@NotNull NamespaceDescriptor namespaceDescriptor) {
        QualifiedName.Builder builder = QualifiedName.newBuilder();
        //default: builder.setKind(QualifiedNameTable.QualifiedName.Kind.PACKAGE);
        builder.setShortName(getSimpleNameIndex(namespaceDescriptor.getName()));

        NamespaceDescriptorParent containingDeclaration = namespaceDescriptor.getContainingDeclaration();
        if (containingDeclaration instanceof NamespaceDescriptor) {
            NamespaceDescriptor parentNamespace = (NamespaceDescriptor) containingDeclaration;
            if (!DescriptorUtils.isRootNamespace(parentNamespace)) {
                builder.setParentQualifiedName(getFqNameIndex(parentNamespace));
            }
        }
        else {
            builder.clearParentQualifiedName();
        }

        return qualifiedNames.intern(builder);
    }
}
