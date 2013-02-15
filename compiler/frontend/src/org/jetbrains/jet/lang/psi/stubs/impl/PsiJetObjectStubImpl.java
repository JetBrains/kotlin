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

package org.jetbrains.jet.lang.psi.stubs.impl;

import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetObjectDeclaration;
import org.jetbrains.jet.lang.psi.stubs.PsiJetObjectStub;
import org.jetbrains.jet.lang.resolve.name.FqName;

public class PsiJetObjectStubImpl extends StubBase<JetObjectDeclaration> implements PsiJetObjectStub {
    private final StringRef name;
    private final FqName fqName;
    private final boolean isTopLevel;
    private final boolean isClassObject;

    public PsiJetObjectStubImpl(
            @NotNull IStubElementType elementType,
            @NotNull StubElement parent,
            @Nullable String name,
            @Nullable FqName fqName,
            boolean isTopLevel,
            boolean isClassObject
    ) {
        this(elementType, parent, StringRef.fromString(name), fqName, isTopLevel, isClassObject);
    }

    public PsiJetObjectStubImpl(
            @NotNull IStubElementType elementType,
            @NotNull StubElement parent,
            @Nullable StringRef name,
            @Nullable FqName fqName,
            boolean isTopLevel,
            boolean isClassObject) {
        super(parent, elementType);

        this.name = name;
        this.fqName = fqName;
        this.isTopLevel = isTopLevel;
        this.isClassObject = isClassObject;
    }

    @Override
    public String getName() {
        return StringRef.toString(name);
    }

    @Nullable
    @Override
    public FqName getFqName() {
        return fqName;
    }

    @Override
    public boolean isTopLevel() {
        return isTopLevel;
    }

    @Override
    public boolean isClassObject() {
        return isClassObject;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("PsiJetObjectStubImpl[");

        if (isClassObject) {
            builder.append("class-object ");
        }

        if (isTopLevel) {
            builder.append("top ");
        }

        builder.append("name=").append(getName());
        builder.append(" fqName=").append(fqName != null ? fqName.toString() : "null");
        builder.append("]");

        return builder.toString();
    }
}
