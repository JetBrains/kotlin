/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.psi.tree.TokenSet;
import org.jetbrains.kotlin.psi.*;

public interface KtStubElementTypes {
    KtClassElementType CLASS = new KtClassElementType("CLASS");
    KtFunctionElementType FUNCTION = new KtFunctionElementType("FUN");
    KtPropertyElementType PROPERTY = new KtPropertyElementType("PROPERTY");
    KtPropertyAccessorElementType PROPERTY_ACCESSOR = new KtPropertyAccessorElementType("PROPERTY_ACCESSOR");
    KtBackingFieldElementType BACKING_FIELD = new KtBackingFieldElementType("BACKING_FIELD");
    KtTypeAliasElementType TYPEALIAS = new KtTypeAliasElementType("TYPEALIAS");

    KtClassElementType ENUM_ENTRY = new KtClassElementType("ENUM_ENTRY");
    KtObjectElementType OBJECT_DECLARATION = new KtObjectElementType("OBJECT_DECLARATION");
    KtPlaceHolderStubElementType<KtClassInitializer> CLASS_INITIALIZER =
            new KtPlaceHolderStubElementType<>("CLASS_INITIALIZER", KtClassInitializer.class);
    KtSecondaryConstructorElementType SECONDARY_CONSTRUCTOR =
            new KtSecondaryConstructorElementType("SECONDARY_CONSTRUCTOR");
    KtPrimaryConstructorElementType PRIMARY_CONSTRUCTOR =
            new KtPrimaryConstructorElementType("PRIMARY_CONSTRUCTOR");

    KtParameterElementType VALUE_PARAMETER = new KtParameterElementType("VALUE_PARAMETER");
    KtPlaceHolderStubElementType<KtParameterList> VALUE_PARAMETER_LIST =
            new KtPlaceHolderStubElementType<>("VALUE_PARAMETER_LIST", KtParameterList.class);

    KtTypeParameterElementType TYPE_PARAMETER = new KtTypeParameterElementType("TYPE_PARAMETER");
    KtPlaceHolderStubElementType<KtTypeParameterList> TYPE_PARAMETER_LIST =
            new KtPlaceHolderStubElementType<>("TYPE_PARAMETER_LIST", KtTypeParameterList.class);

    KtAnnotationEntryElementType ANNOTATION_ENTRY = new KtAnnotationEntryElementType("ANNOTATION_ENTRY");
    KtPlaceHolderStubElementType<KtAnnotation> ANNOTATION =
            new KtPlaceHolderStubElementType<>("ANNOTATION", KtAnnotation.class);

    KtAnnotationUseSiteTargetElementType ANNOTATION_TARGET = new KtAnnotationUseSiteTargetElementType("ANNOTATION_TARGET");

    KtPlaceHolderStubElementType<KtClassBody> CLASS_BODY =
            new KtPlaceHolderStubElementType<>("CLASS_BODY", KtClassBody.class);

    KtPlaceHolderStubElementType<KtImportList> IMPORT_LIST =
            new KtPlaceHolderStubElementType<>("IMPORT_LIST", KtImportList.class);

    KtPlaceHolderStubElementType<KtFileAnnotationList> FILE_ANNOTATION_LIST =
            new KtPlaceHolderStubElementType<>("FILE_ANNOTATION_LIST", KtFileAnnotationList.class);

    KtImportDirectiveElementType IMPORT_DIRECTIVE = new KtImportDirectiveElementType("IMPORT_DIRECTIVE");

    KtImportAliasElementType IMPORT_ALIAS = new KtImportAliasElementType("IMPORT_ALIAS");

    KtPlaceHolderStubElementType<KtPackageDirective> PACKAGE_DIRECTIVE =
            new KtPlaceHolderStubElementType<>("PACKAGE_DIRECTIVE", KtPackageDirective.class);

    KtModifierListElementType<KtDeclarationModifierList> MODIFIER_LIST =
            new KtModifierListElementType<>("MODIFIER_LIST", KtDeclarationModifierList.class);

    KtPlaceHolderStubElementType<KtTypeConstraintList> TYPE_CONSTRAINT_LIST =
            new KtPlaceHolderStubElementType<>("TYPE_CONSTRAINT_LIST", KtTypeConstraintList.class);

