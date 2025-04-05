/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.kotlin.ElementTypeChecker;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;

import static org.jetbrains.kotlin.lexer.KtTokens.ACTUAL_KEYWORD_INDEX;

@SuppressWarnings("WeakerAccess") // Let all static identifiers be public as well as corresponding elements
public class KtStubElementTypes {
    static {
        // It forces initializing tokens in strict order that provides possibility to match indexes and static identifiers
        @SuppressWarnings("unused")
        IElementType dependentTokensInit = KtTokens.EOF;
    }

    public static final int CLASS_INDEX = ACTUAL_KEYWORD_INDEX + 1;
    public static final int FUNCTION_INDEX = CLASS_INDEX + 1;
    public static final int PROPERTY_INDEX = FUNCTION_INDEX + 1;
    public static final int PROPERTY_ACCESSOR_INDEX = PROPERTY_INDEX + 1;
    public static final int BACKING_FIELD_INDEX = PROPERTY_ACCESSOR_INDEX + 1;
    public static final int TYPEALIAS_INDEX = BACKING_FIELD_INDEX + 1;
    public static final int ENUM_ENTRY_INDEX = TYPEALIAS_INDEX + 1;
    public static final int OBJECT_DECLARATION_INDEX = ENUM_ENTRY_INDEX + 1;
    public static final int CLASS_INITIALIZER_INDEX = OBJECT_DECLARATION_INDEX + 1;
    public static final int SECONDARY_CONSTRUCTOR_INDEX = CLASS_INITIALIZER_INDEX + 1;
    public static final int PRIMARY_CONSTRUCTOR_INDEX = SECONDARY_CONSTRUCTOR_INDEX + 1;
    public static final int VALUE_PARAMETER_INDEX = PRIMARY_CONSTRUCTOR_INDEX + 1;
    public static final int VALUE_PARAMETER_LIST_INDEX = VALUE_PARAMETER_INDEX + 1;
    public static final int TYPE_PARAMETER_INDEX = VALUE_PARAMETER_LIST_INDEX + 1;
    public static final int TYPE_PARAMETER_LIST_INDEX = TYPE_PARAMETER_INDEX + 1;
    public static final int ANNOTATION_ENTRY_INDEX = TYPE_PARAMETER_LIST_INDEX + 1;
    public static final int ANNOTATION_INDEX = ANNOTATION_ENTRY_INDEX + 1;
    public static final int ANNOTATION_TARGET_INDEX = ANNOTATION_INDEX + 1;
    public static final int CLASS_BODY_INDEX = ANNOTATION_TARGET_INDEX + 1;
    public static final int IMPORT_LIST_INDEX = CLASS_BODY_INDEX + 1;
    public static final int FILE_ANNOTATION_LIST_INDEX = IMPORT_LIST_INDEX + 1;
    public static final int IMPORT_DIRECTIVE_INDEX = FILE_ANNOTATION_LIST_INDEX + 1;
    public static final int IMPORT_ALIAS_INDEX = IMPORT_DIRECTIVE_INDEX + 1;
    public static final int PACKAGE_DIRECTIVE_INDEX = IMPORT_ALIAS_INDEX + 1;
    public static final int MODIFIER_LIST_INDEX = PACKAGE_DIRECTIVE_INDEX + 1;
    public static final int TYPE_CONSTRAINT_LIST_INDEX = MODIFIER_LIST_INDEX + 1;
    public static final int TYPE_CONSTRAINT_INDEX = TYPE_CONSTRAINT_LIST_INDEX + 1;
    public static final int NULLABLE_TYPE_INDEX = TYPE_CONSTRAINT_INDEX + 1;
    public static final int INTERSECTION_TYPE_INDEX = NULLABLE_TYPE_INDEX + 1;
    public static final int TYPE_REFERENCE_INDEX = INTERSECTION_TYPE_INDEX + 1;
    public static final int USER_TYPE_INDEX = TYPE_REFERENCE_INDEX + 1;
    public static final int DYNAMIC_TYPE_INDEX = USER_TYPE_INDEX + 1;
    public static final int FUNCTION_TYPE_INDEX = DYNAMIC_TYPE_INDEX + 1;
    public static final int TYPE_CODE_FRAGMENT_INDEX = FUNCTION_TYPE_INDEX + 1;
    public static final int EXPRESSION_CODE_FRAGMENT_INDEX = TYPE_CODE_FRAGMENT_INDEX + 1;
    public static final int BLOCK_CODE_FRAGMENT_INDEX = EXPRESSION_CODE_FRAGMENT_INDEX + 1;
    public static final int TYPE_PROJECTION_INDEX = BLOCK_CODE_FRAGMENT_INDEX + 1;
    public static final int FUNCTION_TYPE_RECEIVER_INDEX = TYPE_PROJECTION_INDEX + 1;
    public static final int REFERENCE_EXPRESSION_INDEX = FUNCTION_TYPE_RECEIVER_INDEX + 1;
    public static final int DOT_QUALIFIED_EXPRESSION_INDEX = REFERENCE_EXPRESSION_INDEX + 1;
    public static final int ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION_INDEX = DOT_QUALIFIED_EXPRESSION_INDEX + 1;
    public static final int TYPE_ARGUMENT_LIST_INDEX = ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION_INDEX + 1;
    public static final int VALUE_ARGUMENT_LIST_INDEX = TYPE_ARGUMENT_LIST_INDEX + 1;
    public static final int VALUE_ARGUMENT_INDEX = VALUE_ARGUMENT_LIST_INDEX + 1;
    public static final int CONTRACT_EFFECT_LIST_INDEX = VALUE_ARGUMENT_INDEX + 1;
    public static final int CONTRACT_EFFECT_INDEX = CONTRACT_EFFECT_LIST_INDEX + 1;
    public static final int LAMBDA_ARGUMENT_INDEX = CONTRACT_EFFECT_INDEX + 1;
    public static final int VALUE_ARGUMENT_NAME_INDEX = LAMBDA_ARGUMENT_INDEX + 1;
    public static final int SUPER_TYPE_LIST_INDEX = VALUE_ARGUMENT_NAME_INDEX + 1;
    public static final int INITIALIZER_LIST_INDEX = SUPER_TYPE_LIST_INDEX + 1;
    public static final int DELEGATED_SUPER_TYPE_ENTRY_INDEX = INITIALIZER_LIST_INDEX + 1;
    public static final int SUPER_TYPE_CALL_ENTRY_INDEX = DELEGATED_SUPER_TYPE_ENTRY_INDEX + 1;
    public static final int SUPER_TYPE_ENTRY_INDEX = SUPER_TYPE_CALL_ENTRY_INDEX + 1;
    public static final int CONSTRUCTOR_CALLEE_INDEX = SUPER_TYPE_ENTRY_INDEX + 1;
    public static final int CONTEXT_RECEIVER_INDEX = CONSTRUCTOR_CALLEE_INDEX + 1;
    public static final int CONTEXT_RECEIVER_LIST_INDEX = CONTEXT_RECEIVER_INDEX + 1;
    public static final int NULL_INDEX = CONTEXT_RECEIVER_LIST_INDEX + 1;
    public static final int BOOLEAN_CONSTANT_INDEX = NULL_INDEX + 1;
    public static final int FLOAT_CONSTANT_INDEX = BOOLEAN_CONSTANT_INDEX + 1;
    public static final int CHARACTER_CONSTANT_INDEX = FLOAT_CONSTANT_INDEX + 1;
    public static final int INTEGER_CONSTANT_INDEX = CHARACTER_CONSTANT_INDEX + 1;
    public static final int CLASS_LITERAL_EXPRESSION_INDEX = INTEGER_CONSTANT_INDEX + 1;
    public static final int COLLECTION_LITERAL_EXPRESSION_INDEX = CLASS_LITERAL_EXPRESSION_INDEX + 1;
    public static final int STRING_TEMPLATE_INDEX = COLLECTION_LITERAL_EXPRESSION_INDEX + 1;
    public static final int LONG_STRING_TEMPLATE_ENTRY_INDEX = STRING_TEMPLATE_INDEX + 1;
    public static final int SHORT_STRING_TEMPLATE_ENTRY_INDEX = LONG_STRING_TEMPLATE_ENTRY_INDEX + 1;
    public static final int LITERAL_STRING_TEMPLATE_ENTRY_INDEX = SHORT_STRING_TEMPLATE_ENTRY_INDEX + 1;
    public static final int ESCAPE_STRING_TEMPLATE_ENTRY_INDEX = LITERAL_STRING_TEMPLATE_ENTRY_INDEX + 1;
    public static final int SCRIPT_INDEX = ESCAPE_STRING_TEMPLATE_ENTRY_INDEX + 1;
    public static final int STRING_INTERPOLATION_PREFIX_INDEX = SCRIPT_INDEX + 1;

