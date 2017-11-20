/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.lazy.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.ClassKind;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtObjectDeclaration;
import org.jetbrains.kotlin.psi.KtStubbedPsiUtil;
import org.jetbrains.kotlin.psi.KtTypeParameterList;

public class KtObjectInfo extends KtClassOrObjectInfo<KtObjectDeclaration> {
    @NotNull
    private final ClassKind kind;

    protected KtObjectInfo(@NotNull KtObjectDeclaration element) {
        super(element);
        this.kind = element.isObjectLiteral() ? ClassKind.CLASS : ClassKind.OBJECT;
    }

    @Nullable
    @Override
    public KtTypeParameterList getTypeParameterList() {
        return element.getTypeParameterList();
    }

    @NotNull
    @Override
    public ClassKind getClassKind() {
        return kind;
    }

    public boolean isCompanionObject() {
        return element.isCompanion() &&
               KtStubbedPsiUtil.getContainingDeclaration(element) instanceof KtClassOrObject;
    }
}
