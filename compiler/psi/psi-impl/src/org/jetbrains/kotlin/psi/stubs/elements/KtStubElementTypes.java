/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.lang.ASTNode;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPlaceHolderStubImpl;

public interface KtStubElementTypes {
    KtClassElementType CLASS = KtClassElementType.INSTANCE;
    KtFunctionElementType FUNCTION = new KtFunctionElementType("FUN");
    KtPropertyElementType PROPERTY = new KtPropertyElementType("PROPERTY");
    KtPropertyAccessorElementType PROPERTY_ACCESSOR = KtPropertyAccessorElementType.INSTANCE;
    KtBackingFieldElementType BACKING_FIELD = new KtBackingFieldElementType("BACKING_FIELD");
    KtTypeAliasElementType TYPEALIAS = new KtTypeAliasElementType("TYPEALIAS");

    KtEnumEntryElementType ENUM_ENTRY = KtEnumEntryElementType.INSTANCE;
    KtObjectElementType OBJECT_DECLARATION = new KtObjectElementType("OBJECT_DECLARATION");
    KtPlaceHolderStubElementType<KtClassInitializer> CLASS_INITIALIZER =
            new KtPlaceHolderStubElementType<>("CLASS_INITIALIZER", KtClassInitializer::new, KtClassInitializer::new, KtClassInitializer[]::new, false);
    KtPlaceHolderStubElementType<KtScriptInitializer> SCRIPT_INITIALIZER = KtScriptInitializerElementType.INSTANCE;
    KtSecondaryConstructorElementType SECONDARY_CONSTRUCTOR =
            new KtSecondaryConstructorElementType("SECONDARY_CONSTRUCTOR");
    KtPrimaryConstructorElementType PRIMARY_CONSTRUCTOR =
            new KtPrimaryConstructorElementType("PRIMARY_CONSTRUCTOR");

    KtParameterElementType VALUE_PARAMETER = new KtParameterElementType("VALUE_PARAMETER");
    KtPlaceHolderStubElementType<KtParameterList> VALUE_PARAMETER_LIST =
            new KtPlaceHolderStubElementType<>("VALUE_PARAMETER_LIST", KtParameterList::new, KtParameterList::new, KtParameterList[]::new, false);

    KtTypeParameterElementType TYPE_PARAMETER = new KtTypeParameterElementType("TYPE_PARAMETER");
    KtPlaceHolderStubElementType<KtTypeParameterList> TYPE_PARAMETER_LIST =
            new KtPlaceHolderStubElementType<>("TYPE_PARAMETER_LIST", KtTypeParameterList::new, KtTypeParameterList::new, KtTypeParameterList[]::new, false);

    KtAnnotationEntryElementType ANNOTATION_ENTRY = KtAnnotationEntryElementType.INSTANCE;
    KtPlaceHolderStubElementType<KtAnnotation> ANNOTATION =
            new KtPlaceHolderStubElementType<>("ANNOTATION", KtAnnotation::new, KtAnnotation::new, KtAnnotation[]::new, false);

    KtAnnotationUseSiteTargetElementType ANNOTATION_TARGET = new KtAnnotationUseSiteTargetElementType("ANNOTATION_TARGET");

    KtPlaceHolderStubElementType<KtClassBody> CLASS_BODY =
            new KtPlaceHolderStubElementType<>("CLASS_BODY", KtClassBody::new, KtClassBody::new, KtClassBody[]::new, false);

    KtPlaceHolderStubElementType<KtCompanionBlock> COMPANION_BLOCK =
            new KtPlaceHolderStubElementType<>("COMPANION_BLOCK", KtCompanionBlock::new, KtCompanionBlock::new, KtCompanionBlock[]::new, false);

    KtPlaceHolderStubElementType<KtImportList> IMPORT_LIST =
            new KtPlaceHolderStubElementType<>("IMPORT_LIST", KtImportList::new, KtImportList::new, KtImportList[]::new, false);

