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
import org.jetbrains.jet.lang.psi.JetAnnotationEntry;
import org.jetbrains.jet.lang.psi.stubs.PsiJetAnnotationStub;

public class PsiJetAnnotationStubImpl extends StubBase<JetAnnotationEntry> implements PsiJetAnnotationStub  {
    private final StringRef shortName;

    public PsiJetAnnotationStubImpl(StubElement parent, IStubElementType elementType, @NotNull String shortName) {
        this(parent, elementType, StringRef.fromString(shortName));
    }

    public PsiJetAnnotationStubImpl(StubElement parent, IStubElementType elementType, StringRef shortName) {
        super(parent, elementType);
        this.shortName = shortName;
    }

    @Override
    public String getShortName() {
        return shortName.getString();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PsiJetAnnotationStubImpl[");
        builder.append("shortName=").append(getShortName());
        builder.append("]");
        return builder.toString();
    }
}
