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
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.psi.stubs.PsiJetPropertyStub;
import org.jetbrains.jet.lang.resolve.name.FqName;

public class PsiJetPropertyStubImpl extends StubBase<JetProperty> implements PsiJetPropertyStub {
    private final StringRef name;
    private final boolean isVar;
    private final boolean isTopLevel;
    private final FqName topFQName;
    private final StringRef typeText;
    private final StringRef inferenceBodyText;

    public PsiJetPropertyStubImpl(IStubElementType elementType, StubElement parent, StringRef name,
            boolean isVar, boolean isTopLevel, @Nullable FqName topFQName, StringRef typeText, StringRef inferenceBodyText) {
        super(parent, elementType);

        if (isTopLevel && topFQName == null) {
            throw new IllegalArgumentException("topFQName shouldn't be null for top level properties");
        }

        this.name = name;
        this.isVar = isVar;
        this.isTopLevel = isTopLevel;
        this.topFQName = topFQName;
        this.typeText = typeText;
        this.inferenceBodyText = inferenceBodyText;
    }

    public PsiJetPropertyStubImpl(IStubElementType elementType, StubElement parent, String name,
            boolean isVal, boolean isTopLevel, @Nullable FqName topFQName,
            String typeText, String inferenceBodyText
    ) {
        this(elementType, parent, StringRef.fromString(name),
             isVal, isTopLevel, topFQName, StringRef.fromString(typeText), StringRef.fromString(inferenceBodyText));
    }

    @Override
    public boolean isVar() {
        return isVar;
    }

    @Override
    public boolean isTopLevel() {
        return isTopLevel;
    }

    @Nullable
    @Override
    public FqName getTopFQName() {
        return topFQName;
    }

    @Override
    public String getTypeText() {
        return StringRef.toString(typeText);
    }

    @Override
    public String getInferenceBodyText() {
        return StringRef.toString(inferenceBodyText);
    }

    @Override
    public String getName() {
        return StringRef.toString(name);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("PsiJetPropertyStubImpl[");

        builder.append(isVar() ? "var " : "val ");

        if (isTopLevel()) {
            assert topFQName != null;
            builder.append("top ").append("topFQName=").append(topFQName.toString()).append(" ");
        }

        builder.append("name=").append(getName());
        builder.append(" typeText=").append(getTypeText());
        builder.append(" bodyText=").append(getInferenceBodyText());

        builder.append("]");

        return builder.toString();
    }
}
