/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import com.intellij.psi.impl.source.tree.ICodeFragmentElementType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.ILazyParseableElementType
import org.jetbrains.kotlin.psi.KtExperimentalApi
import org.jetbrains.kotlin.psi.KtImplementationDetail

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
    val constructorCalleeType: KtNodeType
    val propertyAccessorType: KtNodeType
    val backingFieldType: KtNodeType
    val destructuringDeclarationType: KtNodeType
    val initializerListType: KtNodeType

    // Value parameters
    val valueParameterListType: KtNodeType
    val valueParameterType: KtNodeType
    val contextParameterListType: KtNodeType
    val contextReceiverType: KtNodeType

    // Type parameters
    val typeParameterListType: KtNodeType
    val typeParameterType: KtNodeType
    val typeConstraintListType: KtNodeType
    val typeConstraintType: KtNodeType

    // Supertypes
    val superTypeListType: KtNodeType
    val delegatedSuperTypeEntryType: KtNodeType
    val superTypeCallEntryType: KtNodeType
    val superTypeEntryType: KtNodeType

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
    val functionTypeReceiverType: KtNodeType
    val nullableTypeType: KtNodeType
    val intersectionTypeType: KtNodeType
    val typeProjectionType: KtNodeType

    // Constants
    val nullType: KtNodeType
    val booleanConstantType: KtNodeType
    val floatConstantType: KtNodeType
    val characterConstantType: KtNodeType
    val integerConstantType: KtNodeType

    // String templates
    val stringTemplateType: KtNodeType
    val longStringTemplateEntryType: KtNodeType
    val shortStringTemplateEntryType: KtNodeType
    val literalStringTemplateEntryType: KtNodeType
    val escapeStringTemplateEntryType: KtNodeType
    val stringInterpolationPrefixType: KtNodeType

    // Expressions
    val blockExpressionType: IElementType
    val lambdaExpressionType: IElementType
    val referenceExpressionType: KtNodeType
    val enumEntrySuperclassReferenceExpressionType: KtNodeType
    val operationReferenceType: KtNodeType
    val dotQualifiedExpressionType: KtNodeType
    val callExpressionType: KtNodeType
    val prefixExpressionType: KtNodeType
    val postfixExpressionType: KtNodeType
    val binaryExpressionType: KtNodeType
    val parenthesizedExpressionType: KtNodeType
    val classLiteralExpressionType: KtNodeType
    val collectionLiteralExpressionType: KtNodeType
    val objectLiteralType: KtNodeType

    // Arguments
    val typeArgumentListType: KtNodeType
    val valueArgumentListType: KtNodeType
    val valueArgumentType: KtNodeType
    val contractEffectListType: KtNodeType
    val contractEffectType: KtNodeType
    val lambdaArgumentType: KtNodeType
    val valueArgumentNameType: KtNodeType

    // Special
    val packageDirectiveType: KtNodeType
    val fileAnnotationListType: KtNodeType
    val importListType: KtNodeType
    val importDirectiveType: KtNodeType
    val importAliasType: KtNodeType
    val scriptType: KtNodeType

    // Code fragments
    val expressionCodeFragmentType: ICodeFragmentElementType
    val blockCodeFragmentType: ICodeFragmentElementType
    val typeCodeFragmentType: ICodeFragmentElementType

    // KDoc
    val kdocType: ILazyParseableElementType
    val kdocMarkdownLinkType: ILazyParseableElementType
}
