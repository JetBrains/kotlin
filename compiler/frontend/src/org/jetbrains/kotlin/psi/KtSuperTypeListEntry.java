/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.util.ArrayFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

public class KtSuperTypeListEntry extends KtElementImplStub<KotlinPlaceHolderStub<? extends KtSuperTypeListEntry>> {
    private static final KtSuperTypeListEntry[] EMPTY_ARRAY = new KtSuperTypeListEntry[0];

    public static ArrayFactory<KtSuperTypeListEntry> ARRAY_FACTORY = count -> count == 0 ? EMPTY_ARRAY : new KtSuperTypeListEntry[count];

    public KtSuperTypeListEntry(@NotNull ASTNode node) {
        super(node);
    }

    public KtSuperTypeListEntry(
            @NotNull KotlinPlaceHolderStub<? extends KtSuperTypeListEntry> stub,
            @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitSuperTypeListEntry(this, data);
    }

    @Nullable
    public KtTypeReference getTypeReference() {
        return getStubOrPsiChild(KtStubElementTypes.TYPE_REFERENCE);
    }

    @Nullable
    public KtUserType getTypeAsUserType() {
        KtTypeReference reference = getTypeReference();
        if (reference != null) {
            KtTypeElement element = reference.getTypeElement();
            if (element instanceof KtUserType) {
                return ((KtUserType) element);
            }
        }
        return null;
    }
}
