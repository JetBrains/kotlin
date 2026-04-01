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
import org.jetbrains.kotlin.LambdaExpressionElementType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.kdoc.lexer.KDocLexer
import org.jetbrains.kotlin.kdoc.parser.KDocLinkParser
import org.jetbrains.kotlin.kdoc.parser.KDocParser
import org.jetbrains.kotlin.kdoc.psi.impl.KDocImpl
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.stubs.*
import org.jetbrains.kotlin.psi.stubs.elements.KtFileElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

@KtImplementationDetail
@Suppress("unused") // Initiated via reflection in 'KotlinElementTypeProvider'
object KotlinElementTypeProviderImpl : KotlinElementTypeProvider {
    override val fileType: IFileElementType
        get() = KtFileElementType

    // Classifiers

    override val classType: KtStubElementType<out KotlinClassStub, KtClass>
        get() = KtStubElementTypes.CLASS

    override val objectType: KtStubElementType<out KotlinObjectStub, KtObjectDeclaration>
        get() = KtStubElementTypes.OBJECT_DECLARATION

    override val typeAliasType: KtStubElementType<out KotlinTypeAliasStub, KtTypeAlias>
        get() = KtStubElementTypes.TYPEALIAS

    override val classBodyType: KtStubElementType<out KotlinPlaceHolderStub<KtClassBody>, KtClassBody>
        get() = KtStubElementTypes.CLASS_BODY

    @KtExperimentalApi
    override val companionBlockType: KtStubElementType<out KotlinPlaceHolderStub<KtCompanionBlock>, KtCompanionBlock>
        get() = KtStubElementTypes.COMPANION_BLOCK

    // Initializers

    override val classInitializerType: KtStubElementType<out KotlinPlaceHolderStub<KtClassInitializer>, KtClassInitializer>
        get() = KtStubElementTypes.CLASS_INITIALIZER

    override val scriptInitializerType: KtStubElementType<out KotlinPlaceHolderStub<KtScriptInitializer>, KtScriptInitializer>
        get() = KtStubElementTypes.SCRIPT_INITIALIZER

    // Callables

    override val functionType: KtStubElementType<out KotlinFunctionStub, KtNamedFunction>
        get() = KtStubElementTypes.FUNCTION

    override val propertyType: KtStubElementType<out KotlinPropertyStub, KtProperty>
        get() = KtStubElementTypes.PROPERTY

    override val enumEntryType: KtStubElementType<out KotlinClassStub, KtEnumEntry>
        get() = KtStubElementTypes.ENUM_ENTRY

    override val primaryConstructorType: KtStubElementType<out KotlinConstructorStub<KtPrimaryConstructor>, KtPrimaryConstructor>
        get() = KtStubElementTypes.PRIMARY_CONSTRUCTOR

    override val secondaryConstructorType: KtStubElementType<out KotlinConstructorStub<KtSecondaryConstructor>, KtSecondaryConstructor>
        get() = KtStubElementTypes.SECONDARY_CONSTRUCTOR

    override val constructorCalleeType: KtStubElementType<out KotlinPlaceHolderStub<KtConstructorCalleeExpression>, KtConstructorCalleeExpression>
        get() = KtStubElementTypes.CONSTRUCTOR_CALLEE

    override val propertyAccessorType: KtStubElementType<out KotlinPropertyAccessorStub, KtPropertyAccessor>
        get() = KtStubElementTypes.PROPERTY_ACCESSOR

    override val backingFieldType: KtStubElementType<out KotlinBackingFieldStub, KtBackingField>
        get() = KtStubElementTypes.BACKING_FIELD

    override val initializerListType: KtStubElementType<out KotlinPlaceHolderStub<KtInitializerList>, KtInitializerList>
        get() = KtStubElementTypes.INITIALIZER_LIST


    // Value parameters
    override val valueParameterListType: KtStubElementType<out KotlinPlaceHolderStub<KtParameterList>, KtParameterList>
        get() = KtStubElementTypes.VALUE_PARAMETER_LIST

