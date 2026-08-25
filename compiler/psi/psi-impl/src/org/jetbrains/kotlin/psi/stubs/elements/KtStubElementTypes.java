/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.psi.stubs.StubElementTypeHolderEP;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KtNodeType;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.psi.*;

/**
 * A container for all stub-based elements {@link StubElementTypeHolderEP}.
 * <p>
 * The interface is not supposed to be used directly, use {@link KtNodeTypes} instead.
 *
 * @see KtNodeTypes
 */
@KtImplementationDetail
public interface KtStubElementTypes {
    @NotNull KtNodeType CLASS = new KtNodeType("CLASS", KtClass::new);
    @NotNull KtNodeType FUNCTION = new KtNodeType("FUNCTION", KtNamedFunction::new);
    @NotNull KtNodeType PROPERTY = new KtNodeType("PROPERTY", KtProperty::new);
    @NotNull KtNodeType PROPERTY_ACCESSOR = new KtNodeType("PROPERTY_ACCESSOR", KtPropertyAccessor::new);
    @NotNull KtNodeType BACKING_FIELD = new KtNodeType("BACKING_FIELD", KtBackingField::new);
    @NotNull KtNodeType DESTRUCTURING_DECLARATION = new KtNodeType("DESTRUCTURING_DECLARATION", KtDestructuringDeclaration::new);
    @NotNull KtNodeType TYPEALIAS = new KtNodeType("TYPEALIAS", KtTypeAlias::new);

    @NotNull KtNodeType ENUM_ENTRY = new KtNodeType("ENUM_ENTRY", KtEnumEntry::new);
    @NotNull KtNodeType OBJECT_DECLARATION = new KtNodeType("OBJECT_DECLARATION", KtObjectDeclaration::new);
    @NotNull KtNodeType CLASS_INITIALIZER = new KtNodeType("CLASS_INITIALIZER", KtClassInitializer::new);
    @NotNull KtNodeType SCRIPT_INITIALIZER = new KtNodeType("SCRIPT_INITIALIZER", KtScriptInitializer::new);
    @NotNull KtNodeType SECONDARY_CONSTRUCTOR = new KtNodeType("SECONDARY_CONSTRUCTOR", KtSecondaryConstructor::new);
    @NotNull KtNodeType PRIMARY_CONSTRUCTOR = new KtNodeType("PRIMARY_CONSTRUCTOR", KtPrimaryConstructor::new);

    @NotNull KtNodeType VALUE_PARAMETER = new KtNodeType("VALUE_PARAMETER", KtParameter::new);
    @NotNull KtNodeType VALUE_PARAMETER_LIST = new KtNodeType("VALUE_PARAMETER_LIST", KtParameterList::new);

    @NotNull KtNodeType TYPE_PARAMETER = new KtNodeType("TYPE_PARAMETER", KtTypeParameter::new);
    @NotNull KtNodeType TYPE_PARAMETER_LIST = new KtNodeType("TYPE_PARAMETER_LIST", KtTypeParameterList::new);

    @NotNull KtNodeType ANNOTATION_ENTRY = new KtNodeType("ANNOTATION_ENTRY", KtAnnotationEntry::new);
    @NotNull KtNodeType ANNOTATION = new KtNodeType("ANNOTATION", KtAnnotation::new);

    @NotNull KtNodeType ANNOTATION_TARGET = new KtNodeType("ANNOTATION_TARGET", KtAnnotationUseSiteTarget::new);

    @NotNull KtNodeType CLASS_BODY = new KtNodeType("CLASS_BODY", KtClassBody::new);

    @NotNull KtNodeType COMPANION_BLOCK = new KtNodeType("COMPANION_BLOCK", KtCompanionBlock::new);

    @NotNull KtNodeType IMPORT_LIST = new KtNodeType("IMPORT_LIST", KtImportList::new);

    @NotNull KtNodeType FILE_ANNOTATION_LIST = new KtNodeType("FILE_ANNOTATION_LIST", KtFileAnnotationList::new);

    @NotNull KtImportDirectiveElementType IMPORT_DIRECTIVE = new KtImportDirectiveElementType("IMPORT_DIRECTIVE");

    @NotNull KtImportAliasElementType IMPORT_ALIAS = new KtImportAliasElementType("IMPORT_ALIAS");

    @NotNull KtPlaceHolderStubElementType<KtPackageDirective> PACKAGE_DIRECTIVE =
            new KtPlaceHolderStubElementType<>("PACKAGE_DIRECTIVE", KtPackageDirective.class);

    @NotNull KtModifierListElementType<KtDeclarationModifierList> MODIFIER_LIST =
            new KtModifierListElementType<>("MODIFIER_LIST", KtDeclarationModifierList.class);

    @NotNull KtPlaceHolderStubElementType<KtTypeConstraintList> TYPE_CONSTRAINT_LIST =
            new KtPlaceHolderStubElementType<>("TYPE_CONSTRAINT_LIST", KtTypeConstraintList.class);

    @NotNull KtPlaceHolderStubElementType<KtTypeConstraint> TYPE_CONSTRAINT =
            new KtPlaceHolderStubElementType<>("TYPE_CONSTRAINT", KtTypeConstraint.class);

