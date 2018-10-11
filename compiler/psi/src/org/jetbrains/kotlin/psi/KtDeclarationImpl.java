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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.kdoc.psi.api.KDoc;
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken;
import org.jetbrains.kotlin.psi.addRemoveModifier.AddRemoveModifierKt;
import org.jetbrains.kotlin.psi.findDocComment.FindDocCommentKt;

import java.util.Collections;
import java.util.List;

public abstract class KtDeclarationImpl extends KtExpressionImpl implements KtDeclaration {
    public KtDeclarationImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    @Nullable
    public KtModifierList getModifierList() {
        return (KtModifierList) findChildByType(KtNodeTypes.MODIFIER_LIST);
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

    @NotNull
    @Override
    public List<KtAnnotationEntry> getAnnotationEntries() {
        KtModifierList modifierList = getModifierList();
        if (modifierList == null) return Collections.emptyList();
        return modifierList.getAnnotationEntries();
    }

    @NotNull
    @Override
    public List<KtAnnotation> getAnnotations() {
        KtModifierList modifierList = getModifierList();
        if (modifierList == null) return Collections.emptyList();
        return modifierList.getAnnotations();
    }

    @Nullable
    @Override
    public KDoc getDocComment() {
        return FindDocCommentKt.findDocComment(this);
    }
}
