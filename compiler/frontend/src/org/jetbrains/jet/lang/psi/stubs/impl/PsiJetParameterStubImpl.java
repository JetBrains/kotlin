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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.jet.lang.psi.stubs.PsiJetParameterStub;

public class PsiJetParameterStubImpl extends StubBase<JetParameter> implements PsiJetParameterStub {
    private final StringRef name;
    private final boolean isMutable;
    private final boolean isVarArg;
    private final StringRef typeText;
    private final StringRef defaultValueText;

    public PsiJetParameterStubImpl(IStubElementType elementType, StubElement parent,
            StringRef name,
            boolean isMutable,
            boolean isVarArg,
            StringRef typeText, StringRef defaultValueText) {
        super(parent, elementType);
        this.name = name;
        this.isMutable = isMutable;
        this.isVarArg = isVarArg;
        this.typeText = typeText;
        this.defaultValueText = defaultValueText;
    }

    public PsiJetParameterStubImpl(IStubElementType elementType, StubElement parent,
            String name,
            boolean isMutable,
            boolean isVarArg,
            String typeText, String defaultValueText) {
        this(elementType, parent, StringRef.fromString(name), isMutable, isVarArg,
             StringRef.fromString(typeText), StringRef.fromString(defaultValueText));
    }

    @Override
    public String getName() {
        return StringRef.toString(name);
    }

    @Override
    public boolean isMutable() {
        return isMutable;
    }

    @Override
    public boolean isVarArg() {
        return isVarArg;
    }

    @Nullable
    @Override
    public String getTypeText() {
        return StringRef.toString(typeText);
    }

    @Override
    public String getDefaultValueText() {
        return StringRef.toString(defaultValueText);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PsiJetParameterStubImpl[");

        builder.append(isMutable() ? "var " : "val ");

        if (isVarArg()) {
            builder.append("vararg ");
        }

        builder.append("name=").append(getName());
        builder.append(" typeText=").append(getTypeText());
        builder.append(" defaultValue=").append(getDefaultValueText());

        builder.append("]");

        return builder.toString();
    }
}
