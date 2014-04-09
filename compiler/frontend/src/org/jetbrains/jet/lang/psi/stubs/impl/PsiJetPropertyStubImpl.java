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
    private final boolean hasDelegate;
    private final boolean hasDelegateExpression;
    private final boolean hasInitializer;
    private final FqName fqName;

    public PsiJetPropertyStubImpl(
            StubElement parent,
            StringRef name,
            boolean isVar,
            boolean isTopLevel,
            boolean hasDelegate,
            boolean hasDelegateExpression,
            boolean hasInitializer,
            @Nullable FqName fqName
    ) {
        super(parent, JetStubElementTypes.PROPERTY);

        if (isTopLevel && fqName == null) {
            throw new IllegalArgumentException("fqName shouldn't be null for top level properties");
        }
        if (hasDelegateExpression && !hasDelegate) {
            throw new IllegalArgumentException("Can't have delegate expression without delegate");
        }

        this.name = name;
        this.isVar = isVar;
        this.isTopLevel = isTopLevel;
        this.hasDelegate = hasDelegate;
        this.hasDelegateExpression = hasDelegateExpression;
        this.hasInitializer = hasInitializer;
        this.fqName = fqName;
    }

    @Override
    public boolean isVar() {
        return isVar;
    }

    @Override
    public boolean isTopLevel() {
        return isTopLevel;
    }

    @Override
    public boolean hasDelegate() {
        return hasDelegate;
    }

    @Override
    public boolean hasDelegateExpression() {
        return hasDelegateExpression;
    }

    @Override
    public boolean hasInitializer() {
        return hasInitializer;
    }

    @Nullable
    @Override
    public FqName getFqName() {
        return fqName;
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

        builder.append("]");

        return builder.toString();
    }
}
