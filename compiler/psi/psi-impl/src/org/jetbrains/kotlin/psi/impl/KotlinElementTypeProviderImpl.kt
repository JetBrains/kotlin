/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.psi.impl.source.tree.ICodeFragmentElementType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.ILazyParseableElementType
import org.jetbrains.kotlin.BlockExpressionElementType
import org.jetbrains.kotlin.KotlinElementTypeProvider
import org.jetbrains.kotlin.KtNodeType
import org.jetbrains.kotlin.LambdaExpressionElementType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.kdoc.lexer.KDocLexer
import org.jetbrains.kotlin.kdoc.parser.KDocLinkParser
import org.jetbrains.kotlin.kdoc.parser.KDocParser
import org.jetbrains.kotlin.kdoc.psi.impl.KDocImpl
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.stubs.elements.KtFileElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

@KtImplementationDetail
internal object KotlinElementTypeProviderImpl : KotlinElementTypeProvider {
    override val fileType: IFileElementType
        get() = KtFileElementType

    // Classifiers

    override val classType: KtNodeType
        get() = KtStubElementTypes.CLASS

    override val objectType: KtNodeType
        get() = KtStubElementTypes.OBJECT_DECLARATION

    override val typeAliasType: KtNodeType
        get() = KtStubElementTypes.TYPEALIAS

    override val classBodyType: KtNodeType
        get() = KtStubElementTypes.CLASS_BODY

    @KtExperimentalApi
    override val companionBlockType: KtNodeType
        get() = KtStubElementTypes.COMPANION_BLOCK

    // Initializers

    override val classInitializerType: KtNodeType
        get() = KtStubElementTypes.CLASS_INITIALIZER

    override val scriptInitializerType: KtNodeType
        get() = KtStubElementTypes.SCRIPT_INITIALIZER

    // Callables

    override val functionType: KtNodeType
        get() = KtStubElementTypes.FUNCTION

    override val propertyType: KtNodeType
        get() = KtStubElementTypes.PROPERTY

    override val enumEntryType: KtNodeType
        get() = KtStubElementTypes.ENUM_ENTRY

    override val primaryConstructorType: KtNodeType
        get() = KtStubElementTypes.PRIMARY_CONSTRUCTOR

    override val secondaryConstructorType: KtNodeType
        get() = KtStubElementTypes.SECONDARY_CONSTRUCTOR

    override val constructorCalleeType: KtNodeType
        get() = KtStubElementTypes.CONSTRUCTOR_CALLEE

    override val propertyAccessorType: KtNodeType
        get() = KtStubElementTypes.PROPERTY_ACCESSOR

    override val backingFieldType: KtNodeType
        get() = KtStubElementTypes.BACKING_FIELD

    override val destructuringDeclarationType: KtNodeType
        get() = KtStubElementTypes.DESTRUCTURING_DECLARATION

    override val initializerListType: KtNodeType
        get() = KtStubElementTypes.INITIALIZER_LIST


    // Value parameters
    override val valueParameterListType: KtNodeType
        get() = KtStubElementTypes.VALUE_PARAMETER_LIST

    override val valueParameterType: KtNodeType
        get() = KtStubElementTypes.VALUE_PARAMETER

    override val contextParameterListType: KtNodeType
        get() = KtStubElementTypes.CONTEXT_PARAMETER_LIST

    override val contextReceiverType: KtNodeType
        get() = KtStubElementTypes.CONTEXT_RECEIVER


    // Type parameters
    override val typeParameterListType: KtNodeType
        get() = KtStubElementTypes.TYPE_PARAMETER_LIST

    override val typeParameterType: KtNodeType
        get() = KtStubElementTypes.TYPE_PARAMETER

    override val typeConstraintListType: KtNodeType
        get() = KtStubElementTypes.TYPE_CONSTRAINT_LIST

    override val typeConstraintType: KtNodeType
        get() = KtStubElementTypes.TYPE_CONSTRAINT


