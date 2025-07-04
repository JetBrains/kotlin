/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import com.intellij.psi.impl.source.tree.ICodeFragmentElementType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.ILazyParseableElementType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.stubs.*
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType

@KtImplementationDetail
interface KotlinElementTypeProvider {
    @KtImplementationDetail
    companion object {
        private const val IMPL = "org.jetbrains.kotlin.psi.impl.KotlinElementTypeProviderImpl"

        @JvmStatic
        val instance: KotlinElementTypeProvider by lazy(LazyThreadSafetyMode.PUBLICATION) {
            try {
                // This is certainly very hacky.
                // However, IJ services cannot be used here as the platform disallows using services from class initializers.
                // In the future, KT-77985 might eliminate the need for this provider.
                val implClass = Class.forName(IMPL)
                implClass.getDeclaredField("INSTANCE").get(null) as KotlinElementTypeProvider
            } catch (e: ClassNotFoundException) {
                throw IllegalStateException("KotlinElementTypeProvider implementation not found: $IMPL", e)
            }
        }
    }

    val fileType: IFileElementType

    // Classifiers
    val classType: KtStubElementType<KotlinClassStub, KtClass>
    val objectType: KtStubElementType<KotlinObjectStub, KtObjectDeclaration>
    val typeAliasType: KtStubElementType<KotlinTypeAliasStub, KtTypeAlias>
    val classBodyType: KtStubElementType<KotlinPlaceHolderStub<KtClassBody>, KtClassBody>

    // Initializers
    val classInitializerType: KtStubElementType<KotlinPlaceHolderStub<KtClassInitializer>, KtClassInitializer>

    // Callables
    val functionType: KtStubElementType<KotlinFunctionStub, KtNamedFunction>
    val propertyType: KtStubElementType<KotlinPropertyStub, KtProperty>
    val enumEntryType: KtStubElementType<KotlinClassStub, KtClass>
    val primaryConstructorType: KtStubElementType<KotlinConstructorStub<KtPrimaryConstructor>, KtPrimaryConstructor>
    val secondaryConstructorType: KtStubElementType<KotlinConstructorStub<KtSecondaryConstructor>, KtSecondaryConstructor>
    val constructorCalleeType: KtStubElementType<KotlinPlaceHolderStub<KtConstructorCalleeExpression>, KtConstructorCalleeExpression>
    val propertyAccessorType: KtStubElementType<KotlinPropertyAccessorStub, KtPropertyAccessor>
    val backingFieldType: KtStubElementType<KotlinBackingFieldStub, KtBackingField>
    val initializerListType: KtStubElementType<KotlinPlaceHolderStub<KtInitializerList>, KtInitializerList>

    // Value parameters
    val valueParameterListType: KtStubElementType<KotlinPlaceHolderStub<KtParameterList>, KtParameterList>
    val valueParameterType: KtStubElementType<KotlinParameterStub, KtParameter>
    val contextReceiverListType: KtStubElementType<KotlinPlaceHolderStub<KtContextReceiverList>, KtContextReceiverList>
    val contextReceiverType: KtStubElementType<KotlinContextReceiverStub, KtContextReceiver>

    // Type parameters
    val typeParameterListType: KtStubElementType<KotlinPlaceHolderStub<KtTypeParameterList>, KtTypeParameterList>
    val typeParameterType: KtStubElementType<KotlinTypeParameterStub, KtTypeParameter>
    val typeConstraintListType: KtStubElementType<KotlinPlaceHolderStub<KtTypeConstraintList>, KtTypeConstraintList>
    val typeConstraintType: KtStubElementType<KotlinPlaceHolderStub<KtTypeConstraint>, KtTypeConstraint>

    // Supertypes
    val superTypeListType: KtStubElementType<KotlinPlaceHolderStub<KtSuperTypeList>, KtSuperTypeList>
    val delegatedSuperTypeEntryType: KtStubElementType<KotlinPlaceHolderStub<KtDelegatedSuperTypeEntry>, KtDelegatedSuperTypeEntry>
    val superTypeCallEntryType: KtStubElementType<KotlinPlaceHolderStub<KtSuperTypeCallEntry>, KtSuperTypeCallEntry>
    val superTypeEntryType: KtStubElementType<KotlinPlaceHolderStub<KtSuperTypeEntry>, KtSuperTypeEntry>

    // Modifiers and annotations
    val modifierListType: KtStubElementType<KotlinModifierListStub, KtDeclarationModifierList>
    val annotationType: KtStubElementType<KotlinPlaceHolderStub<KtAnnotation>, KtAnnotation>
    val annotationEntryType: KtStubElementType<KotlinAnnotationEntryStub, KtAnnotationEntry>
    val annotationTargetType: KtStubElementType<KotlinAnnotationUseSiteTargetStub, KtAnnotationUseSiteTarget>

