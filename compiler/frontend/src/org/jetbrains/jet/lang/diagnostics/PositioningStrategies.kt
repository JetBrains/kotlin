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

package org.jetbrains.jet.lang.diagnostics


import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.tree.TokenSet
import kotlin.Function1
import org.jetbrains.jet.JetNodeTypes
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lexer.JetKeywordToken
import org.jetbrains.jet.lexer.JetModifierKeywordToken
import org.jetbrains.jet.lexer.JetTokens

import java.util.Arrays
import java.util.Collections

public class PositioningStrategies private() {

    private open class DeclarationHeader<T : JetDeclaration> : PositioningStrategy<T>() {
        override fun isValid(element: T): Boolean {
            if (element is JetNamedDeclaration && !(element is JetObjectDeclaration)) {
                if ((element as JetNamedDeclaration).getNameIdentifier() == null) {
                    return false
                }
            }
            return super.isValid(element)
        }
    }

    class object {

        public val DEFAULT: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
            override fun mark(element: PsiElement): List<TextRange> {
                if (element is JetObjectLiteralExpression) {
                    val objectDeclaration = (element as JetObjectLiteralExpression).getObjectDeclaration()
                    val objectKeyword = objectDeclaration.getObjectKeyword()
                    val delegationSpecifierList = objectDeclaration.getDelegationSpecifierList()
                    if (delegationSpecifierList == null) {
                        return markElement(objectKeyword)
                    }
                    return markRange(objectKeyword, delegationSpecifierList)
                }
                else if (element is JetClassObject) {
                    val classObject = element as JetClassObject
                    val classKeyword = classObject.getClassKeyword()
                    val objectKeyword = classObject.getObjectDeclaration().getObjectKeyword()
                    return markRange(classKeyword, objectKeyword)
                }
                else {
                    return super.mark(element)
                }
            }
        }

        public val DECLARATION_RETURN_TYPE: PositioningStrategy<JetDeclaration> = object : PositioningStrategy<JetDeclaration>() {
            override fun mark(declaration: JetDeclaration): List<TextRange> {
                return markElement(getElementToMark(declaration))
            }

            override fun isValid(declaration: JetDeclaration): Boolean {
                return !hasSyntaxErrors(getElementToMark(declaration))
            }

            private fun getElementToMark(declaration: JetDeclaration): PsiElement {
                var returnTypeRef: JetTypeReference? = null
                var nameIdentifierOrPlaceholder: PsiElement? = null
                if (declaration is JetNamedFunction) {
                    val function = declaration as JetNamedFunction
                    returnTypeRef = function.getTypeReference()
                    nameIdentifierOrPlaceholder = function.getNameIdentifier()
                }
                else if (declaration is JetProperty) {
                    val property = declaration as JetProperty
                    returnTypeRef = property.getTypeReference()
                    nameIdentifierOrPlaceholder = property.getNameIdentifier()
                }
                else if (declaration is JetPropertyAccessor) {
                    val accessor = declaration as JetPropertyAccessor
                    returnTypeRef = accessor.getReturnTypeReference()
                    nameIdentifierOrPlaceholder = accessor.getNamePlaceholder()
                }

                if (returnTypeRef != null) return returnTypeRef
                if (nameIdentifierOrPlaceholder != null) return nameIdentifierOrPlaceholder
                return declaration
            }
        }

