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

package org.jetbrains.kotlin.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.JetNodeTypes
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.lexer.JetKeywordToken
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.utils.sure
import kotlin.platform.platformStatic
import org.jetbrains.kotlin.psi.psiUtil.*

public object PositioningStrategies {
    private open class DeclarationHeader<T : JetDeclaration> : PositioningStrategy<T>() {
        override fun isValid(element: T): Boolean {
            if (element is JetNamedDeclaration &&
                element !is JetObjectDeclaration &&
                element !is JetSecondaryConstructor &&
                element !is JetNamedFunction
            ) {
                if (element.getNameIdentifier() == null) {
                    return false
                }
            }
            return super.isValid(element)
        }
    }

    public val DEFAULT: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            when (element) {
                is JetObjectLiteralExpression -> {
                    val objectDeclaration = element.getObjectDeclaration()
                    val objectKeyword = objectDeclaration.getObjectKeyword()
                    val delegationSpecifierList = objectDeclaration.getDelegationSpecifierList()
                    if (delegationSpecifierList == null) {
                        return markElement(objectKeyword)
                    }
                    return markRange(objectKeyword, delegationSpecifierList)
                }
                is JetObjectDeclaration -> {
                    return markRange(
                            element.getClassKeyword() ?: element.getObjectKeyword(),
                            element.getNameIdentifier() ?: element.getObjectKeyword()
                    )
                }
                else -> {
                    return super.mark(element)
                }
            }
        }
    }

    public val DECLARATION_RETURN_TYPE: PositioningStrategy<JetDeclaration> = object : PositioningStrategy<JetDeclaration>() {
        override fun mark(element: JetDeclaration): List<TextRange> {
            return markElement(getElementToMark(element))
        }

        override fun isValid(element: JetDeclaration): Boolean {
            return !hasSyntaxErrors(getElementToMark(element))
        }

        private fun getElementToMark(declaration: JetDeclaration): PsiElement {
            val (returnTypeRef, nameIdentifierOrPlaceholder) = when (declaration) {
                is JetCallableDeclaration -> Pair(declaration.getTypeReference(), declaration.getNameIdentifier())
                is JetPropertyAccessor -> Pair(declaration.getReturnTypeReference(), declaration.getNamePlaceholder())
                else -> Pair(null, null)
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
                    val startElement =
                            element.getModifierList()?.getModifier(JetTokens.ENUM_KEYWORD)
                            ?: element.getNode().findChildByType(TokenSet.create(JetTokens.CLASS_KEYWORD, JetTokens.OBJECT_KEYWORD))?.getPsi()
                            ?: element

                    return markRange(startElement, nameIdentifier)
                }
                return markElement(nameIdentifier)
            }
            if (element is JetNamedFunction) {
                return DECLARATION_SIGNATURE.mark(element)
            }
            return DEFAULT.mark(element)
        }
    }

    public val DECLARATION_SIGNATURE: PositioningStrategy<JetDeclaration> = object : DeclarationHeader<JetDeclaration>() {
        override fun mark(element: JetDeclaration): List<TextRange> {
            when (element) {
                is JetNamedFunction -> {
                    val endOfSignatureElement =
                            element.getTypeReference()
                            ?: element.getValueParameterList()
                            ?: element.getNameIdentifier()
                            ?: element

                    return markRange(element, endOfSignatureElement)
                }
                is JetProperty -> {
                    val endOfSignatureElement = element.getTypeReference() ?: element.getNameIdentifier() ?: element
                    return markRange(element, endOfSignatureElement)
                }
                is JetPropertyAccessor -> {
                    val endOfSignatureElement =
                            element.getReturnTypeReference()
                            ?: element.getRightParenthesis()?.getPsi()
                            ?: element.getNamePlaceholder()

                    return markRange(element, endOfSignatureElement)
                }
                is JetClass -> {
                    val nameAsDeclaration = element.getNameIdentifier() ?: return markElement(element)
                    val primaryConstructorParameterList = element.getPrimaryConstructorParameterList() ?: return markElement(nameAsDeclaration)
                    return markRange(nameAsDeclaration, primaryConstructorParameterList)
                }
                is JetObjectDeclaration -> {
                    return DECLARATION_NAME.mark(element)
                }
                is JetSecondaryConstructor -> {
                    return markRange(element.getConstructorKeyword(), element.getValueParameterList() ?: element.getConstructorKeyword())
                }
            }
            return super.mark(element)
        }
    }

    public val DECLARATION_SIGNATURE_OR_DEFAULT: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            return if (element is JetDeclaration)
                DECLARATION_SIGNATURE.mark(element)
            else
                DEFAULT.mark(element)
        }

        override fun isValid(element: PsiElement): Boolean {
            return if (element is JetDeclaration)
                DECLARATION_SIGNATURE.isValid(element)
            else
                DEFAULT.isValid(element)
        }
    }

    public val TYPE_PARAMETERS_OR_DECLARATION_SIGNATURE: PositioningStrategy<JetDeclaration> = object : PositioningStrategy<JetDeclaration>() {
        override fun mark(element: JetDeclaration): List<TextRange> {
            if (element is JetTypeParameterListOwner) {
                val jetTypeParameterList = element.getTypeParameterList()
                if (jetTypeParameterList != null) {
                    return markElement(jetTypeParameterList)
                }
            }
            return DECLARATION_SIGNATURE.mark(element)
        }
    }

    public val ABSTRACT_MODIFIER: PositioningStrategy<JetModifierListOwner> = modifierSetPosition(JetTokens.ABSTRACT_KEYWORD)

    public val INNER_MODIFIER: PositioningStrategy<JetModifierListOwner> = modifierSetPosition(JetTokens.INNER_KEYWORD)

    public val OVERRIDE_MODIFIER: PositioningStrategy<JetModifierListOwner> = modifierSetPosition(JetTokens.OVERRIDE_KEYWORD)

    public val FINAL_MODIFIER: PositioningStrategy<JetModifierListOwner> = modifierSetPosition(JetTokens.FINAL_KEYWORD)

    public val VARIANCE_MODIFIER: PositioningStrategy<JetModifierListOwner> = modifierSetPosition(JetTokens.IN_KEYWORD, JetTokens.OUT_KEYWORD)

    public val FOR_REDECLARATION: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            val nameIdentifier = when (element) {
                is JetNamedDeclaration -> element.getNameIdentifier()
                is JetFile -> element.getPackageDirective()!!.getNameIdentifier()
                else -> null
            }

            if (nameIdentifier == null && element is JetObjectDeclaration) return DEFAULT.mark(element)

            return markElement(nameIdentifier ?: element)
        }
    }
    public val FOR_UNRESOLVED_REFERENCE: PositioningStrategy<JetReferenceExpression> = object : PositioningStrategy<JetReferenceExpression>() {
        override fun mark(element: JetReferenceExpression): List<TextRange> {
            if (element is JetArrayAccessExpression) {
                val ranges = element.getBracketRanges()
                if (!ranges.isEmpty()) {
                    return ranges
                }
            }
            return listOf(element.getTextRange())
        }
    }

    platformStatic
    public fun modifierSetPosition(vararg tokens: JetKeywordToken): PositioningStrategy<JetModifierListOwner> {
        return object : PositioningStrategy<JetModifierListOwner>() {
            override fun mark(element: JetModifierListOwner): List<TextRange> {
                val modifierList = element.getModifierList().sure("No modifier list, but modifier has been found by the analyzer")

                for (token in tokens) {
                    val node = modifierList.getModifierNode(token)
                    if (node != null) {
                        return markNode(node)
                    }
                }
                throw IllegalStateException("None of the modifiers is found: " + listOf(*tokens))
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
            val visibilityTokens = listOf(JetTokens.PRIVATE_KEYWORD, JetTokens.PROTECTED_KEYWORD, JetTokens.PUBLIC_KEYWORD, JetTokens.INTERNAL_KEYWORD)
            val modifierList = element.getModifierList()

            val result = visibilityTokens.map { modifierList?.getModifierNode(it)?.getTextRange() }.filterNotNull()
            if (!result.isEmpty()) return result

            // Try to resolve situation when there's no visibility modifiers written before element
            if (element is PsiNameIdentifierOwner) {
                val nameIdentifier = element.getNameIdentifier()
                if (nameIdentifier != null) {
                    return markElement(nameIdentifier)
                }
            }

            val elementToMark = when (element) {
                is JetObjectDeclaration -> element.getObjectKeyword()
                is JetPropertyAccessor -> element.getNamePlaceholder()
                is JetClassInitializer -> element
                else -> throw IllegalArgumentException(
                        "Can't find text range for element '${element.javaClass.getCanonicalName()}' with the text '${element.getText()}'")
            }
            return markElement(elementToMark)
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
        override fun mark(element: PsiElement): List<TextRange> {
            return markElement((element as? JetCallElement)?.getCalleeExpression() ?: element)
        }
    }

    public val DECLARATION_WITH_BODY: PositioningStrategy<JetDeclarationWithBody> = object : PositioningStrategy<JetDeclarationWithBody>() {
        override fun mark(element: JetDeclarationWithBody): List<TextRange> {
            val lastBracketRange = (element.getBodyExpression() as? JetBlockExpression)?.getLastBracketRange()
            return if (lastBracketRange != null)
                markRange(lastBracketRange)
            else
                markElement(element)
        }

        override fun isValid(element: JetDeclarationWithBody): Boolean {
            return super.isValid(element) && (element.getBodyExpression() as? JetBlockExpression)?.getLastBracketRange() != null
        }
    }

    public val VAL_OR_VAR_NODE: PositioningStrategy<JetProperty> = object : PositioningStrategy<JetProperty>() {
        override fun mark(element: JetProperty): List<TextRange> {
            return markNode(element.getValOrVarNode())
        }
    }

    public val ELSE_ENTRY: PositioningStrategy<JetWhenEntry> = object : PositioningStrategy<JetWhenEntry>() {
        override fun mark(element: JetWhenEntry): List<TextRange> {
            return markElement(element.getElseKeywordElement()!!)
        }
    }

    public val WHEN_EXPRESSION: PositioningStrategy<JetWhenExpression> = object : PositioningStrategy<JetWhenExpression>() {
        override fun mark(element: JetWhenExpression): List<TextRange> {
            return markElement(element.getWhenKeywordElement())
        }
    }

    public val WHEN_CONDITION_IN_RANGE: PositioningStrategy<JetWhenConditionInRange> = object : PositioningStrategy<JetWhenConditionInRange>() {
        override fun mark(element: JetWhenConditionInRange): List<TextRange> {
            return markElement(element.getOperationReference())
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
                return markRange(element, element.getTypeArgumentList() ?: element.getCalleeExpression() ?: element)
            }
            return markElement(element)
        }
    }

    public val VALUE_ARGUMENTS: PositioningStrategy<JetElement> = object : PositioningStrategy<JetElement>() {
        override fun mark(element: JetElement): List<TextRange> {
            return markElement((element as? JetValueArgumentList)?.getRightParenthesis() ?: element)
        }
    }

    public val FUNCTION_PARAMETERS: PositioningStrategy<JetFunction> = object : PositioningStrategy<JetFunction>() {
        override fun mark(element: JetFunction): List<TextRange> {
            val valueParameterList = element.getValueParameterList()
            if (valueParameterList != null) {
                return markElement(valueParameterList)
            }
            if (element is JetFunctionLiteral) {
                return markNode(element.getLBrace().getNode())
            }
            return DECLARATION_SIGNATURE_OR_DEFAULT.mark(element)
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
            return markElement(element)
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
            return markElement(element)
        }
    }

    public val UNREACHABLE_CODE: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun markDiagnostic(diagnostic: ParametrizedDiagnostic<out PsiElement>): List<TextRange> {
            return Errors.UNREACHABLE_CODE.cast(diagnostic).getA()
        }
    }

    public val AS_TYPE: PositioningStrategy<JetBinaryExpressionWithTypeRHS> = object : PositioningStrategy<JetBinaryExpressionWithTypeRHS>() {
        override fun mark(element: JetBinaryExpressionWithTypeRHS): List<TextRange> {
            return markRange(element.getOperationReference(), element)
        }
    }

    public val DEFAULT_OBJECT: PositioningStrategy<JetObjectDeclaration> = object : PositioningStrategy<JetObjectDeclaration>() {
        override fun mark(element: JetObjectDeclaration): List<TextRange> {
            if (element.hasModifier(JetTokens.DEFAULT_KEYWORD)) {
                return modifierSetPosition(JetTokens.DEFAULT_KEYWORD).mark(element)
            }
            return DEFAULT.mark(element)
        }
    }

    public val SECONDARY_CONSTRUCTOR_DELEGATION_CALL: PositioningStrategy<JetConstructorDelegationCall> =
            object : PositioningStrategy<JetConstructorDelegationCall>() {
                override fun mark(element: JetConstructorDelegationCall): List<TextRange> {
                    if (element.getCalleeExpression()?.isEmpty() ?: false) {
                        val constructor = element.getStrictParentOfType<JetSecondaryConstructor>()!!
                        return markElement(constructor.getConstructorKeyword())
                    }
                    return markElement(element.getCalleeExpression() ?: element)
                }
            }

    public val SECONDARY_CONSTRUCTOR_DELEGATION_CALL_OR_DEFAULT: PositioningStrategy<PsiElement> =
            object : PositioningStrategy<PsiElement>() {
                override fun mark(element: PsiElement): List<TextRange> {
                    val parent = element.getParent()
                    if (parent is JetConstructorDelegationCall) {
                        return SECONDARY_CONSTRUCTOR_DELEGATION_CALL.mark(parent)
                    }
                    else {
                        return DEFAULT.mark(element)
                    }
                }
            }
}
