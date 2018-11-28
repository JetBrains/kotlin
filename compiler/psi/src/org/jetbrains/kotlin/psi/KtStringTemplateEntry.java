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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderWithTextStub;

public abstract class KtStringTemplateEntry extends KtElementImplStub<KotlinPlaceHolderWithTextStub<? extends KtStringTemplateEntry>> {
    public static final KtStringTemplateEntry[] EMPTY_ARRAY = new KtStringTemplateEntry[0];

    public KtStringTemplateEntry(@NotNull ASTNode node) {
        super(node);
    }

    public KtStringTemplateEntry(
            @NotNull KotlinPlaceHolderWithTextStub<? extends KtStringTemplateEntry> stub,
            @NotNull IStubElementType elementType
    ) {
        super(stub, elementType);
    }

    @Nullable
    public KtExpression getExpression() {
        return findChildByClass(KtExpression.class);
    }
}
