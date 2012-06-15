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

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetDelegationSpecifier;
import org.jetbrains.jet.lang.psi.JetModifierList;

import java.util.List;

/**
 * @author abreslav
 */
public abstract class JetClassOrObjectInfo<E extends JetClassOrObject> implements JetClassLikeInfo {
    protected final E element;

    protected JetClassOrObjectInfo(@NotNull E element) {
        this.element = element;
    }

    @Override
    public JetClassOrObject getCorrespondingClassOrObject() {
        return element;
    }

    @Override
    @NotNull
    public List<JetDelegationSpecifier> getDelegationSpecifiers() {
        return element.getDelegationSpecifiers();
    }

    //@Override
    //@Nullable
    //public Name getNameAsName() {
    //    return element.getNameAsName();
    //}

    @Override
    @Nullable
    public JetModifierList getModifierList() {
        return element.getModifierList();
    }

    @Override
    @NotNull
    public List<JetDeclaration> getDeclarations() {
        return element.getDeclarations();
    }

    @NotNull
    @Override
    public PsiElement getScopeAnchor() {
        return element;
    }
}
