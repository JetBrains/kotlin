/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
                returnTypeRef = property.getTypeRef();
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
                JetTypeReference propertyTypeRef = property.getTypeRef();
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

    public static final PositioningStrategy<JetModifierListOwner> ABSTRACT_MODIFIER = modifierSetPosition(JetTokens.ABSTRACT_KEYWORD);

    public static final PositioningStrategy<JetModifierListOwner> OVERRIDE_MODIFIER = modifierSetPosition(JetTokens.OVERRIDE_KEYWORD);

    public static final PositioningStrategy<JetModifierListOwner> FINAL_MODIFIER = modifierSetPosition(JetTokens.FINAL_KEYWORD);

    public static final PositioningStrategy<JetModifierListOwner> VARIANCE_MODIFIER = modifierSetPosition(JetTokens.IN_KEYWORD,
                                                                                                          JetTokens.OUT_KEYWORD);
    public static final PositioningStrategy<PsiElement> FOR_REDECLARATION = new PositioningStrategy<PsiElement>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull PsiElement element) {
            if (element instanceof JetNamedDeclaration) {
                PsiElement nameIdentifier = ((JetNamedDeclaration) element).getNameIdentifier();
                if (nameIdentifier != null) {
                    return markElement(nameIdentifier);
                }
            }
            else if (element instanceof JetFile) {
                JetFile file = (JetFile) element;
                PsiElement nameIdentifier = file.getNamespaceHeader().getNameIdentifier();
                if (nameIdentifier != null) {
                    return markElement(nameIdentifier);
                }
            }
            return markElement(element);
        }
    };
    public static final PositioningStrategy<JetReferenceExpression> FOR_UNRESOLVED_REFERENCE =
            new PositioningStrategy<JetReferenceExpression>() {
                @NotNull
                @Override
                public List<TextRange> mark(@NotNull JetReferenceExpression element) {
                    if (element instanceof JetArrayAccessExpression) {
                        List<TextRange> ranges = ((JetArrayAccessExpression) element).getBracketRanges();
                        if (!ranges.isEmpty()) {
                            return ranges;
                        }
                    }
                    return Collections.singletonList(element.getTextRange());
                }
            };

    public static PositioningStrategy<JetModifierListOwner> modifierSetPosition(final JetKeywordToken... tokens) {
        return new PositioningStrategy<JetModifierListOwner>() {
            @NotNull
            @Override
            public List<TextRange> mark(@NotNull JetModifierListOwner modifierListOwner) {
                JetModifierList modifierList = modifierListOwner.getModifierList();
                assert modifierList != null : "No modifier list, but modifier has been found by the analyzer";

                for (JetKeywordToken token : tokens) {
                    ASTNode node = modifierList.getModifierNode(token);
                    if (node != null) {
                        return markNode(node);
                    }
                }
                throw new IllegalStateException("None of the modifiers is found: " + Arrays.asList(tokens));
            }
        };
    }

    public static final PositioningStrategy<JetArrayAccessExpression> ARRAY_ACCESS = new PositioningStrategy<JetArrayAccessExpression>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetArrayAccessExpression element) {
            return markElement(element.getIndicesNode());
        }
    };

    public static final PositioningStrategy<JetModifierListOwner> VISIBILITY_MODIFIER = new PositioningStrategy<JetModifierListOwner>() {
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

    public static final PositioningStrategy<JetTypeProjection> VARIANCE_IN_PROJECTION = new PositioningStrategy<JetTypeProjection>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetTypeProjection element) {
            return markNode(element.getProjectionNode());
        }
    };

    public static final PositioningStrategy<JetParameter> PARAMETER_DEFAULT_VALUE = new PositioningStrategy<JetParameter>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetParameter element) {
            return markNode(element.getDefaultValue().getNode());
        }
    };

    public static final PositioningStrategy<PsiElement> CALL_ELEMENT = new PositioningStrategy<PsiElement>() {
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

    public static final PositioningStrategy<JetDeclarationWithBody> DECLARATION_WITH_BODY = new PositioningStrategy<JetDeclarationWithBody>() {
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

    public static final PositioningStrategy<JetProperty> VAL_OR_VAR_NODE = new PositioningStrategy<JetProperty>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetProperty property) {
            return markNode(property.getValOrVarNode());
        }
    };

    public static final PositioningStrategy<JetWhenEntry> ELSE_ENTRY = new PositioningStrategy<JetWhenEntry>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetWhenEntry entry) {
            PsiElement elseKeywordElement = entry.getElseKeywordElement();
            assert elseKeywordElement != null;
            return markElement(elseKeywordElement);
        }
    };

    public static final PositioningStrategy<JetWhenExpression> WHEN_EXPRESSION = new PositioningStrategy<JetWhenExpression>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetWhenExpression element) {
            return markElement(element.getWhenKeywordElement());
        }
    };

    public static final PositioningStrategy<JetWhenConditionInRange> WHEN_CONDITION_IN_RANGE =
            new PositioningStrategy<JetWhenConditionInRange>() {
                @NotNull
                @Override
                public List<TextRange> mark(@NotNull JetWhenConditionInRange condition) {
                    return markElement(condition.getOperationReference());
                }
            };

    public static final PositioningStrategy<JetNullableType> NULLABLE_TYPE = new PositioningStrategy<JetNullableType>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetNullableType element) {
            return markNode(element.getQuestionMarkNode());
        }
    };

    public static final PositioningStrategy<JetExpression> CALL_EXPRESSION = new PositioningStrategy<JetExpression>() {
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

    public static final PositioningStrategy<JetElement> VALUE_ARGUMENTS = new PositioningStrategy<JetElement>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetElement element) {
            if (element instanceof JetValueArgumentList) {
                PsiElement rightParenthesis = ((JetValueArgumentList) element).getRightParenthesis();
                if (rightParenthesis != null) {
                    return markElement(rightParenthesis);
                }

            }
            return super.mark(element);
        }
    };

    public static final PositioningStrategy<JetFunctionLiteral> FUNCTION_LITERAL_PARAMETERS = new PositioningStrategy<JetFunctionLiteral>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetFunctionLiteral functionLiteral) {
            JetParameterList valueParameterList = functionLiteral.getValueParameterList();
            if (valueParameterList != null) {
                return markElement(valueParameterList);
            }
            return markNode(functionLiteral.getOpenBraceNode());
        }
    };

    private PositioningStrategies() {
    }
}