    // Supertypes
    override val superTypeListType: KtNodeType
        get() = KtStubElementTypes.SUPER_TYPE_LIST

    override val delegatedSuperTypeEntryType: KtNodeType
        get() = KtStubElementTypes.DELEGATED_SUPER_TYPE_ENTRY

    override val superTypeCallEntryType: KtNodeType
        get() = KtStubElementTypes.SUPER_TYPE_CALL_ENTRY

    override val superTypeEntryType: KtNodeType
        get() = KtStubElementTypes.SUPER_TYPE_ENTRY


    // Modifiers and annotations

    override val modifierListType: KtNodeType
        get() = KtStubElementTypes.MODIFIER_LIST

    override val annotationType: KtNodeType
        get() = KtStubElementTypes.ANNOTATION

    override val annotationEntryType: KtNodeType
        get() = KtStubElementTypes.ANNOTATION_ENTRY

    override val annotationTargetType: KtNodeType
        get() = KtStubElementTypes.ANNOTATION_TARGET


    // Type references

    override val typeReferenceType: KtNodeType
        get() = KtStubElementTypes.TYPE_REFERENCE

    override val userTypeType: KtNodeType
        get() = KtStubElementTypes.USER_TYPE

    override val dynamicTypeType: KtNodeType
        get() = KtStubElementTypes.DYNAMIC_TYPE

    override val functionTypeType: KtNodeType
        get() = KtStubElementTypes.FUNCTION_TYPE

    override val functionTypeReceiverType: KtNodeType
        get() = KtStubElementTypes.FUNCTION_TYPE_RECEIVER

    override val nullableTypeType: KtNodeType
        get() = KtStubElementTypes.NULLABLE_TYPE

    override val intersectionTypeType: KtNodeType
        get() = KtStubElementTypes.INTERSECTION_TYPE

    override val typeProjectionType: KtNodeType
        get() = KtStubElementTypes.TYPE_PROJECTION


    // Constants
    override val nullType: KtNodeType
        get() = KtStubElementTypes.NULL

    override val booleanConstantType: KtNodeType
        get() = KtStubElementTypes.BOOLEAN_CONSTANT

    override val floatConstantType: KtNodeType
        get() = KtStubElementTypes.FLOAT_CONSTANT

    override val characterConstantType: KtNodeType
        get() = KtStubElementTypes.CHARACTER_CONSTANT

    override val integerConstantType: KtNodeType
        get() = KtStubElementTypes.INTEGER_CONSTANT


    // String templates

    override val stringTemplateType: KtNodeType
        get() = KtStubElementTypes.STRING_TEMPLATE

    override val longStringTemplateEntryType: KtNodeType
        get() = KtStubElementTypes.LONG_STRING_TEMPLATE_ENTRY

    override val shortStringTemplateEntryType: KtNodeType
        get() = KtStubElementTypes.SHORT_STRING_TEMPLATE_ENTRY

    override val literalStringTemplateEntryType: KtNodeType
        get() = KtStubElementTypes.LITERAL_STRING_TEMPLATE_ENTRY

    override val escapeStringTemplateEntryType: KtNodeType
        get() = KtStubElementTypes.ESCAPE_STRING_TEMPLATE_ENTRY

    override val stringInterpolationPrefixType: KtNodeType
        get() = KtStubElementTypes.STRING_INTERPOLATION_PREFIX


    // Expressions

    override val blockExpressionType: IElementType = BlockExpressionElementType()

    override val lambdaExpressionType: IElementType = LambdaExpressionElementType()

    override val referenceExpressionType: KtNodeType
        get() = KtStubElementTypes.REFERENCE_EXPRESSION

    override val enumEntrySuperclassReferenceExpressionType: KtNodeType
        get() = KtStubElementTypes.ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION

    override val dotQualifiedExpressionType: KtNodeType
        get() = KtStubElementTypes.DOT_QUALIFIED_EXPRESSION

