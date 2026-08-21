/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubRegistry
import com.intellij.psi.stubs.StubRegistryExtension
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtNodeType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.stubs.elements.KtFileElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.factories.*
import org.jetbrains.kotlin.psi.utils.toConstantExpressionElementType

/**
 * Associates Kotlin stub serializers and factories with their element types, decoupling stub support from the
 * element types themselves.
 */
internal class KotlinStubRegistryExtension : StubRegistryExtension {
    override fun register(registry: StubRegistry) {
        registry.registerStubSerializer(KtFileElementType, KtFileStubSerializer)

        for ((key, value) in KtStubElementFactories.factories) {
            registry.registerStubSerializingFactory(key, value)
        }
    }
}

/**
 * All Kotlin stub factories by their element types.
 *
 * The mapping is kept here to allow cheap factory lookups by an element type. Consulting
 * [com.intellij.psi.stubs.StubElementRegistryService] instead is too expensive for hot paths
 * such as [KtStubSerializingElementFactory.shouldCreateStub].
 */
internal object KtStubElementFactories {
    @OptIn(KtImplementationDetail::class, KtExperimentalApi::class)
    val factories: Map<KtNodeType, KtStubSerializingElementFactory<*, *>> = buildMap {
        registerStubSerializingFactory(
            type = KtStubElementTypes.SECONDARY_CONSTRUCTOR,
            factory = KtSecondaryConstructorStubSerializingElementFactory,
        )

        registerStubSerializingFactory(
            type = KtStubElementTypes.PRIMARY_CONSTRUCTOR,
            factory = KtPrimaryConstructorStubSerializingElementFactory,
        )

        registerStubSerializingFactory(
            type = KtStubElementTypes.CLASS,
            factory = KtClassStubSerializingElementFactory,
        )

        registerStubSerializingFactory(
            type = KtStubElementTypes.FUNCTION,
            factory = KtFunctionStubSerializingElementFactory,
        )

        registerStubSerializingFactory(
            type = KtStubElementTypes.PROPERTY,
            factory = KtPropertyStubSerializingElementFactory,
        )

        registerStubSerializingFactory(
            type = KtStubElementTypes.PROPERTY_ACCESSOR,
            factory = KtPropertyAccessorStubSerializingElementFactory,
        )

        registerStubSerializingFactory(
            type = KtStubElementTypes.BACKING_FIELD,
            factory = KtBackingFieldStubSerializingElementFactory,
        )

        registerStubSerializingFactory(
            type = KtStubElementTypes.DESTRUCTURING_DECLARATION,
            factory = KtDestructuringDeclarationStubSerializingElementFactory,
        )

        registerStubSerializingFactory(
            type = KtStubElementTypes.TYPEALIAS,
            factory = KtTypeAliasStubSerializingElementFactory,
        )

        registerStubSerializingFactory(
            type = KtStubElementTypes.ENUM_ENTRY,
            factory = KtEnumEntryStubSerializingElementFactory,
        )

        registerStubSerializingFactory(
            type = KtStubElementTypes.OBJECT_DECLARATION,
            factory = KtObjectStubSerializingElementFactory,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.CLASS_INITIALIZER,
            psiFactory = ::KtClassInitializer,
        )

        registerStubSerializingFactory(
            type = KtStubElementTypes.SCRIPT_INITIALIZER,
            factory = KtScriptInitializerStubSerializingElementFactory,
        )

        registerStubSerializingFactory(
            type = KtStubElementTypes.VALUE_PARAMETER,
            factory = KtParameterStubSerializingElementFactory,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.VALUE_PARAMETER_LIST,
            psiFactory = ::KtParameterList,
        )

        registerStubSerializingFactory(
            type = KtStubElementTypes.TYPE_PARAMETER,
            factory = KtTypeParameterStubSerializingElementFactory,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.TYPE_PARAMETER_LIST,
            psiFactory = ::KtTypeParameterList,
        )

        registerStubSerializingFactory(
            type = KtStubElementTypes.ANNOTATION_ENTRY,
            factory = KtAnnotationEntryStubSerializingElementFactory,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.ANNOTATION,
            psiFactory = ::KtAnnotation,
        )

        registerStubSerializingFactory(
            type = KtStubElementTypes.ANNOTATION_TARGET,
            factory = KtAnnotationUseSiteTargetStubSerializingElementFactory,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.CLASS_BODY,
            psiFactory = ::KtClassBody,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.COMPANION_BLOCK,
            psiFactory = ::KtCompanionBlock,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.IMPORT_LIST,
            psiFactory = ::KtImportList,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.FILE_ANNOTATION_LIST,
            psiFactory = ::KtFileAnnotationList,
        )

        registerStubSerializingFactory(
            type = KtStubElementTypes.IMPORT_DIRECTIVE,
            factory = KtImportDirectiveStubSerializingElementFactory,
        )

        registerStubSerializingFactory(
            type = KtStubElementTypes.IMPORT_ALIAS,
            factory = KtImportAliasStubSerializingElementFactory,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.PACKAGE_DIRECTIVE,
            psiFactory = ::KtPackageDirective,
        )

        registerStubSerializingFactory(
            type = KtStubElementTypes.MODIFIER_LIST,
            factory = KtModifierListStubSerializingElementFactory,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.TYPE_CONSTRAINT_LIST,
            psiFactory = ::KtTypeConstraintList,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.TYPE_CONSTRAINT,
            psiFactory = ::KtTypeConstraint,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.NULLABLE_TYPE,
            psiFactory = ::KtNullableType,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.INTERSECTION_TYPE,
            psiFactory = ::KtIntersectionType,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.DYNAMIC_TYPE,
            psiFactory = ::KtDynamicType,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.TYPE_REFERENCE,
            psiFactory = ::KtTypeReference,
        )

        registerStubSerializingFactory(
            type = KtStubElementTypes.USER_TYPE,
            factory = KtUserTypeStubSerializingElementFactory,
        )

        registerStubSerializingFactory(
            type = KtStubElementTypes.FUNCTION_TYPE,
            factory = KtFunctionTypeStubSerializingElementFactory,
        )

        registerStubSerializingFactory(
            type = KtStubElementTypes.TYPE_PROJECTION,
            factory = KtTypeProjectionStubSerializingElementFactory,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.FUNCTION_TYPE_RECEIVER,
            psiFactory = ::KtFunctionTypeReceiver,
        )

        registerStubSerializingFactory(
            type = KtStubElementTypes.REFERENCE_EXPRESSION,
            factory = KtNameReferenceExpressionStubSerializingElementFactory,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.DOT_QUALIFIED_EXPRESSION,
            psiFactory = ::KtDotQualifiedExpression,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.CALL_EXPRESSION,
            psiFactory = ::KtCallExpression,
        )

        registerStubSerializingFactory(
            type = KtStubElementTypes.OPERATION_REFERENCE,
            factory = KtOperationReferenceExpressionStubSerializingElementFactory,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.PREFIX_EXPRESSION,
            psiFactory = ::KtPrefixExpression,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.POSTFIX_EXPRESSION,
            psiFactory = ::KtPostfixExpression,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.BINARY_EXPRESSION,
            psiFactory = ::KtBinaryExpression,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.PARENTHESIZED,
            psiFactory = ::KtParenthesizedExpression,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.OBJECT_LITERAL,
            psiFactory = ::KtObjectLiteralExpression,
        )

        registerStubSerializingFactory(
            type = KtStubElementTypes.ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION,
            factory = KtEnumEntrySuperclassReferenceExpressionStubSerializingElementFactory,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.TYPE_ARGUMENT_LIST,
            psiFactory = ::KtTypeArgumentList,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.VALUE_ARGUMENT_LIST,
            psiFactory = ::KtValueArgumentList,
        )

        registerValueArgumentFactory(
            type = KtStubElementTypes.VALUE_ARGUMENT,
            psiFactory = ::KtValueArgument,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.CONTRACT_EFFECT_LIST,
            psiFactory = ::KtContractEffectList,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.CONTRACT_EFFECT,
            psiFactory = ::KtContractEffect,
        )

        registerValueArgumentFactory(
            type = KtStubElementTypes.LAMBDA_ARGUMENT,
            psiFactory = ::KtLambdaArgument,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.VALUE_ARGUMENT_NAME,
            psiFactory = ::KtValueArgumentName,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.SUPER_TYPE_LIST,
            psiFactory = ::KtSuperTypeList,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.INITIALIZER_LIST,
            psiFactory = ::KtInitializerList,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.DELEGATED_SUPER_TYPE_ENTRY,
            psiFactory = ::KtDelegatedSuperTypeEntry,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.SUPER_TYPE_CALL_ENTRY,
            psiFactory = ::KtSuperTypeCallEntry,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.SUPER_TYPE_ENTRY,
            psiFactory = ::KtSuperTypeEntry,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.CONSTRUCTOR_CALLEE,
            psiFactory = ::KtConstructorCalleeExpression,
        )

        registerStubSerializingFactory(
            type = KtStubElementTypes.CONTEXT_RECEIVER,
            factory = KtContextReceiverStubSerializingElementFactory,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.CONTEXT_PARAMETER_LIST,
            psiFactory = ::KtContextReceiverList,
        )

        for (kind in ConstantValueKind.entries) {
            registerStubSerializingFactory(
                type = kind.toConstantExpressionElementType(),
                factory = KtConstantExpressionStubSerializingElementFactory(kind),
            )
        }

        registerPlaceHolderFactory(
            type = KtStubElementTypes.CLASS_LITERAL_EXPRESSION,
            psiFactory = ::KtClassLiteralExpression,
        )

        registerStubSerializingFactory(
            type = KtStubElementTypes.COLLECTION_LITERAL_EXPRESSION,
            factory = KtCollectionLiteralExpressionStubSerializingElementFactory,
        )

        registerPlaceHolderFactory(
            type = KtStubElementTypes.STRING_TEMPLATE,
            psiFactory = ::KtStringTemplateExpression,
        )

        registerStubSerializingFactory(
            type = KtStubElementTypes.LONG_STRING_TEMPLATE_ENTRY,
            factory = KtBlockStringTemplateEntryStubSerializingElementFactory,
        )

        registerPlaceHolderWithTextFactory(
            type = KtStubElementTypes.SHORT_STRING_TEMPLATE_ENTRY,
            psiFactory = ::KtSimpleNameStringTemplateEntry,
        )

        registerPlaceHolderWithTextFactory(
            type = KtStubElementTypes.LITERAL_STRING_TEMPLATE_ENTRY,
            psiFactory = ::KtLiteralStringTemplateEntry,
        )

        registerPlaceHolderWithTextFactory(
            type = KtStubElementTypes.ESCAPE_STRING_TEMPLATE_ENTRY,
            psiFactory = ::KtEscapeStringTemplateEntry,
        )

        registerStubSerializingFactory(
            type = KtStubElementTypes.SCRIPT,
            factory = KtScriptStubSerializingElementFactory,
        )

        registerStubSerializingFactory(
            type = KtStubElementTypes.STRING_INTERPOLATION_PREFIX,
            factory = KtStringInterpolationPrefixStubSerializingElementFactory,
        )
    }

