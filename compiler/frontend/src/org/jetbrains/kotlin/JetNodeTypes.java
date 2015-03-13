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

package org.jetbrains.kotlin;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import org.jetbrains.kotlin.idea.JetLanguage;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes;

public interface JetNodeTypes {
    IFileElementType JET_FILE = new IFileElementType(JetLanguage.INSTANCE);

    IElementType CLASS     = JetStubElementTypes.CLASS;
    IElementType FUN       = JetStubElementTypes.FUNCTION;
    IElementType PROPERTY  = JetStubElementTypes.PROPERTY;
    IElementType MULTI_VARIABLE_DECLARATION  = new JetNodeType("MULTI_VARIABLE_DECLARATION", JetMultiDeclaration.class);
    IElementType MULTI_VARIABLE_DECLARATION_ENTRY = new JetNodeType("MULTI_VARIABLE_DECLARATION_ENTRY", JetMultiDeclarationEntry.class);

    JetNodeType TYPEDEF    = new JetNodeType("TYPEDEF", JetTypedef.class);
    IElementType OBJECT_DECLARATION = JetStubElementTypes.OBJECT_DECLARATION;
    JetNodeType OBJECT_DECLARATION_NAME = new JetNodeType("OBJECT_DECLARATION_NAME", JetObjectDeclarationName.class);

    IElementType ENUM_ENTRY            = JetStubElementTypes.ENUM_ENTRY;
    IElementType ANONYMOUS_INITIALIZER = JetStubElementTypes.ANONYMOUS_INITIALIZER;
    IElementType SECONDARY_CONSTRUCTOR  = JetStubElementTypes.SECONDARY_CONSTRUCTOR;

    IElementType TYPE_PARAMETER_LIST                 = JetStubElementTypes.TYPE_PARAMETER_LIST;
    IElementType TYPE_PARAMETER                      = JetStubElementTypes.TYPE_PARAMETER;
    IElementType DELEGATION_SPECIFIER_LIST           = JetStubElementTypes.DELEGATION_SPECIFIER_LIST;
    IElementType DELEGATOR_BY                        = JetStubElementTypes.DELEGATOR_BY;
    IElementType DELEGATOR_SUPER_CALL                = JetStubElementTypes.DELEGATOR_SUPER_CALL;
    IElementType DELEGATOR_SUPER_CLASS               = JetStubElementTypes.DELEGATOR_SUPER_CLASS;
    JetNodeType PROPERTY_DELEGATE                    = new JetNodeType("PROPERTY_DELEGATE", JetPropertyDelegate.class);
    IElementType CONSTRUCTOR_CALLEE                  = JetStubElementTypes.CONSTRUCTOR_CALLEE;
    IElementType VALUE_PARAMETER_LIST               = JetStubElementTypes.VALUE_PARAMETER_LIST;
    IElementType VALUE_PARAMETER                    = JetStubElementTypes.VALUE_PARAMETER;

    IElementType CLASS_BODY                         = JetStubElementTypes.CLASS_BODY;
    IElementType IMPORT_LIST                        = JetStubElementTypes.IMPORT_LIST;
    IElementType FILE_ANNOTATION_LIST               = JetStubElementTypes.FILE_ANNOTATION_LIST;
    IElementType IMPORT_DIRECTIVE                   = JetStubElementTypes.IMPORT_DIRECTIVE;
    IElementType MODIFIER_LIST                      = JetStubElementTypes.MODIFIER_LIST;
    IElementType PRIMARY_CONSTRUCTOR_MODIFIER_LIST  = JetStubElementTypes.PRIMARY_CONSTRUCTOR_MODIFIER_LIST;
    IElementType ANNOTATION                         = JetStubElementTypes.ANNOTATION;
    IElementType ANNOTATION_ENTRY                   = JetStubElementTypes.ANNOTATION_ENTRY;

    IElementType TYPE_ARGUMENT_LIST                 = JetStubElementTypes.TYPE_ARGUMENT_LIST;
    JetNodeType VALUE_ARGUMENT_LIST                 = new JetNodeType("VALUE_ARGUMENT_LIST", JetValueArgumentList.class);
    JetNodeType VALUE_ARGUMENT                      = new JetNodeType("VALUE_ARGUMENT", JetValueArgument.class);
    JetNodeType FUNCTION_LITERAL_ARGUMENT           = new JetNodeType("FUNCTION_LITERAL_ARGUMENT", JetFunctionLiteralArgument.class);
    JetNodeType VALUE_ARGUMENT_NAME                 = new JetNodeType("VALUE_ARGUMENT_NAME", JetValueArgumentName.class);
    IElementType TYPE_REFERENCE                     = JetStubElementTypes.TYPE_REFERENCE;