    public static final KtClassElementType CLASS = new KtClassElementType("CLASS");
    public static final KtFunctionElementType FUNCTION = new KtFunctionElementType("FUN");
    public static final KtPropertyElementType PROPERTY = new KtPropertyElementType("PROPERTY");
    public static final KtPropertyAccessorElementType PROPERTY_ACCESSOR = new KtPropertyAccessorElementType("PROPERTY_ACCESSOR");
    public static final KtBackingFieldElementType BACKING_FIELD = new KtBackingFieldElementType("BACKING_FIELD");
    public static final KtTypeAliasElementType TYPEALIAS = new KtTypeAliasElementType("TYPEALIAS");

    public static final KtClassElementType ENUM_ENTRY = new KtClassElementType("ENUM_ENTRY");
    public static final KtObjectElementType OBJECT_DECLARATION = new KtObjectElementType("OBJECT_DECLARATION");
    public static final KtPlaceHolderStubElementType<KtClassInitializer> CLASS_INITIALIZER =
            new KtPlaceHolderStubElementType<>("CLASS_INITIALIZER", KtClassInitializer.class);
    public static final KtSecondaryConstructorElementType SECONDARY_CONSTRUCTOR =
            new KtSecondaryConstructorElementType("SECONDARY_CONSTRUCTOR");
    public static final KtPrimaryConstructorElementType PRIMARY_CONSTRUCTOR =
            new KtPrimaryConstructorElementType("PRIMARY_CONSTRUCTOR");