    KtPlaceHolderStubElementType<KtTypeConstraint> TYPE_CONSTRAINT =
            new KtPlaceHolderStubElementType<>("TYPE_CONSTRAINT", KtTypeConstraint.class);

    KtPlaceHolderStubElementType<KtNullableType> NULLABLE_TYPE =
            new KtPlaceHolderStubElementType<>("NULLABLE_TYPE", KtNullableType.class);

    KtPlaceHolderStubElementType<KtIntersectionType> INTERSECTION_TYPE =
            new KtPlaceHolderStubElementType<>("INTERSECTION_TYPE", KtIntersectionType.class);

    KtPlaceHolderStubElementType<KtTypeReference> TYPE_REFERENCE =
            new KtPlaceHolderStubElementType<>("TYPE_REFERENCE", KtTypeReference.class);

    KtUserTypeElementType USER_TYPE = new KtUserTypeElementType("USER_TYPE");
    KtPlaceHolderStubElementType<KtDynamicType> DYNAMIC_TYPE =
            new KtPlaceHolderStubElementType<>("DYNAMIC_TYPE", KtDynamicType.class);

    KtPlaceHolderStubElementType<KtFunctionType> FUNCTION_TYPE =
            new KtPlaceHolderStubElementType<>("FUNCTION_TYPE", KtFunctionType.class);

    KtTypeCodeFragmentType TYPE_CODE_FRAGMENT = new KtTypeCodeFragmentType();
    KtExpressionCodeFragmentType EXPRESSION_CODE_FRAGMENT = new KtExpressionCodeFragmentType();
    KtBlockCodeFragmentType BLOCK_CODE_FRAGMENT = new KtBlockCodeFragmentType();

    KtTypeProjectionElementType TYPE_PROJECTION = new KtTypeProjectionElementType("TYPE_PROJECTION");

    KtPlaceHolderStubElementType<KtFunctionTypeReceiver> FUNCTION_TYPE_RECEIVER =
            new KtPlaceHolderStubElementType<>("FUNCTION_TYPE_RECEIVER", KtFunctionTypeReceiver.class);

    KtNameReferenceExpressionElementType REFERENCE_EXPRESSION = new KtNameReferenceExpressionElementType("REFERENCE_EXPRESSION");
    KtDotQualifiedExpressionElementType DOT_QUALIFIED_EXPRESSION = new KtDotQualifiedExpressionElementType("DOT_QUALIFIED_EXPRESSION");
    KtEnumEntrySuperClassReferenceExpressionElementType
            ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION =
            new KtEnumEntrySuperClassReferenceExpressionElementType("ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION");
    KtPlaceHolderStubElementType<KtTypeArgumentList> TYPE_ARGUMENT_LIST =
            new KtPlaceHolderStubElementType<>("TYPE_ARGUMENT_LIST", KtTypeArgumentList.class);

    KtPlaceHolderStubElementType<KtValueArgumentList> VALUE_ARGUMENT_LIST =
            new KtValueArgumentListElementType("VALUE_ARGUMENT_LIST");

    KtValueArgumentElementType<KtValueArgument> VALUE_ARGUMENT =
            new KtValueArgumentElementType<>("VALUE_ARGUMENT", KtValueArgument.class);

    KtPlaceHolderStubElementType<KtContractEffectList> CONTRACT_EFFECT_LIST =
            new KtContractEffectListElementType("CONTRACT_EFFECT_LIST");

    KtContractEffectElementType CONTRACT_EFFECT =
            new KtContractEffectElementType("CONTRACT_EFFECT", KtContractEffect.class);

    KtValueArgumentElementType<KtLambdaArgument> LAMBDA_ARGUMENT =
            new KtValueArgumentElementType<>("LAMBDA_ARGUMENT", KtLambdaArgument.class);

    KtPlaceHolderStubElementType<KtValueArgumentName> VALUE_ARGUMENT_NAME =
            new KtPlaceHolderStubElementType<>("VALUE_ARGUMENT_NAME", KtValueArgumentName.class);

    KtPlaceHolderStubElementType<KtSuperTypeList> SUPER_TYPE_LIST =
            new KtPlaceHolderStubElementType<>("SUPER_TYPE_LIST", KtSuperTypeList.class);