    IElementType USER_TYPE                = JetStubElementTypes.USER_TYPE;
    IElementType DYNAMIC_TYPE             = JetStubElementTypes.DYNAMIC_TYPE;
    IElementType FUNCTION_TYPE            = JetStubElementTypes.FUNCTION_TYPE;
    IElementType FUNCTION_TYPE_RECEIVER   = JetStubElementTypes.FUNCTION_TYPE_RECEIVER;
    JetNodeType SELF_TYPE     = new JetNodeType("SELF_TYPE", JetSelfType.class);
    IElementType NULLABLE_TYPE            = JetStubElementTypes.NULLABLE_TYPE;
    IElementType TYPE_PROJECTION          = JetStubElementTypes.TYPE_PROJECTION;

    // TODO: review
    IElementType PROPERTY_ACCESSOR       = JetStubElementTypes.PROPERTY_ACCESSOR;
    IElementType INITIALIZER_LIST        = JetStubElementTypes.INITIALIZER_LIST;
    IElementType TYPE_CONSTRAINT_LIST    = JetStubElementTypes.TYPE_CONSTRAINT_LIST;
    IElementType TYPE_CONSTRAINT         = JetStubElementTypes.TYPE_CONSTRAINT;

    IElementType CONSTRUCTOR_DELEGATION_CALL = new JetNodeType("CONSTRUCTOR_DELEGATION_CALL", JetConstructorDelegationCall.class);
    JetNodeType CONSTRUCTOR_DELEGATION_REFERENCE =
            new JetNodeType("CONSTRUCTOR_DELEGATION_REFERENCE", JetConstructorDelegationReferenceExpression.class);

    // TODO: Not sure if we need separate NT for each kind of constants
    JetNodeType NULL               = new JetNodeType("NULL", JetConstantExpression.class);
    JetNodeType BOOLEAN_CONSTANT   = new JetNodeType("BOOLEAN_CONSTANT", JetConstantExpression.class);
    JetNodeType FLOAT_CONSTANT     = new JetNodeType("FLOAT_CONSTANT", JetConstantExpression.class);
    JetNodeType CHARACTER_CONSTANT = new JetNodeType("CHARACTER_CONSTANT", JetConstantExpression.class);
    JetNodeType INTEGER_CONSTANT   = new JetNodeType("INTEGER_CONSTANT", JetConstantExpression.class);

    JetNodeType STRING_TEMPLATE    = new JetNodeType("STRING_TEMPLATE", JetStringTemplateExpression.class);
    JetNodeType LONG_STRING_TEMPLATE_ENTRY = new JetNodeType("LONG_STRING_TEMPLATE_ENTRY", JetBlockStringTemplateEntry.class);
    JetNodeType SHORT_STRING_TEMPLATE_ENTRY = new JetNodeType("SHORT_STRING_TEMPLATE_ENTRY", JetSimpleNameStringTemplateEntry.class);
    JetNodeType LITERAL_STRING_TEMPLATE_ENTRY = new JetNodeType("LITERAL_STRING_TEMPLATE_ENTRY", JetLiteralStringTemplateEntry.class);
    JetNodeType ESCAPE_STRING_TEMPLATE_ENTRY = new JetNodeType("ESCAPE_STRING_TEMPLATE_ENTRY", JetEscapeStringTemplateEntry.class);

    JetNodeType PARENTHESIZED             = new JetNodeType("PARENTHESIZED", JetParenthesizedExpression.class);
    JetNodeType RETURN                    = new JetNodeType("RETURN", JetReturnExpression.class);
    JetNodeType THROW                     = new JetNodeType("THROW", JetThrowExpression.class);
    JetNodeType CONTINUE                  = new JetNodeType("CONTINUE", JetContinueExpression.class);
    JetNodeType BREAK                     = new JetNodeType("BREAK", JetBreakExpression.class);
    JetNodeType IF                        = new JetNodeType("IF", JetIfExpression.class);
    JetNodeType CONDITION                 = new JetNodeType("CONDITION", JetContainerNode.class);
    JetNodeType THEN                      = new JetNodeType("THEN", JetContainerNode.class);
    JetNodeType ELSE                      = new JetNodeType("ELSE", JetContainerNode.class);
    JetNodeType TRY                       = new JetNodeType("TRY", JetTryExpression.class);
    JetNodeType CATCH                     = new JetNodeType("CATCH", JetCatchClause.class);
    JetNodeType FINALLY                   = new JetNodeType("FINALLY", JetFinallySection.class);
    JetNodeType FOR                       = new JetNodeType("FOR", JetForExpression.class);
    JetNodeType WHILE                     = new JetNodeType("WHILE", JetWhileExpression.class);
    JetNodeType DO_WHILE                  = new JetNodeType("DO_WHILE", JetDoWhileExpression.class);
    JetNodeType LOOP_RANGE                = new JetNodeType("LOOP_RANGE", JetContainerNode.class);
    JetNodeType BODY                      = new JetNodeType("BODY", JetContainerNode.class);
    JetNodeType BLOCK                     = new JetNodeType("BLOCK", JetBlockExpression.class);
    JetNodeType FUNCTION_LITERAL_EXPRESSION = new JetNodeType("FUNCTION_LITERAL_EXPRESSION", JetFunctionLiteralExpression.class);
    JetNodeType FUNCTION_LITERAL          = new JetNodeType("FUNCTION_LITERAL", JetFunctionLiteral.class);
    JetNodeType ANNOTATED_EXPRESSION      = new JetNodeType("ANNOTATED_EXPRESSION", JetAnnotatedExpression.class);

