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

    @NotNull KtNodeType IMPORT_DIRECTIVE = new KtNodeType("IMPORT_DIRECTIVE", KtImportDirective::new);

    @NotNull KtNodeType IMPORT_ALIAS = new KtNodeType("IMPORT_ALIAS", KtImportAlias::new);

    @NotNull KtNodeType PACKAGE_DIRECTIVE = new KtNodeType("PACKAGE_DIRECTIVE", KtPackageDirective::new);

    @NotNull KtNodeType MODIFIER_LIST = new KtNodeType("MODIFIER_LIST", KtDeclarationModifierList::new);

    @NotNull KtNodeType TYPE_CONSTRAINT_LIST = new KtNodeType("TYPE_CONSTRAINT_LIST", KtTypeConstraintList::new);

    @NotNull KtNodeType TYPE_CONSTRAINT = new KtNodeType("TYPE_CONSTRAINT", KtTypeConstraint::new);

    @NotNull KtNodeType NULLABLE_TYPE = new KtNodeType("NULLABLE_TYPE", KtNullableType::new);

    @NotNull KtNodeType INTERSECTION_TYPE = new KtNodeType("INTERSECTION_TYPE", KtIntersectionType::new);

    @NotNull KtNodeType TYPE_REFERENCE = new KtNodeType("TYPE_REFERENCE", KtTypeReference::new);

    @NotNull KtNodeType USER_TYPE = new KtNodeType("USER_TYPE", KtUserType::new);

    @NotNull KtNodeType DYNAMIC_TYPE = new KtNodeType("DYNAMIC_TYPE", KtDynamicType::new);

    @NotNull KtNodeType FUNCTION_TYPE = new KtNodeType("FUNCTION_TYPE", KtFunctionType::new);

    @NotNull KtNodeType TYPE_PROJECTION = new KtNodeType("TYPE_PROJECTION", KtTypeProjection::new);

    @NotNull KtNodeType FUNCTION_TYPE_RECEIVER = new KtNodeType("FUNCTION_TYPE_RECEIVER", KtFunctionTypeReceiver::new);

    @NotNull KtNodeType REFERENCE_EXPRESSION = new KtNodeType("REFERENCE_EXPRESSION", KtNameReferenceExpression::new);

    @NotNull KtNodeType DOT_QUALIFIED_EXPRESSION = new KtNodeType("DOT_QUALIFIED_EXPRESSION", KtDotQualifiedExpression::new);

    @NotNull KtNodeType CALL_EXPRESSION = new KtNodeType("CALL_EXPRESSION", KtCallExpression::new);

    @NotNull KtNodeType OPERATION_REFERENCE = new KtNodeType("OPERATION_REFERENCE", KtOperationReferenceExpression::new);

    @NotNull KtNodeType PREFIX_EXPRESSION = new KtNodeType("PREFIX_EXPRESSION", KtPrefixExpression::new);

    @NotNull KtNodeType POSTFIX_EXPRESSION = new KtNodeType("POSTFIX_EXPRESSION", KtPostfixExpression::new);

    @NotNull KtNodeType BINARY_EXPRESSION = new KtNodeType("BINARY_EXPRESSION", KtBinaryExpression::new);

    @NotNull KtNodeType PARENTHESIZED = new KtNodeType("PARENTHESIZED", KtParenthesizedExpression::new);

    @NotNull KtNodeType OBJECT_LITERAL = new KtNodeType("OBJECT_LITERAL", KtObjectLiteralExpression::new);

    @NotNull KtNodeType ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION =
            new KtNodeType("ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION", KtEnumEntrySuperclassReferenceExpression::new);

    @NotNull KtNodeType TYPE_ARGUMENT_LIST = new KtNodeType("TYPE_ARGUMENT_LIST", KtTypeArgumentList::new);

    @NotNull KtNodeType VALUE_ARGUMENT_LIST = new KtNodeType("VALUE_ARGUMENT_LIST", KtValueArgumentList::new);

    @NotNull KtNodeType VALUE_ARGUMENT = new KtNodeType("VALUE_ARGUMENT", KtValueArgument::new);

    @NotNull KtNodeType CONTRACT_EFFECT_LIST = new KtNodeType("CONTRACT_EFFECT_LIST", KtContractEffectList::new);

    @NotNull KtNodeType CONTRACT_EFFECT = new KtNodeType("CONTRACT_EFFECT", KtContractEffect::new);

    @NotNull KtNodeType LAMBDA_ARGUMENT = new KtNodeType("LAMBDA_ARGUMENT", KtLambdaArgument::new);

    @NotNull KtNodeType VALUE_ARGUMENT_NAME = new KtNodeType("VALUE_ARGUMENT_NAME", KtValueArgumentName::new);

    @NotNull KtNodeType SUPER_TYPE_LIST = new KtNodeType("SUPER_TYPE_LIST", KtSuperTypeList::new);

    @NotNull KtNodeType INITIALIZER_LIST = new KtNodeType("INITIALIZER_LIST", KtInitializerList::new);

    @NotNull KtNodeType DELEGATED_SUPER_TYPE_ENTRY =
            new KtNodeType("DELEGATED_SUPER_TYPE_ENTRY", KtDelegatedSuperTypeEntry::new);

    @NotNull KtNodeType SUPER_TYPE_CALL_ENTRY = new KtNodeType("SUPER_TYPE_CALL_ENTRY", KtSuperTypeCallEntry::new);

    @NotNull KtNodeType SUPER_TYPE_ENTRY = new KtNodeType("SUPER_TYPE_ENTRY", KtSuperTypeEntry::new);

    @NotNull KtNodeType CONSTRUCTOR_CALLEE = new KtNodeType("CONSTRUCTOR_CALLEE", KtConstructorCalleeExpression::new);

    @NotNull KtNodeType CONTEXT_RECEIVER = new KtNodeType("CONTEXT_RECEIVER", KtContextReceiver::new);

    @NotNull KtNodeType CONTEXT_PARAMETER_LIST = new KtNodeType("CONTEXT_PARAMETER_LIST", KtContextReceiverList::new);

    @NotNull KtNodeType NULL               = new KtNodeType("NULL", KtConstantExpression::new);
    @NotNull KtNodeType BOOLEAN_CONSTANT   = new KtNodeType("BOOLEAN_CONSTANT", KtConstantExpression::new);
    @NotNull KtNodeType FLOAT_CONSTANT     = new KtNodeType("FLOAT_CONSTANT", KtConstantExpression::new);
    @NotNull KtNodeType CHARACTER_CONSTANT = new KtNodeType("CHARACTER_CONSTANT", KtConstantExpression::new);
    @NotNull KtNodeType INTEGER_CONSTANT   = new KtNodeType("INTEGER_CONSTANT", KtConstantExpression::new);
    @NotNull KtNodeType CLASS_LITERAL_EXPRESSION = new KtNodeType("CLASS_LITERAL_EXPRESSION", KtClassLiteralExpression::new);
    @NotNull KtNodeType COLLECTION_LITERAL_EXPRESSION =
            new KtNodeType("COLLECTION_LITERAL_EXPRESSION", KtCollectionLiteralExpression::new);

    @NotNull KtNodeType STRING_TEMPLATE = new KtNodeType("STRING_TEMPLATE", KtStringTemplateExpression::new);

    @NotNull KtNodeType LONG_STRING_TEMPLATE_ENTRY =
            new KtNodeType("LONG_STRING_TEMPLATE_ENTRY", KtBlockStringTemplateEntry::new);

    @NotNull KtNodeType SHORT_STRING_TEMPLATE_ENTRY =
            new KtNodeType("SHORT_STRING_TEMPLATE_ENTRY", KtSimpleNameStringTemplateEntry::new);

    @NotNull KtNodeType LITERAL_STRING_TEMPLATE_ENTRY =
            new KtNodeType("LITERAL_STRING_TEMPLATE_ENTRY", KtLiteralStringTemplateEntry::new);

    @NotNull KtNodeType ESCAPE_STRING_TEMPLATE_ENTRY =
            new KtNodeType("ESCAPE_STRING_TEMPLATE_ENTRY", KtEscapeStringTemplateEntry::new);

    @NotNull KtNodeType SCRIPT = new KtNodeType("SCRIPT", KtScript::new);

    @NotNull KtNodeType STRING_INTERPOLATION_PREFIX =
            new KtNodeType("STRING_INTERPOLATION_PREFIX", KtStringInterpolationPrefix::new);
}