    KtPlaceHolderStubElementType<KtInitializerList> INITIALIZER_LIST =
            new KtPlaceHolderStubElementType<>("INITIALIZER_LIST", KtInitializerList.class);

    KtPlaceHolderStubElementType<KtDelegatedSuperTypeEntry> DELEGATED_SUPER_TYPE_ENTRY =
            new KtPlaceHolderStubElementType<>("DELEGATED_SUPER_TYPE_ENTRY", KtDelegatedSuperTypeEntry.class);

    KtPlaceHolderStubElementType<KtSuperTypeCallEntry> SUPER_TYPE_CALL_ENTRY =
            new KtPlaceHolderStubElementType<>("SUPER_TYPE_CALL_ENTRY", KtSuperTypeCallEntry.class);
    KtPlaceHolderStubElementType<KtSuperTypeEntry> SUPER_TYPE_ENTRY =
            new KtPlaceHolderStubElementType<>("SUPER_TYPE_ENTRY", KtSuperTypeEntry.class);
    KtPlaceHolderStubElementType<KtConstructorCalleeExpression> CONSTRUCTOR_CALLEE =
            new KtPlaceHolderStubElementType<>("CONSTRUCTOR_CALLEE", KtConstructorCalleeExpression.class);

    KtContextReceiverElementType CONTEXT_RECEIVER = new KtContextReceiverElementType("CONTEXT_RECEIVER");
    KtPlaceHolderStubElementType<KtContextReceiverList> CONTEXT_RECEIVER_LIST =
            new KtPlaceHolderStubElementType<>("CONTEXT_RECEIVER_LIST", KtContextReceiverList.class);

    KtConstantExpressionElementType NULL                = new KtConstantExpressionElementType("NULL");
    KtConstantExpressionElementType BOOLEAN_CONSTANT    = new KtConstantExpressionElementType("BOOLEAN_CONSTANT");
    KtConstantExpressionElementType FLOAT_CONSTANT      = new KtConstantExpressionElementType("FLOAT_CONSTANT");
    KtConstantExpressionElementType CHARACTER_CONSTANT  = new KtConstantExpressionElementType("CHARACTER_CONSTANT");
    KtConstantExpressionElementType INTEGER_CONSTANT    = new KtConstantExpressionElementType("INTEGER_CONSTANT");
    KtClassLiteralExpressionElementType CLASS_LITERAL_EXPRESSION = new KtClassLiteralExpressionElementType("CLASS_LITERAL_EXPRESSION");
    KtCollectionLiteralExpressionElementType COLLECTION_LITERAL_EXPRESSION = new KtCollectionLiteralExpressionElementType("COLLECTION_LITERAL_EXPRESSION");

    KtPlaceHolderStubElementType<KtStringTemplateExpression> STRING_TEMPLATE =
            new KtStringTemplateExpressionElementType("STRING_TEMPLATE");

    TokenSet CONSTANT_EXPRESSIONS_TYPES = TokenSet.create(
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

    KtPlaceHolderWithTextStubElementType<KtBlockStringTemplateEntry> LONG_STRING_TEMPLATE_ENTRY =
            new KtPlaceHolderWithTextStubElementType<>("LONG_STRING_TEMPLATE_ENTRY", KtBlockStringTemplateEntry.class);

    KtPlaceHolderWithTextStubElementType<KtSimpleNameStringTemplateEntry> SHORT_STRING_TEMPLATE_ENTRY =
            new KtPlaceHolderWithTextStubElementType<>("SHORT_STRING_TEMPLATE_ENTRY", KtSimpleNameStringTemplateEntry.class);

    KtPlaceHolderWithTextStubElementType<KtLiteralStringTemplateEntry> LITERAL_STRING_TEMPLATE_ENTRY =
            new KtPlaceHolderWithTextStubElementType<>("LITERAL_STRING_TEMPLATE_ENTRY", KtLiteralStringTemplateEntry.class);

    KtPlaceHolderWithTextStubElementType<KtEscapeStringTemplateEntry> ESCAPE_STRING_TEMPLATE_ENTRY =
            new KtPlaceHolderWithTextStubElementType<>("ESCAPE_STRING_TEMPLATE_ENTRY", KtEscapeStringTemplateEntry.class);

    KtScriptElementType SCRIPT = new KtScriptElementType("SCRIPT");
}