    IElementType REFERENCE_EXPRESSION     = JetStubElementTypes.REFERENCE_EXPRESSION;
    JetNodeType OPERATION_REFERENCE       = new JetNodeType("OPERATION_REFERENCE", JetOperationReferenceExpression.class);
    JetNodeType LABEL                     = new JetNodeType("LABEL", JetLabelReferenceExpression.class);

    JetNodeType LABEL_QUALIFIER           = new JetNodeType("LABEL_QUALIFIER", JetContainerNode.class);

    JetNodeType THIS_EXPRESSION           = new JetNodeType("THIS_EXPRESSION", JetThisExpression.class);
    JetNodeType SUPER_EXPRESSION          = new JetNodeType("SUPER_EXPRESSION", JetSuperExpression.class);
    JetNodeType BINARY_EXPRESSION         = new JetNodeType("BINARY_EXPRESSION", JetBinaryExpression.class);
    JetNodeType BINARY_WITH_TYPE          = new JetNodeType("BINARY_WITH_TYPE", JetBinaryExpressionWithTypeRHS.class);
    JetNodeType IS_EXPRESSION = new JetNodeType("IS_EXPRESSION", JetIsExpression.class); // TODO:
    JetNodeType PREFIX_EXPRESSION         = new JetNodeType("PREFIX_EXPRESSION", JetPrefixExpression.class);
    JetNodeType POSTFIX_EXPRESSION        = new JetNodeType("POSTFIX_EXPRESSION", JetPostfixExpression.class);
    JetNodeType LABELED_EXPRESSION         = new JetNodeType("LABELED_EXPRESSION", JetLabeledExpression.class);
    JetNodeType CALL_EXPRESSION           = new JetNodeType("CALL_EXPRESSION", JetCallExpression.class);
    JetNodeType ARRAY_ACCESS_EXPRESSION   = new JetNodeType("ARRAY_ACCESS_EXPRESSION", JetArrayAccessExpression.class);
    JetNodeType INDICES                   = new JetNodeType("INDICES", JetContainerNode.class);
    IElementType DOT_QUALIFIED_EXPRESSION = JetStubElementTypes.DOT_QUALIFIED_EXPRESSION;
    JetNodeType CALLABLE_REFERENCE_EXPRESSION = new JetNodeType("CALLABLE_REFERENCE_EXPRESSION", JetCallableReferenceExpression.class);
    JetNodeType CLASS_LITERAL_EXPRESSION  = new JetNodeType("CLASS_LITERAL_EXPRESSION", JetClassLiteralExpression.class);
    JetNodeType SAFE_ACCESS_EXPRESSION    = new JetNodeType("SAFE_ACCESS_EXPRESSION", JetSafeQualifiedExpression.class);

    JetNodeType OBJECT_LITERAL            = new JetNodeType("OBJECT_LITERAL", JetObjectLiteralExpression.class);
    JetNodeType ROOT_PACKAGE = new JetNodeType("ROOT_PACKAGE", JetRootPackageExpression.class);

    JetNodeType WHEN                      = new JetNodeType("WHEN", JetWhenExpression.class);
    JetNodeType WHEN_ENTRY                = new JetNodeType("WHEN_ENTRY", JetWhenEntry.class);

    JetNodeType WHEN_CONDITION_IN_RANGE   = new JetNodeType("WHEN_CONDITION_IN_RANGE", JetWhenConditionInRange.class);
    JetNodeType WHEN_CONDITION_IS_PATTERN = new JetNodeType("WHEN_CONDITION_IS_PATTERN", JetWhenConditionIsPattern.class);
    JetNodeType WHEN_CONDITION_EXPRESSION = new JetNodeType("WHEN_CONDITION_WITH_EXPRESSION", JetWhenConditionWithExpression.class);

    IElementType PACKAGE_DIRECTIVE = JetStubElementTypes.PACKAGE_DIRECTIVE;

    // SCRIPT: script node type
    JetNodeType SCRIPT = new JetNodeType("SCRIPT", JetScript.class);

    IFileElementType TYPE_CODE_FRAGMENT = new JetTypeCodeFragmentType();
    IFileElementType EXPRESSION_CODE_FRAGMENT = new JetExpressionCodeFragmentType();
    IFileElementType BLOCK_CODE_FRAGMENT = new JetBlockCodeFragmentType();
}
