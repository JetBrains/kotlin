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

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface KtCallableDeclaration extends KtNamedDeclaration, KtTypeParameterListOwner {
    @Nullable
    KtParameterList getValueParameterList();

    @NotNull
    List<KtParameter> getValueParameters();

    @Nullable
    KtTypeReference getReceiverTypeReference();

    @Nullable
    KtTypeReference getTypeReference();

    @Nullable
    KtTypeReference setTypeReference(@Nullable KtTypeReference typeRef);

    @Nullable
    PsiElement getColon();
}
