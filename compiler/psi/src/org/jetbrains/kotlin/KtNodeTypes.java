/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

public interface KtNodeTypes {
    IFileElementType KT_FILE = new IFileElementType(KotlinLanguage.INSTANCE);

    IElementType CLASS     = KtStubElementTypes.CLASS;
    IElementType FUN       = KtStubElementTypes.FUNCTION;
    IElementType PROPERTY  = KtStubElementTypes.PROPERTY;
    IElementType DESTRUCTURING_DECLARATION = new KtNodeType("DESTRUCTURING_DECLARATION", KtDestructuringDeclaration.class);
    IElementType DESTRUCTURING_DECLARATION_ENTRY = new KtNodeType("DESTRUCTURING_DECLARATION_ENTRY", KtDestructuringDeclarationEntry.class);

    IElementType OBJECT_DECLARATION = KtStubElementTypes.OBJECT_DECLARATION;
    IElementType TYPEALIAS = KtStubElementTypes.TYPEALIAS;

    IElementType ENUM_ENTRY             = KtStubElementTypes.ENUM_ENTRY;
    IElementType CLASS_INITIALIZER      = KtStubElementTypes.CLASS_INITIALIZER;
    IElementType SCRIPT_INITIALIZER     = new KtNodeType("SCRIPT_INITIALIZER", KtScriptInitializer.class);
    IElementType SECONDARY_CONSTRUCTOR  = KtStubElementTypes.SECONDARY_CONSTRUCTOR;
    IElementType PRIMARY_CONSTRUCTOR    = KtStubElementTypes.PRIMARY_CONSTRUCTOR;
    IElementType CONTEXT_RECEIVER       = KtStubElementTypes.CONTEXT_RECEIVER;

    IElementType TYPE_PARAMETER_LIST                = KtStubElementTypes.TYPE_PARAMETER_LIST;
    IElementType TYPE_PARAMETER                     = KtStubElementTypes.TYPE_PARAMETER;
    IElementType SUPER_TYPE_LIST                    = KtStubElementTypes.SUPER_TYPE_LIST;
    IElementType DELEGATED_SUPER_TYPE_ENTRY         = KtStubElementTypes.DELEGATED_SUPER_TYPE_ENTRY;
    IElementType SUPER_TYPE_CALL_ENTRY              = KtStubElementTypes.SUPER_TYPE_CALL_ENTRY;
    IElementType SUPER_TYPE_ENTRY                   = KtStubElementTypes.SUPER_TYPE_ENTRY;
    IElementType PROPERTY_DELEGATE                  = new KtNodeType("PROPERTY_DELEGATE", KtPropertyDelegate.class);
    IElementType CONSTRUCTOR_CALLEE                 = KtStubElementTypes.CONSTRUCTOR_CALLEE;
    IElementType VALUE_PARAMETER_LIST               = KtStubElementTypes.VALUE_PARAMETER_LIST;
    IElementType VALUE_PARAMETER                    = KtStubElementTypes.VALUE_PARAMETER;

    IElementType CLASS_BODY                         = KtStubElementTypes.CLASS_BODY;
    IElementType IMPORT_LIST                        = KtStubElementTypes.IMPORT_LIST;
    IElementType FILE_ANNOTATION_LIST               = KtStubElementTypes.FILE_ANNOTATION_LIST;
    IElementType IMPORT_DIRECTIVE                   = KtStubElementTypes.IMPORT_DIRECTIVE;
    IElementType IMPORT_ALIAS                       = KtStubElementTypes.IMPORT_ALIAS;
    IElementType MODIFIER_LIST                      = KtStubElementTypes.MODIFIER_LIST;
    IElementType ANNOTATION                         = KtStubElementTypes.ANNOTATION;
    IElementType ANNOTATION_ENTRY                   = KtStubElementTypes.ANNOTATION_ENTRY;
    IElementType ANNOTATION_TARGET                  = KtStubElementTypes.ANNOTATION_TARGET;

    IElementType TYPE_ARGUMENT_LIST                 = KtStubElementTypes.TYPE_ARGUMENT_LIST;
    IElementType VALUE_ARGUMENT_LIST                = KtStubElementTypes.VALUE_ARGUMENT_LIST;
    IElementType VALUE_ARGUMENT                     = KtStubElementTypes.VALUE_ARGUMENT;
    IElementType CONTRACT_EFFECT_LIST               = KtStubElementTypes.CONTRACT_EFFECT_LIST;
    IElementType CONTRACT_EFFECT                    = KtStubElementTypes.CONTRACT_EFFECT;
    IElementType LAMBDA_ARGUMENT                    = KtStubElementTypes.LAMBDA_ARGUMENT;
    IElementType VALUE_ARGUMENT_NAME                = KtStubElementTypes.VALUE_ARGUMENT_NAME;
    IElementType TYPE_REFERENCE                     = KtStubElementTypes.TYPE_REFERENCE;

