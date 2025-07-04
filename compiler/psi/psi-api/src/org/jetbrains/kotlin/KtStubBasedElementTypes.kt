/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import com.intellij.psi.impl.source.tree.ICodeFragmentElementType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.stubs.*
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType

@OptIn(KtImplementationDetail::class)
internal object KtStubBasedElementTypes {
    private val provider = KotlinElementTypeProvider.instance

    @JvmField
    val FILE: IFileElementType =
        provider.fileType


    // Classifiers

    @JvmField
    val CLASS: KtStubElementType<KotlinClassStub, KtClass> =
        provider.classType

    @JvmField
    val OBJECT_DECLARATION: KtStubElementType<KotlinObjectStub, KtObjectDeclaration> =
        provider.objectType

    @JvmField
    val TYPEALIAS: KtStubElementType<KotlinTypeAliasStub, KtTypeAlias> =
        provider.typeAliasType

    @JvmField
    val CLASS_BODY: KtStubElementType<KotlinPlaceHolderStub<KtClassBody>, KtClassBody> =
        provider.classBodyType


    // Initializers

    @JvmField
    val CLASS_INITIALIZER: KtStubElementType<KotlinPlaceHolderStub<KtClassInitializer>, KtClassInitializer> =
        provider.classInitializerType


    // Callables

    @JvmField
    val FUNCTION: KtStubElementType<KotlinFunctionStub, KtNamedFunction> =
        provider.functionType

    @JvmField
    val PROPERTY: KtStubElementType<KotlinPropertyStub, KtProperty> =
        provider.propertyType

    @JvmField
    val ENUM_ENTRY: KtStubElementType<KotlinClassStub, KtClass> =
        provider.enumEntryType

    @JvmField
    val PRIMARY_CONSTRUCTOR: KtStubElementType<KotlinConstructorStub<KtPrimaryConstructor>, KtPrimaryConstructor> =
        provider.primaryConstructorType

    @JvmField
    val SECONDARY_CONSTRUCTOR: KtStubElementType<KotlinConstructorStub<KtSecondaryConstructor>, KtSecondaryConstructor> =
        provider.secondaryConstructorType

    @JvmField
    val CONSTRUCTOR_CALLEE: KtStubElementType<KotlinPlaceHolderStub<KtConstructorCalleeExpression>, KtConstructorCalleeExpression> =
        provider.constructorCalleeType

    @JvmField
    val PROPERTY_ACCESSOR: KtStubElementType<KotlinPropertyAccessorStub, KtPropertyAccessor> =
        provider.propertyAccessorType

    @JvmField
    val BACKING_FIELD: KtStubElementType<KotlinBackingFieldStub, KtBackingField> =
        provider.backingFieldType

    @JvmField
    val INITIALIZER_LIST: KtStubElementType<KotlinPlaceHolderStub<KtInitializerList>, KtInitializerList> =
        provider.initializerListType


    // Value parameters

    @JvmField
    val VALUE_PARAMETER_LIST: KtStubElementType<KotlinPlaceHolderStub<KtParameterList>, KtParameterList> =
        provider.valueParameterListType

    @JvmField
    val VALUE_PARAMETER: KtStubElementType<KotlinParameterStub, KtParameter> =
        provider.valueParameterType

    @JvmField
    val CONTEXT_RECEIVER_LIST: KtStubElementType<KotlinPlaceHolderStub<KtContextReceiverList>, KtContextReceiverList> =
        provider.contextReceiverListType

    @JvmField
    val CONTEXT_RECEIVER: KtStubElementType<KotlinContextReceiverStub, KtContextReceiver> =
        provider.contextReceiverType


    // Type parameters

    @JvmField
    val TYPE_PARAMETER_LIST: KtStubElementType<KotlinPlaceHolderStub<KtTypeParameterList>, KtTypeParameterList> =
        provider.typeParameterListType

    @JvmField
    val TYPE_PARAMETER: KtStubElementType<KotlinTypeParameterStub, KtTypeParameter> =
        provider.typeParameterType

    @JvmField
    val TYPE_CONSTRAINT_LIST: KtStubElementType<KotlinPlaceHolderStub<KtTypeConstraintList>, KtTypeConstraintList> =
        provider.typeConstraintListType

    @JvmField
    val TYPE_CONSTRAINT: KtStubElementType<KotlinPlaceHolderStub<KtTypeConstraint>, KtTypeConstraint> =
        provider.typeConstraintType


    // Supertypes

    @JvmField
    val SUPER_TYPE_LIST: KtStubElementType<KotlinPlaceHolderStub<KtSuperTypeList>, KtSuperTypeList> =
        provider.superTypeListType

