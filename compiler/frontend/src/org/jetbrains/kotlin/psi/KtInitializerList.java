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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

import java.util.Arrays;
import java.util.List;

public class KtInitializerList extends KtElementImplStub<KotlinPlaceHolderStub<KtInitializerList>> {
    public KtInitializerList(@NotNull ASTNode node) {
        super(node);
    }

    public KtInitializerList(@NotNull KotlinPlaceHolderStub<KtInitializerList> stub) {
        super(stub, KtStubElementTypes.INITIALIZER_LIST);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitInitializerList(this, data);
    }

    @NotNull
    public List<KtSuperTypeListEntry> getInitializers() {
        return Arrays.asList(getStubOrPsiChildren(KtStubElementTypes.SUPER_TYPE_LIST_ENTRIES, KtSuperTypeListEntry.ARRAY_FACTORY));
    }
}