    override val valueParameterType: KtStubElementType<out KotlinParameterStub, KtParameter>
        get() = KtStubElementTypes.VALUE_PARAMETER

    override val contextParameterListType: KtStubElementType<out KotlinPlaceHolderStub<KtContextParameterList>, KtContextParameterList>
        get() = KtStubElementTypes.CONTEXT_PARAMETER_LIST

    override val contextReceiverType: KtStubElementType<out KotlinContextReceiverStub, KtContextReceiver>
        get() = KtStubElementTypes.CONTEXT_RECEIVER


    // Type parameters
    override val typeParameterListType: KtStubElementType<out KotlinPlaceHolderStub<KtTypeParameterList>, KtTypeParameterList>
        get() = KtStubElementTypes.TYPE_PARAMETER_LIST

    override val typeParameterType: KtStubElementType<out KotlinTypeParameterStub, KtTypeParameter>
        get() = KtStubElementTypes.TYPE_PARAMETER

    override val typeConstraintListType: KtStubElementType<out KotlinPlaceHolderStub<KtTypeConstraintList>, KtTypeConstraintList>
        get() = KtStubElementTypes.TYPE_CONSTRAINT_LIST

    override val typeConstraintType: KtStubElementType<out KotlinPlaceHolderStub<KtTypeConstraint>, KtTypeConstraint>
        get() = KtStubElementTypes.TYPE_CONSTRAINT


    // Supertypes
    override val superTypeListType: KtStubElementType<out KotlinPlaceHolderStub<KtSuperTypeList>, KtSuperTypeList>
        get() = KtStubElementTypes.SUPER_TYPE_LIST

    override val delegatedSuperTypeEntryType: KtStubElementType<out KotlinPlaceHolderStub<KtDelegatedSuperTypeEntry>, KtDelegatedSuperTypeEntry>
        get() = KtStubElementTypes.DELEGATED_SUPER_TYPE_ENTRY

    override val superTypeCallEntryType: KtStubElementType<out KotlinPlaceHolderStub<KtSuperTypeCallEntry>, KtSuperTypeCallEntry>
        get() = KtStubElementTypes.SUPER_TYPE_CALL_ENTRY

    override val superTypeEntryType: KtStubElementType<out KotlinPlaceHolderStub<KtSuperTypeEntry>, KtSuperTypeEntry>
        get() = KtStubElementTypes.SUPER_TYPE_ENTRY


    // Modifiers and annotations

    override val modifierListType: KtStubElementType<out KotlinModifierListStub, KtDeclarationModifierList>
        get() = KtStubElementTypes.MODIFIER_LIST

    override val annotationType: KtStubElementType<out KotlinPlaceHolderStub<KtAnnotation>, KtAnnotation>
        get() = KtStubElementTypes.ANNOTATION

    override val annotationEntryType: KtStubElementType<out KotlinAnnotationEntryStub, KtAnnotationEntry>
        get() = KtStubElementTypes.ANNOTATION_ENTRY

    override val annotationTargetType: KtStubElementType<out KotlinAnnotationUseSiteTargetStub, KtAnnotationUseSiteTarget>
        get() = KtStubElementTypes.ANNOTATION_TARGET


    // Type references

    override val typeReferenceType: KtStubElementType<out KotlinPlaceHolderStub<KtTypeReference>, KtTypeReference>
        get() = KtStubElementTypes.TYPE_REFERENCE

    override val userTypeType: KtStubElementType<out KotlinUserTypeStub, KtUserType>
        get() = KtStubElementTypes.USER_TYPE

    override val dynamicTypeType: KtStubElementType<out KotlinPlaceHolderStub<KtDynamicType>, KtDynamicType>
        get() = KtStubElementTypes.DYNAMIC_TYPE

    override val functionTypeType: KtStubElementType<out KotlinFunctionTypeStub, KtFunctionType>
        get() = KtStubElementTypes.FUNCTION_TYPE