    IElementType USER_TYPE                = KtStubElementTypes.USER_TYPE;
    IElementType DYNAMIC_TYPE             = KtStubElementTypes.DYNAMIC_TYPE;
    IElementType FUNCTION_TYPE            = KtStubElementTypes.FUNCTION_TYPE;
    IElementType FUNCTION_TYPE_RECEIVER   = KtStubElementTypes.FUNCTION_TYPE_RECEIVER;
    IElementType NULLABLE_TYPE            = KtStubElementTypes.NULLABLE_TYPE;
    IElementType INTERSECTION_TYPE        = KtStubElementTypes.INTERSECTION_TYPE;
    IElementType TYPE_PROJECTION          = KtStubElementTypes.TYPE_PROJECTION;

    IElementType PROPERTY_ACCESSOR       = KtStubElementTypes.PROPERTY_ACCESSOR;
    IElementType BACKING_FIELD           = KtStubElementTypes.BACKING_FIELD;
    IElementType INITIALIZER_LIST        = KtStubElementTypes.INITIALIZER_LIST;
    IElementType TYPE_CONSTRAINT_LIST    = KtStubElementTypes.TYPE_CONSTRAINT_LIST;
    IElementType TYPE_CONSTRAINT         = KtStubElementTypes.TYPE_CONSTRAINT;

    IElementType CONSTRUCTOR_DELEGATION_CALL = new KtNodeType.KtLeftBoundNodeType("CONSTRUCTOR_DELEGATION_CALL", KtConstructorDelegationCall.class);
    IElementType CONSTRUCTOR_DELEGATION_REFERENCE = new KtNodeType.KtLeftBoundNodeType("CONSTRUCTOR_DELEGATION_REFERENCE", KtConstructorDelegationReferenceExpression.class);

    IElementType NULL               = KtStubElementTypes.NULL;
    IElementType BOOLEAN_CONSTANT   = KtStubElementTypes.BOOLEAN_CONSTANT;
    IElementType FLOAT_CONSTANT     = KtStubElementTypes.FLOAT_CONSTANT;
    IElementType CHARACTER_CONSTANT = KtStubElementTypes.CHARACTER_CONSTANT;
    IElementType INTEGER_CONSTANT   = KtStubElementTypes.INTEGER_CONSTANT;

    IElementType STRING_TEMPLATE               = KtStubElementTypes.STRING_TEMPLATE;
    IElementType LONG_STRING_TEMPLATE_ENTRY    = KtStubElementTypes.LONG_STRING_TEMPLATE_ENTRY;
    IElementType SHORT_STRING_TEMPLATE_ENTRY   = KtStubElementTypes.SHORT_STRING_TEMPLATE_ENTRY;
    IElementType LITERAL_STRING_TEMPLATE_ENTRY = KtStubElementTypes.LITERAL_STRING_TEMPLATE_ENTRY;
    IElementType ESCAPE_STRING_TEMPLATE_ENTRY  = KtStubElementTypes.ESCAPE_STRING_TEMPLATE_ENTRY;

    IElementType PARENTHESIZED             = new KtNodeType("PARENTHESIZED", KtParenthesizedExpression.class);
    IElementType RETURN                    = new KtNodeType("RETURN", KtReturnExpression.class);
    IElementType THROW                     = new KtNodeType("THROW", KtThrowExpression.class);
    IElementType CONTINUE                  = new KtNodeType("CONTINUE", KtContinueExpression.class);
    IElementType BREAK                     = new KtNodeType("BREAK", KtBreakExpression.class);
    IElementType IF                        = new KtNodeType("IF", KtIfExpression.class);
    IElementType CONDITION                 = new KtNodeType("CONDITION", KtContainerNode.class);
    IElementType THEN                      = new KtNodeType("THEN", KtContainerNodeForControlStructureBody.class);
    IElementType ELSE                      = new KtNodeType("ELSE", KtContainerNodeForControlStructureBody.class);
    IElementType TRY                       = new KtNodeType("TRY", KtTryExpression.class);
    IElementType CATCH                     = new KtNodeType("CATCH", KtCatchClause.class);
    IElementType FINALLY                   = new KtNodeType("FINALLY", KtFinallySection.class);
    IElementType FOR                       = new KtNodeType("FOR", KtForExpression.class);
    IElementType WHILE                     = new KtNodeType("WHILE", KtWhileExpression.class);
    IElementType DO_WHILE                  = new KtNodeType("DO_WHILE", KtDoWhileExpression.class);
    IElementType LOOP_RANGE                = new KtNodeType("LOOP_RANGE", KtContainerNode.class);
    IElementType BODY                      = new KtNodeType("BODY", KtContainerNodeForControlStructureBody.class);

