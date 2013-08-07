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
import com.intellij.util.ArrayUtil;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.stubs.PsiJetFunctionStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.jet.lang.resolve.name.FqName;

public class PsiJetFunctionStubImpl extends StubBase<JetNamedFunction> implements PsiJetFunctionStub {

    private final StringRef nameRef;
    private final boolean isTopLevel;
    private final boolean isExtension;
    private final FqName fqName;

    public PsiJetFunctionStubImpl(
            @NotNull StubElement parent,
            @Nullable String name,
            boolean isTopLevel,
            @Nullable FqName fqName,
            boolean isExtension
    ) {
        this(parent, StringRef.fromString(name), isTopLevel, fqName, isExtension);
    }

    public PsiJetFunctionStubImpl(
            @NotNull StubElement parent,
            @Nullable StringRef nameRef,
            boolean isTopLevel,
            @Nullable FqName fqName,
            boolean isExtension
    ) {
        super(parent, JetStubElementTypes.FUNCTION);

        if (isTopLevel && fqName == null) {
            throw new IllegalArgumentException("fqName shouldn't be null for top level functions");
        }

        this.nameRef = nameRef;
        this.fqName = fqName;
        this.isTopLevel = isTopLevel;
        this.isExtension = isExtension;
    }

    @Override
    public String getName() {
        return StringRef.toString(nameRef);
    }

    @Override
    public boolean isTopLevel() {
        return isTopLevel;
    }

    @Override
    public boolean isExtension() {
        return isExtension;
    }

    @NotNull
    @Override
    public String[] getAnnotations() {
        // TODO (stubs)
        return ArrayUtil.EMPTY_STRING_ARRAY;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PsiJetFunctionStubImpl[");

        if (isTopLevel()) {
            assert fqName != null;
            builder.append("top ").append("fqName=").append(fqName.toString()).append(" ");
        }

        if (isExtension()) {
            builder.append("ext ");
        }

        builder.append("name=").append(getName());

        builder.append("]");

        return builder.toString();
    }

    @Nullable
    @Override
    public FqName getFqName() {
        return fqName;
    }
}
