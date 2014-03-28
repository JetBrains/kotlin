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

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.ArrayUtil;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetObjectDeclaration;
import org.jetbrains.jet.lang.psi.stubs.PsiJetObjectStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.ArrayList;
import java.util.List;

public class PsiJetObjectStubImpl extends StubBase<JetObjectDeclaration> implements PsiJetObjectStub {
    private final StringRef name;
    private final FqName fqName;
    private final StringRef[] superNames;
    private final boolean isTopLevel;
    private final boolean isClassObject;
    private final boolean isLocal;
    private final boolean isObjectLiteral;

    public PsiJetObjectStubImpl(
            @NotNull StubElement parent,
            @Nullable StringRef name,
            @Nullable FqName fqName,
            @NotNull StringRef[] superNames,
            boolean isTopLevel,
            boolean isClassObject,
            boolean isLocal,
            boolean isObjectLiteral
    ) {
        super(parent, JetStubElementTypes.OBJECT_DECLARATION);
        this.name = name;
        this.fqName = fqName;
        this.superNames = superNames;
        this.isTopLevel = isTopLevel;
        this.isClassObject = isClassObject;
        this.isLocal = isLocal;
        this.isObjectLiteral = isObjectLiteral;
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

    @NotNull
    @Override
    public List<String> getSuperNames() {
        List<String> result = new ArrayList<String>();
        for (StringRef ref : superNames) {
            result.add(ref.toString());
        }
        return result;
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
    public boolean isObjectLiteral() {
        return isObjectLiteral;
    }

    @Override
    public boolean isLocal() {
        return isLocal;
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

        if (isLocal()) {
            builder.append("local ");
        }

        builder.append("name=").append(getName());
        builder.append(" fqName=").append(fqName != null ? fqName.toString() : "null");
        builder.append(" superNames=").append("[").append(StringUtil.join(ArrayUtil.toStringArray(getSuperNames()))).append("]");
        builder.append("]");

        return builder.toString();
    }
}