    KtPlaceHolderStubElementType<KtFileAnnotationList> FILE_ANNOTATION_LIST =
            new KtPlaceHolderStubElementType<>("FILE_ANNOTATION_LIST", KtFileAnnotationList::new, KtFileAnnotationList::new, KtFileAnnotationList[]::new, false);

    KtImportDirectiveElementType IMPORT_DIRECTIVE = new KtImportDirectiveElementType("IMPORT_DIRECTIVE");

    KtImportAliasElementType IMPORT_ALIAS = new KtImportAliasElementType("IMPORT_ALIAS");

    KtPlaceHolderStubElementType<KtPackageDirective> PACKAGE_DIRECTIVE =
            new KtPlaceHolderStubElementType<>("PACKAGE_DIRECTIVE", KtPackageDirective::new, KtPackageDirective::new, KtPackageDirective[]::new, false);

    KtModifierListElementType<KtDeclarationModifierList> MODIFIER_LIST =
            new KtModifierListElementType<>("MODIFIER_LIST", KtDeclarationModifierList::new, KtDeclarationModifierList::new, KtDeclarationModifierList[]::new);

    KtPlaceHolderStubElementType<KtTypeConstraintList> TYPE_CONSTRAINT_LIST =
            new KtPlaceHolderStubElementType<>("TYPE_CONSTRAINT_LIST", KtTypeConstraintList::new, KtTypeConstraintList::new, KtTypeConstraintList[]::new, false);

    KtPlaceHolderStubElementType<KtTypeConstraint> TYPE_CONSTRAINT =
            new KtPlaceHolderStubElementType<>("TYPE_CONSTRAINT", KtTypeConstraint::new, KtTypeConstraint::new, KtTypeConstraint[]::new, false);

    KtPlaceHolderStubElementType<KtNullableType> NULLABLE_TYPE =
            new KtPlaceHolderStubElementType<>("NULLABLE_TYPE", KtNullableType::new, KtNullableType::new, KtNullableType[]::new, false);

    KtPlaceHolderStubElementType<KtIntersectionType> INTERSECTION_TYPE =
            new KtPlaceHolderStubElementType<>("INTERSECTION_TYPE", KtIntersectionType::new, KtIntersectionType::new, KtIntersectionType[]::new, false);

    KtPlaceHolderStubElementType<KtTypeReference> TYPE_REFERENCE =
            new KtPlaceHolderStubElementType<>("TYPE_REFERENCE", KtTypeReference::new, KtTypeReference::new, KtTypeReference[]::new, false);

    KtUserTypeElementType USER_TYPE = new KtUserTypeElementType("USER_TYPE");
    KtPlaceHolderStubElementType<KtDynamicType> DYNAMIC_TYPE =
            new KtPlaceHolderStubElementType<>("DYNAMIC_TYPE", KtDynamicType::new, KtDynamicType::new, KtDynamicType[]::new, false);

    KtFunctionTypeElementType FUNCTION_TYPE = new KtFunctionTypeElementType("FUNCTION_TYPE");

    KtTypeCodeFragmentType TYPE_CODE_FRAGMENT = new KtTypeCodeFragmentType();
    KtExpressionCodeFragmentType EXPRESSION_CODE_FRAGMENT = new KtExpressionCodeFragmentType();
    KtBlockCodeFragmentType BLOCK_CODE_FRAGMENT = new KtBlockCodeFragmentType();

    KtTypeProjectionElementType TYPE_PROJECTION = new KtTypeProjectionElementType("TYPE_PROJECTION");

    KtPlaceHolderStubElementType<KtFunctionTypeReceiver> FUNCTION_TYPE_RECEIVER =
            new KtPlaceHolderStubElementType<>("FUNCTION_TYPE_RECEIVER", KtFunctionTypeReceiver::new, KtFunctionTypeReceiver::new, KtFunctionTypeReceiver[]::new, false);

