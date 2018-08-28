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

package org.jetbrains.kotlin.resolve.lazy.data;

import com.intellij.psi.PsiElement;
import kotlin.annotations.jvm.ReadOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.ClassKind;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.*;

import java.util.List;

public interface KtClassLikeInfo extends KtDeclarationContainer {
    @NotNull
    FqName getContainingPackageFqName();

    @Nullable
    KtModifierList getModifierList();

    @NotNull
    @ReadOnly
    List<KtObjectDeclaration> getCompanionObjects();

    // This element is used to identify resolution scope for the class
    @NotNull
    PsiElement getScopeAnchor();

    @Nullable // can be null in KtScript
    KtClassOrObject getCorrespondingClassOrObject();

    @Nullable
    KtTypeParameterList getTypeParameterList();

    @NotNull
    @ReadOnly
    List<? extends KtParameter> getPrimaryConstructorParameters();

    @NotNull
    ClassKind getClassKind();

    @NotNull
    List<KtAnnotationEntry> getDanglingAnnotations();
}
