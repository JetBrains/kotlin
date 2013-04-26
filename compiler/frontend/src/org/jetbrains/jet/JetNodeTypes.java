/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.jet.plugin.JetLanguage;

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

    JetNodeType CLASS_OBJECT          = new JetNodeType("CLASS_OBJECT", JetClassObject.class);
    IElementType ENUM_ENTRY            = JetStubElementTypes.ENUM_ENTRY;
    JetNodeType ANONYMOUS_INITIALIZER = new JetNodeType("ANONYMOUS_INITIALIZER", JetClassInitializer.class);

    IElementType TYPE_PARAMETER_LIST                 = JetStubElementTypes.TYPE_PARAMETER_LIST;
    IElementType TYPE_PARAMETER                      = JetStubElementTypes.TYPE_PARAMETER;
    JetNodeType DELEGATION_SPECIFIER_LIST           = new JetNodeType("DELEGATION_SPECIFIER_LIST", JetDelegationSpecifierList.class);
    JetNodeType DELEGATOR_BY                        = new JetNodeType("DELEGATOR_BY", JetDelegatorByExpressionSpecifier.class);
    JetNodeType DELEGATOR_SUPER_CALL                = new JetNodeType("DELEGATOR_SUPER_CALL", JetDelegatorToSuperCall.class);
    JetNodeType DELEGATOR_SUPER_CLASS               = new JetNodeType("DELEGATOR_SUPER_CLASS", JetDelegatorToSuperClass.class);
    JetNodeType PROPERTY_DELEGATE                    = new JetNodeType("PROPERTY_DELEGATE", JetPropertyDelegate.class);
    JetNodeType CONSTRUCTOR_CALLEE                  = new JetNodeType("CONSTRUCTOR_CALLEE", JetConstructorCalleeExpression.class);
    IElementType VALUE_PARAMETER_LIST               = JetStubElementTypes.VALUE_PARAMETER_LIST;
    IElementType VALUE_PARAMETER                    = JetStubElementTypes.VALUE_PARAMETER;

    JetNodeType CLASS_BODY                          = new JetNodeType("CLASS_BODY", JetClassBody.class);
    JetNodeType IMPORT_DIRECTIVE                    = new JetNodeType("IMPORT_DIRECTIVE", JetImportDirective.class);
    JetNodeType MODIFIER_LIST                       = new JetNodeType("MODIFIER_LIST", JetModifierList.class);
    JetNodeType PRIMARY_CONSTRUCTOR_MODIFIER_LIST   = new JetNodeType("PRIMARY_CONSTRUCTOR_MODIFIER_LIST", JetModifierList.class);
    JetNodeType ANNOTATION = new JetNodeType("ANNOTATION", JetAnnotation.class);
    IElementType ANNOTATION_ENTRY = JetStubElementTypes.ANNOTATION_ENTRY;

    JetNodeType TYPE_ARGUMENT_LIST                  = new JetNodeType("TYPE_ARGUMENT_LIST", JetTypeArgumentList.class);
    JetNodeType VALUE_ARGUMENT_LIST                 = new JetNodeType("VALUE_ARGUMENT_LIST", JetValueArgumentList.class);
    JetNodeType VALUE_ARGUMENT                      = new JetNodeType("VALUE_ARGUMENT", JetValueArgument.class);
    JetNodeType VALUE_ARGUMENT_NAME                 = new JetNodeType("VALUE_ARGUMENT_NAME", JetValueArgumentName.class);
    JetNodeType TYPE_REFERENCE                      = new JetNodeType("TYPE_REFERENCE", JetTypeReference.class);

    JetNodeType USER_TYPE     = new JetNodeType("USER_TYPE", JetUserType.class);

    JetNodeType FUNCTION_TYPE = new JetNodeType("FUNCTION_TYPE", JetFunctionType.class);
    JetNodeType FUNCTION_TYPE_RECEIVER = new JetNodeType("FUNCTION_TYPE_RECEIVER", JetFunctionTypeReceiver.class);
    JetNodeType SELF_TYPE     = new JetNodeType("SELF_TYPE", JetSelfType.class);
    JetNodeType NULLABLE_TYPE             = new JetNodeType("NULLABLE_TYPE", JetNullableType.class);
    JetNodeType TYPE_PROJECTION           = new JetNodeType("TYPE_PROJECTION", JetTypeProjection.class);

    // TODO: review
    JetNodeType PROPERTY_ACCESSOR        = new JetNodeType("PROPERTY_ACCESSOR", JetPropertyAccessor.class);
    JetNodeType INITIALIZER_LIST         = new JetNodeType("INITIALIZER_LIST", JetInitializerList.class);
    JetNodeType THIS_CALL                = new JetNodeType("THIS_CALL", JetDelegatorToThisCall.class);
    JetNodeType THIS_CONSTRUCTOR_REFERENCE = new JetNodeType("THIS_CONSTRUCTOR_REFERENCE", JetThisReferenceExpression.class);
    JetNodeType TYPE_CONSTRAINT_LIST     = new JetNodeType("TYPE_CONSTRAINT_LIST", JetTypeConstraintList.class);
    JetNodeType TYPE_CONSTRAINT          = new JetNodeType("TYPE_CONSTRAINT", JetTypeConstraint.class);

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
    JetNodeType LOOP_PARAMETER            = new JetNodeType("LOOP_PARAMETER", JetParameter.class); // TODO: Do we need separate type?
    JetNodeType LOOP_RANGE                = new JetNodeType("LOOP_RANGE", JetContainerNode.class);
    JetNodeType BODY                      = new JetNodeType("BODY", JetContainerNode.class);
    JetNodeType BLOCK                     = new JetNodeType("BLOCK", JetBlockExpression.class);
    JetNodeType FUNCTION_LITERAL_EXPRESSION = new JetNodeType("FUNCTION_LITERAL_EXPRESSION", JetFunctionLiteralExpression.class);
    JetNodeType FUNCTION_LITERAL          = new JetNodeType("FUNCTION_LITERAL", JetFunctionLiteral.class);
    JetNodeType ANNOTATED_EXPRESSION      = new JetNodeType("ANNOTATED_EXPRESSION", JetAnnotatedExpression.class);

    JetNodeType REFERENCE_EXPRESSION      = new JetNodeType("REFERENCE_EXPRESSION", JetSimpleNameExpression.class);
    JetNodeType OPERATION_REFERENCE       = new JetNodeType("OPERATION_REFERENCE", JetSimpleNameExpression.class);
    JetNodeType LABEL_REFERENCE           = new JetNodeType("LABEL_REFERENCE", JetSimpleNameExpression.class);

    JetNodeType LABEL_QUALIFIER           = new JetNodeType("LABEL_QUALIFIER", JetContainerNode.class);

    JetNodeType THIS_EXPRESSION           = new JetNodeType("THIS_EXPRESSION", JetThisExpression.class);
    JetNodeType SUPER_EXPRESSION          = new JetNodeType("SUPER_EXPRESSION", JetSuperExpression.class);
    JetNodeType BINARY_EXPRESSION         = new JetNodeType("BINARY_EXPRESSION", JetBinaryExpression.class);
    JetNodeType BINARY_WITH_TYPE          = new JetNodeType("BINARY_WITH_TYPE", JetBinaryExpressionWithTypeRHS.class);
    JetNodeType IS_EXPRESSION = new JetNodeType("IS_EXPRESSION", JetIsExpression.class); // TODO:
    JetNodeType PREFIX_EXPRESSION         = new JetNodeType("PREFIX_EXPRESSION", JetPrefixExpression.class);
    JetNodeType POSTFIX_EXPRESSION        = new JetNodeType("POSTFIX_EXPRESSION", JetPostfixExpression.class);
    JetNodeType CALL_EXPRESSION           = new JetNodeType("CALL_EXPRESSION", JetCallExpression.class);
    JetNodeType ARRAY_ACCESS_EXPRESSION   = new JetNodeType("ARRAY_ACCESS_EXPRESSION", JetArrayAccessExpression.class);
    JetNodeType INDICES                   = new JetNodeType("INDICES", JetContainerNode.class);
    JetNodeType DOT_QUALIFIED_EXPRESSION  = new JetNodeType("DOT_QUALIFIED_EXPRESSION", JetDotQualifiedExpression.class);
    JetNodeType CALLABLE_REFERENCE_EXPRESSION = new JetNodeType("CALLABLE_REFERENCE_EXPRESSION", JetCallableReferenceExpression.class);
    JetNodeType SAFE_ACCESS_EXPRESSION    = new JetNodeType("SAFE_ACCESS_EXPRESSION", JetSafeQualifiedExpression.class);

    JetNodeType OBJECT_LITERAL            = new JetNodeType("OBJECT_LITERAL", JetObjectLiteralExpression.class);
    JetNodeType ROOT_NAMESPACE            = new JetNodeType("ROOT_NAMESPACE", JetRootNamespaceExpression.class);

    JetNodeType WHEN                      = new JetNodeType("WHEN", JetWhenExpression.class);
    JetNodeType WHEN_ENTRY                = new JetNodeType("WHEN_ENTRY", JetWhenEntry.class);

    JetNodeType WHEN_CONDITION_IN_RANGE   = new JetNodeType("WHEN_CONDITION_IN_RANGE", JetWhenConditionInRange.class);
    JetNodeType WHEN_CONDITION_IS_PATTERN = new JetNodeType("WHEN_CONDITION_IS_PATTERN", JetWhenConditionIsPattern.class);
    JetNodeType WHEN_CONDITION_EXPRESSION = new JetNodeType("WHEN_CONDITION_WITH_EXPRESSION", JetWhenConditionWithExpression.class);

    JetNodeType NAMESPACE_HEADER = new JetNodeType("NAMESPACE_HEADER", JetNamespaceHeader.class);
    JetNodeType SCRIPT = new JetNodeType("SCRIPT", JetScript.class);

    JetNodeType IDE_TEMPLATE_EXPRESSION   = new JetNodeType("IDE_TEMPLATE_EXPRESSION", JetIdeTemplate.class);

    IFileElementType TYPE_CODE_FRAGMENT = new JetTypeCodeFragmentType();
    IFileElementType EXPRESSION_CODE_FRAGMENT = new JetExpressionCodeFragmentType();
}
