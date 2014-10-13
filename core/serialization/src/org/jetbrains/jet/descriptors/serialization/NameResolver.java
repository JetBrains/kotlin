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
import org.jetbrains.jet.lang.resolve.name.ClassId;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;

import static org.jetbrains.jet.descriptors.serialization.ProtoBuf.QualifiedNameTable.QualifiedName;

public class NameResolver {
    private final ProtoBuf.StringTable strings;
    private final ProtoBuf.QualifiedNameTable qualifiedNames;

    public NameResolver(
            @NotNull ProtoBuf.StringTable strings,
            @NotNull ProtoBuf.QualifiedNameTable qualifiedNames
    ) {
        this.strings = strings;
        this.qualifiedNames = qualifiedNames;
    }

    @NotNull
    public ProtoBuf.StringTable getStringTable() {
        return strings;
    }

    @NotNull
    public ProtoBuf.QualifiedNameTable getQualifiedNameTable() {
        return qualifiedNames;
    }

    @NotNull
    public String getString(int index) {
        return strings.getString(index);
    }

    @NotNull
    public Name getName(int index) {
        String name = strings.getString(index);
        return Name.guess(name);
    }

    @NotNull
    public ClassId getClassId(int index) {
        QualifiedName fqNameProto = qualifiedNames.getQualifiedName(index);
        assert fqNameProto.getKind() == ProtoBuf.QualifiedNameTable.QualifiedName.Kind.CLASS : "Not a class fqName: " + fqNameProto.getKind();

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
    private QualifiedName renderFqName(StringBuilder sb, QualifiedName fqNameProto, QualifiedName.Kind kind) {
        QualifiedName result = null;
        if (fqNameProto.hasParentQualifiedName()) {
            QualifiedName parentProto = qualifiedNames.getQualifiedName(fqNameProto.getParentQualifiedName());
            if (kind == null || parentProto.getKind() == kind) {
                result = renderFqName(sb, parentProto, kind);
                sb.append(".");
            }
            else {
                result = parentProto;
            }
        }
        sb.append(strings.getString(fqNameProto.getShortName()));
        return result;
    }

    @NotNull
    public FqName getFqName(int index) {
        QualifiedName qualifiedName = qualifiedNames.getQualifiedName(index);
        Name shortName = getName(qualifiedName.getShortName());
        if (!qualifiedName.hasParentQualifiedName()) {
            return FqName.topLevel(shortName);
        }
        return getFqName(qualifiedName.getParentQualifiedName()).child(shortName);
    }
}
