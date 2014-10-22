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


import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.tree.TokenSet;
import kotlin.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.lexer.JetModifierKeywordToken;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PositioningStrategies {

    public static final PositioningStrategy<PsiElement> DEFAULT = new PositioningStrategy<PsiElement>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull PsiElement element) {
            if (element instanceof JetObjectLiteralExpression) {
                JetObjectDeclaration objectDeclaration = ((JetObjectLiteralExpression) element).getObjectDeclaration();
                PsiElement objectKeyword = objectDeclaration.getObjectKeyword();
                JetDelegationSpecifierList delegationSpecifierList = objectDeclaration.getDelegationSpecifierList();
                if (delegationSpecifierList == null) {
                    return markElement(objectKeyword);
                }
                return markRange(objectKeyword, delegationSpecifierList);
            }
            return super.mark(element);
        }
    };

    public static final PositioningStrategy<JetDeclaration> DECLARATION_RETURN_TYPE = new PositioningStrategy<JetDeclaration>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetDeclaration declaration) {
            return markElement(getElementToMark(declaration));
        }

        @Override
        public boolean isValid(@NotNull JetDeclaration declaration) {
            return !hasSyntaxErrors(getElementToMark(declaration));
        }

        private PsiElement getElementToMark(@NotNull JetDeclaration declaration) {
            JetTypeReference returnTypeRef = null;
            PsiElement nameIdentifierOrPlaceholder = null;
            if (declaration instanceof JetNamedFunction) {
                JetFunction function = (JetNamedFunction) declaration;
                returnTypeRef = function.getTypeReference();
                nameIdentifierOrPlaceholder = function.getNameIdentifier();
            }
            else if (declaration instanceof JetProperty) {
                JetProperty property = (JetProperty) declaration;
                returnTypeRef = property.getTypeReference();
                nameIdentifierOrPlaceholder = property.getNameIdentifier();
            }
            else if (declaration instanceof JetPropertyAccessor) {
                JetPropertyAccessor accessor = (JetPropertyAccessor) declaration;
                returnTypeRef = accessor.getReturnTypeReference();
                nameIdentifierOrPlaceholder = accessor.getNamePlaceholder();
            }

            if (returnTypeRef != null) return returnTypeRef;
            if (nameIdentifierOrPlaceholder != null) return nameIdentifierOrPlaceholder;
            return declaration;
        }
    };

    private static class DeclarationHeader<T extends JetDeclaration> extends PositioningStrategy<T> {
        @Override
        public boolean isValid(@NotNull T element) {
            if (element instanceof JetNamedDeclaration && !(element instanceof JetObjectDeclaration)) {
                if (((JetNamedDeclaration) element).getNameIdentifier() == null) {
                    return false;
                }
            }
            return super.isValid(element);
        }
    }

    public static final PositioningStrategy<JetNamedDeclaration> DECLARATION_NAME = new DeclarationHeader<JetNamedDeclaration>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetNamedDeclaration element) {
            PsiElement nameIdentifier = element.getNameIdentifier();
            if (nameIdentifier != null) {
                if (element instanceof JetClassOrObject) {
                    ASTNode startNode = null;
                    if (element.hasModifier(JetTokens.ENUM_KEYWORD)) {
                        //noinspection ConstantConditions
                        startNode = element.getModifierList().getModifier(JetTokens.ENUM_KEYWORD).getNode();
                    }
                    if (startNode == null) {
                        startNode = element.getNode().findChildByType(TokenSet.create(JetTokens.CLASS_KEYWORD, JetTokens.OBJECT_KEYWORD));
                    }
                    if (startNode == null) {
                        startNode = element.getNode();
                    }
                    return markRange(startNode.getPsi(), nameIdentifier);
                }
                return markElement(nameIdentifier);
            }
            if (element instanceof JetObjectDeclaration) {
                PsiElement objectKeyword = ((JetObjectDeclaration) element).getObjectKeyword();
                PsiElement parent = element.getParent();
                if (parent instanceof JetClassObject) {
                    PsiElement classKeyword = ((JetClassObject) parent).getClassKeywordNode();
                    PsiElement start = classKeyword == null ? objectKeyword : classKeyword;
                    return markRange(start, objectKeyword);
                }
                return markElement(objectKeyword);
            }
            return super.mark(element);
        }
    };

    public static final PositioningStrategy<JetDeclaration> DECLARATION_SIGNATURE = new DeclarationHeader<JetDeclaration>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetDeclaration element) {
            if (element instanceof JetNamedFunction) {
                JetNamedFunction function = (JetNamedFunction)element;
                PsiElement endOfSignatureElement;
                JetParameterList valueParameterList = function.getValueParameterList();
                JetElement returnTypeRef = function.getTypeReference();
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
                return markRange(function, endOfSignatureElement);
            }
            else if (element instanceof JetProperty) {
                JetProperty property = (JetProperty) element;
                PsiElement endOfSignatureElement;
                JetTypeReference propertyTypeRef = property.getTypeReference();
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
                return markRange(property, endOfSignatureElement);
            }
            else if (element instanceof JetPropertyAccessor) {
                JetPropertyAccessor accessor = (JetPropertyAccessor) element;
                PsiElement endOfSignatureElement = accessor.getReturnTypeReference();
                if (endOfSignatureElement == null) {
                    ASTNode rpar = accessor.getRightParenthesis();
                    endOfSignatureElement = rpar == null ? null : rpar.getPsi();
                }
                if (endOfSignatureElement == null) {
                    endOfSignatureElement = accessor.getNamePlaceholder();
                }
                return markRange(accessor, endOfSignatureElement);
            }
            else if (element instanceof JetClass) {
                PsiElement nameAsDeclaration = ((JetClass) element).getNameIdentifier();
                if (nameAsDeclaration == null) {
                    return markElement(element);
                }
                PsiElement primaryConstructorParameterList = ((JetClass) element).getPrimaryConstructorParameterList();
                if (primaryConstructorParameterList == null) {
                    return markElement(nameAsDeclaration);
                }
                return markRange(nameAsDeclaration, primaryConstructorParameterList);
            }
            else if (element instanceof JetObjectDeclaration) {
                return DECLARATION_NAME.mark((JetObjectDeclaration) element);
            }
            return super.mark(element);
        }
    };

    public static final PositioningStrategy<PsiElement> DECLARATION_SIGNATURE_OR_DEFAULT = new PositioningStrategy<PsiElement>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull PsiElement element) {
            if (element instanceof JetDeclaration) {
                return DECLARATION_SIGNATURE.mark((JetDeclaration) element);
            }
            return DEFAULT.mark(element);
        }

        @Override
        public boolean isValid(@NotNull PsiElement element) {
            if (element instanceof JetDeclaration) {
                return DECLARATION_SIGNATURE.isValid((JetDeclaration) element);
            }
            return DEFAULT.isValid(element);
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
                PsiElement nameIdentifier = file.getPackageDirective().getNameIdentifier();
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
            List<JetModifierKeywordToken> visibilityTokens = Lists.newArrayList(
                    JetTokens.PRIVATE_KEYWORD, JetTokens.PROTECTED_KEYWORD, JetTokens.PUBLIC_KEYWORD, JetTokens.INTERNAL_KEYWORD);
            List<TextRange> result = Lists.newArrayList();
            for (JetModifierKeywordToken token : visibilityTokens) {
                if (element.hasModifier(token)) {
                    //noinspection ConstantConditions
                    result.add(element.getModifierList().getModifierNode(token).getTextRange());
                }
            }

            if (!result.isEmpty()) return result;

            // Try to resolve situation when there's no visibility modifiers written before element

            if (element instanceof PsiNameIdentifierOwner) {
                PsiElement nameIdentifier = ((PsiNameIdentifierOwner) element).getNameIdentifier();
                if (nameIdentifier != null) {
                    return ImmutableList.of(nameIdentifier.getTextRange());
                }
            }

            if (element instanceof JetObjectDeclaration) {
                return ImmutableList.of(((JetObjectDeclaration) element).getObjectKeyword().getTextRange());
            }

            if (element instanceof JetPropertyAccessor) {
                return ImmutableList.of(((JetPropertyAccessor) element).getNamePlaceholder().getTextRange());
            }

            if (element instanceof JetClassInitializer) {
                return ImmutableList.of(element.getTextRange());
            }

            if (element instanceof JetClassObject) {
                JetObjectDeclaration objectDeclaration = ((JetClassObject) element).getObjectDeclaration();
                return ImmutableList.of(objectDeclaration.getObjectKeyword().getTextRange());
            }

            throw new IllegalArgumentException(
                    String.format("Can't find text range for element '%s' with the text '%s'",
                                  element.getClass().getCanonicalName(), element.getText()));
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
            if (!super.isValid(element)) return false;

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

    public static final PositioningStrategy<PsiElement> CALL_EXPRESSION = new PositioningStrategy<PsiElement>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull PsiElement element) {
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
                return markRange(element, endElement);
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
            return markNode(functionLiteral.getLBrace().getNode());
        }
    };

    public static final PositioningStrategy<JetElement> CUT_CHAR_QUOTES = new PositioningStrategy<JetElement>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetElement element) {
            if (element instanceof JetConstantExpression) {
                if (element.getNode().getElementType() == JetNodeTypes.CHARACTER_CONSTANT) {
                    TextRange elementTextRange = element.getTextRange();
                    return Collections.singletonList(
                            TextRange.create(elementTextRange.getStartOffset() + 1, elementTextRange.getEndOffset() - 1));
                }
            }
            return super.mark(element);
        }
    };

    public static final PositioningStrategy<JetElement> LONG_LITERAL_SUFFIX = new PositioningStrategy<JetElement>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetElement element) {
            if (element instanceof JetConstantExpression) {
                if (element.getNode().getElementType() == JetNodeTypes.INTEGER_CONSTANT) {
                    int endOffset = element.getTextRange().getEndOffset();
                    return Collections.singletonList(TextRange.create(endOffset - 1, endOffset));
                }
            }
            return super.mark(element);
        }
    };

    public static PositioningStrategy<PsiElement> markTextRangesFromDiagnostic(
            @NotNull final Function1<Diagnostic, List<TextRange>> getTextRanges
    ) {
        return new PositioningStrategy<PsiElement>() {
            @NotNull
            @Override
            public List<TextRange> markDiagnostic(@NotNull ParametrizedDiagnostic<? extends PsiElement> diagnostic) {
                return getTextRanges.invoke(diagnostic);
            }
        };
    }

    private PositioningStrategies() {
    }
}