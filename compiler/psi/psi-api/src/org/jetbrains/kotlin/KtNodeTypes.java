/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.psi.*;

public interface KtNodeTypes {
    IFileElementType FILE = KtStubBasedElementTypes.FILE;

    IFileElementType KT_FILE = new IFileElementType(KotlinLanguage.INSTANCE);

    IElementType CLASS     = KtStubBasedElementTypes.CLASS;
    IElementType FUN       = KtStubBasedElementTypes.FUNCTION;
    IElementType PROPERTY  = KtStubBasedElementTypes.PROPERTY;
    IElementType DESTRUCTURING_DECLARATION = new KtNodeType("DESTRUCTURING_DECLARATION", KtDestructuringDeclaration.class);
    IElementType DESTRUCTURING_DECLARATION_ENTRY = new KtNodeType("DESTRUCTURING_DECLARATION_ENTRY", KtDestructuringDeclarationEntry.class);

    IElementType OBJECT_DECLARATION = KtStubBasedElementTypes.OBJECT_DECLARATION;
    IElementType TYPEALIAS = KtStubBasedElementTypes.TYPEALIAS;

    IElementType ENUM_ENTRY             = KtStubBasedElementTypes.ENUM_ENTRY;
    IElementType CLASS_INITIALIZER      = KtStubBasedElementTypes.CLASS_INITIALIZER;
    IElementType SCRIPT_INITIALIZER     = KtStubBasedElementTypes.SCRIPT_INITIALIZER;
    IElementType SECONDARY_CONSTRUCTOR  = KtStubBasedElementTypes.SECONDARY_CONSTRUCTOR;
    IElementType PRIMARY_CONSTRUCTOR    = KtStubBasedElementTypes.PRIMARY_CONSTRUCTOR;
    IElementType CONTEXT_RECEIVER       = KtStubBasedElementTypes.CONTEXT_RECEIVER;
    IElementType CONTEXT_RECEIVER_LIST  = KtStubBasedElementTypes.CONTEXT_PARAMETER_LIST;

    IElementType TYPE_PARAMETER_LIST                = KtStubBasedElementTypes.TYPE_PARAMETER_LIST;
    IElementType TYPE_PARAMETER                     = KtStubBasedElementTypes.TYPE_PARAMETER;
    IElementType SUPER_TYPE_LIST                    = KtStubBasedElementTypes.SUPER_TYPE_LIST;
    IElementType DELEGATED_SUPER_TYPE_ENTRY         = KtStubBasedElementTypes.DELEGATED_SUPER_TYPE_ENTRY;
    IElementType SUPER_TYPE_CALL_ENTRY              = KtStubBasedElementTypes.SUPER_TYPE_CALL_ENTRY;
    IElementType SUPER_TYPE_ENTRY                   = KtStubBasedElementTypes.SUPER_TYPE_ENTRY;
    IElementType PROPERTY_DELEGATE                  = new KtNodeType("PROPERTY_DELEGATE", KtPropertyDelegate.class);
    IElementType CONSTRUCTOR_CALLEE                 = KtStubBasedElementTypes.CONSTRUCTOR_CALLEE;
    IElementType VALUE_PARAMETER_LIST               = KtStubBasedElementTypes.VALUE_PARAMETER_LIST;
    IElementType VALUE_PARAMETER                    = KtStubBasedElementTypes.VALUE_PARAMETER;

    IElementType CLASS_BODY                         = KtStubBasedElementTypes.CLASS_BODY;
    IElementType IMPORT_LIST                        = KtStubBasedElementTypes.IMPORT_LIST;
    IElementType FILE_ANNOTATION_LIST               = KtStubBasedElementTypes.FILE_ANNOTATION_LIST;
    IElementType IMPORT_DIRECTIVE                   = KtStubBasedElementTypes.IMPORT_DIRECTIVE;
    IElementType IMPORT_ALIAS                       = KtStubBasedElementTypes.IMPORT_ALIAS;
    IElementType MODIFIER_LIST                      = KtStubBasedElementTypes.MODIFIER_LIST;
    IElementType ANNOTATION                         = KtStubBasedElementTypes.ANNOTATION;
    IElementType ANNOTATION_ENTRY                   = KtStubBasedElementTypes.ANNOTATION_ENTRY;
    IElementType ANNOTATION_TARGET                  = KtStubBasedElementTypes.ANNOTATION_TARGET;

