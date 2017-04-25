/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.utils.sure

object PositioningStrategies {
    private open class DeclarationHeader<T : KtDeclaration> : PositioningStrategy<T>() {
        override fun isValid(element: T): Boolean {
            if (element is KtNamedDeclaration &&
                element !is KtObjectDeclaration &&
                element !is KtSecondaryConstructor &&
                element !is KtFunction
            ) {
                if (element.nameIdentifier == null) {
                    return false
                }
            }
            return super.isValid(element)
        }
    }

    @JvmField val DEFAULT: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            when (element) {
                is KtObjectLiteralExpression -> {
                    val objectDeclaration = element.objectDeclaration
                    val objectKeyword = objectDeclaration.getObjectKeyword()!!
                    val delegationSpecifierList = objectDeclaration.getSuperTypeList()
                    if (delegationSpecifierList == null) {
                        return markElement(objectKeyword)
                    }
                    return markRange(objectKeyword, delegationSpecifierList)
                }
                is KtObjectDeclaration -> {
                    return markRange(
                            element.getObjectKeyword()!!,
                            element.nameIdentifier ?: element.getObjectKeyword()!!
                    )
                }
                is KtConstructorDelegationCall -> {
                    return SECONDARY_CONSTRUCTOR_DELEGATION_CALL.mark(element)
                }
                else -> {
                    return super.mark(element)
                }
            }
        }
    }

    @JvmField val DECLARATION_RETURN_TYPE: PositioningStrategy<KtDeclaration> = object : PositioningStrategy<KtDeclaration>() {
        override fun mark(element: KtDeclaration): List<TextRange> {
            return markElement(getElementToMark(element))
        }

        override fun isValid(element: KtDeclaration): Boolean {
            return !hasSyntaxErrors(getElementToMark(element))
        }

        private fun getElementToMark(declaration: KtDeclaration): PsiElement {
            val (returnTypeRef, nameIdentifierOrPlaceholder) = when (declaration) {
                is KtCallableDeclaration -> Pair(declaration.typeReference, declaration.nameIdentifier)
                is KtPropertyAccessor -> Pair(declaration.returnTypeReference, declaration.namePlaceholder)
                else -> Pair(null, null)
            }

            if (returnTypeRef != null) return returnTypeRef
            if (nameIdentifierOrPlaceholder != null) return nameIdentifierOrPlaceholder
            return declaration
        }
    }

    @JvmField val DECLARATION_NAME: PositioningStrategy<KtNamedDeclaration> = object : DeclarationHeader<KtNamedDeclaration>() {
        override fun mark(element: KtNamedDeclaration): List<TextRange> {
            val nameIdentifier = element.nameIdentifier
            if (nameIdentifier != null) {
                if (element is KtClassOrObject) {
                    val startElement =
                            element.getModifierList()?.getModifier(KtTokens.ENUM_KEYWORD)
                            ?: element.node.findChildByType(TokenSet.create(KtTokens.CLASS_KEYWORD, KtTokens.OBJECT_KEYWORD))?.psi
                            ?: element

                    return markRange(startElement, nameIdentifier)
                }
                return markElement(nameIdentifier)
            }
            if (element is KtNamedFunction) {
                return DECLARATION_SIGNATURE.mark(element)
            }
            return DEFAULT.mark(element)
        }
    }

    @JvmField val DECLARATION_SIGNATURE: PositioningStrategy<KtDeclaration> = object : DeclarationHeader<KtDeclaration>() {
        override fun mark(element: KtDeclaration): List<TextRange> {
            when (element) {
                is KtConstructor<*> -> {
                    val begin = element.getConstructorKeyword() ?: element.getValueParameterList() ?: return markElement(element)
                    val end = element.getValueParameterList() ?: element.getConstructorKeyword() ?: return markElement(element)
                    return markRange(begin, end)
                }
                is KtFunction -> {
                    val endOfSignatureElement =
                            element.typeReference
                            ?: element.valueParameterList
                            ?: element.nameIdentifier
                            ?: element
                    val startElement
                            = if (element is KtFunctionLiteral) {
                                element.getReceiverTypeReference()
                                ?: element.getValueParameterList()
                                ?: element
                            }
                            else element
                    return markRange(startElement, endOfSignatureElement)
                }
                is KtProperty -> {
                    val endOfSignatureElement = element.typeReference ?: element.nameIdentifier ?: element
                    return markRange(element, endOfSignatureElement)
                }
                is KtPropertyAccessor -> {
                    val endOfSignatureElement =
                            element.returnTypeReference
                            ?: element.rightParenthesis?.psi
                            ?: element.namePlaceholder

                    return markRange(element, endOfSignatureElement)
                }
                is KtClass -> {
                    val nameAsDeclaration = element.nameIdentifier ?: return markElement(element)
                    val primaryConstructorParameterList = element.getPrimaryConstructorParameterList() ?: return markElement(nameAsDeclaration)
                    return markRange(nameAsDeclaration, primaryConstructorParameterList)
                }
                is KtObjectDeclaration -> {
                    return DECLARATION_NAME.mark(element)
                }
                is KtClassInitializer -> {
                    return markRange(element.initKeyword.textRange)
                }
            }
            return super.mark(element)
        }
    }

    @JvmField val DECLARATION_SIGNATURE_OR_DEFAULT: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            return if (element is KtDeclaration)
                DECLARATION_SIGNATURE.mark(element)
            else
                DEFAULT.mark(element)
        }

        override fun isValid(element: PsiElement): Boolean {
            return if (element is KtDeclaration)
                DECLARATION_SIGNATURE.isValid(element)
            else
                DEFAULT.isValid(element)
        }
    }

    @JvmField val TYPE_PARAMETERS_OR_DECLARATION_SIGNATURE: PositioningStrategy<KtDeclaration> = object : PositioningStrategy<KtDeclaration>() {
        override fun mark(element: KtDeclaration): List<TextRange> {
            if (element is KtTypeParameterListOwner) {
                val jetTypeParameterList = element.typeParameterList
                if (jetTypeParameterList != null) {
                    return markElement(jetTypeParameterList)
                }
            }
            return DECLARATION_SIGNATURE.mark(element)
        }
    }

    @JvmField val ABSTRACT_MODIFIER: PositioningStrategy<KtModifierListOwner> = modifierSetPosition(KtTokens.ABSTRACT_KEYWORD)

    @JvmField val OPEN_MODIFIER: PositioningStrategy<KtModifierListOwner> = modifierSetPosition(KtTokens.OPEN_KEYWORD)

    @JvmField val OVERRIDE_MODIFIER: PositioningStrategy<KtModifierListOwner> = modifierSetPosition(KtTokens.OVERRIDE_KEYWORD)

    @JvmField val PRIVATE_MODIFIER: PositioningStrategy<KtModifierListOwner> = modifierSetPosition(KtTokens.PRIVATE_KEYWORD)

    @JvmField val LATEINIT_MODIFIER: PositioningStrategy<KtModifierListOwner> = modifierSetPosition(KtTokens.LATEINIT_KEYWORD)

    @JvmField val VARIANCE_MODIFIER: PositioningStrategy<KtModifierListOwner> = modifierSetPosition(KtTokens.IN_KEYWORD, KtTokens.OUT_KEYWORD)

    @JvmField val FOR_REDECLARATION: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            val nameIdentifier = when (element) {
                is KtNamedDeclaration -> element.nameIdentifier
                is KtFile -> element.packageDirective!!.nameIdentifier
                else -> null
            }

            if (nameIdentifier == null && element is KtObjectDeclaration) return DEFAULT.mark(element)

            return markElement(nameIdentifier ?: element)
        }
    }
    @JvmField val FOR_UNRESOLVED_REFERENCE: PositioningStrategy<KtReferenceExpression> = object : PositioningStrategy<KtReferenceExpression>() {
        override fun mark(element: KtReferenceExpression): List<TextRange> {
            if (element is KtArrayAccessExpression) {
                val ranges = element.bracketRanges
                if (!ranges.isEmpty()) {
                    return ranges
                }
            }
            return listOf(element.textRange)
        }
    }

    @JvmStatic fun modifierSetPosition(vararg tokens: KtModifierKeywordToken): PositioningStrategy<KtModifierListOwner> {
        return object : PositioningStrategy<KtModifierListOwner>() {
            override fun mark(element: KtModifierListOwner): List<TextRange> {
                val modifierList = element.modifierList.sure { "No modifier list, but modifier has been found by the analyzer" }

                for (token in tokens) {
                    val modifier = modifierList.getModifier(token)
                    if (modifier != null) {
                        return markElement(modifier)
                    }
                }
                throw IllegalStateException("None of the modifiers is found: " + listOf(*tokens))
            }
        }
    }

    @JvmField val ARRAY_ACCESS: PositioningStrategy<KtArrayAccessExpression> = object : PositioningStrategy<KtArrayAccessExpression>() {
        override fun mark(element: KtArrayAccessExpression): List<TextRange> {
            return markElement(element.indicesNode)
        }
    }

    @JvmField val VISIBILITY_MODIFIER: PositioningStrategy<KtModifierListOwner> = object : PositioningStrategy<KtModifierListOwner>() {
        override fun mark(element: KtModifierListOwner): List<TextRange> {
            val visibilityTokens = listOf(KtTokens.PRIVATE_KEYWORD, KtTokens.PROTECTED_KEYWORD, KtTokens.PUBLIC_KEYWORD, KtTokens.INTERNAL_KEYWORD)
            val modifierList = element.modifierList

            val result = visibilityTokens.mapNotNull { modifierList?.getModifier(it)?.textRange }
            if (!result.isEmpty()) return result

            // Try to resolve situation when there's no visibility modifiers written before element
            if (element is PsiNameIdentifierOwner) {
                val nameIdentifier = element.nameIdentifier
                if (nameIdentifier != null) {
                    return markElement(nameIdentifier)
                }
            }

            val elementToMark = when (element) {
                is KtObjectDeclaration -> element.getObjectKeyword()!!
                is KtPropertyAccessor -> element.namePlaceholder
                is KtAnonymousInitializer -> element
                else -> throw IllegalArgumentException(
                        "Can't find text range for element '${element::class.java.canonicalName}' with the text '${element.text}'")
            }
            return markElement(elementToMark)
        }
    }

    @JvmField val VARIANCE_IN_PROJECTION: PositioningStrategy<KtTypeProjection> = object : PositioningStrategy<KtTypeProjection>() {
        override fun mark(element: KtTypeProjection): List<TextRange> {
            return markElement(element.projectionToken!!)
        }
    }

    @JvmField val PARAMETER_DEFAULT_VALUE: PositioningStrategy<KtParameter> = object : PositioningStrategy<KtParameter>() {
        override fun mark(element: KtParameter): List<TextRange> {
            return markNode(element.defaultValue!!.node)
        }
    }

    @JvmField val PARAMETER_VARARG_MODIFIER: PositioningStrategy<KtParameter> = object : PositioningStrategy<KtParameter>() {
        override fun mark(element: KtParameter): List<TextRange> {
            val varargModifier = element.modifierList!!.getModifier(KtTokens.VARARG_KEYWORD)!!
            return markNode(varargModifier.node)
        }
    }

    @JvmField val CALL_ELEMENT: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            return markElement((element as? KtCallElement)?.calleeExpression ?: element)
        }
    }

    @JvmField val DECLARATION_WITH_BODY: PositioningStrategy<KtDeclarationWithBody> = object : PositioningStrategy<KtDeclarationWithBody>() {
        override fun mark(element: KtDeclarationWithBody): List<TextRange> {
            val lastBracketRange = (element.bodyExpression as? KtBlockExpression)?.lastBracketRange
            return if (lastBracketRange != null)
                markRange(lastBracketRange)
            else
                markElement(element)
        }

        override fun isValid(element: KtDeclarationWithBody): Boolean {
            return super.isValid(element) && (element.bodyExpression as? KtBlockExpression)?.lastBracketRange != null
        }
    }

    @JvmField val VAL_OR_VAR_NODE: PositioningStrategy<KtNamedDeclaration> = object : PositioningStrategy<KtNamedDeclaration>() {
        override fun mark(element: KtNamedDeclaration): List<TextRange> {
            return when (element) {
                is KtParameter -> markElement(element.valOrVarKeyword ?: element)
                is KtProperty -> markElement(element.valOrVarKeyword)
                else -> error("Declaration is neither a parameter nor a property: " + element.getElementTextWithContext())
            }
        }
    }

    @JvmField val ELSE_ENTRY: PositioningStrategy<KtWhenEntry> = object : PositioningStrategy<KtWhenEntry>() {
        override fun mark(element: KtWhenEntry): List<TextRange> {
            return markElement(element.elseKeyword!!)
        }
    }

    @JvmField val WHEN_EXPRESSION: PositioningStrategy<KtWhenExpression> = object : PositioningStrategy<KtWhenExpression>() {
        override fun mark(element: KtWhenExpression): List<TextRange> {
            return markElement(element.whenKeyword)
        }
    }

    @JvmField val WHEN_CONDITION_IN_RANGE: PositioningStrategy<KtWhenConditionInRange> = object : PositioningStrategy<KtWhenConditionInRange>() {
        override fun mark(element: KtWhenConditionInRange): List<TextRange> {
            return markElement(element.operationReference)
        }
    }

    @JvmField val SPECIAL_CONSTRUCT_TOKEN: PositioningStrategy<KtExpression> = object : PositioningStrategy<KtExpression>() {
        override fun mark(element: KtExpression): List<TextRange> =
                when (element) {
                    is KtWhenExpression -> markElement(element.whenKeyword)
                    is KtIfExpression -> markElement(element.ifKeyword)
                    is KtOperationExpression -> markElement(element.operationReference)
                    else -> error("Expression is not an if, when or operation expression: ${element.getElementTextWithContext()}")
                }
    }

    @JvmField val NULLABLE_TYPE: PositioningStrategy<KtNullableType> = object : PositioningStrategy<KtNullableType>() {
        override fun mark(element: KtNullableType): List<TextRange> {
            return markNode(element.questionMarkNode)
        }
    }

    @JvmField val CALL_EXPRESSION: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            if (element is KtCallExpression) {
                return markRange(element, element.typeArgumentList ?: element.calleeExpression ?: element)
            }
            return markElement(element)
        }
    }

    @JvmField val VALUE_ARGUMENTS: PositioningStrategy<KtElement> = object : PositioningStrategy<KtElement>() {
        override fun mark(element: KtElement): List<TextRange> {
            return markElement((element as? KtValueArgumentList)?.rightParenthesis ?: element)
        }
    }

    @JvmField val FUNCTION_PARAMETERS: PositioningStrategy<KtFunction> = object : PositioningStrategy<KtFunction>() {
        override fun mark(element: KtFunction): List<TextRange> {
            val valueParameterList = element.valueParameterList
            if (valueParameterList != null) {
                return markElement(valueParameterList)
            }
            if (element is KtFunctionLiteral) {
                return markNode(element.lBrace.node)
            }
            return DECLARATION_SIGNATURE_OR_DEFAULT.mark(element)
        }
    }

    @JvmField val CUT_CHAR_QUOTES: PositioningStrategy<KtElement> = object : PositioningStrategy<KtElement>() {
        override fun mark(element: KtElement): List<TextRange> {
            if (element is KtConstantExpression) {
                if (element.node.elementType == KtNodeTypes.CHARACTER_CONSTANT) {
                    val elementTextRange = element.getTextRange()
                    return listOf(TextRange.create(elementTextRange.startOffset + 1, elementTextRange.endOffset - 1))
                }
            }
            return markElement(element)
        }
    }

    @JvmField val LONG_LITERAL_SUFFIX: PositioningStrategy<KtElement> = object : PositioningStrategy<KtElement>() {
        override fun mark(element: KtElement): List<TextRange> {
            if (element is KtConstantExpression) {
                if (element.node.elementType == KtNodeTypes.INTEGER_CONSTANT) {
                    val endOffset = element.endOffset
                    return listOf(TextRange.create(endOffset - 1, endOffset))
                }
            }
            return markElement(element)
        }
    }

    @JvmField val UNREACHABLE_CODE: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun markDiagnostic(diagnostic: ParametrizedDiagnostic<out PsiElement>): List<TextRange> {
            return Errors.UNREACHABLE_CODE.cast(diagnostic).a
        }
    }

    @JvmField val AS_TYPE: PositioningStrategy<KtBinaryExpressionWithTypeRHS> = object : PositioningStrategy<KtBinaryExpressionWithTypeRHS>() {
        override fun mark(element: KtBinaryExpressionWithTypeRHS): List<TextRange> {
            return markRange(element.operationReference, element)
        }
    }

    @JvmField val COMPANION_OBJECT: PositioningStrategy<KtObjectDeclaration> = object : PositioningStrategy<KtObjectDeclaration>() {
        override fun mark(element: KtObjectDeclaration): List<TextRange> {
            if (element.hasModifier(KtTokens.COMPANION_KEYWORD)) {
                return modifierSetPosition(KtTokens.COMPANION_KEYWORD).mark(element)
            }
            return DEFAULT.mark(element)
        }
    }

    @JvmField val SECONDARY_CONSTRUCTOR_DELEGATION_CALL: PositioningStrategy<KtConstructorDelegationCall> =
            object : PositioningStrategy<KtConstructorDelegationCall>() {
                override fun mark(element: KtConstructorDelegationCall): List<TextRange> {
                    if (element.isImplicit) {
                        val constructor = element.getStrictParentOfType<KtSecondaryConstructor>()!!
                        val valueParameterList = constructor.valueParameterList ?: return markElement(constructor)
                        return markRange(constructor.getConstructorKeyword(), valueParameterList.lastChild)
                    }
                    return markElement(element.calleeExpression ?: element)
                }
            }

    @JvmField val DELEGATOR_SUPER_CALL: PositioningStrategy<KtEnumEntry> = object: PositioningStrategy<KtEnumEntry>() {
        override fun mark(element: KtEnumEntry): List<TextRange> {
            val specifiers = element.superTypeListEntries
            return markElement(if (specifiers.isEmpty()) element else specifiers[0])
        }
    }

    @JvmField val UNUSED_VALUE: PositioningStrategy<KtBinaryExpression> = object: PositioningStrategy<KtBinaryExpression>() {
        override fun mark(element: KtBinaryExpression): List<TextRange> {
            return listOf(TextRange(element.left!!.startOffset, element.operationReference.endOffset))
        }
    }

    @JvmField val USELESS_ELVIS: PositioningStrategy<KtBinaryExpression> = object: PositioningStrategy<KtBinaryExpression>() {
        override fun mark(element: KtBinaryExpression): List<TextRange> {
            return listOf(TextRange(element.operationReference.startOffset, element.endOffset))
        }
    }

    @JvmField val IMPORT_ALIAS: PositioningStrategy<KtImportDirective> = object: PositioningStrategy<KtImportDirective>() {
        override fun mark(element: KtImportDirective): List<TextRange> {
            element.alias?.nameIdentifier?.let { return markElement(it) }
            element.importedReference?.let {
                if (it is KtQualifiedExpression) {
                    it.selectorExpression?.let { return markElement(it) }
                }
                return markElement(it)
            }
            return markElement(element)
        }
    }

    @JvmField val RETURN_WITH_LABEL: PositioningStrategy<KtReturnExpression> = object: PositioningStrategy<KtReturnExpression>() {
        override fun mark(element: KtReturnExpression): List<TextRange> {
            val labeledExpression = element.labeledExpression
            if (labeledExpression != null) {
                return markRange(element, labeledExpression)
            }

            return markElement(element.returnKeyword)
        }
    }

    @JvmField val RECEIVER: PositioningStrategy<KtCallableDeclaration> = object : DeclarationHeader<KtCallableDeclaration>() {
        override fun mark(element: KtCallableDeclaration): List<TextRange> {
            element.receiverTypeReference?.let { return markElement(it) }
            return DEFAULT.mark(element)
        }
    }
}
