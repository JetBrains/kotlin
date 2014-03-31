/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.psi.stubs.impl;

import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetModifierList;
import org.jetbrains.jet.lang.psi.stubs.PsiJetModifierListStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetModifierListElementType;
import org.jetbrains.jet.lexer.JetModifierKeywordToken;

import static org.jetbrains.jet.lexer.JetTokens.MODIFIER_KEYWORDS_ARRAY;

public class PsiJetModifierListStubImpl extends StubBase<JetModifierList> implements PsiJetModifierListStub {

    static {
        assert MODIFIER_KEYWORDS_ARRAY.length <= 32 : "Current implementation depends on the ability to represent modifier list as bit mask";
    }

    public static int computeMaskFromPsi(@NotNull JetModifierList modifierList) {
        int mask = 0;
        JetModifierKeywordToken[] orderedKeywords = MODIFIER_KEYWORDS_ARRAY;
        for (int i = 0; i < orderedKeywords.length; i++) {
            JetModifierKeywordToken modifierKeywordToken = orderedKeywords[i];
            if (modifierList.hasModifier(modifierKeywordToken)) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    private final int mask;

    public PsiJetModifierListStubImpl(StubElement parent, int mask, @NotNull JetModifierListElementType<?> elementType) {
        super(parent, elementType);
        this.mask = mask;
    }

    @Override
    public boolean hasModifier(@NotNull JetModifierKeywordToken modifierToken) {
        int index = ArrayUtil.indexOf(MODIFIER_KEYWORDS_ARRAY, modifierToken);
        assert index >= 0 : "All JetModifierKeywordTokens should present in MODIFIER_KEYWORDS_ARRAY";
        return (mask & (1 << index)) != 0;
    }

    public int getMask() {
        return mask;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append("[");
        for (JetModifierKeywordToken modifierKeyword : MODIFIER_KEYWORDS_ARRAY) {
            if (hasModifier(modifierKeyword)) {
                sb.append(modifierKeyword.getValue()).append(" ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
