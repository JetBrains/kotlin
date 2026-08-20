/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
    val classType: KtNodeType
    val objectType: KtNodeType
    val typeAliasType: KtNodeType
    val classBodyType: KtNodeType

    @KtExperimentalApi
    val companionBlockType: KtNodeType

    // Initializers
    val classInitializerType: KtNodeType
    val scriptInitializerType: KtNodeType

    // Callables
    val functionType: KtNodeType
    val propertyType: KtNodeType
    val enumEntryType: KtNodeType
    val primaryConstructorType: KtNodeType
    val secondaryConstructorType: KtNodeType
    val constructorCalleeType: KtStubElementType<out KotlinPlaceHolderStub<KtConstructorCalleeExpression>, KtConstructorCalleeExpression>
    val propertyAccessorType: KtNodeType
    val backingFieldType: KtNodeType
    val destructuringDeclarationType: KtNodeType
    val initializerListType: KtStubElementType<out KotlinPlaceHolderStub<KtInitializerList>, KtInitializerList>

    // Value parameters
    val valueParameterListType: KtNodeType
    val valueParameterType: KtNodeType
    val contextParameterListType: KtStubElementType<out KotlinPlaceHolderStub<KtContextParameterList>, KtContextParameterList>
    val contextReceiverType: KtStubElementType<out KotlinContextReceiverStub, KtContextReceiver>

    // Type parameters
    val typeParameterListType: KtNodeType
    val typeParameterType: KtNodeType
    val typeConstraintListType: KtNodeType
    val typeConstraintType: KtNodeType

    // Supertypes
    val superTypeListType: KtStubElementType<out KotlinPlaceHolderStub<KtSuperTypeList>, KtSuperTypeList>
    val delegatedSuperTypeEntryType: KtStubElementType<out KotlinPlaceHolderStub<KtDelegatedSuperTypeEntry>, KtDelegatedSuperTypeEntry>
    val superTypeCallEntryType: KtStubElementType<out KotlinPlaceHolderStub<KtSuperTypeCallEntry>, KtSuperTypeCallEntry>
    val superTypeEntryType: KtStubElementType<out KotlinPlaceHolderStub<KtSuperTypeEntry>, KtSuperTypeEntry>

    // Modifiers and annotations
    val modifierListType: KtNodeType
    val annotationType: KtNodeType
    val annotationEntryType: KtNodeType
    val annotationTargetType: KtNodeType

    // Type references
    val typeReferenceType: KtNodeType
    val userTypeType: KtNodeType
    val dynamicTypeType: KtNodeType
    val functionTypeType: KtNodeType
    val functionTypeReceiverType: KtStubElementType<out KotlinPlaceHolderStub<KtFunctionTypeReceiver>, KtFunctionTypeReceiver>
    val nullableTypeType: KtNodeType
    val intersectionTypeType: KtNodeType
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
    val operationReferenceType: KtStubElementType<out KotlinOperationReferenceExpressionStub, KtOperationReferenceExpression>
    val dotQualifiedExpressionType: KtStubElementType<out KotlinPlaceHolderStub<KtDotQualifiedExpression>, KtDotQualifiedExpression>
    val callExpressionType: KtStubElementType<out KotlinPlaceHolderStub<KtCallExpression>, KtCallExpression>
    val prefixExpressionType: KtStubElementType<out KotlinPlaceHolderStub<KtPrefixExpression>, KtPrefixExpression>
    val postfixExpressionType: KtStubElementType<out KotlinPlaceHolderStub<KtPostfixExpression>, KtPostfixExpression>
    val binaryExpressionType: KtStubElementType<out KotlinPlaceHolderStub<KtBinaryExpression>, KtBinaryExpression>
    val parenthesizedExpressionType: KtStubElementType<out KotlinPlaceHolderStub<KtParenthesizedExpression>, KtParenthesizedExpression>
    val classLiteralExpressionType: KtStubElementType<out KotlinClassLiteralExpressionStub, KtClassLiteralExpression>
    val collectionLiteralExpressionType: KtStubElementType<out KotlinCollectionLiteralExpressionStub, KtCollectionLiteralExpression>
    val objectLiteralType: KtStubElementType<out KotlinPlaceHolderStub<KtObjectLiteralExpression>, KtObjectLiteralExpression>

    // Arguments
    val typeArgumentListType: KtStubElementType<out KotlinPlaceHolderStub<KtTypeArgumentList>, KtTypeArgumentList>
    val valueArgumentListType: KtStubElementType<out KotlinPlaceHolderStub<KtValueArgumentList>, KtValueArgumentList>
    val valueArgumentType: KtStubElementType<out KotlinValueArgumentStub<KtValueArgument>, KtValueArgument>
    val contractEffectListType: KtStubElementType<out KotlinPlaceHolderStub<KtContractEffectList>, KtContractEffectList>
    val contractEffectType: KtStubElementType<out KotlinContractEffectStub, KtContractEffect>
    val lambdaArgumentType: KtStubElementType<out KotlinValueArgumentStub<KtLambdaArgument>, KtLambdaArgument>
    val valueArgumentNameType: KtStubElementType<out KotlinPlaceHolderStub<KtValueArgumentName>, KtValueArgumentName>

    // Special
    val packageDirectiveType: KtNodeType
    val fileAnnotationListType: KtNodeType
    val importListType: KtNodeType
    val importDirectiveType: KtNodeType
    val importAliasType: KtNodeType
    val scriptType: KtStubElementType<out KotlinScriptStub, KtScript>

    // Code fragments
    val expressionCodeFragmentType: ICodeFragmentElementType
    val blockCodeFragmentType: ICodeFragmentElementType
    val typeCodeFragmentType: ICodeFragmentElementType

    // KDoc
    val kdocType: ILazyParseableElementType
    val kdocMarkdownLinkType: ILazyParseableElementType
}