    public static final KtParameterElementType VALUE_PARAMETER = new KtParameterElementType("VALUE_PARAMETER");
    public static final KtPlaceHolderStubElementType<KtParameterList> VALUE_PARAMETER_LIST =
            new KtPlaceHolderStubElementType<>("VALUE_PARAMETER_LIST", KtParameterList.class);

    public static final KtTypeParameterElementType TYPE_PARAMETER = new KtTypeParameterElementType("TYPE_PARAMETER");
    public static final KtPlaceHolderStubElementType<KtTypeParameterList> TYPE_PARAMETER_LIST =
            new KtPlaceHolderStubElementType<>("TYPE_PARAMETER_LIST", KtTypeParameterList.class);

    public static final KtAnnotationEntryElementType ANNOTATION_ENTRY = new KtAnnotationEntryElementType("ANNOTATION_ENTRY");
    public static final KtPlaceHolderStubElementType<KtAnnotation> ANNOTATION =
            new KtPlaceHolderStubElementType<>("ANNOTATION", KtAnnotation.class);

    public static final KtAnnotationUseSiteTargetElementType ANNOTATION_TARGET = new KtAnnotationUseSiteTargetElementType("ANNOTATION_TARGET");

    public static final KtPlaceHolderStubElementType<KtClassBody> CLASS_BODY =
            new KtPlaceHolderStubElementType<>("CLASS_BODY", KtClassBody.class);

    public static final KtPlaceHolderStubElementType<KtImportList> IMPORT_LIST =
            new KtPlaceHolderStubElementType<>("IMPORT_LIST", KtImportList.class);

    public static final KtPlaceHolderStubElementType<KtFileAnnotationList> FILE_ANNOTATION_LIST =
            new KtPlaceHolderStubElementType<>("FILE_ANNOTATION_LIST", KtFileAnnotationList.class);

    public static final KtImportDirectiveElementType IMPORT_DIRECTIVE = new KtImportDirectiveElementType("IMPORT_DIRECTIVE");

    public static final KtImportAliasElementType IMPORT_ALIAS = new KtImportAliasElementType("IMPORT_ALIAS");

    public static final KtPlaceHolderStubElementType<KtPackageDirective> PACKAGE_DIRECTIVE =
            new KtPlaceHolderStubElementType<>("PACKAGE_DIRECTIVE", KtPackageDirective.class);

    public static final KtModifierListElementType<KtDeclarationModifierList> MODIFIER_LIST =
            new KtModifierListElementType<>("MODIFIER_LIST", KtDeclarationModifierList.class);

    public static final KtPlaceHolderStubElementType<KtTypeConstraintList> TYPE_CONSTRAINT_LIST =
            new KtPlaceHolderStubElementType<>("TYPE_CONSTRAINT_LIST", KtTypeConstraintList.class);

    public static final KtPlaceHolderStubElementType<KtTypeConstraint> TYPE_CONSTRAINT =
            new KtPlaceHolderStubElementType<>("TYPE_CONSTRAINT", KtTypeConstraint.class);

    public static final KtPlaceHolderStubElementType<KtNullableType> NULLABLE_TYPE =
            new KtPlaceHolderStubElementType<>("NULLABLE_TYPE", KtNullableType.class);

