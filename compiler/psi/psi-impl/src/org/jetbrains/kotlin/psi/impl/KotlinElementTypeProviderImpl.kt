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

    override val classType: KtStubElementType<out KotlinClassStub, KtClass> =
        KtStubElementTypes.CLASS

    override val objectType: KtStubElementType<out KotlinObjectStub, KtObjectDeclaration> =
        KtStubElementTypes.OBJECT_DECLARATION

    override val typeAliasType: KtStubElementType<out KotlinTypeAliasStub, KtTypeAlias> =
        KtStubElementTypes.TYPEALIAS

    override val classBodyType: KtStubElementType<out KotlinPlaceHolderStub<KtClassBody>, KtClassBody> =
        KtStubElementTypes.CLASS_BODY


    // Initializers

    override val classInitializerType: KtStubElementType<out KotlinPlaceHolderStub<KtClassInitializer>, KtClassInitializer> =
        KtStubElementTypes.CLASS_INITIALIZER

    override val scriptInitializerType: KtStubElementType<out KotlinPlaceHolderStub<KtScriptInitializer>, KtScriptInitializer> =
        KtStubElementTypes.SCRIPT_INITIALIZER

    // Callables

    override val functionType: KtStubElementType<out KotlinFunctionStub, KtNamedFunction> =
        KtStubElementTypes.FUNCTION

    override val propertyType: KtStubElementType<out KotlinPropertyStub, KtProperty> =
        KtStubElementTypes.PROPERTY

    override val enumEntryType: KtStubElementType<out KotlinClassStub, KtClass> =
        KtStubElementTypes.ENUM_ENTRY

    override val primaryConstructorType: KtStubElementType<out KotlinConstructorStub<KtPrimaryConstructor>, KtPrimaryConstructor> =
        KtStubElementTypes.PRIMARY_CONSTRUCTOR

    override val secondaryConstructorType: KtStubElementType<out KotlinConstructorStub<KtSecondaryConstructor>, KtSecondaryConstructor> =
        KtStubElementTypes.SECONDARY_CONSTRUCTOR

    override val constructorCalleeType: KtStubElementType<out KotlinPlaceHolderStub<KtConstructorCalleeExpression>, KtConstructorCalleeExpression> =
        KtStubElementTypes.CONSTRUCTOR_CALLEE

    override val propertyAccessorType: KtStubElementType<out KotlinPropertyAccessorStub, KtPropertyAccessor> =
        KtStubElementTypes.PROPERTY_ACCESSOR

    override val backingFieldType: KtStubElementType<out KotlinBackingFieldStub, KtBackingField> =
        KtStubElementTypes.BACKING_FIELD

    override val initializerListType: KtStubElementType<out KotlinPlaceHolderStub<KtInitializerList>, KtInitializerList> =
        KtStubElementTypes.INITIALIZER_LIST


    // Value parameters
    override val valueParameterListType: KtStubElementType<out KotlinPlaceHolderStub<KtParameterList>, KtParameterList> =
        KtStubElementTypes.VALUE_PARAMETER_LIST

    override val valueParameterType: KtStubElementType<out KotlinParameterStub, KtParameter> =
        KtStubElementTypes.VALUE_PARAMETER

    override val contextReceiverListType: KtStubElementType<out KotlinPlaceHolderStub<KtContextReceiverList>, KtContextReceiverList> =
        KtStubElementTypes.CONTEXT_RECEIVER_LIST

    override val contextReceiverType: KtStubElementType<out KotlinContextReceiverStub, KtContextReceiver> =
        KtStubElementTypes.CONTEXT_RECEIVER


    // Type parameters
    override val typeParameterListType: KtStubElementType<out KotlinPlaceHolderStub<KtTypeParameterList>, KtTypeParameterList> =
        KtStubElementTypes.TYPE_PARAMETER_LIST

    override val typeParameterType: KtStubElementType<out KotlinTypeParameterStub, KtTypeParameter> =
        KtStubElementTypes.TYPE_PARAMETER

    override val typeConstraintListType: KtStubElementType<out KotlinPlaceHolderStub<KtTypeConstraintList>, KtTypeConstraintList> =
        KtStubElementTypes.TYPE_CONSTRAINT_LIST

    override val typeConstraintType: KtStubElementType<out KotlinPlaceHolderStub<KtTypeConstraint>, KtTypeConstraint> =
        KtStubElementTypes.TYPE_CONSTRAINT


    // Supertypes
    override val superTypeListType: KtStubElementType<out KotlinPlaceHolderStub<KtSuperTypeList>, KtSuperTypeList> =
        KtStubElementTypes.SUPER_TYPE_LIST

    override val delegatedSuperTypeEntryType: KtStubElementType<out KotlinPlaceHolderStub<KtDelegatedSuperTypeEntry>, KtDelegatedSuperTypeEntry> =
        KtStubElementTypes.DELEGATED_SUPER_TYPE_ENTRY

    override val superTypeCallEntryType: KtStubElementType<out KotlinPlaceHolderStub<KtSuperTypeCallEntry>, KtSuperTypeCallEntry> =
        KtStubElementTypes.SUPER_TYPE_CALL_ENTRY

    override val superTypeEntryType: KtStubElementType<out KotlinPlaceHolderStub<KtSuperTypeEntry>, KtSuperTypeEntry> =
        KtStubElementTypes.SUPER_TYPE_ENTRY


    // Modifiers and annotations

    override val modifierListType: KtStubElementType<out KotlinModifierListStub, KtDeclarationModifierList> =
        KtStubElementTypes.MODIFIER_LIST

    override val annotationType: KtStubElementType<out KotlinPlaceHolderStub<KtAnnotation>, KtAnnotation> =
        KtStubElementTypes.ANNOTATION

    override val annotationEntryType: KtStubElementType<out KotlinAnnotationEntryStub, KtAnnotationEntry> =
        KtStubElementTypes.ANNOTATION_ENTRY

    override val annotationTargetType: KtStubElementType<out KotlinAnnotationUseSiteTargetStub, KtAnnotationUseSiteTarget> =
        KtStubElementTypes.ANNOTATION_TARGET


    // Type references

    override val typeReferenceType: KtStubElementType<out KotlinPlaceHolderStub<KtTypeReference>, KtTypeReference> =
        KtStubElementTypes.TYPE_REFERENCE

    override val userTypeType: KtStubElementType<out KotlinUserTypeStub, KtUserType> =
        KtStubElementTypes.USER_TYPE

    override val dynamicTypeType: KtStubElementType<out KotlinPlaceHolderStub<KtDynamicType>, KtDynamicType> =
        KtStubElementTypes.DYNAMIC_TYPE

    override val functionTypeType: KtStubElementType<out KotlinFunctionTypeStub, KtFunctionType> =
        KtStubElementTypes.FUNCTION_TYPE

    override val functionTypeReceiverType: KtStubElementType<out KotlinPlaceHolderStub<KtFunctionTypeReceiver>, KtFunctionTypeReceiver> =
        KtStubElementTypes.FUNCTION_TYPE_RECEIVER

    override val nullableTypeType: KtStubElementType<out KotlinPlaceHolderStub<KtNullableType>, KtNullableType> =
        KtStubElementTypes.NULLABLE_TYPE

    override val intersectionTypeType: KtStubElementType<out KotlinPlaceHolderStub<KtIntersectionType>, KtIntersectionType> =
        KtStubElementTypes.INTERSECTION_TYPE

    override val typeProjectionType: KtStubElementType<out KotlinTypeProjectionStub, KtTypeProjection> =
        KtStubElementTypes.TYPE_PROJECTION


    // Constants
    override val nullType: KtStubElementType<out KotlinConstantExpressionStub, KtConstantExpression> =
        KtStubElementTypes.NULL

    override val booleanConstantType: KtStubElementType<out KotlinConstantExpressionStub, KtConstantExpression> =
        KtStubElementTypes.BOOLEAN_CONSTANT

    override val floatConstantType: KtStubElementType<out KotlinConstantExpressionStub, KtConstantExpression> =
        KtStubElementTypes.FLOAT_CONSTANT

    override val characterConstantType: KtStubElementType<out KotlinConstantExpressionStub, KtConstantExpression> =
        KtStubElementTypes.CHARACTER_CONSTANT

    override val integerConstantType: KtStubElementType<out KotlinConstantExpressionStub, KtConstantExpression> =
        KtStubElementTypes.INTEGER_CONSTANT


    // String templates

    override val stringTemplateType: KtStubElementType<out KotlinPlaceHolderStub<KtStringTemplateExpression>, KtStringTemplateExpression> =
        KtStubElementTypes.STRING_TEMPLATE

    override val longStringTemplateEntryType: KtStubElementType<out KotlinBlockStringTemplateEntryStub, KtBlockStringTemplateEntry> =
        KtStubElementTypes.LONG_STRING_TEMPLATE_ENTRY

    override val shortStringTemplateEntryType: KtStubElementType<out KotlinPlaceHolderWithTextStub<KtSimpleNameStringTemplateEntry>, KtSimpleNameStringTemplateEntry> =
        KtStubElementTypes.SHORT_STRING_TEMPLATE_ENTRY

    override val literalStringTemplateEntryType: KtStubElementType<out KotlinPlaceHolderWithTextStub<KtLiteralStringTemplateEntry>, KtLiteralStringTemplateEntry> =
        KtStubElementTypes.LITERAL_STRING_TEMPLATE_ENTRY

    override val escapeStringTemplateEntryType: KtStubElementType<out KotlinPlaceHolderWithTextStub<KtEscapeStringTemplateEntry>, KtEscapeStringTemplateEntry> =
        KtStubElementTypes.ESCAPE_STRING_TEMPLATE_ENTRY

    override val stringInterpolationPrefixType: KtStubElementType<out KotlinStringInterpolationPrefixStub, KtStringInterpolationPrefix> =
        KtStubElementTypes.STRING_INTERPOLATION_PREFIX


    // Expressions

    override val blockExpressionType =
        BlockExpressionElementType()

    override val lambdaExpressionType =
        LambdaExpressionElementType()

    override val referenceExpressionType: KtStubElementType<out KotlinNameReferenceExpressionStub, KtNameReferenceExpression> =
        KtStubElementTypes.REFERENCE_EXPRESSION

    override val enumEntrySuperclassReferenceExpressionType: KtStubElementType<out KotlinEnumEntrySuperclassReferenceExpressionStub, KtEnumEntrySuperclassReferenceExpression> =
        KtStubElementTypes.ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION

    override val dotQualifiedExpressionType: KtStubElementType<out KotlinPlaceHolderStub<KtDotQualifiedExpression>, KtDotQualifiedExpression> =
        KtStubElementTypes.DOT_QUALIFIED_EXPRESSION

    override val callExpressionType: KtStubElementType<out KotlinPlaceHolderStub<KtCallExpression>, KtCallExpression> =
        KtStubElementTypes.CALL_EXPRESSION

    override val classLiteralExpressionType: KtStubElementType<out KotlinClassLiteralExpressionStub, KtClassLiteralExpression> =
        KtStubElementTypes.CLASS_LITERAL_EXPRESSION

    override val collectionLiteralExpressionType: KtStubElementType<out KotlinCollectionLiteralExpressionStub, KtCollectionLiteralExpression> =
        KtStubElementTypes.COLLECTION_LITERAL_EXPRESSION


    // Arguments
    override val typeArgumentListType: KtStubElementType<out KotlinPlaceHolderStub<KtTypeArgumentList>, KtTypeArgumentList> =
        KtStubElementTypes.TYPE_ARGUMENT_LIST

    override val valueArgumentListType: KtStubElementType<out KotlinPlaceHolderStub<KtValueArgumentList>, KtValueArgumentList> =
        KtStubElementTypes.VALUE_ARGUMENT_LIST

    override val valueArgumentType: KtStubElementType<out KotlinValueArgumentStub<KtValueArgument>, KtValueArgument> =
        KtStubElementTypes.VALUE_ARGUMENT

    override val contractEffectListType: KtStubElementType<out KotlinPlaceHolderStub<KtContractEffectList>, KtContractEffectList> =
        KtStubElementTypes.CONTRACT_EFFECT_LIST

    override val contractEffectType: KtStubElementType<out KotlinContractEffectStub, KtContractEffect> =
        KtStubElementTypes.CONTRACT_EFFECT

    override val lambdaArgumentType: KtStubElementType<out KotlinValueArgumentStub<KtLambdaArgument>, KtLambdaArgument> =
        KtStubElementTypes.LAMBDA_ARGUMENT

    override val valueArgumentNameType: KtStubElementType<out KotlinPlaceHolderStub<KtValueArgumentName>, KtValueArgumentName> =
        KtStubElementTypes.VALUE_ARGUMENT_NAME


    // Special

    override val packageDirectiveType: KtStubElementType<out KotlinPlaceHolderStub<KtPackageDirective>, KtPackageDirective> =
        KtStubElementTypes.PACKAGE_DIRECTIVE

    override val fileAnnotationListType: KtStubElementType<out KotlinPlaceHolderStub<KtFileAnnotationList>, KtFileAnnotationList> =
        KtStubElementTypes.FILE_ANNOTATION_LIST

    override val importListType: KtStubElementType<out KotlinPlaceHolderStub<KtImportList>, KtImportList> =
        KtStubElementTypes.IMPORT_LIST

    override val importDirectiveType: KtStubElementType<out KotlinImportDirectiveStub, KtImportDirective> =
        KtStubElementTypes.IMPORT_DIRECTIVE

    override val importAliasType: KtStubElementType<out KotlinImportAliasStub, KtImportAlias> =
        KtStubElementTypes.IMPORT_ALIAS

    override val scriptType: KtStubElementType<out KotlinScriptStub, KtScript> =
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