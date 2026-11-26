/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.descendants
import com.intellij.psi.util.elementType
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.cfg.UnreachableCode
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.kotlin.utils.sure

@Suppress("UNUSED") // Most strategies are only accessed via `kindToStrategy`, where they are registered by `registerAs`
object PsiPositioningStrategies : PositioningStrategyRegistry<PsiPositioningStrategy<*>>() {
    open class DeclarationHeader<T : PsiElement>(private val checkValidity: Boolean) : PsiPositioningStrategy<T>() {
        @OptIn(DiagnosticLossRisk::class)
        override fun isValid(element: KtSourceElement): Boolean {
            val psi = element.unwrapPsi()
            if (checkValidity &&
                psi is KtNamedDeclaration &&
                psi !is KtObjectDeclaration &&
                psi !is KtSecondaryConstructor &&
                psi !is KtFunction
            ) {
                if (psi.nameIdentifier == null) {
                    return false
                }
            }
            return super.isValid(element)
        }
    }

    @JvmField
    val DEFAULT: PsiPositioningStrategy<PsiElement> = object : PsiPositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            when (element) {
                is KtObjectLiteralExpression -> {
                    val objectDeclaration = element.objectDeclaration
                    val objectKeyword = objectDeclaration.getObjectKeyword()!!
                    val delegationSpecifierList = objectDeclaration.getSuperTypeList() ?: return markElement(objectKeyword)
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
    }.registerAs(PositioningStrategyKinds.DEFAULT)

    val SYNTAX_ERROR: PsiPositioningStrategy<PsiElement> = object : PsiPositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            return DEFAULT.mark(element)
        }