    override val callExpressionType: KtNodeType
        get() = KtStubElementTypes.CALL_EXPRESSION

    override val operationReferenceType: KtNodeType
        get() = KtStubElementTypes.OPERATION_REFERENCE

    override val prefixExpressionType: KtNodeType
        get() = KtStubElementTypes.PREFIX_EXPRESSION

    override val postfixExpressionType: KtNodeType
        get() = KtStubElementTypes.POSTFIX_EXPRESSION

    override val binaryExpressionType: KtNodeType
        get() = KtStubElementTypes.BINARY_EXPRESSION

    override val parenthesizedExpressionType: KtNodeType
        get() = KtStubElementTypes.PARENTHESIZED

    override val classLiteralExpressionType: KtNodeType
        get() = KtStubElementTypes.CLASS_LITERAL_EXPRESSION

    override val collectionLiteralExpressionType: KtNodeType
        get() = KtStubElementTypes.COLLECTION_LITERAL_EXPRESSION

    override val objectLiteralType: KtNodeType
        get() = KtStubElementTypes.OBJECT_LITERAL

    // Arguments
    override val typeArgumentListType: KtNodeType
        get() = KtStubElementTypes.TYPE_ARGUMENT_LIST

    override val valueArgumentListType: KtNodeType
        get() = KtStubElementTypes.VALUE_ARGUMENT_LIST

    override val valueArgumentType: KtNodeType
        get() = KtStubElementTypes.VALUE_ARGUMENT

    override val contractEffectListType: KtNodeType
        get() = KtStubElementTypes.CONTRACT_EFFECT_LIST

    override val contractEffectType: KtNodeType
        get() = KtStubElementTypes.CONTRACT_EFFECT

    override val lambdaArgumentType: KtNodeType
        get() = KtStubElementTypes.LAMBDA_ARGUMENT

    override val valueArgumentNameType: KtNodeType
        get() = KtStubElementTypes.VALUE_ARGUMENT_NAME


    // Special

    override val packageDirectiveType: KtNodeType
        get() = KtStubElementTypes.PACKAGE_DIRECTIVE

    override val fileAnnotationListType: KtNodeType
        get() = KtStubElementTypes.FILE_ANNOTATION_LIST

    override val importListType: KtNodeType
        get() = KtStubElementTypes.IMPORT_LIST

    override val importDirectiveType: KtNodeType
        get() = KtStubElementTypes.IMPORT_DIRECTIVE

    override val importAliasType: KtNodeType
        get() = KtStubElementTypes.IMPORT_ALIAS

    override val scriptType: KtNodeType
        get() = KtStubElementTypes.SCRIPT


    // Code fragments

    override val expressionCodeFragmentType: ICodeFragmentElementType = KtExpressionCodeFragmentType()

    override val blockCodeFragmentType: ICodeFragmentElementType = KtBlockCodeFragmentType()

    override val typeCodeFragmentType: ICodeFragmentElementType = KtTypeCodeFragmentType()


    // KDoc

    override val kdocType: ILazyParseableElementType =
        object : ILazyParseableElementType("KDoc", KotlinLanguage.INSTANCE) {
            override fun parseContents(chameleon: ASTNode): ASTNode? {
                val parentElement = chameleon.treeParent.psi
                val project = parentElement.project
                val builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon, KDocLexer(), language, chameleon.text)
                val parser = KDocParser()
                return parser.parse(this, builder).firstChildNode
            }

            override fun createNode(text: CharSequence?): ASTNode {
                return KDocImpl(text)
            }
        }

    override val kdocMarkdownLinkType: ILazyParseableElementType =
        object : ILazyParseableElementType("KDOC_MARKDOWN_LINK", KotlinLanguage.INSTANCE) {
            override fun parseContents(chameleon: ASTNode): ASTNode {
                return KDocLinkParser.parseMarkdownLink(this, chameleon)
            }
        }
}
