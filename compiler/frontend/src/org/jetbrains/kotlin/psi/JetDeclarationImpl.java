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
import org.jetbrains.kotlin.JetNodeTypes;
import org.jetbrains.kotlin.kdoc.psi.api.KDoc;
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.psi.addRemoveModifier.AddRemoveModifierPackage;
import org.jetbrains.kotlin.psi.findDocComment.FindDocCommentPackage;

import java.util.Collections;
import java.util.List;

abstract class JetDeclarationImpl extends JetExpressionImpl implements JetDeclaration {
    public JetDeclarationImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    @Nullable
    public JetModifierList getModifierList() {
        return (JetModifierList) findChildByType(JetNodeTypes.MODIFIER_LIST);
    }

    @Override
    public boolean hasModifier(@NotNull JetModifierKeywordToken modifier) {
        JetModifierList modifierList = getModifierList();
        return modifierList != null && modifierList.hasModifier(modifier);
    }

    @Override
    public void addModifier(@NotNull JetModifierKeywordToken modifier) {
        AddRemoveModifierPackage.addModifier(this, modifier, JetTokens.INTERNAL_KEYWORD);
    }

    @Override
    public void removeModifier(@NotNull JetModifierKeywordToken modifier) {
        AddRemoveModifierPackage.removeModifier(this, modifier);
    }

    @NotNull
    @Override
    public JetAnnotationEntry addAnnotationEntry(@NotNull JetAnnotationEntry annotationEntry) {
        return AddRemoveModifierPackage.addAnnotationEntry(this, annotationEntry);
    }

    @NotNull
    @Override
    public List<JetAnnotationEntry> getAnnotationEntries() {
        JetModifierList modifierList = getModifierList();
        if (modifierList == null) return Collections.emptyList();
        return modifierList.getAnnotationEntries();
    }

    @NotNull
    @Override
    public List<JetAnnotation> getAnnotations() {
        JetModifierList modifierList = getModifierList();
        if (modifierList == null) return Collections.emptyList();
        return modifierList.getAnnotations();
    }

    @Nullable
    @Override
    public KDoc getDocComment() {
        return FindDocCommentPackage.findDocComment(this);
    }
}
