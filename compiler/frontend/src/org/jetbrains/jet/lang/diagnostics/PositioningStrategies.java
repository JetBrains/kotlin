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

package org.jetbrains.jet.lang.diagnostics;


import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.List;

/**
 * @author svtk
 */
public class PositioningStrategies {

    public static final PositioningStrategy<PsiElement> DEFAULT = new PositioningStrategy<PsiElement>();

    public static final PositioningStrategy<JetDeclaration> DECLARATION_RETURN_TYPE = new PositioningStrategy<JetDeclaration>() {
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
            if (returnTypeRef != null) return markElement(returnTypeRef);
            if (nameNode != null) return markNode(nameNode);
            return markElement(declaration);
        }

        private ASTNode getNameNode(JetNamedDeclaration function) {
            PsiElement nameIdentifier = function.getNameIdentifier();
            return nameIdentifier == null ? null : nameIdentifier.getNode();
        }
    };

    public static final PositioningStrategy<PsiNameIdentifierOwner> NAME_IDENTIFIER = new PositioningStrategy<PsiNameIdentifierOwner>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull PsiNameIdentifierOwner element) {
            PsiElement nameIdentifier = element.getNameIdentifier();
            if (nameIdentifier != null) {
                return markElement(nameIdentifier);
            }
            return markElement(element);
        }
    };

    public static final PositioningStrategy<PsiNameIdentifierOwner> NAMED_ELEMENT = new PositioningStrategy<PsiNameIdentifierOwner>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull PsiNameIdentifierOwner element) {
            if (element instanceof JetNamedFunction) {
                JetNamedFunction function = (JetNamedFunction)element;
                PsiElement endOfSignatureElement;
                JetParameterList valueParameterList = function.getValueParameterList();
                JetElement returnTypeRef = function.getReturnTypeRef();
                PsiElement nameIdentifier = function.getNameIdentifier();
                if (returnTypeRef != null) {
                    endOfSignatureElement = returnTypeRef;
                }
                else if (valueParameterList != null) {
                    endOfSignatureElement = valueParameterList;
                }
                else if (nameIdentifier != null) {
                    endOfSignatureElement = nameIdentifier;
                }
                else {
                    endOfSignatureElement = function;
                }
                return markRange(new TextRange(
                        function.getTextRange().getStartOffset(), endOfSignatureElement.getTextRange().getEndOffset()));
            }
            else if (element instanceof JetProperty) {
                JetProperty property = (JetProperty) element;
                PsiElement endOfSignatureElement;
                JetTypeReference propertyTypeRef = property.getPropertyTypeRef();
                PsiElement nameIdentifier = property.getNameIdentifier();
                if (propertyTypeRef != null) {
                    endOfSignatureElement = propertyTypeRef;
                }
                else if (nameIdentifier != null) {
                    endOfSignatureElement = nameIdentifier;
                }
                else {
                    endOfSignatureElement = property;
                }
                return markRange(new TextRange(
                        property.getTextRange().getStartOffset(), endOfSignatureElement.getTextRange().getEndOffset()));
            }
            else if (element instanceof JetClass) {
                // primary constructor
                JetClass klass = (JetClass)element;
                PsiElement nameAsDeclaration = klass.getNameIdentifier();
                if (nameAsDeclaration == null) {
                    return markElement(klass);
                }
                PsiElement primaryConstructorParameterList = klass.getPrimaryConstructorParameterList();
                if (primaryConstructorParameterList == null) {
                    return markRange(nameAsDeclaration.getTextRange());
                }
                return markRange(new TextRange(
                        nameAsDeclaration.getTextRange().getStartOffset(), primaryConstructorParameterList.getTextRange().getEndOffset()));
            }
            return super.mark(element);
        }
        @Override
        public boolean isValid(@NotNull PsiNameIdentifierOwner element) {
            return element.getNameIdentifier() != null;
        }
    };

    public static final PositioningStrategy<JetDeclaration> DECLARATION = new PositioningStrategy<JetDeclaration>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetDeclaration element) {
            if (element instanceof PsiNameIdentifierOwner) {
                return NAMED_ELEMENT.mark((PsiNameIdentifierOwner) element);
            }
            return super.mark(element);
        }

        @Override
        public boolean isValid(@NotNull JetDeclaration element) {
            if (element instanceof PsiNameIdentifierOwner) {
                return NAMED_ELEMENT.isValid((PsiNameIdentifierOwner) element);
            }
            return super.isValid(element);
        }
    };

    public static final PositioningStrategy<JetModifierListOwner> ABSTRACT_MODIFIER = positionModifier(JetTokens.ABSTRACT_KEYWORD);

    public static final PositioningStrategy<JetModifierListOwner> OVERRIDE_MODIFIER = positionModifier(JetTokens.OVERRIDE_KEYWORD);

    public static PositioningStrategy<JetModifierListOwner> positionModifier(final JetKeywordToken token) {
        return new PositioningStrategy<JetModifierListOwner>() {
            @NotNull
            @Override
            public List<TextRange> mark(@NotNull JetModifierListOwner modifierListOwner) {
                assert modifierListOwner.hasModifier(token);
                JetModifierList modifierList = modifierListOwner.getModifierList();
                assert modifierList != null;
                ASTNode node = modifierList.getModifierNode(token);
                assert node != null;
                return markNode(node);
            }
        };
    }

    public static PositioningStrategy<JetArrayAccessExpression> ARRAY_ACCESS = new PositioningStrategy<JetArrayAccessExpression>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetArrayAccessExpression element) {
            return markElement(element.getIndicesNode());
        }
    };

    public static PositioningStrategy<JetModifierListOwner> VISIBILITY_MODIFIER = new PositioningStrategy<JetModifierListOwner>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetModifierListOwner element) {
            List<JetKeywordToken> visibilityTokens = Lists
                .newArrayList(JetTokens.PRIVATE_KEYWORD, JetTokens.PROTECTED_KEYWORD, JetTokens.PUBLIC_KEYWORD, JetTokens.INTERNAL_KEYWORD);
            List<TextRange> result = Lists.newArrayList();
            for (JetKeywordToken token : visibilityTokens) {
                if (element.hasModifier(token)) {
                    result.add(element.getModifierList().getModifierNode(token).getTextRange());
                }
            }
            if (result.isEmpty()) {
                if (element.hasModifier(JetTokens.OVERRIDE_KEYWORD)) {
                    result.add(element.getModifierList().getModifierNode(JetTokens.OVERRIDE_KEYWORD).getTextRange());
                }
            }
            return result;
        }
    };

    public static PositioningStrategy<JetTypeProjection> PROJECTION_MODIFIER = new PositioningStrategy<JetTypeProjection>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetTypeProjection element) {
            return markNode(element.getProjectionNode());
        }
    };

    public static PositioningStrategy<JetParameter> PARAMETER_DEFAULT_VALUE = new PositioningStrategy<JetParameter>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetParameter element) {
            return markNode(element.getDefaultValue().getNode());
        }
    };

    public static PositioningStrategy<PsiElement> CALL_ELEMENT = new PositioningStrategy<PsiElement>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull PsiElement callElement) {
            if (callElement instanceof JetCallElement) {
                JetExpression calleeExpression = ((JetCallElement) callElement).getCalleeExpression();
                if (calleeExpression != null) {
                    return markElement(calleeExpression);
                }
            }
            return markElement(callElement);
        }
    };

    public static PositioningStrategy<JetDeclarationWithBody> DECLARATION_WITH_BODY = new PositioningStrategy<JetDeclarationWithBody>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetDeclarationWithBody element) {
            JetExpression bodyExpression = element.getBodyExpression();
            if ((bodyExpression instanceof JetBlockExpression)) {
                TextRange lastBracketRange = ((JetBlockExpression) bodyExpression).getLastBracketRange();
                if (lastBracketRange != null) {
                    return markRange(lastBracketRange);
                }
            }
            return markElement(element);
        }

        @Override
        public boolean isValid(@NotNull JetDeclarationWithBody element) {
            JetExpression bodyExpression = element.getBodyExpression();
            if (!(bodyExpression instanceof JetBlockExpression)) return false;
            if (((JetBlockExpression) bodyExpression).getLastBracketRange() == null) return false;
            return true;
        }
    };

    public static PositioningStrategy<JetWhenEntry> ELSE_ENTRY = new PositioningStrategy<JetWhenEntry>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetWhenEntry entry) {
            PsiElement elseKeywordElement = entry.getElseKeywordElement();
            assert elseKeywordElement != null;
            return markElement(elseKeywordElement);
        }
    };

    public static PositioningStrategy<JetWhenExpression> WHEN_EXPRESSION = new PositioningStrategy<JetWhenExpression>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetWhenExpression element) {
            return markElement(element.getWhenKeywordElement());
        }
    };

    public static PositioningStrategy<JetWhenConditionInRange> WHEN_CONDITION_IN_RANGE =
            new PositioningStrategy<JetWhenConditionInRange>() {
                @NotNull
                @Override
                public List<TextRange> mark(@NotNull JetWhenConditionInRange condition) {
                    return markElement(condition.getOperationReference());
                }
            };

    public static PositioningStrategy<JetNullableType> NULLABLE_TYPE = new PositioningStrategy<JetNullableType>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetNullableType element) {
            return markNode(element.getQuestionMarkNode());
        }
    };

    public static PositioningStrategy<JetExpression> CALL_EXPRESSION = new PositioningStrategy<JetExpression>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetExpression element) {
            if (element instanceof JetCallExpression) {
                JetCallExpression callExpression = (JetCallExpression) element;
                PsiElement endElement;
                JetTypeArgumentList typeArgumentList = callExpression.getTypeArgumentList();
                JetExpression calleeExpression = callExpression.getCalleeExpression();
                if (typeArgumentList != null) {
                    endElement = typeArgumentList;
                }
                else if (calleeExpression != null) {
                    endElement = calleeExpression;
                }
                else {
                    endElement = element;
                }
                return markRange(new TextRange(element.getTextRange().getStartOffset(), endElement.getTextRange().getEndOffset()));
            }
            return super.mark(element);
        }
    };
}