    @NotNull KtPlaceHolderStubElementType<KtNullableType> NULLABLE_TYPE =
            new KtPlaceHolderStubElementType<>("NULLABLE_TYPE", KtNullableType.class);

    @NotNull KtPlaceHolderStubElementType<KtIntersectionType> INTERSECTION_TYPE =
            new KtPlaceHolderStubElementType<>("INTERSECTION_TYPE", KtIntersectionType.class);

    @NotNull KtPlaceHolderStubElementType<KtTypeReference> TYPE_REFERENCE =
            new KtPlaceHolderStubElementType<>("TYPE_REFERENCE", KtTypeReference.class);

    @NotNull KtUserTypeElementType USER_TYPE = new KtUserTypeElementType("USER_TYPE");
    @NotNull KtPlaceHolderStubElementType<KtDynamicType> DYNAMIC_TYPE =
            new KtPlaceHolderStubElementType<>("DYNAMIC_TYPE", KtDynamicType.class);

    @NotNull KtFunctionTypeElementType FUNCTION_TYPE = new KtFunctionTypeElementType("FUNCTION_TYPE");

    @NotNull KtTypeProjectionElementType TYPE_PROJECTION = new KtTypeProjectionElementType("TYPE_PROJECTION");

    @NotNull KtPlaceHolderStubElementType<KtFunctionTypeReceiver> FUNCTION_TYPE_RECEIVER =
            new KtPlaceHolderStubElementType<>("FUNCTION_TYPE_RECEIVER", KtFunctionTypeReceiver.class);

    @NotNull KtNameReferenceExpressionElementType REFERENCE_EXPRESSION = new KtNameReferenceExpressionElementType("REFERENCE_EXPRESSION");
    @NotNull KtPlaceHolderStubElementType<KtDotQualifiedExpression> DOT_QUALIFIED_EXPRESSION =
            new KtPlaceHolderStubElementType<>("DOT_QUALIFIED_EXPRESSION", KtDotQualifiedExpression.class);
    @NotNull KtPlaceHolderStubElementType<KtCallExpression> CALL_EXPRESSION =
            new KtPlaceHolderStubElementType<>("CALL_EXPRESSION", KtCallExpression.class);
    @NotNull KtOperationReferenceExpressionElementType OPERATION_REFERENCE = KtOperationReferenceExpressionElementType.INSTANCE;

    @NotNull KtPlaceHolderStubElementType<KtPrefixExpression> PREFIX_EXPRESSION =
            new KtPlaceHolderStubElementType<>("PREFIX_EXPRESSION", KtPrefixExpression.class);
    @NotNull KtPlaceHolderStubElementType<KtPostfixExpression> POSTFIX_EXPRESSION =
            new KtPlaceHolderStubElementType<>("POSTFIX_EXPRESSION", KtPostfixExpression.class);
    @NotNull KtPlaceHolderStubElementType<KtBinaryExpression> BINARY_EXPRESSION =
            new KtPlaceHolderStubElementType<>("BINARY_EXPRESSION", KtBinaryExpression.class);
    @NotNull KtPlaceHolderStubElementType<KtParenthesizedExpression> PARENTHESIZED =
            new KtPlaceHolderStubElementType<>("PARENTHESIZED", KtParenthesizedExpression.class);
    @NotNull KtPlaceHolderStubElementType<KtObjectLiteralExpression> OBJECT_LITERAL =
            new KtPlaceHolderStubElementType<>("OBJECT_LITERAL", KtObjectLiteralExpression.class);

    @NotNull KtEnumEntrySuperClassReferenceExpressionElementType ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION =
            new KtEnumEntrySuperClassReferenceExpressionElementType("ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION");
    @NotNull KtPlaceHolderStubElementType<KtTypeArgumentList> TYPE_ARGUMENT_LIST =
            new KtPlaceHolderStubElementType<>("TYPE_ARGUMENT_LIST", KtTypeArgumentList.class);

    @NotNull KtPlaceHolderStubElementType<KtValueArgumentList> VALUE_ARGUMENT_LIST =
            new KtPlaceHolderStubElementType<>("VALUE_ARGUMENT_LIST", KtValueArgumentList.class);

    @NotNull KtValueArgumentElementType<KtValueArgument> VALUE_ARGUMENT =
            new KtValueArgumentElementType<>("VALUE_ARGUMENT", KtValueArgument.class);

    @NotNull KtPlaceHolderStubElementType<KtContractEffectList> CONTRACT_EFFECT_LIST =
            new KtContractEffectListElementType("CONTRACT_EFFECT_LIST");

    @NotNull KtContractEffectElementType CONTRACT_EFFECT =
            new KtContractEffectElementType("CONTRACT_EFFECT", KtContractEffect.class);

    @NotNull KtValueArgumentElementType<KtLambdaArgument> LAMBDA_ARGUMENT =
            new KtValueArgumentElementType<>("LAMBDA_ARGUMENT", KtLambdaArgument.class);

