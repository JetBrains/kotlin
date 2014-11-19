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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.psi.stubs.KotlinPropertyStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.jet.lang.resolve.name.FqName;

public class KotlinPropertyStubImpl extends KotlinStubBaseImpl<JetProperty> implements KotlinPropertyStub {
    private final StringRef name;
    private final boolean isVar;
    private final boolean isTopLevel;
    private final boolean hasDelegate;
    private final boolean hasDelegateExpression;
    private final boolean hasInitializer;
    private final boolean hasReceiverTypeRef;
    private final boolean hasReturnTypeRef;
    private final boolean probablyNothingType;
    private final FqName fqName;

    public KotlinPropertyStubImpl(
            StubElement parent,
            StringRef name,
            boolean isVar,
            boolean isTopLevel,
            boolean hasDelegate,
            boolean hasDelegateExpression,
            boolean hasInitializer,
            boolean hasReceiverTypeRef,
            boolean hasReturnTypeRef,
            boolean probablyNothingType,
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
        this.hasReceiverTypeRef = hasReceiverTypeRef;
        this.hasReturnTypeRef = hasReturnTypeRef;
        this.probablyNothingType = probablyNothingType;
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

    @Override
    public boolean hasReceiverTypeRef() {
        return hasReceiverTypeRef;
    }

    @Override
    public boolean hasReturnTypeRef() {
        return hasReturnTypeRef;
    }

    @Override
    public boolean isProbablyNothingType() {
        return probablyNothingType;
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
}
