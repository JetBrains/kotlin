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

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.ArrayUtil;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.stubs.PsiJetClassStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetClassElementType;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.ArrayList;
import java.util.List;

public class PsiJetClassStubImpl extends StubBase<JetClass> implements PsiJetClassStub {
    private final StringRef qualifiedName;
    private final StringRef name;
    private final StringRef[] superNames;
    private final boolean isTrait;
    private final boolean isEnumClass;
    private final boolean isEnumEntry;
    private final boolean isAnnotation;
    private final boolean isInner;

    public PsiJetClassStubImpl(
            JetClassElementType type,
            StubElement parent,
            @Nullable final String qualifiedName,
            String name,
            List<String> superNames,
            boolean isTrait, boolean isEnumClass, boolean isEnumEntry, boolean isAnnotation, boolean isInner) {
        this(type, parent, StringRef.fromString(qualifiedName), StringRef.fromString(name), wrapStrings(superNames),
             isTrait, isEnumClass, isEnumEntry, isAnnotation, isInner);
    }

    public PsiJetClassStubImpl(
            JetClassElementType type,
            StubElement parent,
            StringRef qualifiedName,
            StringRef name,
            StringRef[] superNames,
            boolean isTrait,
            boolean isEnumClass,
            boolean isEnumEntry,
            boolean isAnnotation,
            boolean isInner) {
        super(parent, type);
        this.qualifiedName = qualifiedName;
        this.name = name;
        this.superNames = superNames;
        this.isTrait = isTrait;
        this.isEnumClass = isEnumClass;
        this.isEnumEntry = isEnumEntry;
        this.isAnnotation = isAnnotation;
        this.isInner = isInner;
    }

    private static StringRef[] wrapStrings(List<String> names) {
        StringRef[] refs = new StringRef[names.size()];
        for (int i = 0; i < names.size(); i++) {
            refs[i] = StringRef.fromString(names.get(i));
        }
        return refs;
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
    public boolean isAnnotation() {
        return isAnnotation;
    }

    @Override
    public boolean isEnumClass() {
        return isEnumClass;
    }

    @Override
    public boolean isEnumEntry() {
        return isEnumEntry;
    }
    
    @Override
    public boolean isInner() {
        return isInner;
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
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PsiJetClassStubImpl[");

        if (isEnumClass()) {
            builder.append("enumClass ");
        }

        if (isEnumEntry()) {
            builder.append("enumEntry ");
        }

        if (isTrait()) {
            builder.append("trait ");
        }

        if (isAnnotation()) {
            builder.append("isAnnotation ");
        }

        if (isInner()) {
            builder.append("inner ");
        }

        builder.append("name=").append(getName());
        builder.append(" fqn=").append(getFqName());
        builder.append(" superNames=").append("[").append(StringUtil.join(ArrayUtil.toStringArray(getSuperNames()))).append("]");

        builder.append("]");

        return builder.toString();
    }
}
