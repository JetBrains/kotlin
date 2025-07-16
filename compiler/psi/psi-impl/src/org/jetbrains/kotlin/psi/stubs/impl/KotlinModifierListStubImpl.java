/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl;

import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken;
import org.jetbrains.kotlin.psi.KtDeclarationModifierList;
import org.jetbrains.kotlin.psi.stubs.KotlinModifierListStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtModifierListElementType;

public class KotlinModifierListStubImpl extends KotlinStubBaseImpl<KtDeclarationModifierList> implements KotlinModifierListStub {

    private final long mask;

    public KotlinModifierListStubImpl(StubElement parent, long mask, @NotNull KtModifierListElementType<?> elementType) {
        super(parent, elementType);
        this.mask = mask;
    }

    public long getMask() {
        return mask;
    }

    @Override
    public boolean hasModifier(@NotNull KtModifierKeywordToken modifierToken) {
        return ModifierMaskUtils.maskHasModifier(mask, modifierToken);
    }

    @Override
    public boolean hasSpecialFlag(@NotNull SpecialFlag flag) {
        return ModifierMaskUtils.maskHasSpecialFlag(mask, flag);
    }

    @NotNull
    @Override
    public String toString() {
        return super.toString() + ModifierMaskUtils.maskToString(mask);
    }

    public boolean hasAnyModifier() {
        return mask != 0;
    }
}
