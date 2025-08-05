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
    val classType: KtStubElementType<out KotlinClassStub, KtClass>
    val objectType: KtStubElementType<out KotlinObjectStub, KtObjectDeclaration>
    val typeAliasType: KtStubElementType<out KotlinTypeAliasStub, KtTypeAlias>
    val classBodyType: KtStubElementType<out KotlinPlaceHolderStub<KtClassBody>, KtClassBody>

    // Initializers
    val classInitializerType: KtStubElementType<out KotlinPlaceHolderStub<KtClassInitializer>, KtClassInitializer>
    val scriptInitializerType: KtStubElementType<out KotlinPlaceHolderStub<KtScriptInitializer>, KtScriptInitializer>

    // Callables
    val functionType: KtStubElementType<out KotlinFunctionStub, KtNamedFunction>
    val propertyType: KtStubElementType<out KotlinPropertyStub, KtProperty>
    val enumEntryType: KtStubElementType<out KotlinClassStub, KtClass>
    val primaryConstructorType: KtStubElementType<out KotlinConstructorStub<KtPrimaryConstructor>, KtPrimaryConstructor>
    val secondaryConstructorType: KtStubElementType<out KotlinConstructorStub<KtSecondaryConstructor>, KtSecondaryConstructor>
    val constructorCalleeType: KtStubElementType<out KotlinPlaceHolderStub<KtConstructorCalleeExpression>, KtConstructorCalleeExpression>
    val propertyAccessorType: KtStubElementType<out KotlinPropertyAccessorStub, KtPropertyAccessor>
    val backingFieldType: KtStubElementType<out KotlinBackingFieldStub, KtBackingField>
    val initializerListType: KtStubElementType<out KotlinPlaceHolderStub<KtInitializerList>, KtInitializerList>

    // Value parameters
    val valueParameterListType: KtStubElementType<out KotlinPlaceHolderStub<KtParameterList>, KtParameterList>
    val valueParameterType: KtStubElementType<out KotlinParameterStub, KtParameter>
    val contextReceiverListType: KtStubElementType<out KotlinPlaceHolderStub<KtContextReceiverList>, KtContextReceiverList>
    val contextReceiverType: KtStubElementType<out KotlinContextReceiverStub, KtContextReceiver>

    // Type parameters
    val typeParameterListType: KtStubElementType<out KotlinPlaceHolderStub<KtTypeParameterList>, KtTypeParameterList>
    val typeParameterType: KtStubElementType<out KotlinTypeParameterStub, KtTypeParameter>
    val typeConstraintListType: KtStubElementType<out KotlinPlaceHolderStub<KtTypeConstraintList>, KtTypeConstraintList>
    val typeConstraintType: KtStubElementType<out KotlinPlaceHolderStub<KtTypeConstraint>, KtTypeConstraint>

    // Supertypes
    val superTypeListType: KtStubElementType<out KotlinPlaceHolderStub<KtSuperTypeList>, KtSuperTypeList>
    val delegatedSuperTypeEntryType: KtStubElementType<out KotlinPlaceHolderStub<KtDelegatedSuperTypeEntry>, KtDelegatedSuperTypeEntry>
    val superTypeCallEntryType: KtStubElementType<out KotlinPlaceHolderStub<KtSuperTypeCallEntry>, KtSuperTypeCallEntry>
    val superTypeEntryType: KtStubElementType<out KotlinPlaceHolderStub<KtSuperTypeEntry>, KtSuperTypeEntry>

    // Modifiers and annotations
    val modifierListType: KtStubElementType<out KotlinModifierListStub, KtDeclarationModifierList>
    val annotationType: KtStubElementType<out KotlinPlaceHolderStub<KtAnnotation>, KtAnnotation>
    val annotationEntryType: KtStubElementType<out KotlinAnnotationEntryStub, KtAnnotationEntry>
    val annotationTargetType: KtStubElementType<out KotlinAnnotationUseSiteTargetStub, KtAnnotationUseSiteTarget>

    // Type references
    val typeReferenceType: KtStubElementType<out KotlinPlaceHolderStub<KtTypeReference>, KtTypeReference>
    val userTypeType: KtStubElementType<out KotlinUserTypeStub, KtUserType>
    val dynamicTypeType: KtStubElementType<out KotlinPlaceHolderStub<KtDynamicType>, KtDynamicType>
    val functionTypeType: KtStubElementType<out KotlinFunctionTypeStub, KtFunctionType>
    val functionTypeReceiverType: KtStubElementType<out KotlinPlaceHolderStub<KtFunctionTypeReceiver>, KtFunctionTypeReceiver>
    val nullableTypeType: KtStubElementType<out KotlinPlaceHolderStub<KtNullableType>, KtNullableType>
    val intersectionTypeType: KtStubElementType<out KotlinPlaceHolderStub<KtIntersectionType>, KtIntersectionType>
    val typeProjectionType: KtStubElementType<out KotlinTypeProjectionStub, KtTypeProjection>

    // Constants
    val nullType: KtStubElementType<out KotlinConstantExpressionStub, KtConstantExpression>
    val booleanConstantType: KtStubElementType<out KotlinConstantExpressionStub, KtConstantExpression>
    val floatConstantType: KtStubElementType<out KotlinConstantExpressionStub, KtConstantExpression>
    val characterConstantType: KtStubElementType<out KotlinConstantExpressionStub, KtConstantExpression>
    val integerConstantType: KtStubElementType<out KotlinConstantExpressionStub, KtConstantExpression>

    // String templates
    val stringTemplateType: KtStubElementType<out KotlinPlaceHolderStub<KtStringTemplateExpression>, KtStringTemplateExpression>
    val longStringTemplateEntryType: KtStubElementType<out KotlinBlockStringTemplateEntryStub, KtBlockStringTemplateEntry>
    val shortStringTemplateEntryType: KtStubElementType<out KotlinPlaceHolderWithTextStub<KtSimpleNameStringTemplateEntry>, KtSimpleNameStringTemplateEntry>
    val literalStringTemplateEntryType: KtStubElementType<out KotlinPlaceHolderWithTextStub<KtLiteralStringTemplateEntry>, KtLiteralStringTemplateEntry>
    val escapeStringTemplateEntryType: KtStubElementType<out KotlinPlaceHolderWithTextStub<KtEscapeStringTemplateEntry>, KtEscapeStringTemplateEntry>
    val stringInterpolationPrefixType: KtStubElementType<out KotlinStringInterpolationPrefixStub, KtStringInterpolationPrefix>

    // Expressions
    val blockExpressionType: IElementType
    val lambdaExpressionType: IElementType
    val referenceExpressionType: KtStubElementType<out KotlinNameReferenceExpressionStub, KtNameReferenceExpression>
    val enumEntrySuperclassReferenceExpressionType: KtStubElementType<out KotlinEnumEntrySuperclassReferenceExpressionStub, KtEnumEntrySuperclassReferenceExpression>
    val dotQualifiedExpressionType: KtStubElementType<out KotlinPlaceHolderStub<KtDotQualifiedExpression>, KtDotQualifiedExpression>
    val callExpressionType: KtStubElementType<out KotlinPlaceHolderStub<KtCallExpression>, KtCallExpression>
    val classLiteralExpressionType: KtStubElementType<out KotlinClassLiteralExpressionStub, KtClassLiteralExpression>
    val collectionLiteralExpressionType: KtStubElementType<out KotlinCollectionLiteralExpressionStub, KtCollectionLiteralExpression>

    // Arguments
    val typeArgumentListType: KtStubElementType<out KotlinPlaceHolderStub<KtTypeArgumentList>, KtTypeArgumentList>
    val valueArgumentListType: KtStubElementType<out KotlinPlaceHolderStub<KtValueArgumentList>, KtValueArgumentList>
    val valueArgumentType: KtStubElementType<out KotlinValueArgumentStub<KtValueArgument>, KtValueArgument>
    val contractEffectListType: KtStubElementType<out KotlinPlaceHolderStub<KtContractEffectList>, KtContractEffectList>
    val contractEffectType: KtStubElementType<out KotlinContractEffectStub, KtContractEffect>
    val lambdaArgumentType: KtStubElementType<out KotlinValueArgumentStub<KtLambdaArgument>, KtLambdaArgument>
    val valueArgumentNameType: KtStubElementType<out KotlinPlaceHolderStub<KtValueArgumentName>, KtValueArgumentName>

    // Special
    val packageDirectiveType: KtStubElementType<out KotlinPlaceHolderStub<KtPackageDirective>, KtPackageDirective>
    val fileAnnotationListType: KtStubElementType<out KotlinPlaceHolderStub<KtFileAnnotationList>, KtFileAnnotationList>
    val importListType: KtStubElementType<out KotlinPlaceHolderStub<KtImportList>, KtImportList>
    val importDirectiveType: KtStubElementType<out KotlinImportDirectiveStub, KtImportDirective>
    val importAliasType: KtStubElementType<out KotlinImportAliasStub, KtImportAlias>
    val scriptType: KtStubElementType<out KotlinScriptStub, KtScript>

    // Code fragments
    val expressionCodeFragmentType: ICodeFragmentElementType
    val blockCodeFragmentType: ICodeFragmentElementType
    val typeCodeFragmentType: ICodeFragmentElementType

    // KDoc
    val kdocType: ILazyParseableElementType
    val kdocMarkdownLinkType: ILazyParseableElementType
}