        public val DECLARATION_NAME: PositioningStrategy<JetNamedDeclaration> = object : DeclarationHeader<JetNamedDeclaration>() {
            override fun mark(element: JetNamedDeclaration): List<TextRange> {
                val nameIdentifier = element.getNameIdentifier()
                if (nameIdentifier != null) {
                    if (element is JetClassOrObject) {
                        var startNode: ASTNode? = null
                        if (element.hasModifier(JetTokens.ENUM_KEYWORD)) {
                            //noinspection ConstantConditions
                            startNode = element.getModifierList()!!.getModifier(JetTokens.ENUM_KEYWORD)!!.getNode()
                        }
                        if (startNode == null) {
                            startNode = element.getNode().findChildByType(TokenSet.create(JetTokens.CLASS_KEYWORD, JetTokens.OBJECT_KEYWORD))
                        }
                        if (startNode == null) {
                            startNode = element.getNode()
                        }
                        return markRange(startNode!!.getPsi(), nameIdentifier)
                    }
                    return markElement(nameIdentifier)
                }
                if (element is JetObjectDeclaration) {
                    val objectKeyword = (element as JetObjectDeclaration).getObjectKeyword()
                    val parent = element.getParent()
                    if (parent is JetClassObject) {
                        val classKeyword = (parent as JetClassObject).getClassKeyword()
                        val start = if (classKeyword == null) objectKeyword else classKeyword
                        return markRange(start, objectKeyword)
                    }
                    return markElement(objectKeyword)
                }
                return super.mark(element)
            }
        }

        public val DECLARATION_SIGNATURE: PositioningStrategy<JetDeclaration> = object : DeclarationHeader<JetDeclaration>() {
            override fun mark(element: JetDeclaration): List<TextRange> {
                if (element is JetNamedFunction) {
                    val function = element as JetNamedFunction
                    val endOfSignatureElement: PsiElement
                    val valueParameterList = function.getValueParameterList()
                    val returnTypeRef = function.getTypeReference()
                    val nameIdentifier = function.getNameIdentifier()
                    if (returnTypeRef != null) {
                        endOfSignatureElement = returnTypeRef
                    }
                    else if (valueParameterList != null) {
                        endOfSignatureElement = valueParameterList
                    }
                    else if (nameIdentifier != null) {
                        endOfSignatureElement = nameIdentifier
                    }
                    else {
                        endOfSignatureElement = function
                    }
                    return markRange(function, endOfSignatureElement)
                }
                else if (element is JetProperty) {
                    val property = element as JetProperty
                    val endOfSignatureElement: PsiElement
                    val propertyTypeRef = property.getTypeReference()
                    val nameIdentifier = property.getNameIdentifier()
                    if (propertyTypeRef != null) {
                        endOfSignatureElement = propertyTypeRef
                    }
                    else if (nameIdentifier != null) {
                        endOfSignatureElement = nameIdentifier
                    }
                    else {
                        endOfSignatureElement = property
                    }
                    return markRange(property, endOfSignatureElement)
                }
                else if (element is JetPropertyAccessor) {
                    val accessor = element as JetPropertyAccessor
                    var endOfSignatureElement: PsiElement? = accessor.getReturnTypeReference()
                    if (endOfSignatureElement == null) {
                        val rpar = accessor.getRightParenthesis()
                        endOfSignatureElement = if (rpar == null) null else rpar.getPsi()
                    }
                    if (endOfSignatureElement == null) {
                        endOfSignatureElement = accessor.getNamePlaceholder()
                    }
                    return markRange(accessor, endOfSignatureElement)
                }
                else if (element is JetClass) {
                    val nameAsDeclaration = (element as JetClass).getNameIdentifier()
                    if (nameAsDeclaration == null) {
                        return markElement(element)
                    }
                    val primaryConstructorParameterList = (element as JetClass).getPrimaryConstructorParameterList()
                    if (primaryConstructorParameterList == null) {
                        return markElement(nameAsDeclaration)
                    }
                    return markRange(nameAsDeclaration, primaryConstructorParameterList)
                }
                else if (element is JetObjectDeclaration) {
                    return DECLARATION_NAME.mark(element as JetObjectDeclaration)
                }
                return super.mark(element)
            }
        }

        public val DECLARATION_SIGNATURE_OR_DEFAULT: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
            override fun mark(element: PsiElement): List<TextRange> {
                if (element is JetDeclaration) {
                    return DECLARATION_SIGNATURE.mark(element as JetDeclaration)
                }
                return DEFAULT.mark(element)
            }

