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
import org.jetbrains.jet.lang.psi.JetTypeParameter;
import org.jetbrains.jet.lang.psi.stubs.PsiJetTypeParameterStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.jet.lang.resolve.name.FqName;

public class PsiJetTypeParameterStubImpl extends StubBase<JetTypeParameter> implements PsiJetTypeParameterStub {
    private final StringRef name;
    private final boolean isInVariance;
    private final boolean isOutVariance;

    public PsiJetTypeParameterStubImpl(StubElement parent, StringRef name, boolean isInVariance, boolean isOutVariance) {
        super(parent, JetStubElementTypes.TYPE_PARAMETER);

        this.name = name;
        this.isInVariance = isInVariance;
        this.isOutVariance = isOutVariance;
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
        builder.append("]");

        return builder.toString();
    }

    @Nullable
    @Override
    public FqName getFqName() {
        // type parameters doesn't have FqNames
        return null;
    }
}
