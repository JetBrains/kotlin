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

    @NotNull
    @Override
    public String toString() {
        return super.toString() + ModifierMaskUtils.maskToString(mask);
    }
}
