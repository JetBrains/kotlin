/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import org.jetbrains.jet.lang.psi.JetAnnotationEntry;
import org.jetbrains.jet.lang.psi.stubs.PsiJetAnnotationStub;

public class PsiJetAnnotationStubImpl extends StubBase<JetAnnotationEntry> implements PsiJetAnnotationStub  {
    private final StringRef text;

    public PsiJetAnnotationStubImpl(StubElement parent, IStubElementType elementType, String text) {
        this(parent, elementType, StringRef.fromString(text));
    }

    public PsiJetAnnotationStubImpl(StubElement parent, IStubElementType elementType, StringRef text) {
        super(parent, elementType);
        this.text = text;
    }

    @Override
    public String getText() {
        return text.getString();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PsiJetAnnotationStubImpl[");
        builder.append("text=").append(getText());
        builder.append("]");
        return builder.toString();
    }
}