    /**
     * Returns the factory registered for [type], or `null` if the element type is not stubbed.
     */
    operator fun get(type: IElementType): KtStubSerializingElementFactory<*, *>? = factories[type]
}

/**
 * A builder of the [KtStubElementFactories.factories] mapping.
 */
private typealias KtStubFactoryBuilder = MutableMap<KtNodeType, KtStubSerializingElementFactory<*, *>>

/**
 * Registers a factory for the given element [type].
 */
private fun KtStubFactoryBuilder.registerStubSerializingFactory(
    type: KtNodeType,
    factory: KtStubSerializingElementFactory<*, *>,
) {
    put(type, factory)
}

/**
 * Registers a factory for an element whose stub carries no data beyond its own presence.
 */
private fun <Psi : KtElementImplStub<out StubElement<*>>> KtStubFactoryBuilder.registerPlaceHolderFactory(
    type: KtNodeType,
    psiFactory: (KotlinPlaceHolderStub<Psi>) -> Psi,
) {
    registerStubSerializingFactory(type, KtPlaceHolderStubSerializingElementFactory(type, psiFactory))
}

/**
 * Registers a factory for an element which represents an argument of a call.
 */
private fun <Psi : KtValueArgument> KtStubFactoryBuilder.registerValueArgumentFactory(
    type: KtNodeType,
    psiFactory: (KotlinValueArgumentStub<Psi>) -> Psi,
) {
    registerStubSerializingFactory(type, KtValueArgumentStubSerializingElementFactory(type, psiFactory))
}

/**
 * Registers a factory for an element whose stub carries only its source text.
 */
private fun <Psi : KtElementImplStub<out StubElement<*>>> KtStubFactoryBuilder.registerPlaceHolderWithTextFactory(
    type: KtNodeType,
    psiFactory: (KotlinPlaceHolderWithTextStub<Psi>) -> Psi,
) {
    registerStubSerializingFactory(type, KtPlaceHolderWithTextStubSerializingElementFactory(type, psiFactory))
}