    public static final KtPlaceHolderStubElementType<KtIntersectionType> INTERSECTION_TYPE =
            new KtPlaceHolderStubElementType<>("INTERSECTION_TYPE", KtIntersectionType.class);

    public static final KtPlaceHolderStubElementType<KtTypeReference> TYPE_REFERENCE =
            new KtPlaceHolderStubElementType<>("TYPE_REFERENCE", KtTypeReference.class);

    public static final KtUserTypeElementType USER_TYPE = new KtUserTypeElementType("USER_TYPE");
    public static final KtPlaceHolderStubElementType<KtDynamicType> DYNAMIC_TYPE =
            new KtPlaceHolderStubElementType<>("DYNAMIC_TYPE", KtDynamicType.class);

    public static final KtFunctionTypeElementType FUNCTION_TYPE = new KtFunctionTypeElementType("FUNCTION_TYPE");

    public static final KtTypeCodeFragmentType TYPE_CODE_FRAGMENT = new KtTypeCodeFragmentType();
    public static final KtExpressionCodeFragmentType EXPRESSION_CODE_FRAGMENT = new KtExpressionCodeFragmentType();
    public static final KtBlockCodeFragmentType BLOCK_CODE_FRAGMENT = new KtBlockCodeFragmentType();

    public static final KtTypeProjectionElementType TYPE_PROJECTION = new KtTypeProjectionElementType("TYPE_PROJECTION");

    public static final KtPlaceHolderStubElementType<KtFunctionTypeReceiver> FUNCTION_TYPE_RECEIVER =
            new KtPlaceHolderStubElementType<>("FUNCTION_TYPE_RECEIVER", KtFunctionTypeReceiver.class);

    public static final KtNameReferenceExpressionElementType REFERENCE_EXPRESSION = new KtNameReferenceExpressionElementType("REFERENCE_EXPRESSION");
    public static final KtDotQualifiedExpressionElementType DOT_QUALIFIED_EXPRESSION = new KtDotQualifiedExpressionElementType("DOT_QUALIFIED_EXPRESSION");
    public static final KtEnumEntrySuperClassReferenceExpressionElementType
            ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION =
            new KtEnumEntrySuperClassReferenceExpressionElementType("ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION");
    public static final KtPlaceHolderStubElementType<KtTypeArgumentList> TYPE_ARGUMENT_LIST =
            new KtPlaceHolderStubElementType<>("TYPE_ARGUMENT_LIST", KtTypeArgumentList.class);

    public static final KtPlaceHolderStubElementType<KtValueArgumentList> VALUE_ARGUMENT_LIST =
            new KtValueArgumentListElementType("VALUE_ARGUMENT_LIST");

    public static final KtValueArgumentElementType<KtValueArgument> VALUE_ARGUMENT =
            new KtValueArgumentElementType<>("VALUE_ARGUMENT", KtValueArgument.class);

    public static final KtPlaceHolderStubElementType<KtContractEffectList> CONTRACT_EFFECT_LIST =
            new KtContractEffectListElementType("CONTRACT_EFFECT_LIST");

    public static final KtContractEffectElementType CONTRACT_EFFECT =
            new KtContractEffectElementType("CONTRACT_EFFECT", KtContractEffect.class);

    public static final KtValueArgumentElementType<KtLambdaArgument> LAMBDA_ARGUMENT =
            new KtValueArgumentElementType<>("LAMBDA_ARGUMENT", KtLambdaArgument.class);

    public static final KtPlaceHolderStubElementType<KtValueArgumentName> VALUE_ARGUMENT_NAME =
            new KtPlaceHolderStubElementType<>("VALUE_ARGUMENT_NAME", KtValueArgumentName.class);

    public static final KtPlaceHolderStubElementType<KtSuperTypeList> SUPER_TYPE_LIST =
            new KtPlaceHolderStubElementType<>("SUPER_TYPE_LIST", KtSuperTypeList.class);

    public static final KtPlaceHolderStubElementType<KtInitializerList> INITIALIZER_LIST =
            new KtPlaceHolderStubElementType<>("INITIALIZER_LIST", KtInitializerList.class);

    public static final KtPlaceHolderStubElementType<KtDelegatedSuperTypeEntry> DELEGATED_SUPER_TYPE_ENTRY =
            new KtPlaceHolderStubElementType<>("DELEGATED_SUPER_TYPE_ENTRY", KtDelegatedSuperTypeEntry.class);

