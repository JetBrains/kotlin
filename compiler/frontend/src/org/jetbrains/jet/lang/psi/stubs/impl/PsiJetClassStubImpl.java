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

import com.intellij.psi.stubs.StubElement;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.stubs.PsiJetClassStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetClassElementType;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.ArrayList;
import java.util.List;

public class PsiJetClassStubImpl extends JetStubBaseImpl<JetClass> implements PsiJetClassStub {
    private final StringRef qualifiedName;
    private final StringRef name;
    private final StringRef[] superNames;
    private final boolean isTrait;
    private final boolean isEnumEntry;
    private final boolean isLocal;
    private final boolean isTopLevel;

    public PsiJetClassStubImpl(
            JetClassElementType type,
            StubElement parent,
            StringRef qualifiedName,
            StringRef name,
            StringRef[] superNames,
            boolean isTrait,
            boolean isEnumEntry,
            boolean isLocal,
            boolean isTopLevel
    ) {
        super(parent, type);
        this.qualifiedName = qualifiedName;
        this.name = name;
        this.superNames = superNames;
        this.isTrait = isTrait;
        this.isEnumEntry = isEnumEntry;
        this.isLocal = isLocal;
        this.isTopLevel = isTopLevel;
    }

    @Override
    public FqName getFqName() {
        String stringRef = StringRef.toString(qualifiedName);
        if (stringRef == null) {
            return null;
        }
        return new FqName(stringRef);
    }

    @Override
    public boolean isTrait() {
        return isTrait;
    }

    @Override
    public boolean isEnumEntry() {
        return isEnumEntry;
    }
    
    @Override
    public boolean isLocal() {
        return isLocal;
    }

    @Override
    public String getName() {
        return StringRef.toString(name);
    }

    @NotNull
    @Override
    public List<String> getSuperNames() {
        List<String> result = new ArrayList<String>();
        for (StringRef ref : superNames) {
            result.add(ref.toString());
        }
        return result;
    }

    @Override
    public boolean isTopLevel() {
        return isTopLevel;
    }
}
