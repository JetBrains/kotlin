/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.psi.impl.source.tree.ICodeFragmentElementType
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

    override val classType: KtStubElementType<KotlinClassStub, KtClass> =
        KtStubElementTypes.CLASS

    override val objectType: KtStubElementType<KotlinObjectStub, KtObjectDeclaration> =
        KtStubElementTypes.OBJECT_DECLARATION

    override val typeAliasType: KtStubElementType<KotlinTypeAliasStub, KtTypeAlias> =
        KtStubElementTypes.TYPEALIAS

    override val classBodyType: KtStubElementType<KotlinPlaceHolderStub<KtClassBody>, KtClassBody> =
        KtStubElementTypes.CLASS_BODY


    // Initializers

    override val classInitializerType: KtStubElementType<KotlinPlaceHolderStub<KtClassInitializer>, KtClassInitializer> =
        KtStubElementTypes.CLASS_INITIALIZER


    // Callables

    override val functionType: KtStubElementType<KotlinFunctionStub, KtNamedFunction> =
        KtStubElementTypes.FUNCTION

    override val propertyType: KtStubElementType<KotlinPropertyStub, KtProperty> =
        KtStubElementTypes.PROPERTY

    override val enumEntryType: KtStubElementType<KotlinClassStub, KtClass> =
        KtStubElementTypes.ENUM_ENTRY

    override val primaryConstructorType: KtStubElementType<KotlinConstructorStub<KtPrimaryConstructor>, KtPrimaryConstructor> =
        KtStubElementTypes.PRIMARY_CONSTRUCTOR

    override val secondaryConstructorType: KtStubElementType<KotlinConstructorStub<KtSecondaryConstructor>, KtSecondaryConstructor> =
        KtStubElementTypes.SECONDARY_CONSTRUCTOR

    override val constructorCalleeType: KtStubElementType<KotlinPlaceHolderStub<KtConstructorCalleeExpression>, KtConstructorCalleeExpression> =
        KtStubElementTypes.CONSTRUCTOR_CALLEE

    override val propertyAccessorType: KtStubElementType<KotlinPropertyAccessorStub, KtPropertyAccessor> =
        KtStubElementTypes.PROPERTY_ACCESSOR

    override val backingFieldType: KtStubElementType<KotlinBackingFieldStub, KtBackingField> =
        KtStubElementTypes.BACKING_FIELD

    override val initializerListType: KtStubElementType<KotlinPlaceHolderStub<KtInitializerList>, KtInitializerList> =
        KtStubElementTypes.INITIALIZER_LIST


    // Value parameters
    override val valueParameterListType: KtStubElementType<KotlinPlaceHolderStub<KtParameterList>, KtParameterList> =
        KtStubElementTypes.VALUE_PARAMETER_LIST

    override val valueParameterType: KtStubElementType<KotlinParameterStub, KtParameter> =
        KtStubElementTypes.VALUE_PARAMETER

    override val contextReceiverListType: KtStubElementType<KotlinPlaceHolderStub<KtContextReceiverList>, KtContextReceiverList> =
        KtStubElementTypes.CONTEXT_RECEIVER_LIST

    override val contextReceiverType: KtStubElementType<KotlinContextReceiverStub, KtContextReceiver> =
        KtStubElementTypes.CONTEXT_RECEIVER


    // Type parameters
    override val typeParameterListType: KtStubElementType<KotlinPlaceHolderStub<KtTypeParameterList>, KtTypeParameterList> =
        KtStubElementTypes.TYPE_PARAMETER_LIST

    override val typeParameterType: KtStubElementType<KotlinTypeParameterStub, KtTypeParameter> =
        KtStubElementTypes.TYPE_PARAMETER

    override val typeConstraintListType: KtStubElementType<KotlinPlaceHolderStub<KtTypeConstraintList>, KtTypeConstraintList> =
        KtStubElementTypes.TYPE_CONSTRAINT_LIST

    override val typeConstraintType: KtStubElementType<KotlinPlaceHolderStub<KtTypeConstraint>, KtTypeConstraint> =
        KtStubElementTypes.TYPE_CONSTRAINT


    // Supertypes
    override val superTypeListType: KtStubElementType<KotlinPlaceHolderStub<KtSuperTypeList>, KtSuperTypeList> =
        KtStubElementTypes.SUPER_TYPE_LIST

    override val delegatedSuperTypeEntryType: KtStubElementType<KotlinPlaceHolderStub<KtDelegatedSuperTypeEntry>, KtDelegatedSuperTypeEntry> =
        KtStubElementTypes.DELEGATED_SUPER_TYPE_ENTRY

    override val superTypeCallEntryType: KtStubElementType<KotlinPlaceHolderStub<KtSuperTypeCallEntry>, KtSuperTypeCallEntry> =
        KtStubElementTypes.SUPER_TYPE_CALL_ENTRY

    override val superTypeEntryType: KtStubElementType<KotlinPlaceHolderStub<KtSuperTypeEntry>, KtSuperTypeEntry> =
        KtStubElementTypes.SUPER_TYPE_ENTRY


    // Modifiers and annotations

    override val modifierListType: KtStubElementType<KotlinModifierListStub, KtDeclarationModifierList> =
        KtStubElementTypes.MODIFIER_LIST

    override val annotationType: KtStubElementType<KotlinPlaceHolderStub<KtAnnotation>, KtAnnotation> =
        KtStubElementTypes.ANNOTATION

    override val annotationEntryType: KtStubElementType<KotlinAnnotationEntryStub, KtAnnotationEntry> =
        KtStubElementTypes.ANNOTATION_ENTRY

    override val annotationTargetType: KtStubElementType<KotlinAnnotationUseSiteTargetStub, KtAnnotationUseSiteTarget> =
        KtStubElementTypes.ANNOTATION_TARGET


    // Type references

    override val typeReferenceType: KtStubElementType<KotlinPlaceHolderStub<KtTypeReference>, KtTypeReference> =
        KtStubElementTypes.TYPE_REFERENCE

    override val userTypeType: KtStubElementType<KotlinUserTypeStub, KtUserType> =
        KtStubElementTypes.USER_TYPE

    override val dynamicTypeType: KtStubElementType<KotlinPlaceHolderStub<KtDynamicType>, KtDynamicType> =
        KtStubElementTypes.DYNAMIC_TYPE

    override val functionTypeType: KtStubElementType<KotlinFunctionTypeStub, KtFunctionType> =
        KtStubElementTypes.FUNCTION_TYPE

    override val functionTypeReceiverType: KtStubElementType<KotlinPlaceHolderStub<KtFunctionTypeReceiver>, KtFunctionTypeReceiver> =
        KtStubElementTypes.FUNCTION_TYPE_RECEIVER

    override val nullableTypeType: KtStubElementType<KotlinPlaceHolderStub<KtNullableType>, KtNullableType> =
        KtStubElementTypes.NULLABLE_TYPE

    override val intersectionTypeType: KtStubElementType<KotlinPlaceHolderStub<KtIntersectionType>, KtIntersectionType> =
        KtStubElementTypes.INTERSECTION_TYPE

    override val typeProjectionType: KtStubElementType<KotlinTypeProjectionStub, KtTypeProjection> =
        KtStubElementTypes.TYPE_PROJECTION


    // Constants
    override val nullType: KtStubElementType<KotlinConstantExpressionStub, KtConstantExpression> =
        KtStubElementTypes.NULL

    override val booleanConstantType: KtStubElementType<KotlinConstantExpressionStub, KtConstantExpression> =
        KtStubElementTypes.BOOLEAN_CONSTANT

    override val floatConstantType: KtStubElementType<KotlinConstantExpressionStub, KtConstantExpression> =
        KtStubElementTypes.FLOAT_CONSTANT

    override val characterConstantType: KtStubElementType<KotlinConstantExpressionStub, KtConstantExpression> =
        KtStubElementTypes.CHARACTER_CONSTANT

    override val integerConstantType: KtStubElementType<KotlinConstantExpressionStub, KtConstantExpression> =
        KtStubElementTypes.INTEGER_CONSTANT


    // String templates

    override val stringTemplateType: KtStubElementType<KotlinPlaceHolderStub<KtStringTemplateExpression>, KtStringTemplateExpression> =
        KtStubElementTypes.STRING_TEMPLATE

    override val longStringTemplateEntryType: KtStubElementType<KotlinBlockStringTemplateEntryStub, KtBlockStringTemplateEntry> =
        KtStubElementTypes.LONG_STRING_TEMPLATE_ENTRY

    override val shortStringTemplateEntryType: KtStubElementType<KotlinPlaceHolderWithTextStub<KtSimpleNameStringTemplateEntry>, KtSimpleNameStringTemplateEntry> =
        KtStubElementTypes.SHORT_STRING_TEMPLATE_ENTRY

    override val literalStringTemplateEntryType: KtStubElementType<KotlinPlaceHolderWithTextStub<KtLiteralStringTemplateEntry>, KtLiteralStringTemplateEntry> =
        KtStubElementTypes.LITERAL_STRING_TEMPLATE_ENTRY

    override val escapeStringTemplateEntryType: KtStubElementType<KotlinPlaceHolderWithTextStub<KtEscapeStringTemplateEntry>, KtEscapeStringTemplateEntry> =
        KtStubElementTypes.ESCAPE_STRING_TEMPLATE_ENTRY

    override val stringInterpolationPrefixType: KtStubElementType<KotlinStringInterpolationPrefixStub, KtStringInterpolationPrefix> =
        KtStubElementTypes.STRING_INTERPOLATION_PREFIX


    // Expressions

    override val blockExpressionType =
        BlockExpressionElementType()

    override val lambdaExpressionType =
        LambdaExpressionElementType()

    override val referenceExpressionType: KtStubElementType<KotlinNameReferenceExpressionStub, KtNameReferenceExpression> =
        KtStubElementTypes.REFERENCE_EXPRESSION

    override val enumEntrySuperclassReferenceExpressionType: KtStubElementType<KotlinEnumEntrySuperclassReferenceExpressionStub, KtEnumEntrySuperclassReferenceExpression> =
        KtStubElementTypes.ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION

    override val dotQualifiedExpressionType: KtStubElementType<KotlinPlaceHolderStub<KtDotQualifiedExpression>, KtDotQualifiedExpression> =
        KtStubElementTypes.DOT_QUALIFIED_EXPRESSION

    override val callExpressionType: KtStubElementType<KotlinPlaceHolderStub<KtCallExpression>, KtCallExpression> =
        KtStubElementTypes.CALL_EXPRESSION

    override val classLiteralExpressionType: KtStubElementType<KotlinClassLiteralExpressionStub, KtClassLiteralExpression> =
        KtStubElementTypes.CLASS_LITERAL_EXPRESSION

    override val collectionLiteralExpressionType: KtStubElementType<KotlinCollectionLiteralExpressionStub, KtCollectionLiteralExpression> =
        KtStubElementTypes.COLLECTION_LITERAL_EXPRESSION


    // Arguments
    override val typeArgumentListType: KtStubElementType<KotlinPlaceHolderStub<KtTypeArgumentList>, KtTypeArgumentList> =
        KtStubElementTypes.TYPE_ARGUMENT_LIST

    override val valueArgumentListType: KtStubElementType<KotlinPlaceHolderStub<KtValueArgumentList>, KtValueArgumentList> =
        KtStubElementTypes.VALUE_ARGUMENT_LIST

    override val valueArgumentType: KtStubElementType<KotlinValueArgumentStub<KtValueArgument>, KtValueArgument> =
        KtStubElementTypes.VALUE_ARGUMENT

    override val contractEffectListType: KtStubElementType<KotlinPlaceHolderStub<KtContractEffectList>, KtContractEffectList> =
        KtStubElementTypes.CONTRACT_EFFECT_LIST

    override val contractEffectType: KtStubElementType<KotlinContractEffectStub, KtContractEffect> =
        KtStubElementTypes.CONTRACT_EFFECT

    override val lambdaArgumentType: KtStubElementType<KotlinValueArgumentStub<KtLambdaArgument>, KtLambdaArgument> =
        KtStubElementTypes.LAMBDA_ARGUMENT

    override val valueArgumentNameType: KtStubElementType<KotlinPlaceHolderStub<KtValueArgumentName>, KtValueArgumentName> =
        KtStubElementTypes.VALUE_ARGUMENT_NAME


    // Special

    override val packageDirectiveType: KtStubElementType<KotlinPlaceHolderStub<KtPackageDirective>, KtPackageDirective> =
        KtStubElementTypes.PACKAGE_DIRECTIVE

    override val fileAnnotationListType: KtStubElementType<KotlinPlaceHolderStub<KtFileAnnotationList>, KtFileAnnotationList> =
        KtStubElementTypes.FILE_ANNOTATION_LIST

    override val importListType: KtStubElementType<KotlinPlaceHolderStub<KtImportList>, KtImportList> =
        KtStubElementTypes.IMPORT_LIST

    override val importDirectiveType: KtStubElementType<KotlinImportDirectiveStub, KtImportDirective> =
        KtStubElementTypes.IMPORT_DIRECTIVE

    override val importAliasType: KtStubElementType<KotlinImportAliasStub, KtImportAlias> =
        KtStubElementTypes.IMPORT_ALIAS

    override val scriptType: KtStubElementType<KotlinScriptStub, KtScript> =
        KtStubElementTypes.SCRIPT


    // Code fragments

    override val expressionCodeFragmentType: ICodeFragmentElementType =
        KtExpressionCodeFragmentType()

    override val blockCodeFragmentType: ICodeFragmentElementType =
        KtBlockCodeFragmentType()

    override val typeCodeFragmentType: ICodeFragmentElementType =
        KtTypeCodeFragmentType()


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

            override fun createNode(text: CharSequence?): ASTNode? {
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