        @DiagnosticLossRisk
        override fun isValid(element: KtSourceElement): Boolean {
            if (element.unwrapPsi() is PsiErrorElement) return true
            return DEFAULT.isValid(element)
        }
    }.registerAs(PositioningStrategyKinds.SYNTAX_ERROR)

    @JvmField
    val SUPERTYPES_LIST: PsiPositioningStrategy<PsiElement> = object : PsiPositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            val supertypes = ((
                    element as? KtClassOrObject
                    ) ?: return markElement(element)
                    ).superTypeListEntries
            return if (supertypes.isEmpty())
                markElement(element)
            else
                markRange(supertypes[0], supertypes.last())
        }
    }.registerAs(PositioningStrategyKinds.SUPERTYPES_LIST)

    @JvmField
    val DECLARATION_RETURN_TYPE: PsiPositioningStrategy<KtDeclaration> = object : PsiPositioningStrategy<KtDeclaration>() {
        override fun mark(element: KtDeclaration): List<TextRange> {
            return markElement(getElementToMark(element))
        }

        @OptIn(DiagnosticLossRisk::class)
        override fun isValid(element: KtSourceElement): Boolean {
            return !hasSyntaxErrors(getElementToMark(element.unwrapPsi() as KtDeclaration))
        }

        private fun getElementToMark(declaration: KtDeclaration): PsiElement {
            val [returnTypeRef, nameIdentifierOrPlaceholder] = when (declaration) {
                is KtCallableDeclaration -> Pair(declaration.typeReference, declaration.nameIdentifier)
                is KtPropertyAccessor -> Pair(declaration.typeReference, declaration.namePlaceholder)
                else -> Pair(null, null)
            }

            if (returnTypeRef != null) return returnTypeRef
            if (nameIdentifierOrPlaceholder != null) return nameIdentifierOrPlaceholder
            return declaration
        }
    }.registerAs(PositioningStrategyKinds.DECLARATION_RETURN_TYPE)

    @JvmField
    val DECLARATION_START_TO_NAME: PsiPositioningStrategy<KtDeclaration> = object : PsiPositioningStrategy<KtDeclaration>() {

        private fun PsiElement.firstNonCommentNonAnnotationLeaf(): PsiElement? {
            var child: PsiElement? = firstChild ?: return this // this is leaf
            while (true) {
                if (child is PsiComment || child is PsiWhiteSpace || child is KtAnnotationEntry) {
                    child = child.nextSibling
                    continue
                }
                if (child == null) return null // no children of accepted type in this
                val leaf = child.firstNonCommentNonAnnotationLeaf()
                if (leaf == null) {
                    child = child.nextSibling
                    continue
                }
                return leaf
            }
        }

        override fun mark(element: KtDeclaration): List<TextRange> {
            val startElement = element.firstNonCommentNonAnnotationLeaf() ?: element
            val nameIdentifier = (element as? KtNamedDeclaration)?.nameIdentifier
            return if (nameIdentifier != null) {
                markRange(startElement, nameIdentifier)
            } else when (element) {
                // companion object/constructors without name
                is KtConstructor<*> -> {
                    markRange(startElement, element.getConstructorKeyword() ?: element)
                }
                is KtObjectDeclaration -> {
                    markRange(startElement, element.getObjectKeyword() ?: element)
                }
                else -> DEFAULT.mark(element)
            }
        }
    }.registerAs(PositioningStrategyKinds.DECLARATION_START_TO_NAME)

    @JvmField
    val CONTEXT_KEYWORD: PsiPositioningStrategy<PsiElement> = object : PsiPositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> =
            element.descendants().firstIsInstanceOrNull<KtContextParameterList>()?.firstChild?.textRange?.let(::markRange)
                ?: DEFAULT.mark(element)
    }.registerAs(PositioningStrategyKinds.CONTEXT_KEYWORD)

    private fun findStartingPsiElementForDeclarationName(
        element: KtNamedDeclaration,
    ): PsiElement =
        element.getModifierList()?.getModifier(ENUM_KEYWORD)
            ?: element.node.findChildByType(TokenSet.create(KtTokens.CLASS_KEYWORD, KtTokens.OBJECT_KEYWORD))?.psi
            ?: element

    @JvmField
    val DECLARATION_NAME_WITH_VALIDITY_CHECK: PsiPositioningStrategy<KtDeclaration> = DeclarationName(true)

    val DECLARATION_NAME: PsiPositioningStrategy<PsiElement> = DeclarationName(false)
        .registerAs(PositioningStrategyKinds.DECLARATION_NAME)

    private class DeclarationName(checkValidity: Boolean) : DeclarationHeader<PsiElement>(checkValidity) {
        override fun mark(element: PsiElement): List<TextRange> {
            if (element is KtNamedDeclaration) {
                val nameIdentifier = element.nameIdentifier
                if (nameIdentifier != null) {
                    if (element is KtClassOrObject) {
                        val startElement = findStartingPsiElementForDeclarationName(element)
                        return markRange(startElement, nameIdentifier)
                    }
                    return markElement(nameIdentifier)
                }
                if (element is KtNamedFunction) {
                    return DECLARATION_SIGNATURE_WITH_VALIDITY_CHECK.mark(element)
                }
            } else if (element is KtPropertyAccessor) {
                return markElement(element.namePlaceholder)
            }
            return DEFAULT.mark(element)
        }
    }

    @JvmField
    val DECLARATION_NAME_ONLY: PsiPositioningStrategy<KtNamedDeclaration> = object : PsiPositioningStrategy<KtNamedDeclaration>() {
        override fun mark(element: KtNamedDeclaration): List<TextRange> {
            val nameIdentifier = element.nameIdentifier
            if (nameIdentifier != null) {
                return markElement(nameIdentifier)
            }
            if (element is KtNamedFunction) {
                return DECLARATION_SIGNATURE_WITH_VALIDITY_CHECK.mark(element)
            }
            return DEFAULT.mark(element)
        }
    }.registerAs(PositioningStrategyKinds.DECLARATION_NAME_ONLY)

    @JvmField
    val DECLARATION_SIGNATURE_WITH_VALIDITY_CHECK: PsiPositioningStrategy<KtDeclaration> = DeclarationSignature(true)

    val DECLARATION_SIGNATURE: PsiPositioningStrategy<KtDeclaration> = DeclarationSignature(false)
        .registerAs(PositioningStrategyKinds.DECLARATION_SIGNATURE)

    private class DeclarationSignature(checkValidity: Boolean) : DeclarationHeader<KtDeclaration>(checkValidity) {
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
                    val startElement = if (element is KtFunctionLiteral) {
                        element.getReceiverTypeReference()
                            ?: element.getValueParameterList()
                            ?: element
                    } else element
                    return markRange(startElement, endOfSignatureElement)
                }
                is KtProperty -> {
                    val endOfSignatureElement = element.typeReference ?: element.nameIdentifier ?: element
                    return markRange(element, endOfSignatureElement)
                }
                is KtPropertyAccessor -> {
                    val endOfSignatureElement =
                        element.typeReference
                            ?: element.parameterList
                            ?: element.namePlaceholder

                    return markRange(element, endOfSignatureElement)
                }
                is KtClass -> {
                    val nameAsDeclaration = element.nameIdentifier ?: return markElement(element)
                    val primaryConstructorParameterList =
                        element.getPrimaryConstructorParameterList() ?: return markElement(nameAsDeclaration)
                    return markRange(nameAsDeclaration, primaryConstructorParameterList)
                }
                is KtObjectDeclaration -> {
                    return DECLARATION_NAME_WITH_VALIDITY_CHECK.mark(element)
                }
                is KtClassInitializer -> {
                    return markRange(element.initKeyword.textRange)
                }
            }
            return super.mark(element)
        }
    }

    @JvmField
    val CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS: PsiPositioningStrategy<PsiElement> = object : PsiPositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            when (element) {
                is KtNamedFunction -> {
                    val startElement = element.funKeyword ?: element
                    val endOfSignatureElement =
                        element.typeReference
                            ?: element.valueParameterList
                            ?: element.nameIdentifier
                            ?: element
                    return markRange(startElement, endOfSignatureElement)
                }
                is KtProperty -> {
                    val endOfSignatureElement = element.typeReference ?: element.nameIdentifier ?: element
                    return markRange(element.valOrVarKeyword, endOfSignatureElement)
                }
                is KtPropertyAccessor -> {
                    val endOfSignatureElement =
                        element.typeReference
                            ?: element.parameterList
                            ?: element.namePlaceholder

                    return markRange(element.namePlaceholder, endOfSignatureElement)
                }
            }
            return if (element is KtDeclaration) {
                DECLARATION_SIGNATURE_WITH_VALIDITY_CHECK.mark(element)
            } else {
                DEFAULT.mark(element)
            }
        }
    }.registerAs(PositioningStrategyKinds.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS)

    val DECLARATION_SIGNATURE_OR_DEFAULT: PsiPositioningStrategy<PsiElement> = object : PsiPositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            return if (element is KtDeclaration)
                DECLARATION_SIGNATURE.mark(element)
            else
                DEFAULT.mark(element)
        }
    }.registerAs(PositioningStrategyKinds.DECLARATION_SIGNATURE_OR_DEFAULT)

    @JvmField
    val NOT_SUPPORTED_IN_INLINE_MOST_RELEVANT: PsiPositioningStrategy<KtDeclaration> = object : PsiPositioningStrategy<KtDeclaration>() {
        override fun mark(element: KtDeclaration): List<TextRange> =
            markElement(
                when (element) {
                    is KtClassOrObject ->
                        element.getDeclarationKeyword() ?: element.nameIdentifier ?: element

                    is KtNamedFunction ->
                        element.modifierList?.getModifier(KtTokens.INLINE_KEYWORD) ?: element.funKeyword ?: element

                    else -> element
                }
            )
    }.registerAs(PositioningStrategyKinds.NOT_SUPPORTED_IN_INLINE_MOST_RELEVANT)

    @JvmField
    val ABSTRACT_MODIFIER: PsiPositioningStrategy<KtModifierListOwner> =
        ModifierSetBasedPositioningStrategy(KtTokens.ABSTRACT_KEYWORD)
            .registerAs(PositioningStrategyKinds.ABSTRACT_MODIFIER)

    @JvmField
    val OPEN_MODIFIER: PsiPositioningStrategy<KtModifierListOwner> =
        ModifierSetBasedPositioningStrategy(KtTokens.OPEN_KEYWORD)
            .registerAs(PositioningStrategyKinds.OPEN_MODIFIER)

    @JvmField
    val OVERRIDE_MODIFIER: PsiPositioningStrategy<KtModifierListOwner> =
        ModifierSetBasedPositioningStrategy(KtTokens.OVERRIDE_KEYWORD)
            .registerAs(PositioningStrategyKinds.OVERRIDE_MODIFIER)

    @JvmField
    val PRIVATE_MODIFIER: PsiPositioningStrategy<KtModifierListOwner> =
        ModifierSetBasedPositioningStrategy(KtTokens.PRIVATE_KEYWORD)
            .registerAs(PositioningStrategyKinds.PRIVATE_MODIFIER)

    @JvmField
    val LATEINIT_MODIFIER: PsiPositioningStrategy<KtModifierListOwner> =
        ModifierSetBasedPositioningStrategy(KtTokens.LATEINIT_KEYWORD)
            .registerAs(PositioningStrategyKinds.LATEINIT_MODIFIER)

    @JvmField
    val VARIANCE_MODIFIER: PsiPositioningStrategy<KtModifierListOwner> = projectionPosition()
        .registerAs(PositioningStrategyKinds.VARIANCE_MODIFIER)

    @JvmField
    val CONST_MODIFIER: PsiPositioningStrategy<KtModifierListOwner> =
        ModifierSetBasedPositioningStrategy(KtTokens.CONST_KEYWORD)
            .registerAs(PositioningStrategyKinds.CONST_MODIFIER)

    @JvmField
    val FUN_MODIFIER: PsiPositioningStrategy<KtModifierListOwner> =
        ModifierSetBasedPositioningStrategy(KtTokens.FUN_KEYWORD)
            .registerAs(PositioningStrategyKinds.FUN_MODIFIER)

    @JvmField
    val SUSPEND_MODIFIER: PsiPositioningStrategy<KtModifierListOwner> =
        ModifierSetBasedPositioningStrategy(KtTokens.SUSPEND_KEYWORD)
            .registerAs(PositioningStrategyKinds.SUSPEND_MODIFIER)

    @JvmField
    val DATA_MODIFIER: PsiPositioningStrategy<KtModifierListOwner> =
        ModifierSetBasedPositioningStrategy(KtTokens.DATA_KEYWORD)
            .registerAs(PositioningStrategyKinds.DATA_MODIFIER)

    @JvmField
    val OPERATOR_MODIFIER: PsiPositioningStrategy<KtModifierListOwner> =
        ModifierSetBasedPositioningStrategy(KtTokens.OPERATOR_KEYWORD)
            .registerAs(PositioningStrategyKinds.OPERATOR_MODIFIER)

    @JvmField
    val INFIX_MODIFIER: PsiPositioningStrategy<KtModifierListOwner> =
        ModifierSetBasedPositioningStrategy(KtTokens.INFIX_KEYWORD)
            .registerAs(PositioningStrategyKinds.INFIX_MODIFIER)

    @JvmField
    val ENUM_MODIFIER: PsiPositioningStrategy<KtModifierListOwner> =
        ModifierSetBasedPositioningStrategy(KtTokens.ENUM_KEYWORD)
            .registerAs(PositioningStrategyKinds.ENUM_MODIFIER)

    @JvmField
    val TAILREC_MODIFIER: PsiPositioningStrategy<KtModifierListOwner> =
        ModifierSetBasedPositioningStrategy(KtTokens.TAILREC_KEYWORD)
            .registerAs(PositioningStrategyKinds.TAILREC_MODIFIER)

    @JvmField
    val EXTERNAL_MODIFIER: PsiPositioningStrategy<KtModifierListOwner> =
        ModifierSetBasedPositioningStrategy(KtTokens.EXTERNAL_KEYWORD)
            .registerAs(PositioningStrategyKinds.EXTERNAL_MODIFIER)

    @JvmField
    val EXPECT_ACTUAL_MODIFIER: PsiPositioningStrategy<KtModifierListOwner> =
        ModifierSetBasedPositioningStrategy(KtTokens.EXPECT_KEYWORD, KtTokens.ACTUAL_KEYWORD)
            .registerAs(PositioningStrategyKinds.EXPECT_ACTUAL_MODIFIER)

    @JvmField
    val OBJECT_KEYWORD: PsiPositioningStrategy<KtObjectDeclaration> = object : PsiPositioningStrategy<KtObjectDeclaration>() {
        override fun mark(element: KtObjectDeclaration): List<TextRange> {
            return markElement(element.getObjectKeyword() ?: element)
        }
    }.registerAs(PositioningStrategyKinds.OBJECT_KEYWORD)

    @JvmField
    val FIELD_KEYWORD: PsiPositioningStrategy<KtBackingField> = object : PsiPositioningStrategy<KtBackingField>() {
        override fun mark(element: KtBackingField): List<TextRange> {
            return markElement(element.fieldKeyword!!)
        }
    }.registerAs(PositioningStrategyKinds.FIELD_KEYWORD)

    @JvmField
    val PROPERTY_DELEGATE: PsiPositioningStrategy<KtProperty> = object : PsiPositioningStrategy<KtProperty>() {
        override fun mark(element: KtProperty): List<TextRange> {
            val delegate = element.delegate
            return if (delegate != null) {
                markElement(delegate)
            } else {
                DEFAULT.mark(element)
            }
        }
    }.registerAs(PositioningStrategyKinds.PROPERTY_DELEGATE)

    @JvmStatic
    fun projectionPosition(): PsiPositioningStrategy<KtModifierListOwner> {
        return object : PsiPositioningStrategy<KtModifierListOwner>() {
            override fun mark(element: KtModifierListOwner): List<TextRange> {
                if (element is KtTypeProjection && element.projectionKind == KtProjectionKind.STAR) {
                    return markElement(element)
                }

                val modifierList = element.modifierList.sure { "No modifier list, but modifier has been found by the analyzer" }
                modifierList.getModifier(KtTokens.IN_KEYWORD)?.let { return markElement(it) }
                modifierList.getModifier(KtTokens.OUT_KEYWORD)?.let { return markElement(it) }

                throw IllegalStateException("None of the modifiers is found: in, out")
            }
        }
    }

    @JvmField
    val ARRAY_ACCESS: PsiPositioningStrategy<KtArrayAccessExpression> = object : PsiPositioningStrategy<KtArrayAccessExpression>() {
        override fun mark(element: KtArrayAccessExpression): List<TextRange> {
            return markElement(element.indicesNode)
        }
    }.registerAs(PositioningStrategyKinds.ARRAY_ACCESS)

    @JvmField
    val SAFE_ACCESS: PsiPositioningStrategy<PsiElement> = object : PsiPositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            return markElement(element.node.findChildByType(ALL_SAFE_ACCESSES)?.psi ?: element)
        }
    }.registerAs(PositioningStrategyKinds.SAFE_ACCESS)

    private open class ModifierSetBasedPositioningStrategy(private val modifierSet: TokenSet) : PsiPositioningStrategy<PsiElement>() {
        constructor(vararg tokens: IElementType) : this(TokenSet.create(*tokens))

        protected fun markModifier(element: KtModifierListOwner?): List<TextRange>? =
            modifierSet.types.mapNotNull {
                element?.modifierList?.getModifier(it as KtModifierKeywordToken)?.textRange
            }.takeIf { it.isNotEmpty() }

        override fun mark(element: PsiElement): List<TextRange> {
            if (element is KtModifierListOwner) {
                val result = markModifier(element)
                if (result != null) return result
            }

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
                else -> element
            }
            return markElement(elementToMark)
        }
    }

    private class InlineFunPositioningStrategy : ModifierSetBasedPositioningStrategy(KtTokens.INLINE_KEYWORD) {
        override fun mark(element: PsiElement): List<TextRange> {
            if (element is KtProperty) {
                return markModifier(element.getter) ?: markModifier(element.setter) ?: super.mark(element)
            }
            return super.mark(element)
        }
    }

    @JvmField
    val VISIBILITY_MODIFIER: PsiPositioningStrategy<KtModifierListOwner> = ModifierSetBasedPositioningStrategy(VISIBILITY_MODIFIERS)
        .registerAs(PositioningStrategyKinds.VISIBILITY_MODIFIER)

    @JvmField
    val MODALITY_MODIFIER: PsiPositioningStrategy<KtModifierListOwner> = ModifierSetBasedPositioningStrategy(MODALITY_MODIFIERS)
        .registerAs(PositioningStrategyKinds.MODALITY_MODIFIER)

    @JvmField
    val INLINE_OR_VALUE_MODIFIER: PsiPositioningStrategy<KtModifierListOwner> =
        ModifierSetBasedPositioningStrategy(KtTokens.INLINE_KEYWORD, KtTokens.VALUE_KEYWORD)
            .registerAs(PositioningStrategyKinds.INLINE_OR_VALUE_MODIFIER)

    @JvmField
    val VALUE_MODIFIER: PsiPositioningStrategy<KtModifierListOwner> =
        ModifierSetBasedPositioningStrategy(KtTokens.VALUE_KEYWORD)
            .registerAs(PositioningStrategyKinds.VALUE_MODIFIER)

    @JvmField
    val INNER_MODIFIER: PsiPositioningStrategy<KtModifierListOwner> =
        ModifierSetBasedPositioningStrategy(KtTokens.INNER_KEYWORD)
            .registerAs(PositioningStrategyKinds.INNER_MODIFIER)

    @JvmField
    val INLINE_PARAMETER_MODIFIER: PsiPositioningStrategy<KtModifierListOwner> =
        ModifierSetBasedPositioningStrategy(KtTokens.NOINLINE_KEYWORD, KtTokens.CROSSINLINE_KEYWORD)
            .registerAs(PositioningStrategyKinds.INLINE_PARAMETER_MODIFIER)

    @JvmField
    val INLINE_FUN_MODIFIER: PsiPositioningStrategy<KtModifierListOwner> = InlineFunPositioningStrategy()
        .registerAs(PositioningStrategyKinds.INLINE_FUN_MODIFIER)

    @JvmField
    val PARAMETER_DEFAULT_VALUE: PsiPositioningStrategy<KtParameter> = object : PsiPositioningStrategy<KtParameter>() {
        override fun mark(element: KtParameter): List<TextRange> {
            return markNode(element.defaultValue!!.node)
        }
    }.registerAs(PositioningStrategyKinds.PARAMETER_DEFAULT_VALUE)

    @JvmField
    val PARAMETERS_WITH_DEFAULT_VALUE: PsiPositioningStrategy<KtFunction> = object : PsiPositioningStrategy<KtFunction>() {
        override fun mark(element: KtFunction): List<TextRange> =
            element.valueParameters.filter(KtParameter::hasDefaultValue).takeIf(List<*>::isNotEmpty)?.flatMap { markNode(it.node) }
                ?: element.valueParameterList?.let { markNode(it.node) }
                ?: element.nameIdentifier?.let { markNode(it.node) }
                ?: markNode(element.node)
    }.registerAs(PositioningStrategyKinds.PARAMETERS_WITH_DEFAULT_VALUE)

    @JvmField
    val PARAMETER_VARARG_MODIFIER: PsiPositioningStrategy<KtParameter> = object : PsiPositioningStrategy<KtParameter>() {
        override fun mark(element: KtParameter): List<TextRange> {
            val varargModifier = element.modifierList!!.getModifier(KtTokens.VARARG_KEYWORD)!!
            return markNode(varargModifier.node)
        }
    }.registerAs(PositioningStrategyKinds.PARAMETER_VARARG_MODIFIER)

    /**
     * Mark the name of a named argument. If the given element is not a named argument or doesn't have a name, then the entire given element
     * is marked instead.
     */
    @JvmField
    val NAME_OF_NAMED_ARGUMENT: PsiPositioningStrategy<KtValueArgument> = object : PsiPositioningStrategy<KtValueArgument>() {
        override fun mark(element: KtValueArgument): List<TextRange> {
            return markElement(element.getArgumentName() ?: element)
        }
    }.registerAs(PositioningStrategyKinds.NAME_OF_NAMED_ARGUMENT)

    @JvmField
    val CALL_ELEMENT_WITH_DOT: PsiPositioningStrategy<KtQualifiedExpression> = object : PsiPositioningStrategy<KtQualifiedExpression>() {
        override fun mark(element: KtQualifiedExpression): List<TextRange> {
            val callElementRanges = SELECTOR_BY_QUALIFIED.mark(element)
            val callElementRange = when (callElementRanges.size) {
                1 -> callElementRanges.first()
                else -> return callElementRanges
            }

            val dotRanges = SAFE_ACCESS.mark(element)
            val dotRange = when (dotRanges.size) {
                1 -> dotRanges.first()
                else -> return dotRanges
            }

            return listOf(TextRange(dotRange.startOffset, callElementRange.endOffset))
        }
    }.registerAs(PositioningStrategyKinds.CALL_ELEMENT_WITH_DOT)

    @JvmField
    val DECLARATION_WITH_BODY: PsiPositioningStrategy<KtDeclarationWithBody> = object : PsiPositioningStrategy<KtDeclarationWithBody>() {
        override fun mark(element: KtDeclarationWithBody): List<TextRange> {
            val lastBracketRange = element.bodyBlockExpression?.lastBracketRange
            return if (lastBracketRange != null)
                markRange(lastBracketRange)
            else
                markElement(element)
        }
    }.registerAs(PositioningStrategyKinds.DECLARATION_WITH_BODY)

    @JvmField
    val VAL_OR_VAR_NODE: PsiPositioningStrategy<KtDeclaration> = object : PsiPositioningStrategy<KtDeclaration>() {
        override fun mark(element: KtDeclaration): List<TextRange> {
            return when (element) {
                is KtParameter -> markElement(element.valOrVarKeyword ?: element)
                is KtProperty -> markElement(element.valOrVarKeyword)
                is KtDestructuringDeclaration -> markElement(element.valOrVarKeyword ?: element)
                else -> markElement(element)
            }
        }
    }.registerAs(PositioningStrategyKinds.VAL_OR_VAR_NODE)

    @JvmField
    val TRY_KEYWORD: PsiPositioningStrategy<KtTryExpression> =
        object : PsiPositioningStrategy<KtTryExpression>() {
            override fun mark(element: KtTryExpression): List<TextRange> {
                element.tryKeyword?.let {
                    return markElement(it)
                }
                return DEFAULT.mark(element)
            }
        }.registerAs(PositioningStrategyKinds.TRY_KEYWORD)

    @JvmField
    val ELSE_ENTRY: PsiPositioningStrategy<KtWhenEntry> = object : PsiPositioningStrategy<KtWhenEntry>() {
        override fun mark(element: KtWhenEntry): List<TextRange> {
            return markElement(element.elseKeyword!!)
        }
    }.registerAs(PositioningStrategyKinds.ELSE_ENTRY)

    @JvmField
    val WHEN_EXPRESSION: PsiPositioningStrategy<KtWhenExpression> = object : PsiPositioningStrategy<KtWhenExpression>() {
        override fun mark(element: KtWhenExpression): List<TextRange> {
            return markElement(element.whenKeyword)
        }
    }.registerAs(PositioningStrategyKinds.WHEN_EXPRESSION)

    @JvmField
    val WHEN_GUARD: PsiPositioningStrategy<KtWhenEntry> = object : PsiPositioningStrategy<KtWhenEntry>() {
        override fun mark(element: KtWhenEntry): List<TextRange> {
            return markElement(element.guard ?: element)
        }
    }.registerAs(PositioningStrategyKinds.WHEN_GUARD)

    @JvmField
    val IF_EXPRESSION: PsiPositioningStrategy<KtIfExpression> = object : PsiPositioningStrategy<KtIfExpression>() {
        override fun mark(element: KtIfExpression): List<TextRange> {
            return markElement(element.ifKeyword)
        }
    }.registerAs(PositioningStrategyKinds.IF_EXPRESSION)

    @JvmField
    val REDUNDANT_NULLABLE: PsiPositioningStrategy<KtTypeReference> = object : PsiPositioningStrategy<KtTypeReference>() {
        override fun mark(element: KtTypeReference): List<TextRange> {
            var typeElement = element.typeElement
            var question: ASTNode? = null
            var prevQuestion: ASTNode? = null
            var lastQuestion: ASTNode? = null
            while (typeElement is KtNullableType) {
                prevQuestion = question
                question = typeElement.questionMarkNode
                if (lastQuestion == null) {
                    lastQuestion = question
                }
                typeElement = typeElement.innerType
            }

            if (lastQuestion != null) {
                return markRange((prevQuestion ?: lastQuestion).psi, lastQuestion.psi)
            }

            return super.mark(element)
        }
    }.registerAs(PositioningStrategyKinds.REDUNDANT_NULLABLE)

    @JvmField
    val QUESTION_MARK_BY_TYPE: PsiPositioningStrategy<KtTypeReference> = object : PsiPositioningStrategy<KtTypeReference>() {
        override fun mark(element: KtTypeReference): List<TextRange> {
            val typeElement = element.typeElement
            if (typeElement is KtNullableType) {
                return markNode(typeElement.questionMarkNode)
            }
            return super.mark(element)
        }
    }.registerAs(PositioningStrategyKinds.QUESTION_MARK_BY_TYPE)

    @JvmField
    val VALUE_ARGUMENTS_LIST: PsiPositioningStrategy<KtElement> = object : PsiPositioningStrategy<KtElement>() {
        override fun mark(element: KtElement): List<TextRange> {
            return markElement(element.getChildOfType<KtValueArgumentList>() ?: element)
        }
    }.registerAs(PositioningStrategyKinds.VALUE_ARGUMENTS_LIST)

    @JvmField
    val LONG_LITERAL_SUFFIX: PsiPositioningStrategy<KtElement> = object : PsiPositioningStrategy<KtElement>() {
        override fun mark(element: KtElement): List<TextRange> {
            if (element is KtConstantExpression) {
                if (element.node.elementType == KtNodeTypes.INTEGER_CONSTANT) {
                    val endOffset = element.endOffset
                    return listOf(TextRange.create(endOffset - 1, endOffset))
                }
            }
            return markElement(element)
        }
    }.registerAs(PositioningStrategyKinds.LONG_LITERAL_SUFFIX)

    @JvmField
    val AS_TYPE: PsiPositioningStrategy<KtBinaryExpressionWithTypeRHS> = object : PsiPositioningStrategy<KtBinaryExpressionWithTypeRHS>() {
        override fun mark(element: KtBinaryExpressionWithTypeRHS): List<TextRange> {
            return markRange(element.operationReference, element)
        }
    }.registerAs(PositioningStrategyKinds.AS_TYPE)

    @JvmField
    val COMPANION_OBJECT: PsiPositioningStrategy<KtModifierListOwner> =
        ModifierSetBasedPositioningStrategy(KtTokens.COMPANION_KEYWORD)
            .registerAs(PositioningStrategyKinds.COMPANION_OBJECT)

    @JvmField
    val SECONDARY_CONSTRUCTOR_DELEGATION_CALL: PsiPositioningStrategy<PsiElement> =
        object : PsiPositioningStrategy<PsiElement>() {
            override fun mark(element: PsiElement): List<TextRange> {
                return when (element) {
                    is KtSecondaryConstructor -> {
                        val valueParameterList = element.valueParameterList ?: return markElement(element)
                        markRange(element.getConstructorKeyword(), valueParameterList.lastChild)
                    }
                    is KtConstructorDelegationCall -> {
                        if (element.isImplicit) {
                            // TODO: [VD] FIR collects for some reason implicit KtConstructorDelegationCall
                            // check(!element.isImplicit) { "Implicit KtConstructorDelegationCall should not be collected directly" }
                            val constructor = element.getStrictParentOfType<KtSecondaryConstructor>()!!
                            val valueParameterList = constructor.valueParameterList ?: return markElement(constructor)
                            return markRange(constructor.getConstructorKeyword(), valueParameterList.lastChild)
                        }
                        markElement(element.calleeExpression ?: element)
                    }
                    else -> markElement(element)
                }
            }
        }.registerAs(PositioningStrategyKinds.SECONDARY_CONSTRUCTOR_DELEGATION_CALL)

    @JvmField
    val USELESS_ELVIS: PsiPositioningStrategy<KtBinaryExpression> = object : PsiPositioningStrategy<KtBinaryExpression>() {
        override fun mark(element: KtBinaryExpression): List<TextRange> {
            return listOf(TextRange(element.operationReference.startOffset, element.endOffset))
        }
    }.registerAs(PositioningStrategyKinds.USELESS_ELVIS)

    @JvmField
    val USELESS_ELVIS_LEFT: PsiPositioningStrategy<KtBinaryExpression> = object : PsiPositioningStrategy<KtBinaryExpression>() {
        override fun mark(element: KtBinaryExpression): List<TextRange> {
            return listOf(TextRange(element.startOffset, element.operationReference.endOffset))
        }
    }.registerAs(PositioningStrategyKinds.USELESS_ELVIS_LEFT)

    @JvmField
    val IMPORT_ALIAS: PsiPositioningStrategy<KtImportDirective> = object : PsiPositioningStrategy<KtImportDirective>() {
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
    }.registerAs(PositioningStrategyKinds.IMPORT_ALIAS)

    @JvmField
    val RETURN_WITH_LABEL: PsiPositioningStrategy<KtReturnExpression> = object : PsiPositioningStrategy<KtReturnExpression>() {
        override fun mark(element: KtReturnExpression): List<TextRange> {
            val labeledExpression = element.labeledExpression
            if (labeledExpression != null) {
                return markRange(element, labeledExpression)
            }

            return markElement(element.returnKeyword)
        }
    }.registerAs(PositioningStrategyKinds.RETURN_WITH_LABEL)

    @JvmField
    val RECEIVER: PsiPositioningStrategy<KtCallableDeclaration> = object : PsiPositioningStrategy<KtCallableDeclaration>() {
        override fun mark(element: KtCallableDeclaration): List<TextRange> {
            element.receiverTypeReference?.let { return markElement(it) }
            return DEFAULT.mark(element)
        }
    }

    val OPERATOR: PsiPositioningStrategy<KtExpression> = object : PsiPositioningStrategy<KtExpression>() {
        override fun mark(element: KtExpression): List<TextRange> {
            return when (element) {
                is KtBinaryExpression -> mark(element.operationReference)
                is KtBinaryExpressionWithTypeRHS -> mark(element.operationReference)
                is KtUnaryExpression -> mark(element.operationReference)
                else -> super.mark(element)
            }
        }
    }.registerAs(PositioningStrategyKinds.OPERATOR)

    val REFERENCE_OR_OPERATOR: PsiPositioningStrategy<KtExpression> = object : PsiPositioningStrategy<KtExpression>() {
        override fun mark(element: KtExpression): List<TextRange> {
            (element as? KtBinaryExpression)?.right?.referenceIfShort()?.let { return super.mark(it) }
            return OPERATOR.mark(element)
        }
    }.registerAs(PositioningStrategyKinds.REFERENCE_OR_OPERATOR)

    val DOT_BY_QUALIFIED: PsiPositioningStrategy<PsiElement> = object : PsiPositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            if (element is KtBinaryExpression && element.operationToken in KtTokens.ALL_ASSIGNMENTS) {
                element.left?.let { left ->
                    left.findDescendantOfType<KtDotQualifiedExpression>()?.let { return mark(it) }
                }
            }
            if (element is KtQualifiedExpression) {
                return mark(element.operationTokenNode.psi)
            }
            // Fallback to mark the callee reference.
            return REFERENCE_BY_QUALIFIED.mark(element)
        }
    }.registerAs(PositioningStrategyKinds.DOT_BY_QUALIFIED)

    val SELECTOR_BY_QUALIFIED: PsiPositioningStrategy<PsiElement> = object : PsiPositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            if (element is KtBinaryExpression && element.operationToken in KtTokens.ALL_ASSIGNMENTS) {
                element.left?.let { return mark(it) }
            }
            if (element is KtQualifiedExpression) {
                when (val selectorExpression = element.selectorExpression) {
                    is KtElement -> return mark(selectorExpression)
                }
            }
            if (element is KtImportDirective) {
                element.alias?.nameIdentifier?.let { return mark(it) }
                element.importedReference?.let { return mark(it) }
            }
            if (element is KtTypeReference) {
                element.typeElement?.getReferencedTypeExpression()?.let { return mark(it) }
            }
            return super.mark(element)
        }
    }.registerAs(PositioningStrategyKinds.SELECTOR_BY_QUALIFIED)

    private fun KtTypeElement.getReferencedTypeExpression(): KtElement? {
        return when (this) {
            is KtUserType -> referenceExpression
            is KtNullableType -> innerType?.getReferencedTypeExpression()
            else -> null
        }
    }

    val NAME_IDENTIFIER: PsiPositioningStrategy<PsiElement> = object : PsiPositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            if (element is PsiNameIdentifierOwner) {
                val nameIdentifier = element.nameIdentifier
                if (nameIdentifier != null) {
                    return super.mark(nameIdentifier)
                }
            } else if (element is KtLabelReferenceExpression) {
                return super.mark(element.getReferencedNameElement())
            } else if (element is KtPackageDirective) {
                element.packageNameExpression?.let {
                    return super.mark(it)
                }
            }

            return DEFAULT.mark(element)
        }
    }.registerAs(PositioningStrategyKinds.NAME_IDENTIFIER)

    @JvmField
    val SPREAD_OPERATOR: PsiPositioningStrategy<PsiElement> = object : PsiPositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            return super.mark((element as? KtValueArgument)?.getSpreadElement()?.node?.psi ?: element)
        }
    }.registerAs(PositioningStrategyKinds.SPREAD_OPERATOR)

    @JvmField
    val FUN_INTERFACE: PsiPositioningStrategy<KtDeclaration> = object : PsiPositioningStrategy<KtDeclaration>() {
        override fun mark(element: KtDeclaration): List<TextRange> {
            return when (element) {
                is KtClass -> FUN_MODIFIER.mark(element)
                is KtProperty -> markElement(element.valOrVarKeyword)
                is KtNamedFunction -> {
                    val typeParameterList = element.typeParameterList
                    when {
                        typeParameterList != null -> markElement(typeParameterList)
                        element.hasModifier(KtTokens.SUSPEND_KEYWORD) -> SUSPEND_MODIFIER.mark(element)
                        else -> markElement(element.funKeyword ?: element)
                    }
                }
                else -> markElement(element)
            }
        }
    }.registerAs(PositioningStrategyKinds.FUN_INTERFACE)

    val REFERENCE_BY_QUALIFIED: PsiPositioningStrategy<PsiElement> = FindReferencePositioningStrategy(false)
        .registerAs(PositioningStrategyKinds.REFERENCE_BY_QUALIFIED)
    val REFERENCED_NAME_BY_QUALIFIED: PsiPositioningStrategy<PsiElement> = FindReferencePositioningStrategy(true)
        .registerAs(PositioningStrategyKinds.REFERENCED_NAME_BY_QUALIFIED)

    val RECEIVER_OF_DOT_QUALIFIED: PsiPositioningStrategy<PsiElement> = object : PsiPositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            if (element is KtDotQualifiedExpression) {
                return mark(element.receiverExpression)
            }
            return DEFAULT.mark(element)
        }
    }.registerAs(PositioningStrategyKinds.RECEIVER_OF_DOT_QUALIFIED)

    val REIFIED_MODIFIER: PsiPositioningStrategy<KtModifierListOwner> =
        ModifierSetBasedPositioningStrategy(KtTokens.REIFIED_KEYWORD)
            .registerAs(PositioningStrategyKinds.REIFIED_MODIFIER)

    private fun KtExpression.referenceIfShort(): KtExpression? =
        when (this) {
            is KtCallableReferenceExpression -> callableReference.referenceIfShort()
            is KtQualifiedExpression -> selectorExpression?.referenceIfShort()
            is KtCallExpression -> calleeExpression
            is KtArrayAccessExpression -> null
            is KtReferenceExpression -> this
            is KtThisExpression -> this
            else -> null
        }

    val VARIABLE_INITIALIZER: PsiPositioningStrategy<KtElement> = object : PsiPositioningStrategy<KtElement>() {

        override fun mark(element: KtElement): List<TextRange> = markElement(
            when (element) {
                is KtProperty -> {
                    element.initializer?.referenceIfShort() ?: element.equalsToken ?: element.initializer
                    ?: element
                }
                // Type reference is used as a target for loop variable type mismatches
                is KtParameter -> {
                    element.defaultValue?.referenceIfShort() ?: element.equalsToken ?: element.defaultValue
                    ?: element.typeReference ?: element
                }
                is KtDestructuringDeclarationEntry -> {
                    element.initializer?.referenceIfShort() ?: element.equalsToken ?: element.initializer
                    ?: element.typeReference ?: element
                }
                is KtBackingField -> {
                    element.initializer?.referenceIfShort() ?: element.equalsToken ?: element.initializer
                    ?: element
                }
                else -> element
            }
        )
    }.registerAs(PositioningStrategyKinds.VARIABLE_INITIALIZER)

    val WHOLE_ELEMENT: PsiPositioningStrategy<KtElement> = object : PsiPositioningStrategy<KtElement>() {}
        .registerAs(PositioningStrategyKinds.WHOLE_ELEMENT)

    val TYPE_PARAMETERS_LIST: PsiPositioningStrategy<KtDeclaration> = object : PsiPositioningStrategy<KtDeclaration>() {
        override fun mark(element: KtDeclaration): List<TextRange> {
            if (element is KtTypeParameterListOwner) {
                return markElement(element.typeParameterList ?: element)
            }
            return markElement(element)
        }
    }.registerAs(PositioningStrategyKinds.TYPE_PARAMETERS_LIST)

    val FUNCTION_TYPE_RECEIVER: PsiPositioningStrategy<KtElement> = object : PsiPositioningStrategy<KtElement>() {
        override fun mark(element: KtElement): List<TextRange> {
            val target = element.takeIf { it.elementType == KtNodeTypes.VALUE_PARAMETER }
                ?.getChildOfType<KtTypeReference>()
                ?.children?.firstOrNull()?.takeIf { it.elementType == KtNodeTypes.FUNCTION_TYPE }
                ?.getChildOfType<KtFunctionTypeReceiver>()
                ?: element
            return markElement(target)
        }
    }.registerAs(PositioningStrategyKinds.FUNCTION_TYPE_RECEIVER)

    val ANNOTATION_USE_SITE: PsiPositioningStrategy<KtAnnotationEntry> = object : PsiPositioningStrategy<KtAnnotationEntry>() {
        override fun mark(element: KtAnnotationEntry): List<TextRange> {
            return markElement(element.useSiteTarget ?: element)
        }
    }.registerAs(PositioningStrategyKinds.ANNOTATION_USE_SITE)

    val IMPORT_LAST_NAME: PsiPositioningStrategy<PsiElement> = object : PsiPositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            if (element is KtImportDirective) {
                val importedReference = element.importedReference
                if (importedReference is KtDotQualifiedExpression) {
                    importedReference.selectorExpression?.let { return super.mark(it) }
                }
                return super.mark(element.importedReference ?: element)
            }
            return super.mark(element)
        }
    }.registerAs(PositioningStrategyKinds.IMPORT_LAST_NAME)

    @OptIn(UnsafeCastFunction::class)
    val IMPORT_LAST_BUT_ONE_NAME: PsiPositioningStrategy<KtImportDirective> = object : PsiPositioningStrategy<KtImportDirective>() {
        override fun mark(element: KtImportDirective): List<TextRange> {
            element.importedReference
                ?.safeAs<KtDotQualifiedExpression>()
                ?.receiverExpression
                ?.safeAs<KtDotQualifiedExpression>()
                ?.selectorExpression
                ?.let { return markElement(it) }

            return super.mark(element)
        }
    }.registerAs(PositioningStrategyKinds.IMPORT_LAST_BUT_ONE_NAME)

    val LABEL: PsiPositioningStrategy<KtElement> = object : PsiPositioningStrategy<KtElement>() {
        override fun mark(element: KtElement): List<TextRange> {
            return super.mark((element as? KtExpressionWithLabel)?.labelQualifier ?: element)
        }
    }.registerAs(PositioningStrategyKinds.LABEL)

    val COMMAS: PsiPositioningStrategy<PsiElement> = object : PsiPositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            return buildList {
                for (child in element.allChildren) {
                    if (child.node.elementType == KtTokens.COMMA) {
                        add(markSingleElement(child))
                    }
                }
            }
        }

        @OptIn(DiagnosticLossRisk::class)
        override fun isValid(element: KtSourceElement): Boolean = true
    }.registerAs(PositioningStrategyKinds.COMMAS)

    val NON_FINAL_MODIFIER_OR_NAME: PsiPositioningStrategy<KtModifierListOwner> =
        ModifierSetBasedPositioningStrategy(KtTokens.ABSTRACT_KEYWORD, KtTokens.OPEN_KEYWORD, KtTokens.SEALED_KEYWORD)
            .registerAs(PositioningStrategyKinds.NON_FINAL_MODIFIER_OR_NAME)

    val DELEGATED_SUPERTYPE_BY_KEYWORD: PsiPositioningStrategy<KtTypeReference> = object : PsiPositioningStrategy<KtTypeReference>() {
        override fun mark(element: KtTypeReference): List<TextRange> {
            val parent = element.parent as? KtDelegatedSuperTypeEntry ?: return super.mark(element)
            return markElement(parent.byKeywordNode.psi ?: element)
        }
    }.registerAs(PositioningStrategyKinds.DELEGATED_SUPERTYPE_BY_KEYWORD)

    val PROPERTY_DELEGATE_BY_KEYWORD: PsiPositioningStrategy<KtExpression> = object : PsiPositioningStrategy<KtExpression>() {
        override fun mark(element: KtExpression): List<TextRange> {
            return markElement((element.parent as? KtPropertyDelegate)?.byKeywordNode?.psi ?: element)
        }
    }.registerAs(PositioningStrategyKinds.PROPERTY_DELEGATE_BY_KEYWORD)

    @JvmField
    val TYPEALIAS_TYPE_REFERENCE = object : PsiPositioningStrategy<KtTypeAlias>() {
        override fun mark(element: KtTypeAlias): List<TextRange> {
            return markElement(element.getTypeReference() ?: element)
        }
    }.registerAs(PositioningStrategyKinds.TYPEALIAS_TYPE_REFERENCE)

    @JvmField
    val SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS_DIAGNOSTIC = object : PsiPositioningStrategy<KtElement>() {
        override fun mark(element: KtElement): List<TextRange> {
            val elementToMark = when (element) {
                is KtEnumEntry -> element.initializerList ?: element
                is KtTypeReference -> {
                    val superTypeCallEntry = (element.parent as? KtConstructorCalleeExpression)?.parent as? KtSuperTypeCallEntry
                    superTypeCallEntry?.valueArgumentList ?: element
                }
                else -> element
            }
            return markElement(elementToMark)
        }
    }.registerAs(PositioningStrategyKinds.SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS_DIAGNOSTIC)

    /**
     * @param locateReferencedName whether to remove any nested parentheses while locating the reference element. This is useful for
     * diagnostics on super and unresolved references. For example, with the following, only the part inside the parentheses should be
     * highlighted.
     *
     * ```
     * fun foo() {
     *   (super)()
     *    ^^^^^
     *   (random123)()
     *    ^^^^^^^^^
     * }
     * ```
     */
    class FindReferencePositioningStrategy(val locateReferencedName: Boolean) : PsiPositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            if (element is KtObjectLiteralExpression) {
                return DEFAULT.mark(element)
            }
            if (element is KtBinaryExpression && element.operationToken == KtTokens.EQ) {
                // Look for reference in LHS of variable assignment.
                element.left?.let { return mark(it) }
            }
            if (element is KtClassOrObject) {
                val nameIdentifier = (element as? KtNamedDeclaration)?.nameIdentifier
                if (nameIdentifier != null) {
                    val startElement = findStartingPsiElementForDeclarationName(element)
                    return markRange(startElement, nameIdentifier)
                }
            }
            var result: PsiElement = when (element) {
                is KtQualifiedExpression -> {
                    when (val selectorExpression = element.selectorExpression) {
                        is KtCallExpression -> selectorExpression.calleeExpression ?: selectorExpression
                        is KtReferenceExpression -> selectorExpression
                        else -> element
                    }
                }
                is KtCallableReferenceExpression -> element.callableReference
                is KtCallExpression -> element.calleeExpression ?: element
                is KtConstructorDelegationCall -> element.calleeExpression ?: element
                is KtSuperTypeCallEntry -> element.calleeExpression.also {
                    if (it.textRange.isEmpty) {
                        val grandParent = element.parent?.parent
                        if (grandParent is KtEnumEntry) return mark(grandParent)
                    }
                }
                is KtConstructorCalleeExpression if element.textRange.isEmpty -> {
                    val ggParent = element.parent?.parent?.parent
                    if (ggParent is KtEnumEntry) return mark(ggParent) else element
                }
                is KtOperationExpression -> element.operationReference
                is KtWhenConditionInRange -> element.operationReference
                is KtAnnotationEntry -> element.calleeExpression ?: element
                is KtTypeReference -> (element.typeElement as? KtNullableType)?.innerType ?: element
                is KtImportDirective -> element.importedReference ?: element
                is KtImportAlias -> element.nameIdentifier ?: element
                is KtDestructuringDeclarationEntry -> element.nameIdentifier ?: element
                else -> element
            }
            while (locateReferencedName && result is KtParenthesizedExpression) {
                result = result.expression ?: break
            }
            return super.mark(result)
        }
    }

    val TYPE_ARGUMENT_LIST_OR_SELF = object : PsiPositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            ((element as? KtQualifiedExpression)?.selectorExpression ?: element).getChildOfType<KtTypeArgumentList>()?.let {
                return markElement(it)
            }
            return super.mark(element)
        }
    }.registerAs(PositioningStrategyKinds.TYPE_ARGUMENT_LIST_OR_SELF)

    val TYPE_ARGUMENT_LIST_OR_WITHOUT_RECEIVER = object : PsiPositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            val selector = (element as? KtQualifiedExpression)?.selectorExpression
            (selector ?: element).getChildOfType<KtTypeArgumentList>()?.let {
                return markElement(it)
            }
            selector?.let {
                return markElement(it)
            }
            return super.mark(element)
        }
    }.registerAs(PositioningStrategyKinds.TYPE_ARGUMENT_LIST_OR_WITHOUT_RECEIVER)

    val PACKAGE_DIRECTIVE_NAME_EXPRESSION: PsiPositioningStrategy<KtElement> = object : PsiPositioningStrategy<KtElement>() {
        override fun mark(element: KtElement): List<TextRange> {
            val packageNameExpression = (element as? KtPackageDirective)?.packageNameExpression
            return super.mark(packageNameExpression ?: element)
        }
    }.registerAs(PositioningStrategyKinds.PACKAGE_DIRECTIVE_NAME_EXPRESSION)

    val OUTERMOST_PARENTHESES_IN_ASSIGNMENT_LHS: PsiPositioningStrategy<PsiElement> = object : PsiPositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            val parenthesized = element.getAssignmentLhsIfUnwrappable() ?: return super.mark(element)
            return super.mark(parenthesized)
        }
    }.registerAs(PositioningStrategyKinds.OUTERMOST_PARENTHESES_IN_ASSIGNMENT_LHS)

    /**
     * Marks the brackets of a destructuring declaration together with its entries, e.g., `(a, b)` in `val (a, b) = pair`.
     */
    val DESTRUCTURING_DECLARATION_ENTRIES: PsiPositioningStrategy<KtDestructuringDeclaration> =
        object : PsiPositioningStrategy<KtDestructuringDeclaration>() {
            override fun mark(element: KtDestructuringDeclaration): List<TextRange> {
                val opening = element.lPar ?: return super.mark(element)
                val closing = element.rPar ?: element.entries.lastOrNull() ?: opening
                return markRange(opening, closing)
            }
        }.registerAs(PositioningStrategyKinds.DESTRUCTURING_DECLARATION_ENTRIES)

    val DEPRECATION: PsiPositioningStrategy<PsiElement> = object : PsiPositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            if (element is KtConstructorCalleeExpression) element.typeReference?.let { return mark(it) }
            if (element is KtTypeReference) return SELECTOR_BY_QUALIFIED.mark(element)
            return REFERENCED_NAME_BY_QUALIFIED.mark(element)
        }
    }.registerAs(PositioningStrategyKinds.DEPRECATION)

    val UNREACHABLE_CODE = object : PsiPositioningStrategy<PsiElement>() {
        override fun markDiagnostic(diagnostic: KtDiagnosticWithSource): List<TextRange> {
            //todo it is better to implement arguments extraction in KtDiagnosticFactory, but kotlin struggle with checking types in it atm
            @Suppress("UNCHECKED_CAST")
            val typed = diagnostic as KtDiagnosticWithParameters2<Set<KtSourceElement>, Set<KtSourceElement>>
            val source = diagnostic.element as KtPsiSourceElement
            return UnreachableCode.getUnreachableTextRanges(
                source.psi as KtElement,
                typed.a.mapNotNull { it.psi as? KtElement }.toSet(),
                typed.b.mapNotNull { it.psi as? KtElement }.toSet()
            )
        }
    }.registerAs(PositioningStrategyKinds.UNREACHABLE_CODE)

    val ACTUAL_DECLARATION_NAME = object : PsiPositioningStrategy<PsiElement>() {
        override fun markDiagnostic(diagnostic: KtDiagnosticWithSource): List<TextRange> {
            val element = diagnostic.element.psi ?: return emptyList()
            (element as? KtNamedDeclaration)?.nameIdentifier?.let { nameIdentifier ->
                return mark(nameIdentifier)
            }
            return mark(element)
        }
    }.registerAs(PositioningStrategyKinds.ACTUAL_DECLARATION_NAME)
}
