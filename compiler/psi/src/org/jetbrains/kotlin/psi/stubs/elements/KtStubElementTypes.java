/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;

public class KtStubElementTypes {
    static {
        // It forces initializing tokens in strict order that provides possibility to match indexes and static identifiers
        @SuppressWarnings("unused")
        IElementType dependentTokensInit = KtTokens.EOF;
    }

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
}
