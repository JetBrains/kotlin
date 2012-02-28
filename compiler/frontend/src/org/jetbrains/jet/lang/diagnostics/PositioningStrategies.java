/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.diagnostics;


import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collections;
import java.util.List;

/**
 * @author svtk
 */
public class PositioningStrategies {

    public static final PositioningStrategy<PsiElement> DEFAULT = new PositioningStrategy<PsiElement>();
    
    public static final PositioningStrategy<JetFunction> MARK_FUNCTION = new PositioningStrategy<JetFunction>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetFunction function) {
            TextRange textRange;
            PsiElement nameIdentifier = function.getNameIdentifier();
            JetTypeReference returnTypeRef = function.getReturnTypeRef();
            JetParameterList valueParameterList = function.getValueParameterList();
            if (nameIdentifier == null) {
                textRange = TextRange.from(function.getTextRange().getEndOffset(), 0);
            }
            else if (returnTypeRef != null) {
                textRange = TextRange.from(returnTypeRef.getTextRange().getEndOffset(), 1);
            }
            else if (valueParameterList != null) {
                textRange = TextRange.from(valueParameterList.getTextRange().getEndOffset(), 1);
            }
            else {
                textRange = TextRange.from(nameIdentifier.getTextRange().getEndOffset(), 1);
            }
            return markRange(textRange);

        }
    };
    
    public static final PositioningStrategy<JetDeclaration> POSITION_DECLARATION = new PositioningStrategy<JetDeclaration>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetDeclaration declaration) {
            JetTypeReference returnTypeRef = null;
            ASTNode nameNode = null;
            if (declaration instanceof JetNamedFunction) {
                JetFunction function = (JetNamedFunction) declaration;
                returnTypeRef = function.getReturnTypeRef();
                nameNode = getNameNode(function);
            }
            else if (declaration instanceof JetProperty) {
                JetProperty property = (JetProperty) declaration;
                returnTypeRef = property.getPropertyTypeRef();
                nameNode = getNameNode(property);
            }
            else if (declaration instanceof JetPropertyAccessor) {
                JetPropertyAccessor accessor = (JetPropertyAccessor) declaration;
                returnTypeRef = accessor.getReturnTypeReference();
                nameNode = accessor.getNamePlaceholder().getNode();
            }
            if (returnTypeRef != null) return Collections.singletonList(returnTypeRef.getTextRange());
            if (nameNode != null) return Collections.singletonList(nameNode.getTextRange());
            return super.mark(declaration);

        }
        private ASTNode getNameNode(JetNamedDeclaration function) {
            PsiElement nameIdentifier = function.getNameIdentifier();
            return nameIdentifier == null ? null : nameIdentifier.getNode();
        }
    };
    
    public static final PositioningStrategy<PsiNameIdentifierOwner> POSITION_NAME_IDENTIFIER = new PositioningStrategy<PsiNameIdentifierOwner>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull PsiNameIdentifierOwner element) {
            PsiElement nameIdentifier = element.getNameIdentifier();
            if (nameIdentifier != null) {
                return markElement(nameIdentifier);
            }
            return Collections.emptyList();
        }
    };

    public static final PositioningStrategy<JetModifierListOwner> POSITION_ABSTRACT_MODIFIER = positionModifier(JetTokens.ABSTRACT_KEYWORD);

    public static PositioningStrategy<JetModifierListOwner> positionModifier(final JetKeywordToken token) {
        return new PositioningStrategy<JetModifierListOwner>() {
            @NotNull
            @Override
            public List<TextRange> mark(@NotNull JetModifierListOwner modifierListOwner) {
                if (modifierListOwner.hasModifier(token)) {
                    JetModifierList modifierList = modifierListOwner.getModifierList();
                    assert  modifierList != null;
                    ASTNode node = modifierList.getModifierNode(token);
                    assert node != null;
                    return Collections.singletonList(node.getTextRange());
                }
                return Collections.emptyList();
            }
        };
    }
}