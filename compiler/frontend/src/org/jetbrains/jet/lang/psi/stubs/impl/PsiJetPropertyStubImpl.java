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
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.psi.stubs.PsiJetPropertyStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.jet.lang.resolve.name.FqName;

public class PsiJetPropertyStubImpl extends StubBase<JetProperty> implements PsiJetPropertyStub {
    private final StringRef name;
    private final boolean isVar;
    private final boolean isTopLevel;
    private final FqName fqName;
    private final StringRef typeText;
    private final StringRef inferenceBodyText;

    public PsiJetPropertyStubImpl(
            StubElement parent, StringRef name,
            boolean isVar, boolean isTopLevel, @Nullable FqName fqName, StringRef typeText, StringRef inferenceBodyText
    ) {
        super(parent, JetStubElementTypes.PROPERTY);

        if (isTopLevel && fqName == null) {
            throw new IllegalArgumentException("fqName shouldn't be null for top level properties");
        }

        this.name = name;
        this.isVar = isVar;
        this.isTopLevel = isTopLevel;
        this.fqName = fqName;
        this.typeText = typeText;
        this.inferenceBodyText = inferenceBodyText;
    }

    public PsiJetPropertyStubImpl(StubElement parent, String name,
            boolean isVar, boolean isTopLevel, @Nullable FqName topFQName,
            String typeText, String inferenceBodyText
    ) {
        this(parent, StringRef.fromString(name),
             isVar, isTopLevel, topFQName, StringRef.fromString(typeText), StringRef.fromString(inferenceBodyText));
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
    public FqName getFqName() {
        return fqName;
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
            assert fqName != null;
            builder.append("top ").append("fqName=").append(fqName.toString()).append(" ");
        }

        builder.append("name=").append(getName());
        builder.append(" typeText=").append(getTypeText());
        builder.append(" bodyText=").append(getInferenceBodyText());

        builder.append("]");

        return builder.toString();
    }
}