    KtNameReferenceExpressionElementType REFERENCE_EXPRESSION = new KtNameReferenceExpressionElementType("REFERENCE_EXPRESSION");
    KtDotQualifiedExpressionElementType DOT_QUALIFIED_EXPRESSION = new KtDotQualifiedExpressionElementType("DOT_QUALIFIED_EXPRESSION");
    KtPlaceHolderStubElementType<KtCallExpression> CALL_EXPRESSION = KtCallExpressionElementType.INSTANCE;
    KtEnumEntrySuperClassReferenceExpressionElementType
            ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION =
            new KtEnumEntrySuperClassReferenceExpressionElementType("ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION");
    KtPlaceHolderStubElementType<KtTypeArgumentList> TYPE_ARGUMENT_LIST =
            new KtPlaceHolderStubElementType<>("TYPE_ARGUMENT_LIST", KtTypeArgumentList::new, KtTypeArgumentList::new, KtTypeArgumentList[]::new, false);

    KtPlaceHolderStubElementType<KtValueArgumentList> VALUE_ARGUMENT_LIST =
            new KtPlaceHolderStubElementType<>("VALUE_ARGUMENT_LIST", KtValueArgumentList::new, KtValueArgumentList::new, KtValueArgumentList[]::new, false);

    KtValueArgumentElementType<KtValueArgument> VALUE_ARGUMENT =
            new KtValueArgumentElementType<>("VALUE_ARGUMENT", KtValueArgument::new, KtValueArgument::new, KtValueArgument[]::new);

    KtPlaceHolderStubElementType<KtContractEffectList> CONTRACT_EFFECT_LIST =
            new KtContractEffectListElementType("CONTRACT_EFFECT_LIST");

    KtContractEffectElementType CONTRACT_EFFECT =
            new KtContractEffectElementType("CONTRACT_EFFECT", KtContractEffect::new, KtContractEffect::new, KtContractEffect[]::new);

    KtValueArgumentElementType<KtLambdaArgument> LAMBDA_ARGUMENT =
            new KtValueArgumentElementType<>("LAMBDA_ARGUMENT", KtLambdaArgument::new, KtLambdaArgument::new, KtLambdaArgument[]::new);

    KtPlaceHolderStubElementType<KtValueArgumentName> VALUE_ARGUMENT_NAME =
            new KtPlaceHolderStubElementType<>("VALUE_ARGUMENT_NAME", KtValueArgumentName::new, KtValueArgumentName::new, KtValueArgumentName[]::new, false);

    KtPlaceHolderStubElementType<KtSuperTypeList> SUPER_TYPE_LIST =
            new KtPlaceHolderStubElementType<>("SUPER_TYPE_LIST", KtSuperTypeList::new, KtSuperTypeList::new, KtSuperTypeList[]::new, false);

    KtPlaceHolderStubElementType<KtInitializerList> INITIALIZER_LIST =
            new KtPlaceHolderStubElementType<>("INITIALIZER_LIST", KtInitializerList::new, KtInitializerList::new, KtInitializerList[]::new, false);

    KtPlaceHolderStubElementType<KtDelegatedSuperTypeEntry> DELEGATED_SUPER_TYPE_ENTRY =
            new KtPlaceHolderStubElementType<>("DELEGATED_SUPER_TYPE_ENTRY", KtDelegatedSuperTypeEntry::new, KtDelegatedSuperTypeEntry::new, KtDelegatedSuperTypeEntry[]::new, false);

    KtPlaceHolderStubElementType<KtSuperTypeCallEntry> SUPER_TYPE_CALL_ENTRY =
            new KtPlaceHolderStubElementType<>("SUPER_TYPE_CALL_ENTRY", KtSuperTypeCallEntry::new, KtSuperTypeCallEntry::new, KtSuperTypeCallEntry[]::new, false);
    KtPlaceHolderStubElementType<KtSuperTypeEntry> SUPER_TYPE_ENTRY =
            new KtPlaceHolderStubElementType<>("SUPER_TYPE_ENTRY", KtSuperTypeEntry::new, KtSuperTypeEntry::new, KtSuperTypeEntry[]::new, false);
    KtPlaceHolderStubElementType<KtConstructorCalleeExpression> CONSTRUCTOR_CALLEE =
            new KtPlaceHolderStubElementType<>("CONSTRUCTOR_CALLEE", KtConstructorCalleeExpression::new, KtConstructorCalleeExpression::new, KtConstructorCalleeExpression[]::new, false);