            override fun isValid(element: PsiElement): Boolean {
                if (element is JetDeclaration) {
                    return DECLARATION_SIGNATURE.isValid(element as JetDeclaration)
                }
                return DEFAULT.isValid(element)
            }
        }

        public val ABSTRACT_MODIFIER: PositioningStrategy<JetModifierListOwner> = modifierSetPosition(JetTokens.ABSTRACT_KEYWORD)

        public val OVERRIDE_MODIFIER: PositioningStrategy<JetModifierListOwner> = modifierSetPosition(JetTokens.OVERRIDE_KEYWORD)

        public val FINAL_MODIFIER: PositioningStrategy<JetModifierListOwner> = modifierSetPosition(JetTokens.FINAL_KEYWORD)

        public val VARIANCE_MODIFIER: PositioningStrategy<JetModifierListOwner> = modifierSetPosition(JetTokens.IN_KEYWORD, JetTokens.OUT_KEYWORD)
        public val FOR_REDECLARATION: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
            override fun mark(element: PsiElement): List<TextRange> {
                if (element is JetNamedDeclaration) {
                    val nameIdentifier = (element as JetNamedDeclaration).getNameIdentifier()
                    if (nameIdentifier != null) {
                        return markElement(nameIdentifier)
                    }
                }
                else if (element is JetFile) {
                    val file = element as JetFile
                    val nameIdentifier = file.getPackageDirective()!!.getNameIdentifier()
                    if (nameIdentifier != null) {
                        return markElement(nameIdentifier)
                    }
                }
                return markElement(element)
            }
        }
        public val FOR_UNRESOLVED_REFERENCE: PositioningStrategy<JetReferenceExpression> = object : PositioningStrategy<JetReferenceExpression>() {
            override fun mark(element: JetReferenceExpression): List<TextRange> {
                if (element is JetArrayAccessExpression) {
                    val ranges = (element as JetArrayAccessExpression).getBracketRanges()
                    if (!ranges.isEmpty()) {
                        return ranges
                    }
                }
                return listOf(element.getTextRange())
            }
        }

        public fun modifierSetPosition(vararg tokens: JetKeywordToken): PositioningStrategy<JetModifierListOwner> {
            return object : PositioningStrategy<JetModifierListOwner>() {
                override fun mark(modifierListOwner: JetModifierListOwner): List<TextRange> {
                    val modifierList = modifierListOwner.getModifierList()
                    assert(modifierList != null, "No modifier list, but modifier has been found by the analyzer")

                    for (token in tokens) {
                        val node = modifierList!!.getModifierNode(token)
                        if (node != null) {
                            return markNode(node)
                        }
                    }
                    throw IllegalStateException("None of the modifiers is found: " + Arrays.asList<JetKeywordToken>(*tokens))
                }
            }
        }

        public val ARRAY_ACCESS: PositioningStrategy<JetArrayAccessExpression> = object : PositioningStrategy<JetArrayAccessExpression>() {
            override fun mark(element: JetArrayAccessExpression): List<TextRange> {
                return markElement(element.getIndicesNode())
            }
        }

        public val VISIBILITY_MODIFIER: PositioningStrategy<JetModifierListOwner> = object : PositioningStrategy<JetModifierListOwner>() {
            override fun mark(element: JetModifierListOwner): List<TextRange> {
                val visibilityTokens = Lists.newArrayList<JetModifierKeywordToken>(JetTokens.PRIVATE_KEYWORD, JetTokens.PROTECTED_KEYWORD, JetTokens.PUBLIC_KEYWORD, JetTokens.INTERNAL_KEYWORD)
                val result = Lists.newArrayList<TextRange>()
                for (token in visibilityTokens) {
                    if (element.hasModifier(token)) {
                        //noinspection ConstantConditions
                        result.add(element.getModifierList()!!.getModifierNode(token)!!.getTextRange())
                    }
                }

                if (!result.isEmpty()) return result

                // Try to resolve situation when there's no visibility modifiers written before element

                if (element is PsiNameIdentifierOwner) {
                    val nameIdentifier = (element as PsiNameIdentifierOwner).getNameIdentifier()
                    if (nameIdentifier != null) {
                        return ImmutableList.of<TextRange>(nameIdentifier.getTextRange())
                    }
                }

                if (element is JetObjectDeclaration) {
                    return ImmutableList.of<TextRange>((element as JetObjectDeclaration).getObjectKeyword().getTextRange())
                }

                if (element is JetPropertyAccessor) {
                    return ImmutableList.of<TextRange>((element as JetPropertyAccessor).getNamePlaceholder().getTextRange())
                }

                if (element is JetClassInitializer) {
                    return ImmutableList.of<TextRange>(element.getTextRange())
                }

                if (element is JetClassObject) {
                    val objectDeclaration = (element as JetClassObject).getObjectDeclaration()
                    return ImmutableList.of<TextRange>(objectDeclaration.getObjectKeyword().getTextRange())
                }

                throw IllegalArgumentException(String.format("Can't find text range for element '%s' with the text '%s'", element.javaClass.getCanonicalName(), element.getText()))
            }
        }

        public val VARIANCE_IN_PROJECTION: PositioningStrategy<JetTypeProjection> = object : PositioningStrategy<JetTypeProjection>() {
            override fun mark(element: JetTypeProjection): List<TextRange> {
                return markNode(element.getProjectionNode())
            }
        }

        public val PARAMETER_DEFAULT_VALUE: PositioningStrategy<JetParameter> = object : PositioningStrategy<JetParameter>() {
            override fun mark(element: JetParameter): List<TextRange> {
                return markNode(element.getDefaultValue()!!.getNode())
            }
        }

        public val CALL_ELEMENT: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
            override fun mark(callElement: PsiElement): List<TextRange> {
                if (callElement is JetCallElement) {
                    val calleeExpression = (callElement as JetCallElement).getCalleeExpression()
                    if (calleeExpression != null) {
                        return markElement(calleeExpression)
                    }
                }
                return markElement(callElement)
            }
        }

        public val DECLARATION_WITH_BODY: PositioningStrategy<JetDeclarationWithBody> = object : PositioningStrategy<JetDeclarationWithBody>() {
            override fun mark(element: JetDeclarationWithBody): List<TextRange> {
                val bodyExpression = element.getBodyExpression()
                if ((bodyExpression is JetBlockExpression)) {
                    val lastBracketRange = (bodyExpression as JetBlockExpression).getLastBracketRange()
                    if (lastBracketRange != null) {
                        return markRange(lastBracketRange)
                    }
                }
                return markElement(element)
            }

            override fun isValid(element: JetDeclarationWithBody): Boolean {
                if (!super.isValid(element)) return false

                val bodyExpression = element.getBodyExpression()
                if (!(bodyExpression is JetBlockExpression)) return false
                if ((bodyExpression as JetBlockExpression).getLastBracketRange() == null) return false
                return true
            }
        }

        public val VAL_OR_VAR_NODE: PositioningStrategy<JetProperty> = object : PositioningStrategy<JetProperty>() {
            override fun mark(property: JetProperty): List<TextRange> {
                return markNode(property.getValOrVarNode())
            }
        }

        public val ELSE_ENTRY: PositioningStrategy<JetWhenEntry> = object : PositioningStrategy<JetWhenEntry>() {
            override fun mark(entry: JetWhenEntry): List<TextRange> {
                val elseKeywordElement = entry.getElseKeywordElement()
                assert(elseKeywordElement != null)
                return markElement(elseKeywordElement)
            }
        }

        public val WHEN_EXPRESSION: PositioningStrategy<JetWhenExpression> = object : PositioningStrategy<JetWhenExpression>() {
            override fun mark(element: JetWhenExpression): List<TextRange> {
                return markElement(element.getWhenKeywordElement())
            }
        }

        public val WHEN_CONDITION_IN_RANGE: PositioningStrategy<JetWhenConditionInRange> = object : PositioningStrategy<JetWhenConditionInRange>() {
            override fun mark(condition: JetWhenConditionInRange): List<TextRange> {
                return markElement(condition.getOperationReference())
            }
        }

        public val NULLABLE_TYPE: PositioningStrategy<JetNullableType> = object : PositioningStrategy<JetNullableType>() {
            override fun mark(element: JetNullableType): List<TextRange> {
                return markNode(element.getQuestionMarkNode())
            }
        }

        public val CALL_EXPRESSION: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
            override fun mark(element: PsiElement): List<TextRange> {
                if (element is JetCallExpression) {
                    val callExpression = element as JetCallExpression
                    val endElement: PsiElement
                    val typeArgumentList = callExpression.getTypeArgumentList()
                    val calleeExpression = callExpression.getCalleeExpression()
                    if (typeArgumentList != null) {
                        endElement = typeArgumentList
                    }
                    else if (calleeExpression != null) {
                        endElement = calleeExpression
                    }
                    else {
                        endElement = element
                    }
                    return markRange(element, endElement)
                }
                return super.mark(element)
            }
        }

        public val VALUE_ARGUMENTS: PositioningStrategy<JetElement> = object : PositioningStrategy<JetElement>() {
            override fun mark(element: JetElement): List<TextRange> {
                if (element is JetValueArgumentList) {
                    val rightParenthesis = (element as JetValueArgumentList).getRightParenthesis()
                    if (rightParenthesis != null) {
                        return markElement(rightParenthesis)
                    }

                }
                return super.mark(element)
            }
        }

        public val FUNCTION_LITERAL_PARAMETERS: PositioningStrategy<JetFunctionLiteral> = object : PositioningStrategy<JetFunctionLiteral>() {
            override fun mark(functionLiteral: JetFunctionLiteral): List<TextRange> {
                val valueParameterList = functionLiteral.getValueParameterList()
                if (valueParameterList != null) {
                    return markElement(valueParameterList)
                }
                return markNode(functionLiteral.getLBrace().getNode())
            }
        }

        public val CUT_CHAR_QUOTES: PositioningStrategy<JetElement> = object : PositioningStrategy<JetElement>() {
            override fun mark(element: JetElement): List<TextRange> {
                if (element is JetConstantExpression) {
                    if (element.getNode().getElementType() == JetNodeTypes.CHARACTER_CONSTANT) {
                        val elementTextRange = element.getTextRange()
                        return listOf(TextRange.create(elementTextRange.getStartOffset() + 1, elementTextRange.getEndOffset() - 1))
                    }
                }
                return super.mark(element)
            }
        }

        public val LONG_LITERAL_SUFFIX: PositioningStrategy<JetElement> = object : PositioningStrategy<JetElement>() {
            override fun mark(element: JetElement): List<TextRange> {
                if (element is JetConstantExpression) {
                    if (element.getNode().getElementType() == JetNodeTypes.INTEGER_CONSTANT) {
                        val endOffset = element.getTextRange().getEndOffset()
                        return listOf(TextRange.create(endOffset - 1, endOffset))
                    }
                }
                return super.mark(element)
            }
        }

        public fun markTextRangesFromDiagnostic(getTextRanges: Function1<Diagnostic, List<TextRange>>): PositioningStrategy<PsiElement> {
            return object : PositioningStrategy<PsiElement>() {
                override fun markDiagnostic(diagnostic: ParametrizedDiagnostic<out PsiElement>): List<TextRange> {
                    return getTextRanges.invoke(diagnostic)
                }
            }
        }
    }
}