    override val functionTypeReceiverType: KtStubElementType<out KotlinPlaceHolderStub<KtFunctionTypeReceiver>, KtFunctionTypeReceiver>
        get() = KtStubElementTypes.FUNCTION_TYPE_RECEIVER

    override val nullableTypeType: KtStubElementType<out KotlinPlaceHolderStub<KtNullableType>, KtNullableType>
        get() = KtStubElementTypes.NULLABLE_TYPE

    override val intersectionTypeType: KtStubElementType<out KotlinPlaceHolderStub<KtIntersectionType>, KtIntersectionType>
        get() = KtStubElementTypes.INTERSECTION_TYPE

    override val typeProjectionType: KtStubElementType<out KotlinTypeProjectionStub, KtTypeProjection>
        get() = KtStubElementTypes.TYPE_PROJECTION


    // Constants
    override val nullType: KtStubElementType<out KotlinConstantExpressionStub, KtConstantExpression>
        get() = KtStubElementTypes.NULL

    override val booleanConstantType: KtStubElementType<out KotlinConstantExpressionStub, KtConstantExpression>
        get() = KtStubElementTypes.BOOLEAN_CONSTANT

    override val floatConstantType: KtStubElementType<out KotlinConstantExpressionStub, KtConstantExpression>
        get() = KtStubElementTypes.FLOAT_CONSTANT

    override val characterConstantType: KtStubElementType<out KotlinConstantExpressionStub, KtConstantExpression>
        get() = KtStubElementTypes.CHARACTER_CONSTANT

    override val integerConstantType: KtStubElementType<out KotlinConstantExpressionStub, KtConstantExpression>
        get() = KtStubElementTypes.INTEGER_CONSTANT


    // String templates

    override val stringTemplateType: KtStubElementType<out KotlinPlaceHolderStub<KtStringTemplateExpression>, KtStringTemplateExpression>
        get() = KtStubElementTypes.STRING_TEMPLATE

    override val longStringTemplateEntryType: KtStubElementType<out KotlinBlockStringTemplateEntryStub, KtBlockStringTemplateEntry>
        get() = KtStubElementTypes.LONG_STRING_TEMPLATE_ENTRY

    override val shortStringTemplateEntryType: KtStubElementType<out KotlinPlaceHolderWithTextStub<KtSimpleNameStringTemplateEntry>, KtSimpleNameStringTemplateEntry>
        get() = KtStubElementTypes.SHORT_STRING_TEMPLATE_ENTRY

    override val literalStringTemplateEntryType: KtStubElementType<out KotlinPlaceHolderWithTextStub<KtLiteralStringTemplateEntry>, KtLiteralStringTemplateEntry>
        get() = KtStubElementTypes.LITERAL_STRING_TEMPLATE_ENTRY

    override val escapeStringTemplateEntryType: KtStubElementType<out KotlinPlaceHolderWithTextStub<KtEscapeStringTemplateEntry>, KtEscapeStringTemplateEntry>
        get() = KtStubElementTypes.ESCAPE_STRING_TEMPLATE_ENTRY

    override val stringInterpolationPrefixType: KtStubElementType<out KotlinStringInterpolationPrefixStub, KtStringInterpolationPrefix>
        get() = KtStubElementTypes.STRING_INTERPOLATION_PREFIX


    // Expressions

    override val blockExpressionType: IElementType = BlockExpressionElementType()

    override val lambdaExpressionType: IElementType = LambdaExpressionElementType()

    override val referenceExpressionType: KtStubElementType<out KotlinNameReferenceExpressionStub, KtNameReferenceExpression>
        get() = KtStubElementTypes.REFERENCE_EXPRESSION

    override val enumEntrySuperclassReferenceExpressionType: KtStubElementType<out KotlinEnumEntrySuperclassReferenceExpressionStub, KtEnumEntrySuperclassReferenceExpression>
        get() = KtStubElementTypes.ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION

