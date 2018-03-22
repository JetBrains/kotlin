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
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken;
import org.jetbrains.kotlin.psi.addRemoveModifier.AddRemoveModifierKt;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

import java.util.Collections;
import java.util.List;

public class KtModifierListOwnerStub<T extends StubElement<?>> extends KtElementImplStub<T> implements KtModifierListOwner {
    public KtModifierListOwnerStub(ASTNode node) {
        super(node);
    }

    public KtModifierListOwnerStub(T stub, IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Override
    @Nullable
    public KtModifierList getModifierList() {
        return getStubOrPsiChild(KtStubElementTypes.MODIFIER_LIST);
    }

    @Override
    public boolean hasModifier(@NotNull KtModifierKeywordToken modifier) {
        KtModifierList modifierList = getModifierList();
        return modifierList != null && modifierList.hasModifier(modifier);
    }

    @Override
    public void addModifier(@NotNull KtModifierKeywordToken modifier) {
        AddRemoveModifierKt.addModifier(this, modifier);
    }

    @Override
    public void removeModifier(@NotNull KtModifierKeywordToken modifier) {
        AddRemoveModifierKt.removeModifier(this, modifier);
    }

    @NotNull
    @Override
    public KtAnnotationEntry addAnnotationEntry(@NotNull KtAnnotationEntry annotationEntry) {
        return AddRemoveModifierKt.addAnnotationEntry(this, annotationEntry);
    }

    @Override
    @NotNull
    public List<KtAnnotationEntry> getAnnotationEntries() {
        KtModifierList modifierList = getModifierList();
        if (modifierList == null) return Collections.emptyList();
        return modifierList.getAnnotationEntries();
    }

    @Override
    @NotNull
    public List<KtAnnotation> getAnnotations() {
        KtModifierList modifierList = getModifierList();
        if (modifierList == null) return Collections.emptyList();
        return modifierList.getAnnotations();
    }
}
