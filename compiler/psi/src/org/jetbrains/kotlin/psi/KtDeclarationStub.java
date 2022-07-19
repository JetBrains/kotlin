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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.kdoc.psi.api.KDoc;
import org.jetbrains.kotlin.psi.findDocComment.FindDocCommentKt;
import org.jetbrains.kotlin.psi.stubs.KotlinClassOrObjectStub;

import java.util.concurrent.atomic.AtomicLong;

public abstract class KtDeclarationStub<T extends StubElement<?>> extends KtModifierListOwnerStub<T> implements KtDeclaration {
    private final AtomicLong modificationStamp = new AtomicLong();

    public KtDeclarationStub(@NotNull T stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    public KtDeclarationStub(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void subtreeChanged() {
        super.subtreeChanged();
        modificationStamp.getAndIncrement();
    }

    public long getModificationStamp() {
        return modificationStamp.get();
    }

    @Nullable
    @Override
    public KDoc getDocComment() {
        return FindDocCommentKt.findDocComment(this);
    }

    @Override
    public PsiElement getParent() {
        T stub = getStub();
        // we build stubs for local classes/objects too but they have wrong parent
        if (stub != null && !(stub instanceof KotlinClassOrObjectStub && ((KotlinClassOrObjectStub) stub).isLocal())) {
            return stub.getParentStub().getPsi();
        }
        return super.getParent();
    }

    @Override
    public PsiElement getOriginalElement() {
        KotlinDeclarationNavigationPolicy navigationPolicy = ApplicationManager.getApplication().getService(KotlinDeclarationNavigationPolicy.class);
        return navigationPolicy != null ? navigationPolicy.getOriginalElement(this) : this;
    }

    @NotNull
    @Override
    public PsiElement getNavigationElement() {
        KotlinDeclarationNavigationPolicy navigationPolicy = ApplicationManager.getApplication().getService(KotlinDeclarationNavigationPolicy.class);
        return navigationPolicy != null ? navigationPolicy.getNavigationElement(this) : this;
    }
}