    override val dotQualifiedExpressionType: KtStubElementType<out KotlinPlaceHolderStub<KtDotQualifiedExpression>, KtDotQualifiedExpression>
        get() = KtStubElementTypes.DOT_QUALIFIED_EXPRESSION

    override val callExpressionType: KtStubElementType<out KotlinPlaceHolderStub<KtCallExpression>, KtCallExpression>
        get() = KtStubElementTypes.CALL_EXPRESSION

    override val classLiteralExpressionType: KtStubElementType<out KotlinClassLiteralExpressionStub, KtClassLiteralExpression>
        get() = KtStubElementTypes.CLASS_LITERAL_EXPRESSION

    override val collectionLiteralExpressionType: KtStubElementType<out KotlinCollectionLiteralExpressionStub, KtCollectionLiteralExpression>
        get() = KtStubElementTypes.COLLECTION_LITERAL_EXPRESSION


    // Arguments
    override val typeArgumentListType: KtStubElementType<out KotlinPlaceHolderStub<KtTypeArgumentList>, KtTypeArgumentList>
        get() = KtStubElementTypes.TYPE_ARGUMENT_LIST

    override val valueArgumentListType: KtStubElementType<out KotlinPlaceHolderStub<KtValueArgumentList>, KtValueArgumentList>
        get() = KtStubElementTypes.VALUE_ARGUMENT_LIST

    override val valueArgumentType: KtStubElementType<out KotlinValueArgumentStub<KtValueArgument>, KtValueArgument>
        get() = KtStubElementTypes.VALUE_ARGUMENT

    override val contractEffectListType: KtStubElementType<out KotlinPlaceHolderStub<KtContractEffectList>, KtContractEffectList>
        get() = KtStubElementTypes.CONTRACT_EFFECT_LIST

    override val contractEffectType: KtStubElementType<out KotlinContractEffectStub, KtContractEffect>
        get() = KtStubElementTypes.CONTRACT_EFFECT

    override val lambdaArgumentType: KtStubElementType<out KotlinValueArgumentStub<KtLambdaArgument>, KtLambdaArgument>
        get() = KtStubElementTypes.LAMBDA_ARGUMENT

    override val valueArgumentNameType: KtStubElementType<out KotlinPlaceHolderStub<KtValueArgumentName>, KtValueArgumentName>
        get() = KtStubElementTypes.VALUE_ARGUMENT_NAME


    // Special

    override val packageDirectiveType: KtStubElementType<out KotlinPlaceHolderStub<KtPackageDirective>, KtPackageDirective>
        get() = KtStubElementTypes.PACKAGE_DIRECTIVE

    override val fileAnnotationListType: KtStubElementType<out KotlinPlaceHolderStub<KtFileAnnotationList>, KtFileAnnotationList>
        get() = KtStubElementTypes.FILE_ANNOTATION_LIST

    override val importListType: KtStubElementType<out KotlinPlaceHolderStub<KtImportList>, KtImportList>
        get() = KtStubElementTypes.IMPORT_LIST

    override val importDirectiveType: KtStubElementType<out KotlinImportDirectiveStub, KtImportDirective>
        get() = KtStubElementTypes.IMPORT_DIRECTIVE

    override val importAliasType: KtStubElementType<out KotlinImportAliasStub, KtImportAlias>
        get() = KtStubElementTypes.IMPORT_ALIAS

    override val scriptType: KtStubElementType<out KotlinScriptStub, KtScript>
        get() = KtStubElementTypes.SCRIPT


    // Code fragments

    override val expressionCodeFragmentType: ICodeFragmentElementType
        get() = KtStubElementTypes.EXPRESSION_CODE_FRAGMENT

    override val blockCodeFragmentType: ICodeFragmentElementType
        get() = KtStubElementTypes.BLOCK_CODE_FRAGMENT

    override val typeCodeFragmentType: ICodeFragmentElementType
        get() = KtStubElementTypes.TYPE_CODE_FRAGMENT


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