    @JvmField
    val DELEGATED_SUPER_TYPE_ENTRY: KtStubElementType<KotlinPlaceHolderStub<KtDelegatedSuperTypeEntry>, KtDelegatedSuperTypeEntry> =
        provider.delegatedSuperTypeEntryType

    @JvmField
    val SUPER_TYPE_CALL_ENTRY: KtStubElementType<KotlinPlaceHolderStub<KtSuperTypeCallEntry>, KtSuperTypeCallEntry> =
        provider.superTypeCallEntryType

    @JvmField
    val SUPER_TYPE_ENTRY: KtStubElementType<KotlinPlaceHolderStub<KtSuperTypeEntry>, KtSuperTypeEntry> =
        provider.superTypeEntryType


    // Modifiers and annotations

    @JvmField
    val MODIFIER_LIST: KtStubElementType<KotlinModifierListStub, KtDeclarationModifierList> =
        provider.modifierListType

    @JvmField
    val ANNOTATION: KtStubElementType<KotlinPlaceHolderStub<KtAnnotation>, KtAnnotation> =
        provider.annotationType

    @JvmField
    val ANNOTATION_ENTRY: KtStubElementType<KotlinAnnotationEntryStub, KtAnnotationEntry> =
        provider.annotationEntryType

    @JvmField
    val ANNOTATION_TARGET: KtStubElementType<KotlinAnnotationUseSiteTargetStub, KtAnnotationUseSiteTarget> =
        provider.annotationTargetType


    // Type references

    @JvmField
    val TYPE_REFERENCE: KtStubElementType<KotlinPlaceHolderStub<KtTypeReference>, KtTypeReference> =
        provider.typeReferenceType

    @JvmField
    val USER_TYPE: KtStubElementType<KotlinUserTypeStub, KtUserType> =
        provider.userTypeType

    @JvmField
    val DYNAMIC_TYPE: KtStubElementType<KotlinPlaceHolderStub<KtDynamicType>, KtDynamicType> =
        provider.dynamicTypeType

    @JvmField
    val FUNCTION_TYPE: KtStubElementType<KotlinFunctionTypeStub, KtFunctionType> =
        provider.functionTypeType

    @JvmField
    val FUNCTION_TYPE_RECEIVER: KtStubElementType<KotlinPlaceHolderStub<KtFunctionTypeReceiver>, KtFunctionTypeReceiver> =
        provider.functionTypeReceiverType

    @JvmField
    val NULLABLE_TYPE: KtStubElementType<KotlinPlaceHolderStub<KtNullableType>, KtNullableType> =
        provider.nullableTypeType

    @JvmField
    val INTERSECTION_TYPE: KtStubElementType<KotlinPlaceHolderStub<KtIntersectionType>, KtIntersectionType> =
        provider.intersectionTypeType

    @JvmField
    val TYPE_PROJECTION: KtStubElementType<KotlinTypeProjectionStub, KtTypeProjection> =
        provider.typeProjectionType


    // Constants

    @JvmField
    val NULL: KtStubElementType<KotlinConstantExpressionStub, KtConstantExpression> =
        provider.nullType

    @JvmField
    val BOOLEAN_CONSTANT: KtStubElementType<KotlinConstantExpressionStub, KtConstantExpression> =
        provider.booleanConstantType

    @JvmField
    val FLOAT_CONSTANT: KtStubElementType<KotlinConstantExpressionStub, KtConstantExpression> =
        provider.floatConstantType

    @JvmField
    val CHARACTER_CONSTANT: KtStubElementType<KotlinConstantExpressionStub, KtConstantExpression> =
        provider.characterConstantType

    @JvmField
    val INTEGER_CONSTANT: KtStubElementType<KotlinConstantExpressionStub, KtConstantExpression> =
        provider.integerConstantType


    // String templates

    @JvmField
    val STRING_TEMPLATE: KtStubElementType<KotlinPlaceHolderStub<KtStringTemplateExpression>, KtStringTemplateExpression> =
        provider.stringTemplateType

    @JvmField
    val LONG_STRING_TEMPLATE_ENTRY: KtStubElementType<KotlinBlockStringTemplateEntryStub, KtBlockStringTemplateEntry> =
        provider.longStringTemplateEntryType

    @JvmField
    val SHORT_STRING_TEMPLATE_ENTRY: KtStubElementType<KotlinPlaceHolderWithTextStub<KtSimpleNameStringTemplateEntry>, KtSimpleNameStringTemplateEntry> =
        provider.shortStringTemplateEntryType

    @JvmField
    val LITERAL_STRING_TEMPLATE_ENTRY: KtStubElementType<KotlinPlaceHolderWithTextStub<KtLiteralStringTemplateEntry>, KtLiteralStringTemplateEntry> =
        provider.literalStringTemplateEntryType

