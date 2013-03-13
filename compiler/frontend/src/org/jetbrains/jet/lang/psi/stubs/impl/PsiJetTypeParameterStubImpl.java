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
import org.jetbrains.jet.lang.psi.JetTypeParameter;
import org.jetbrains.jet.lang.psi.stubs.PsiJetTypeParameterStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetTypeParameterElementType;

public class PsiJetTypeParameterStubImpl extends StubBase<JetTypeParameter> implements PsiJetTypeParameterStub {
    private final StringRef name;
    private final StringRef extendBoundTypeText;
    private final boolean isInVariance;
    private final boolean isOutVariance;

    public PsiJetTypeParameterStubImpl(JetTypeParameterElementType type, StubElement parent,
            StringRef name, StringRef extendBoundTypeText, boolean isInVariance, boolean isOutVariance) {
        super(parent, type);

        this.name = name;
        this.extendBoundTypeText = extendBoundTypeText;
        this.isInVariance = isInVariance;
        this.isOutVariance = isOutVariance;
    }

    public PsiJetTypeParameterStubImpl(JetTypeParameterElementType type, StubElement parent,
            String name, String extendBoundTypeText, boolean isInVariance, boolean isOutVariance) {
        this(type, parent, StringRef.fromString(name), StringRef.fromString(extendBoundTypeText), isInVariance, isOutVariance);
    }

    @Override
    public String getExtendBoundTypeText() {
        return StringRef.toString(extendBoundTypeText);
    }

    @Override
    public boolean isInVariance() {
        return isInVariance;
    }

    @Override
    public boolean isOutVariance() {
        return isOutVariance;
    }

    @Override
    public String getName() {
        return StringRef.toString(name);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PsiJetTypeParameterStubImpl[");

        if (isInVariance()) {
            builder.append("in ");
        }

        if (isOutVariance()) {
            builder.append("out ");
        }

        builder.append("name=").append(getName());
        builder.append(" extendText=").append(getExtendBoundTypeText());

        builder.append("]");

        return builder.toString();
    }
}