    // Type references
    val typeReferenceType: KtStubElementType<KotlinPlaceHolderStub<KtTypeReference>, KtTypeReference>
    val userTypeType: KtStubElementType<KotlinUserTypeStub, KtUserType>
    val dynamicTypeType: KtStubElementType<KotlinPlaceHolderStub<KtDynamicType>, KtDynamicType>
    val functionTypeType: KtStubElementType<KotlinFunctionTypeStub, KtFunctionType>
    val functionTypeReceiverType: KtStubElementType<KotlinPlaceHolderStub<KtFunctionTypeReceiver>, KtFunctionTypeReceiver>
    val nullableTypeType: KtStubElementType<KotlinPlaceHolderStub<KtNullableType>, KtNullableType>
    val intersectionTypeType: KtStubElementType<KotlinPlaceHolderStub<KtIntersectionType>, KtIntersectionType>
    val typeProjectionType: KtStubElementType<KotlinTypeProjectionStub, KtTypeProjection>

    // Constants
    val nullType: KtStubElementType<KotlinConstantExpressionStub, KtConstantExpression>
    val booleanConstantType: KtStubElementType<KotlinConstantExpressionStub, KtConstantExpression>
    val floatConstantType: KtStubElementType<KotlinConstantExpressionStub, KtConstantExpression>
    val characterConstantType: KtStubElementType<KotlinConstantExpressionStub, KtConstantExpression>
    val integerConstantType: KtStubElementType<KotlinConstantExpressionStub, KtConstantExpression>

    // String templates
    val stringTemplateType: KtStubElementType<KotlinPlaceHolderStub<KtStringTemplateExpression>, KtStringTemplateExpression>
    val longStringTemplateEntryType: KtStubElementType<KotlinBlockStringTemplateEntryStub, KtBlockStringTemplateEntry>
    val shortStringTemplateEntryType: KtStubElementType<KotlinPlaceHolderWithTextStub<KtSimpleNameStringTemplateEntry>, KtSimpleNameStringTemplateEntry>
    val literalStringTemplateEntryType: KtStubElementType<KotlinPlaceHolderWithTextStub<KtLiteralStringTemplateEntry>, KtLiteralStringTemplateEntry>
    val escapeStringTemplateEntryType: KtStubElementType<KotlinPlaceHolderWithTextStub<KtEscapeStringTemplateEntry>, KtEscapeStringTemplateEntry>
    val stringInterpolationPrefixType: KtStubElementType<KotlinStringInterpolationPrefixStub, KtStringInterpolationPrefix>

    // Expressions
    val blockExpressionType: IElementType
    val lambdaExpressionType: IElementType
    val referenceExpressionType: KtStubElementType<KotlinNameReferenceExpressionStub, KtNameReferenceExpression>
    val enumEntrySuperclassReferenceExpressionType: KtStubElementType<KotlinEnumEntrySuperclassReferenceExpressionStub, KtEnumEntrySuperclassReferenceExpression>
    val dotQualifiedExpressionType: KtStubElementType<KotlinPlaceHolderStub<KtDotQualifiedExpression>, KtDotQualifiedExpression>
    val callExpressionType: KtStubElementType<KotlinPlaceHolderStub<KtCallExpression>, KtCallExpression>
    val classLiteralExpressionType: KtStubElementType<KotlinClassLiteralExpressionStub, KtClassLiteralExpression>
    val collectionLiteralExpressionType: KtStubElementType<KotlinCollectionLiteralExpressionStub, KtCollectionLiteralExpression>

    // Arguments
    val typeArgumentListType: KtStubElementType<KotlinPlaceHolderStub<KtTypeArgumentList>, KtTypeArgumentList>
    val valueArgumentListType: KtStubElementType<KotlinPlaceHolderStub<KtValueArgumentList>, KtValueArgumentList>
    val valueArgumentType: KtStubElementType<KotlinValueArgumentStub<KtValueArgument>, KtValueArgument>
    val contractEffectListType: KtStubElementType<KotlinPlaceHolderStub<KtContractEffectList>, KtContractEffectList>
    val contractEffectType: KtStubElementType<KotlinContractEffectStub, KtContractEffect>
    val lambdaArgumentType: KtStubElementType<KotlinValueArgumentStub<KtLambdaArgument>, KtLambdaArgument>
    val valueArgumentNameType: KtStubElementType<KotlinPlaceHolderStub<KtValueArgumentName>, KtValueArgumentName>

    // Special
    val packageDirectiveType: KtStubElementType<KotlinPlaceHolderStub<KtPackageDirective>, KtPackageDirective>
    val fileAnnotationListType: KtStubElementType<KotlinPlaceHolderStub<KtFileAnnotationList>, KtFileAnnotationList>
    val importListType: KtStubElementType<KotlinPlaceHolderStub<KtImportList>, KtImportList>
    val importDirectiveType: KtStubElementType<KotlinImportDirectiveStub, KtImportDirective>
    val importAliasType: KtStubElementType<KotlinImportAliasStub, KtImportAlias>
    val scriptType: KtStubElementType<KotlinScriptStub, KtScript>

    // Code fragments
    val expressionCodeFragmentType: ICodeFragmentElementType
    val blockCodeFragmentType: ICodeFragmentElementType
    val typeCodeFragmentType: ICodeFragmentElementType

    // KDoc
    val kdocType: ILazyParseableElementType
    val kdocMarkdownLinkType: ILazyParseableElementType
}