    IElementType TYPE_ARGUMENT_LIST                 = KtStubBasedElementTypes.TYPE_ARGUMENT_LIST;
    IElementType VALUE_ARGUMENT_LIST                = KtStubBasedElementTypes.VALUE_ARGUMENT_LIST;
    IElementType VALUE_ARGUMENT                     = KtStubBasedElementTypes.VALUE_ARGUMENT;
    IElementType CONTRACT_EFFECT_LIST               = KtStubBasedElementTypes.CONTRACT_EFFECT_LIST;
    IElementType CONTRACT_EFFECT                    = KtStubBasedElementTypes.CONTRACT_EFFECT;
    IElementType LAMBDA_ARGUMENT                    = KtStubBasedElementTypes.LAMBDA_ARGUMENT;
    IElementType VALUE_ARGUMENT_NAME                = KtStubBasedElementTypes.VALUE_ARGUMENT_NAME;
    IElementType TYPE_REFERENCE                     = KtStubBasedElementTypes.TYPE_REFERENCE;

    IElementType USER_TYPE                = KtStubBasedElementTypes.USER_TYPE;
    IElementType DYNAMIC_TYPE             = KtStubBasedElementTypes.DYNAMIC_TYPE;
    IElementType FUNCTION_TYPE            = KtStubBasedElementTypes.FUNCTION_TYPE;
    IElementType FUNCTION_TYPE_RECEIVER   = KtStubBasedElementTypes.FUNCTION_TYPE_RECEIVER;
    IElementType NULLABLE_TYPE            = KtStubBasedElementTypes.NULLABLE_TYPE;
    IElementType INTERSECTION_TYPE        = KtStubBasedElementTypes.INTERSECTION_TYPE;
    IElementType TYPE_PROJECTION          = KtStubBasedElementTypes.TYPE_PROJECTION;

    IElementType PROPERTY_ACCESSOR       = KtStubBasedElementTypes.PROPERTY_ACCESSOR;
    IElementType BACKING_FIELD           = KtStubBasedElementTypes.BACKING_FIELD;
    IElementType INITIALIZER_LIST        = KtStubBasedElementTypes.INITIALIZER_LIST;
    IElementType TYPE_CONSTRAINT_LIST    = KtStubBasedElementTypes.TYPE_CONSTRAINT_LIST;
    IElementType TYPE_CONSTRAINT         = KtStubBasedElementTypes.TYPE_CONSTRAINT;

    IElementType CONSTRUCTOR_DELEGATION_CALL = new KtNodeType.KtLeftBoundNodeType("CONSTRUCTOR_DELEGATION_CALL", KtConstructorDelegationCall.class);
    IElementType CONSTRUCTOR_DELEGATION_REFERENCE = new KtNodeType.KtLeftBoundNodeType("CONSTRUCTOR_DELEGATION_REFERENCE", KtConstructorDelegationReferenceExpression.class);

    IElementType NULL               = KtStubBasedElementTypes.NULL;
    IElementType BOOLEAN_CONSTANT   = KtStubBasedElementTypes.BOOLEAN_CONSTANT;
    IElementType FLOAT_CONSTANT     = KtStubBasedElementTypes.FLOAT_CONSTANT;
    IElementType CHARACTER_CONSTANT = KtStubBasedElementTypes.CHARACTER_CONSTANT;
    IElementType INTEGER_CONSTANT   = KtStubBasedElementTypes.INTEGER_CONSTANT;