    @JvmField
    val ESCAPE_STRING_TEMPLATE_ENTRY: KtStubElementType<KotlinPlaceHolderWithTextStub<KtEscapeStringTemplateEntry>, KtEscapeStringTemplateEntry> =
        provider.escapeStringTemplateEntryType

    @JvmField
    val STRING_INTERPOLATION_PREFIX: KtStubElementType<KotlinStringInterpolationPrefixStub, KtStringInterpolationPrefix> =
        provider.stringInterpolationPrefixType


    // Expressions

    @JvmField
    val BLOCK: IElementType =
        provider.blockExpressionType

    @JvmField
    val LAMBDA_EXPRESSION: IElementType =
        provider.lambdaExpressionType

    @JvmField
    val REFERENCE_EXPRESSION: KtStubElementType<KotlinNameReferenceExpressionStub, KtNameReferenceExpression> =
        provider.referenceExpressionType

    @JvmField
    val ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION: KtStubElementType<KotlinEnumEntrySuperclassReferenceExpressionStub, KtEnumEntrySuperclassReferenceExpression> =
        provider.enumEntrySuperclassReferenceExpressionType

    @JvmField
    val DOT_QUALIFIED_EXPRESSION: KtStubElementType<KotlinPlaceHolderStub<KtDotQualifiedExpression>, KtDotQualifiedExpression> =
        provider.dotQualifiedExpressionType

    @JvmField
    val CALL_EXPRESSION: KtStubElementType<KotlinPlaceHolderStub<KtCallExpression>, KtCallExpression> =
        provider.callExpressionType

    @JvmField
    val CLASS_LITERAL_EXPRESSION: KtStubElementType<KotlinClassLiteralExpressionStub, KtClassLiteralExpression> =
        provider.classLiteralExpressionType

    @JvmField
    val COLLECTION_LITERAL_EXPRESSION: KtStubElementType<KotlinCollectionLiteralExpressionStub, KtCollectionLiteralExpression> =
        provider.collectionLiteralExpressionType


    // Arguments

    @JvmField
    val TYPE_ARGUMENT_LIST: KtStubElementType<KotlinPlaceHolderStub<KtTypeArgumentList>, KtTypeArgumentList> =
        provider.typeArgumentListType

    @JvmField
    val VALUE_ARGUMENT_LIST: KtStubElementType<KotlinPlaceHolderStub<KtValueArgumentList>, KtValueArgumentList> =
        provider.valueArgumentListType

    @JvmField
    val VALUE_ARGUMENT: KtStubElementType<KotlinValueArgumentStub<KtValueArgument>, KtValueArgument> =
        provider.valueArgumentType

    @JvmField
    val CONTRACT_EFFECT_LIST: KtStubElementType<KotlinPlaceHolderStub<KtContractEffectList>, KtContractEffectList> =
        provider.contractEffectListType

    @JvmField
    val CONTRACT_EFFECT: KtStubElementType<KotlinContractEffectStub, KtContractEffect> =
        provider.contractEffectType

    @JvmField
    val LAMBDA_ARGUMENT: KtStubElementType<KotlinValueArgumentStub<KtLambdaArgument>, KtLambdaArgument> =
        provider.lambdaArgumentType

    @JvmField
    val VALUE_ARGUMENT_NAME: KtStubElementType<KotlinPlaceHolderStub<KtValueArgumentName>, KtValueArgumentName> =
        provider.valueArgumentNameType


    // Special

    @JvmField
    val PACKAGE_DIRECTIVE: KtStubElementType<KotlinPlaceHolderStub<KtPackageDirective>, KtPackageDirective> =
        provider.packageDirectiveType

    @JvmField
    val FILE_ANNOTATION_LIST: KtStubElementType<KotlinPlaceHolderStub<KtFileAnnotationList>, KtFileAnnotationList> =
        provider.fileAnnotationListType

    @JvmField
    val IMPORT_LIST: KtStubElementType<KotlinPlaceHolderStub<KtImportList>, KtImportList> =
        provider.importListType

    @JvmField
    val IMPORT_DIRECTIVE: KtStubElementType<KotlinImportDirectiveStub, KtImportDirective> =
        provider.importDirectiveType

    @JvmField
    val IMPORT_ALIAS: KtStubElementType<KotlinImportAliasStub, KtImportAlias> =
        provider.importAliasType

    @JvmField
    val SCRIPT: KtStubElementType<KotlinScriptStub, KtScript> =
        provider.scriptType


    // Code fragments

    @JvmField
    val EXPRESSION_CODE_FRAGMENT: ICodeFragmentElementType =
        provider.expressionCodeFragmentType

    @JvmField
    val BLOCK_CODE_FRAGMENT: ICodeFragmentElementType =
        provider.blockCodeFragmentType

    @JvmField
    val TYPE_CODE_FRAGMENT: ICodeFragmentElementType =
        provider.typeCodeFragmentType
}