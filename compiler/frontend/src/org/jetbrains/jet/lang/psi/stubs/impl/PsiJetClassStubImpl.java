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

import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.stubs.PsiJetClassStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetClassElementType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Nikolay Krasko
 */
public class PsiJetClassStubImpl extends StubBase<JetClass> implements PsiJetClassStub {

    private final StringRef qualifiedName;
    private final StringRef name;
    private final StringRef[] superNames;

    public PsiJetClassStubImpl(
            JetClassElementType type,
            final StubElement parent,
            @Nullable final String qualifiedName,
            final String name,
            final List<String> superNames) {

        this(type, parent, StringRef.fromString(qualifiedName), StringRef.fromString(name), wrapStrings(superNames));
    }

    private static StringRef[] wrapStrings(List<String> names) {
        StringRef[] refs = new StringRef[names.size()];
        for (int i = 0; i < names.size(); i++) {
            refs[i] = StringRef.fromString(names.get(i));
        }
        return refs;
    }

    public PsiJetClassStubImpl(
            JetClassElementType type,
            final StubElement parent,
            final StringRef qualifiedName,
            final StringRef name,
            final StringRef[] superNames) {

        super(parent, type);
        this.qualifiedName = qualifiedName;
        this.name = name;
        this.superNames = superNames;
    }

    @Override
    public String getQualifiedName() {
        return StringRef.toString(qualifiedName);
    }

    @Override
    public boolean isDeprecated() {
        return false;
    }

    @Override
    public boolean hasDeprecatedAnnotation() {
        return false;
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
}