    KtContextReceiverElementType CONTEXT_RECEIVER = new KtContextReceiverElementType("CONTEXT_RECEIVER");

    @SuppressWarnings({"unchecked", "rawtypes"})
    KtPlaceHolderStubElementType<KtContextParameterList> CONTEXT_PARAMETER_LIST =
            new KtPlaceHolderStubElementType("CONTEXT_PARAMETER_LIST",
                    (java.util.function.Function<ASTNode, ?>) KtContextReceiverList::new,
                    (java.util.function.Function<KotlinPlaceHolderStubImpl, ?>) (KotlinPlaceHolderStubImpl stub) -> new KtContextReceiverList(stub),
                    KtContextReceiverList[]::new, false);

    KtConstantExpressionElementType NULL                = new KtConstantExpressionElementType("NULL");
    KtConstantExpressionElementType BOOLEAN_CONSTANT    = new KtConstantExpressionElementType("BOOLEAN_CONSTANT");
    KtConstantExpressionElementType FLOAT_CONSTANT      = new KtConstantExpressionElementType("FLOAT_CONSTANT");
    KtConstantExpressionElementType CHARACTER_CONSTANT  = new KtConstantExpressionElementType("CHARACTER_CONSTANT");
    KtConstantExpressionElementType INTEGER_CONSTANT    = new KtConstantExpressionElementType("INTEGER_CONSTANT");
    KtClassLiteralExpressionElementType CLASS_LITERAL_EXPRESSION = new KtClassLiteralExpressionElementType("CLASS_LITERAL_EXPRESSION");
    KtCollectionLiteralExpressionElementType COLLECTION_LITERAL_EXPRESSION = new KtCollectionLiteralExpressionElementType("COLLECTION_LITERAL_EXPRESSION");

    KtPlaceHolderStubElementType<KtStringTemplateExpression> STRING_TEMPLATE =
            new KtStringTemplateExpressionElementType("STRING_TEMPLATE");

    KtBlockStringTemplateEntryElementType LONG_STRING_TEMPLATE_ENTRY =
            new KtBlockStringTemplateEntryElementType("LONG_STRING_TEMPLATE_ENTRY");

    KtPlaceHolderWithTextStubElementType<KtSimpleNameStringTemplateEntry> SHORT_STRING_TEMPLATE_ENTRY =
            new KtPlaceHolderWithTextStubElementType<>("SHORT_STRING_TEMPLATE_ENTRY", KtSimpleNameStringTemplateEntry::new, KtSimpleNameStringTemplateEntry::new, KtSimpleNameStringTemplateEntry[]::new);

    KtPlaceHolderWithTextStubElementType<KtLiteralStringTemplateEntry> LITERAL_STRING_TEMPLATE_ENTRY =
            new KtPlaceHolderWithTextStubElementType<>("LITERAL_STRING_TEMPLATE_ENTRY", KtLiteralStringTemplateEntry::new, KtLiteralStringTemplateEntry::new, KtLiteralStringTemplateEntry[]::new);

    KtPlaceHolderWithTextStubElementType<KtEscapeStringTemplateEntry> ESCAPE_STRING_TEMPLATE_ENTRY =
            new KtPlaceHolderWithTextStubElementType<>("ESCAPE_STRING_TEMPLATE_ENTRY", KtEscapeStringTemplateEntry::new, KtEscapeStringTemplateEntry::new, KtEscapeStringTemplateEntry[]::new);

    KtScriptElementType SCRIPT = new KtScriptElementType("SCRIPT");

    KtStringInterpolationPrefixElementType STRING_INTERPOLATION_PREFIX = new KtStringInterpolationPrefixElementType("STRING_INTERPOLATION_PREFIX");
}