    IElementType STRING_TEMPLATE               = KtStubBasedElementTypes.STRING_TEMPLATE;
    IElementType LONG_STRING_TEMPLATE_ENTRY    = KtStubBasedElementTypes.LONG_STRING_TEMPLATE_ENTRY;
    IElementType SHORT_STRING_TEMPLATE_ENTRY   = KtStubBasedElementTypes.SHORT_STRING_TEMPLATE_ENTRY;
    IElementType LITERAL_STRING_TEMPLATE_ENTRY = KtStubBasedElementTypes.LITERAL_STRING_TEMPLATE_ENTRY;
    IElementType ESCAPE_STRING_TEMPLATE_ENTRY  = KtStubBasedElementTypes.ESCAPE_STRING_TEMPLATE_ENTRY;
    IElementType STRING_INTERPOLATION_PREFIX   = KtStubBasedElementTypes.STRING_INTERPOLATION_PREFIX;

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

    IElementType BLOCK                     = KtStubBasedElementTypes.BLOCK;

    IElementType LAMBDA_EXPRESSION         = KtStubBasedElementTypes.LAMBDA_EXPRESSION;

    IElementType FUNCTION_LITERAL          = new KtNodeType("FUNCTION_LITERAL", KtFunctionLiteral.class);
    IElementType ANNOTATED_EXPRESSION      = new KtNodeType("ANNOTATED_EXPRESSION", KtAnnotatedExpression.class);

    IElementType REFERENCE_EXPRESSION     = KtStubBasedElementTypes.REFERENCE_EXPRESSION;
    IElementType ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION = KtStubBasedElementTypes.ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION;
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
    IElementType CALL_EXPRESSION           = KtStubBasedElementTypes.CALL_EXPRESSION;
    IElementType ARRAY_ACCESS_EXPRESSION   = new KtNodeType("ARRAY_ACCESS_EXPRESSION", KtArrayAccessExpression.class);
    IElementType INDICES                   = new KtNodeType("INDICES", KtContainerNode.class);
    IElementType DOT_QUALIFIED_EXPRESSION  = KtStubBasedElementTypes.DOT_QUALIFIED_EXPRESSION;
    IElementType CALLABLE_REFERENCE_EXPRESSION = new KtNodeType("CALLABLE_REFERENCE_EXPRESSION", KtCallableReferenceExpression.class);
    IElementType CLASS_LITERAL_EXPRESSION  = KtStubBasedElementTypes.CLASS_LITERAL_EXPRESSION;
    IElementType SAFE_ACCESS_EXPRESSION    = new KtNodeType("SAFE_ACCESS_EXPRESSION", KtSafeQualifiedExpression.class);

    IElementType OBJECT_LITERAL            = new KtNodeType("OBJECT_LITERAL", KtObjectLiteralExpression.class);

    IElementType WHEN                      = new KtNodeType("WHEN", KtWhenExpression.class);
    IElementType WHEN_ENTRY                = new KtNodeType("WHEN_ENTRY", KtWhenEntry.class);
    IElementType WHEN_ENTRY_GUARD          = new KtNodeType("WHEN_ENTRY_GUARD", KtWhenEntryGuard.class);

    IElementType WHEN_CONDITION_IN_RANGE   = new KtNodeType("WHEN_CONDITION_IN_RANGE", KtWhenConditionInRange.class);
    IElementType WHEN_CONDITION_IS_PATTERN = new KtNodeType("WHEN_CONDITION_IS_PATTERN", KtWhenConditionIsPattern.class);
    IElementType WHEN_CONDITION_EXPRESSION = new KtNodeType("WHEN_CONDITION_WITH_EXPRESSION", KtWhenConditionWithExpression.class);

    IElementType COLLECTION_LITERAL_EXPRESSION = KtStubBasedElementTypes.COLLECTION_LITERAL_EXPRESSION;

    IElementType PACKAGE_DIRECTIVE = KtStubBasedElementTypes.PACKAGE_DIRECTIVE;

    IElementType SCRIPT = KtStubBasedElementTypes.SCRIPT;

    IFileElementType TYPE_CODE_FRAGMENT = KtStubBasedElementTypes.TYPE_CODE_FRAGMENT;
    IFileElementType EXPRESSION_CODE_FRAGMENT = KtStubBasedElementTypes.EXPRESSION_CODE_FRAGMENT;
    IFileElementType BLOCK_CODE_FRAGMENT = KtStubBasedElementTypes.BLOCK_CODE_FRAGMENT;
}