    IElementType BLOCK                     = new BlockExpressionElementType();

    IElementType LAMBDA_EXPRESSION         = new LambdaExpressionElementType();

    IElementType FUNCTION_LITERAL          = new KtNodeType("FUNCTION_LITERAL", KtFunctionLiteral.class);
    IElementType ANNOTATED_EXPRESSION      = new KtNodeType("ANNOTATED_EXPRESSION", KtAnnotatedExpression.class);

    IElementType REFERENCE_EXPRESSION     = KtStubElementTypes.REFERENCE_EXPRESSION;
    IElementType ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION = KtStubElementTypes.ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION;
    IElementType OPERATION_REFERENCE       = new KtNodeType("OPERATION_REFERENCE", KtOperationReferenceExpression.class);
    IElementType LABEL                     = new KtNodeType("LABEL", KtLabelReferenceExpression.class);

    IElementType LABEL_QUALIFIER           = new KtNodeType("LABEL_QUALIFIER", KtContainerNode.class);

    IElementType THIS_EXPRESSION           = new KtNodeType("THIS_EXPRESSION", KtThisExpression.class);
    IElementType SUPER_EXPRESSION          = new KtNodeType("SUPER_EXPRESSION", KtSuperExpression.class);
    IElementType BINARY_EXPRESSION         = new KtNodeType("BINARY_EXPRESSION", KtBinaryExpression.class);
    IElementType BINARY_WITH_TYPE          = new KtNodeType("BINARY_WITH_TYPE", KtBinaryExpressionWithTypeRHS.class);
    IElementType IS_EXPRESSION             = new KtNodeType("IS_EXPRESSION", KtIsExpression.class);
    IElementType PREFIX_EXPRESSION         = new KtNodeType("PREFIX_EXPRESSION", KtPrefixExpression.class);
    IElementType POSTFIX_EXPRESSION        = new KtNodeType("POSTFIX_EXPRESSION", KtPostfixExpression.class);
    IElementType LABELED_EXPRESSION        = new KtNodeType("LABELED_EXPRESSION", KtLabeledExpression.class);
    IElementType CALL_EXPRESSION           = new KtNodeType("CALL_EXPRESSION", KtCallExpression.class);
    IElementType ARRAY_ACCESS_EXPRESSION   = new KtNodeType("ARRAY_ACCESS_EXPRESSION", KtArrayAccessExpression.class);
    IElementType INDICES                   = new KtNodeType("INDICES", KtContainerNode.class);
    IElementType DOT_QUALIFIED_EXPRESSION  = KtStubElementTypes.DOT_QUALIFIED_EXPRESSION;
    IElementType CALLABLE_REFERENCE_EXPRESSION = new KtNodeType("CALLABLE_REFERENCE_EXPRESSION", KtCallableReferenceExpression.class);
    IElementType CLASS_LITERAL_EXPRESSION  = new KtNodeType("CLASS_LITERAL_EXPRESSION", KtClassLiteralExpression.class);
    IElementType SAFE_ACCESS_EXPRESSION    = new KtNodeType("SAFE_ACCESS_EXPRESSION", KtSafeQualifiedExpression.class);

    IElementType OBJECT_LITERAL            = new KtNodeType("OBJECT_LITERAL", KtObjectLiteralExpression.class);

    IElementType WHEN                      = new KtNodeType("WHEN", KtWhenExpression.class);
    IElementType WHEN_ENTRY                = new KtNodeType("WHEN_ENTRY", KtWhenEntry.class);

    IElementType WHEN_CONDITION_IN_RANGE   = new KtNodeType("WHEN_CONDITION_IN_RANGE", KtWhenConditionInRange.class);
    IElementType WHEN_CONDITION_IS_PATTERN = new KtNodeType("WHEN_CONDITION_IS_PATTERN", KtWhenConditionIsPattern.class);
    IElementType WHEN_CONDITION_EXPRESSION = new KtNodeType("WHEN_CONDITION_WITH_EXPRESSION", KtWhenConditionWithExpression.class);

    IElementType COLLECTION_LITERAL_EXPRESSION = new KtNodeType("COLLECTION_LITERAL_EXPRESSION", KtCollectionLiteralExpression.class);

    IElementType PACKAGE_DIRECTIVE = KtStubElementTypes.PACKAGE_DIRECTIVE;

    IElementType SCRIPT = KtStubElementTypes.SCRIPT;

    IFileElementType TYPE_CODE_FRAGMENT = KtStubElementTypes.TYPE_CODE_FRAGMENT;
    IFileElementType EXPRESSION_CODE_FRAGMENT = KtStubElementTypes.EXPRESSION_CODE_FRAGMENT;
    IFileElementType BLOCK_CODE_FRAGMENT = KtStubElementTypes.BLOCK_CODE_FRAGMENT;
}