    @NotNull KtPlaceHolderStubElementType<KtValueArgumentName> VALUE_ARGUMENT_NAME =
            new KtPlaceHolderStubElementType<>("VALUE_ARGUMENT_NAME", KtValueArgumentName.class);

    @NotNull KtPlaceHolderStubElementType<KtSuperTypeList> SUPER_TYPE_LIST =
            new KtPlaceHolderStubElementType<>("SUPER_TYPE_LIST", KtSuperTypeList.class);

    @NotNull KtPlaceHolderStubElementType<KtInitializerList> INITIALIZER_LIST =
            new KtPlaceHolderStubElementType<>("INITIALIZER_LIST", KtInitializerList.class);

    @NotNull KtPlaceHolderStubElementType<KtDelegatedSuperTypeEntry> DELEGATED_SUPER_TYPE_ENTRY =
            new KtPlaceHolderStubElementType<>("DELEGATED_SUPER_TYPE_ENTRY", KtDelegatedSuperTypeEntry.class);

    @NotNull KtPlaceHolderStubElementType<KtSuperTypeCallEntry> SUPER_TYPE_CALL_ENTRY =
            new KtPlaceHolderStubElementType<>("SUPER_TYPE_CALL_ENTRY", KtSuperTypeCallEntry.class);
    @NotNull KtPlaceHolderStubElementType<KtSuperTypeEntry> SUPER_TYPE_ENTRY =
            new KtPlaceHolderStubElementType<>("SUPER_TYPE_ENTRY", KtSuperTypeEntry.class);
    @NotNull KtPlaceHolderStubElementType<KtConstructorCalleeExpression> CONSTRUCTOR_CALLEE =
            new KtPlaceHolderStubElementType<>("CONSTRUCTOR_CALLEE", KtConstructorCalleeExpression.class);

    @NotNull KtContextReceiverElementType CONTEXT_RECEIVER = new KtContextReceiverElementType("CONTEXT_RECEIVER");

    @SuppressWarnings({"unchecked", "rawtypes"})
    @NotNull KtPlaceHolderStubElementType<KtContextParameterList> CONTEXT_PARAMETER_LIST =
            new KtPlaceHolderStubElementType("CONTEXT_PARAMETER_LIST", KtContextReceiverList.class);

    @NotNull KtConstantExpressionElementType NULL                = new KtConstantExpressionElementType("NULL");
    @NotNull KtConstantExpressionElementType BOOLEAN_CONSTANT    = new KtConstantExpressionElementType("BOOLEAN_CONSTANT");
    @NotNull KtConstantExpressionElementType FLOAT_CONSTANT      = new KtConstantExpressionElementType("FLOAT_CONSTANT");
    @NotNull KtConstantExpressionElementType CHARACTER_CONSTANT  = new KtConstantExpressionElementType("CHARACTER_CONSTANT");
    @NotNull KtConstantExpressionElementType INTEGER_CONSTANT    = new KtConstantExpressionElementType("INTEGER_CONSTANT");
    @NotNull KtClassLiteralExpressionElementType CLASS_LITERAL_EXPRESSION = new KtClassLiteralExpressionElementType("CLASS_LITERAL_EXPRESSION");
    @NotNull KtCollectionLiteralExpressionElementType COLLECTION_LITERAL_EXPRESSION = new KtCollectionLiteralExpressionElementType("COLLECTION_LITERAL_EXPRESSION");

    @NotNull KtPlaceHolderStubElementType<KtStringTemplateExpression> STRING_TEMPLATE =
            new KtPlaceHolderStubElementType<>("STRING_TEMPLATE", KtStringTemplateExpression.class);

    @NotNull KtBlockStringTemplateEntryElementType LONG_STRING_TEMPLATE_ENTRY =
            new KtBlockStringTemplateEntryElementType("LONG_STRING_TEMPLATE_ENTRY");

    @NotNull KtPlaceHolderWithTextStubElementType<KtSimpleNameStringTemplateEntry> SHORT_STRING_TEMPLATE_ENTRY =
            new KtPlaceHolderWithTextStubElementType<>("SHORT_STRING_TEMPLATE_ENTRY", KtSimpleNameStringTemplateEntry.class);

    @NotNull KtPlaceHolderWithTextStubElementType<KtLiteralStringTemplateEntry> LITERAL_STRING_TEMPLATE_ENTRY =
            new KtPlaceHolderWithTextStubElementType<>("LITERAL_STRING_TEMPLATE_ENTRY", KtLiteralStringTemplateEntry.class);

    @NotNull KtPlaceHolderWithTextStubElementType<KtEscapeStringTemplateEntry> ESCAPE_STRING_TEMPLATE_ENTRY =
            new KtPlaceHolderWithTextStubElementType<>("ESCAPE_STRING_TEMPLATE_ENTRY", KtEscapeStringTemplateEntry.class);

    @NotNull KtScriptElementType SCRIPT = new KtScriptElementType("SCRIPT");

    @NotNull KtStringInterpolationPrefixElementType STRING_INTERPOLATION_PREFIX = new KtStringInterpolationPrefixElementType("STRING_INTERPOLATION_PREFIX");
}