    public static final KtPlaceHolderStubElementType<KtSuperTypeCallEntry> SUPER_TYPE_CALL_ENTRY =
            new KtPlaceHolderStubElementType<>("SUPER_TYPE_CALL_ENTRY", KtSuperTypeCallEntry.class);
    public static final KtPlaceHolderStubElementType<KtSuperTypeEntry> SUPER_TYPE_ENTRY =
            new KtPlaceHolderStubElementType<>("SUPER_TYPE_ENTRY", KtSuperTypeEntry.class);
    public static final KtPlaceHolderStubElementType<KtConstructorCalleeExpression> CONSTRUCTOR_CALLEE =
            new KtPlaceHolderStubElementType<>("CONSTRUCTOR_CALLEE", KtConstructorCalleeExpression.class);

    public static final KtContextReceiverElementType CONTEXT_RECEIVER = new KtContextReceiverElementType("CONTEXT_RECEIVER");
    public static final KtPlaceHolderStubElementType<KtContextReceiverList> CONTEXT_RECEIVER_LIST =
            new KtPlaceHolderStubElementType<>("CONTEXT_RECEIVER_LIST", KtContextReceiverList.class);

    public static final KtConstantExpressionElementType NULL                = new KtConstantExpressionElementType("NULL");
    public static final KtConstantExpressionElementType BOOLEAN_CONSTANT    = new KtConstantExpressionElementType("BOOLEAN_CONSTANT");
    public static final KtConstantExpressionElementType FLOAT_CONSTANT      = new KtConstantExpressionElementType("FLOAT_CONSTANT");
    public static final KtConstantExpressionElementType CHARACTER_CONSTANT  = new KtConstantExpressionElementType("CHARACTER_CONSTANT");
    public static final KtConstantExpressionElementType INTEGER_CONSTANT    = new KtConstantExpressionElementType("INTEGER_CONSTANT");
    public static final KtClassLiteralExpressionElementType CLASS_LITERAL_EXPRESSION = new KtClassLiteralExpressionElementType("CLASS_LITERAL_EXPRESSION");
    public static final KtCollectionLiteralExpressionElementType COLLECTION_LITERAL_EXPRESSION = new KtCollectionLiteralExpressionElementType("COLLECTION_LITERAL_EXPRESSION");

    public static final KtPlaceHolderStubElementType<KtStringTemplateExpression> STRING_TEMPLATE =
            new KtStringTemplateExpressionElementType("STRING_TEMPLATE");

    public static final TokenSet CONSTANT_EXPRESSIONS_TYPES = TokenSet.create(
            NULL,
            BOOLEAN_CONSTANT,
            FLOAT_CONSTANT,
            CHARACTER_CONSTANT,
            INTEGER_CONSTANT,

            REFERENCE_EXPRESSION,
            DOT_QUALIFIED_EXPRESSION,

            STRING_TEMPLATE,

            CLASS_LITERAL_EXPRESSION,

            COLLECTION_LITERAL_EXPRESSION
    );

    public static final KtBlockStringTemplateEntryElementType LONG_STRING_TEMPLATE_ENTRY =
            new KtBlockStringTemplateEntryElementType("LONG_STRING_TEMPLATE_ENTRY");

    public static final KtPlaceHolderWithTextStubElementType<KtSimpleNameStringTemplateEntry> SHORT_STRING_TEMPLATE_ENTRY =
            new KtPlaceHolderWithTextStubElementType<>("SHORT_STRING_TEMPLATE_ENTRY", KtSimpleNameStringTemplateEntry.class);

    public static final KtPlaceHolderWithTextStubElementType<KtLiteralStringTemplateEntry> LITERAL_STRING_TEMPLATE_ENTRY =
            new KtPlaceHolderWithTextStubElementType<>("LITERAL_STRING_TEMPLATE_ENTRY", KtLiteralStringTemplateEntry.class);

    public static final KtPlaceHolderWithTextStubElementType<KtEscapeStringTemplateEntry> ESCAPE_STRING_TEMPLATE_ENTRY =
            new KtPlaceHolderWithTextStubElementType<>("ESCAPE_STRING_TEMPLATE_ENTRY", KtEscapeStringTemplateEntry.class);

    public static final KtScriptElementType SCRIPT = new KtScriptElementType("SCRIPT");

    public static final KtStringInterpolationPrefixElementType STRING_INTERPOLATION_PREFIX = new KtStringInterpolationPrefixElementType("STRING_INTERPOLATION_PREFIX");

    static {
        ElementTypeChecker.checkExplicitStaticIndexesMatchImplicit(KtStubElementTypes.class);
    }
}
