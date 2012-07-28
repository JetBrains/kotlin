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

package org.jetbrains.jet.lang.resolve.lazy.data;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.List;

/**
 * @author abreslav
 */
public class FilteringClassLikeInfo implements JetClassLikeInfo {
    private final JetClassLikeInfo delegate;
    private final Predicate<? super JetDeclaration> declarationFilter;
    private List<JetDeclaration> filteredDeclarations;

    public FilteringClassLikeInfo(@NotNull JetClassLikeInfo delegate, @NotNull Predicate<? super JetDeclaration> declarationFilter) {
        this.delegate = delegate;
        this.declarationFilter = declarationFilter;
    }

    @NotNull
    @Override
    public FqName getContainingPackageFqName() {
        return delegate.getContainingPackageFqName();
    }

    @Override
    @NotNull
    public List<JetDelegationSpecifier> getDelegationSpecifiers() {
        return delegate.getDelegationSpecifiers();
    }

    @Override
    @Nullable
    public JetModifierList getModifierList() {
        return delegate.getModifierList();
    }

    @Override
    @Nullable
    public JetClassObject getClassObject() {
        return delegate.getClassObject();
    }

    @Override
    @NotNull
    public PsiElement getScopeAnchor() {
        return delegate.getScopeAnchor();
    }

    @Override
    @Nullable
    public JetClassOrObject getCorrespondingClassOrObject() {
        return delegate.getCorrespondingClassOrObject();
    }

    @Override
    @NotNull
    public List<JetTypeParameter> getTypeParameters() {
        return delegate.getTypeParameters();
    }

    @Override
    @NotNull
    public List<? extends JetParameter> getPrimaryConstructorParameters() {
        return delegate.getPrimaryConstructorParameters();
    }

    @Override
    @NotNull
    public ClassKind getClassKind() {
        return delegate.getClassKind();
    }

    @Override
    @NotNull
    public List<JetDeclaration> getDeclarations() {
        if (filteredDeclarations == null) {
            filteredDeclarations = Lists.newArrayList(Collections2.filter(delegate.getDeclarations(), declarationFilter));
        }
        return filteredDeclarations;
    }

    @Override
    public String toString() {
        return "filtering " + delegate.toString();
    }
}
