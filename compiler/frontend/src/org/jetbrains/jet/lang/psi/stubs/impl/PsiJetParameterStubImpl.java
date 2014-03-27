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

import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.jet.lang.psi.stubs.PsiJetParameterStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.jet.lang.resolve.name.FqName;

public class PsiJetParameterStubImpl extends StubBase<JetParameter> implements PsiJetParameterStub {
    private final StringRef name;
    private final boolean isMutable;
    private final boolean isVarArg;
    //TODO: store as StringRef
    private final FqName fqName;
    private final boolean hasValOrValNode;
    private final boolean hasDefaultValue;

    public PsiJetParameterStubImpl(
            StubElement parent,
            FqName fqName, StringRef name,
            boolean isMutable,
            boolean isVarArg,
            boolean hasValOrValNode,
            boolean hasDefaultValue
    ) {
        super(parent, JetStubElementTypes.VALUE_PARAMETER);
        this.name = name;
        this.isMutable = isMutable;
        this.isVarArg = isVarArg;
        this.fqName = fqName;
        this.hasValOrValNode = hasValOrValNode;
        this.hasDefaultValue = hasDefaultValue;
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

    @Override
    public boolean hasValOrValNode() {
        return hasValOrValNode;
    }

    @Override
    public boolean hasDefaultValue() {
        return hasDefaultValue;
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
        if (fqName != null) {
            builder.append(" fqName=").append(fqName.toString()).append(" ");
        }

        builder.append("]");

        return builder.toString();
    }

    @Nullable
    @Override
    public FqName getFqName() {
        return fqName;
    }
}
