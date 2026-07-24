/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.*;

public interface KtStubElementTypes {
    @NotNull KtClassElementType CLASS = KtClassElementType.INSTANCE;
    @NotNull KtFunctionElementType FUNCTION = new KtFunctionElementType("FUN");
    @NotNull KtPropertyElementType PROPERTY = new KtPropertyElementType("PROPERTY");
    @NotNull KtPropertyAccessorElementType PROPERTY_ACCESSOR = KtPropertyAccessorElementType.INSTANCE;
    @NotNull KtBackingFieldElementType BACKING_FIELD = new KtBackingFieldElementType("BACKING_FIELD");
    @NotNull KtDestructuringDeclarationElementType DESTRUCTURING_DECLARATION = KtDestructuringDeclarationElementType.INSTANCE;
    @NotNull KtTypeAliasElementType TYPEALIAS = new KtTypeAliasElementType("TYPEALIAS");

    @NotNull KtEnumEntryElementType ENUM_ENTRY = KtEnumEntryElementType.INSTANCE;
    @NotNull KtObjectElementType OBJECT_DECLARATION = new KtObjectElementType("OBJECT_DECLARATION");
    @NotNull KtPlaceHolderStubElementType<KtClassInitializer> CLASS_INITIALIZER =
            new KtPlaceHolderStubElementType<>("CLASS_INITIALIZER", KtClassInitializer.class);
    @NotNull KtPlaceHolderStubElementType<KtScriptInitializer> SCRIPT_INITIALIZER = KtScriptInitializerElementType.INSTANCE;
    @NotNull KtSecondaryConstructorElementType SECONDARY_CONSTRUCTOR =
            new KtSecondaryConstructorElementType("SECONDARY_CONSTRUCTOR");
    @NotNull KtPrimaryConstructorElementType PRIMARY_CONSTRUCTOR =
            new KtPrimaryConstructorElementType("PRIMARY_CONSTRUCTOR");

    @NotNull KtParameterElementType VALUE_PARAMETER = new KtParameterElementType("VALUE_PARAMETER");
    @NotNull KtPlaceHolderStubElementType<KtParameterList> VALUE_PARAMETER_LIST =
            new KtPlaceHolderStubElementType<>("VALUE_PARAMETER_LIST", KtParameterList.class);

    @NotNull KtTypeParameterElementType TYPE_PARAMETER = new KtTypeParameterElementType("TYPE_PARAMETER");
    @NotNull KtPlaceHolderStubElementType<KtTypeParameterList> TYPE_PARAMETER_LIST =
            new KtPlaceHolderStubElementType<>("TYPE_PARAMETER_LIST", KtTypeParameterList.class);

    @NotNull KtAnnotationEntryElementType ANNOTATION_ENTRY = KtAnnotationEntryElementType.INSTANCE;
    @NotNull KtPlaceHolderStubElementType<KtAnnotation> ANNOTATION =
            new KtPlaceHolderStubElementType<>("ANNOTATION", KtAnnotation.class);

    @NotNull KtAnnotationUseSiteTargetElementType ANNOTATION_TARGET = new KtAnnotationUseSiteTargetElementType("ANNOTATION_TARGET");

    @NotNull KtPlaceHolderStubElementType<KtClassBody> CLASS_BODY =
            new KtPlaceHolderStubElementType<>("CLASS_BODY", KtClassBody.class);

    @NotNull KtPlaceHolderStubElementType<KtCompanionBlock> COMPANION_BLOCK =
            new KtPlaceHolderStubElementType<>("COMPANION_BLOCK", KtCompanionBlock.class);

    @NotNull KtPlaceHolderStubElementType<KtImportList> IMPORT_LIST =
            new KtPlaceHolderStubElementType<>("IMPORT_LIST", KtImportList.class);

    @NotNull KtPlaceHolderStubElementType<KtFileAnnotationList> FILE_ANNOTATION_LIST =
            new KtPlaceHolderStubElementType<>("FILE_ANNOTATION_LIST", KtFileAnnotationList.class);

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

    @NotNull KtTypeCodeFragmentType TYPE_CODE_FRAGMENT = new KtTypeCodeFragmentType();
    @NotNull KtExpressionCodeFragmentType EXPRESSION_CODE_FRAGMENT = new KtExpressionCodeFragmentType();
    @NotNull KtBlockCodeFragmentType BLOCK_CODE_FRAGMENT = new KtBlockCodeFragmentType();

    @NotNull KtTypeProjectionElementType TYPE_PROJECTION = new KtTypeProjectionElementType("TYPE_PROJECTION");

    @NotNull KtPlaceHolderStubElementType<KtFunctionTypeReceiver> FUNCTION_TYPE_RECEIVER =
            new KtPlaceHolderStubElementType<>("FUNCTION_TYPE_RECEIVER", KtFunctionTypeReceiver.class);

    @NotNull KtNameReferenceExpressionElementType REFERENCE_EXPRESSION = new KtNameReferenceExpressionElementType("REFERENCE_EXPRESSION");
    @NotNull KtDotQualifiedExpressionElementType DOT_QUALIFIED_EXPRESSION = new KtDotQualifiedExpressionElementType("DOT_QUALIFIED_EXPRESSION");
    @NotNull KtPlaceHolderStubElementType<KtCallExpression> CALL_EXPRESSION = KtCallExpressionElementType.INSTANCE;
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
            new KtStringTemplateExpressionElementType("STRING_TEMPLATE");

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
