/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

@SuppressWarnings("WeakerAccess") // Let all static identifiers be public as well as corresponding elements
public class KtNodeTypes {
    public static final IElementType CLASS     = KtStubElementTypes.CLASS;
    public static final IElementType FUN       = KtStubElementTypes.FUNCTION;
    public static final IElementType PROPERTY  = KtStubElementTypes.PROPERTY;

    public static final IElementType OBJECT_DECLARATION = KtStubElementTypes.OBJECT_DECLARATION;
    public static final IElementType TYPEALIAS = KtStubElementTypes.TYPEALIAS;

    public static final IElementType ENUM_ENTRY             = KtStubElementTypes.ENUM_ENTRY;
    public static final IElementType CLASS_INITIALIZER      = KtStubElementTypes.CLASS_INITIALIZER;
    public static final IElementType SECONDARY_CONSTRUCTOR  = KtStubElementTypes.SECONDARY_CONSTRUCTOR;
    public static final IElementType PRIMARY_CONSTRUCTOR    = KtStubElementTypes.PRIMARY_CONSTRUCTOR;
    public static final IElementType CONTEXT_RECEIVER       = KtStubElementTypes.CONTEXT_RECEIVER;
    public static final IElementType CONTEXT_RECEIVER_LIST  = KtStubElementTypes.CONTEXT_RECEIVER_LIST;

    public static final IElementType TYPE_PARAMETER_LIST                = KtStubElementTypes.TYPE_PARAMETER_LIST;
    public static final IElementType TYPE_PARAMETER                     = KtStubElementTypes.TYPE_PARAMETER;
    public static final IElementType SUPER_TYPE_LIST                    = KtStubElementTypes.SUPER_TYPE_LIST;
    public static final IElementType DELEGATED_SUPER_TYPE_ENTRY         = KtStubElementTypes.DELEGATED_SUPER_TYPE_ENTRY;
    public static final IElementType SUPER_TYPE_CALL_ENTRY              = KtStubElementTypes.SUPER_TYPE_CALL_ENTRY;
    public static final IElementType SUPER_TYPE_ENTRY                   = KtStubElementTypes.SUPER_TYPE_ENTRY;
    public static final IElementType CONSTRUCTOR_CALLEE                 = KtStubElementTypes.CONSTRUCTOR_CALLEE;
    public static final IElementType VALUE_PARAMETER_LIST               = KtStubElementTypes.VALUE_PARAMETER_LIST;
    public static final IElementType VALUE_PARAMETER                    = KtStubElementTypes.VALUE_PARAMETER;

    public static final IElementType CLASS_BODY                         = KtStubElementTypes.CLASS_BODY;
    public static final IElementType IMPORT_LIST                        = KtStubElementTypes.IMPORT_LIST;
    public static final IElementType FILE_ANNOTATION_LIST               = KtStubElementTypes.FILE_ANNOTATION_LIST;
    public static final IElementType IMPORT_DIRECTIVE                   = KtStubElementTypes.IMPORT_DIRECTIVE;
    public static final IElementType IMPORT_ALIAS                       = KtStubElementTypes.IMPORT_ALIAS;
    public static final IElementType MODIFIER_LIST                      = KtStubElementTypes.MODIFIER_LIST;
    public static final IElementType ANNOTATION                         = KtStubElementTypes.ANNOTATION;
    public static final IElementType ANNOTATION_ENTRY                   = KtStubElementTypes.ANNOTATION_ENTRY;
    public static final IElementType ANNOTATION_TARGET                  = KtStubElementTypes.ANNOTATION_TARGET;

    public static final IElementType TYPE_ARGUMENT_LIST                 = KtStubElementTypes.TYPE_ARGUMENT_LIST;
    public static final IElementType VALUE_ARGUMENT_LIST                = KtStubElementTypes.VALUE_ARGUMENT_LIST;
    public static final IElementType VALUE_ARGUMENT                     = KtStubElementTypes.VALUE_ARGUMENT;
    public static final IElementType CONTRACT_EFFECT_LIST               = KtStubElementTypes.CONTRACT_EFFECT_LIST;
    public static final IElementType CONTRACT_EFFECT                    = KtStubElementTypes.CONTRACT_EFFECT;
    public static final IElementType LAMBDA_ARGUMENT                    = KtStubElementTypes.LAMBDA_ARGUMENT;
    public static final IElementType VALUE_ARGUMENT_NAME                = KtStubElementTypes.VALUE_ARGUMENT_NAME;
    public static final IElementType TYPE_REFERENCE                     = KtStubElementTypes.TYPE_REFERENCE;

    public static final IElementType USER_TYPE                = KtStubElementTypes.USER_TYPE;
    public static final IElementType DYNAMIC_TYPE             = KtStubElementTypes.DYNAMIC_TYPE;
    public static final IElementType FUNCTION_TYPE            = KtStubElementTypes.FUNCTION_TYPE;
    public static final IElementType FUNCTION_TYPE_RECEIVER   = KtStubElementTypes.FUNCTION_TYPE_RECEIVER;
    public static final IElementType NULLABLE_TYPE            = KtStubElementTypes.NULLABLE_TYPE;
    public static final IElementType INTERSECTION_TYPE        = KtStubElementTypes.INTERSECTION_TYPE;
    public static final IElementType TYPE_PROJECTION          = KtStubElementTypes.TYPE_PROJECTION;

    public static final IElementType PROPERTY_ACCESSOR       = KtStubElementTypes.PROPERTY_ACCESSOR;
    public static final IElementType BACKING_FIELD           = KtStubElementTypes.BACKING_FIELD;
    public static final IElementType INITIALIZER_LIST        = KtStubElementTypes.INITIALIZER_LIST;
    public static final IElementType TYPE_CONSTRAINT_LIST    = KtStubElementTypes.TYPE_CONSTRAINT_LIST;
    public static final IElementType TYPE_CONSTRAINT         = KtStubElementTypes.TYPE_CONSTRAINT;

    public static final IElementType NULL               = KtStubElementTypes.NULL;
    public static final IElementType BOOLEAN_CONSTANT   = KtStubElementTypes.BOOLEAN_CONSTANT;
    public static final IElementType FLOAT_CONSTANT     = KtStubElementTypes.FLOAT_CONSTANT;
    public static final IElementType CHARACTER_CONSTANT = KtStubElementTypes.CHARACTER_CONSTANT;
    public static final IElementType INTEGER_CONSTANT   = KtStubElementTypes.INTEGER_CONSTANT;

    public static final IElementType STRING_TEMPLATE               = KtStubElementTypes.STRING_TEMPLATE;
    public static final IElementType LONG_STRING_TEMPLATE_ENTRY    = KtStubElementTypes.LONG_STRING_TEMPLATE_ENTRY;
    public static final IElementType SHORT_STRING_TEMPLATE_ENTRY   = KtStubElementTypes.SHORT_STRING_TEMPLATE_ENTRY;
    public static final IElementType LITERAL_STRING_TEMPLATE_ENTRY = KtStubElementTypes.LITERAL_STRING_TEMPLATE_ENTRY;
    public static final IElementType ESCAPE_STRING_TEMPLATE_ENTRY  = KtStubElementTypes.ESCAPE_STRING_TEMPLATE_ENTRY;
    public static final IElementType STRING_INTERPOLATION_PREFIX   = KtStubElementTypes.STRING_INTERPOLATION_PREFIX;

    public static final IElementType REFERENCE_EXPRESSION     = KtStubElementTypes.REFERENCE_EXPRESSION;
    public static final IElementType ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION = KtStubElementTypes.ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION;

    public static final IElementType DOT_QUALIFIED_EXPRESSION  = KtStubElementTypes.DOT_QUALIFIED_EXPRESSION;
    public static final IElementType CLASS_LITERAL_EXPRESSION  = KtStubElementTypes.CLASS_LITERAL_EXPRESSION;

    public static final IElementType COLLECTION_LITERAL_EXPRESSION = KtStubElementTypes.COLLECTION_LITERAL_EXPRESSION;

    public static final IElementType PACKAGE_DIRECTIVE = KtStubElementTypes.PACKAGE_DIRECTIVE;

    public static final IElementType SCRIPT = KtStubElementTypes.SCRIPT;

    public static final IFileElementType TYPE_CODE_FRAGMENT = KtStubElementTypes.TYPE_CODE_FRAGMENT;
    public static final IFileElementType EXPRESSION_CODE_FRAGMENT = KtStubElementTypes.EXPRESSION_CODE_FRAGMENT;
    public static final IFileElementType BLOCK_CODE_FRAGMENT = KtStubElementTypes.BLOCK_CODE_FRAGMENT;

    public static final int KT_FILE_INDEX = KtStubElementTypes.STRING_INTERPOLATION_PREFIX_INDEX + 1;
    public static final int DESTRUCTURING_DECLARATION_INDEX = KT_FILE_INDEX + 1;
    public static final int DESTRUCTURING_DECLARATION_ENTRY_INDEX = DESTRUCTURING_DECLARATION_INDEX + 1;
    public static final int SCRIPT_INITIALIZER_INDEX = DESTRUCTURING_DECLARATION_ENTRY_INDEX + 1;
    public static final int PROPERTY_DELEGATE_INDEX = SCRIPT_INITIALIZER_INDEX + 1;
    public static final int CONSTRUCTOR_DELEGATION_CALL_INDEX = PROPERTY_DELEGATE_INDEX + 1;
    public static final int CONSTRUCTOR_DELEGATION_REFERENCE_INDEX = CONSTRUCTOR_DELEGATION_CALL_INDEX + 1;
    public static final int PARENTHESIZED_INDEX = CONSTRUCTOR_DELEGATION_REFERENCE_INDEX + 1;
    public static final int RETURN_INDEX = PARENTHESIZED_INDEX + 1;
    public static final int THROW_INDEX = RETURN_INDEX + 1;
    public static final int CONTINUE_INDEX = THROW_INDEX + 1;
    public static final int BREAK_INDEX = CONTINUE_INDEX + 1;
    public static final int IF_INDEX = BREAK_INDEX + 1;
    public static final int CONDITION_INDEX = IF_INDEX + 1;
    public static final int THEN_INDEX = CONDITION_INDEX + 1;
    public static final int ELSE_INDEX = THEN_INDEX + 1;
    public static final int TRY_INDEX = ELSE_INDEX + 1;
    public static final int CATCH_INDEX = TRY_INDEX + 1;
    public static final int FINALLY_INDEX = CATCH_INDEX + 1;
    public static final int FOR_INDEX = FINALLY_INDEX + 1;
    public static final int WHILE_INDEX = FOR_INDEX + 1;
    public static final int DO_WHILE_INDEX = WHILE_INDEX + 1;
    public static final int LOOP_RANGE_INDEX = DO_WHILE_INDEX + 1;
    public static final int BODY_INDEX = LOOP_RANGE_INDEX + 1;
    public static final int BLOCK_INDEX = BODY_INDEX + 1;
    public static final int LAMBDA_EXPRESSION_INDEX = BLOCK_INDEX + 1;
    public static final int FUNCTION_LITERAL_INDEX = LAMBDA_EXPRESSION_INDEX + 1;
    public static final int ANNOTATED_EXPRESSION_INDEX = FUNCTION_LITERAL_INDEX + 1;
    public static final int OPERATION_REFERENCE_INDEX = ANNOTATED_EXPRESSION_INDEX + 1;
    public static final int LABEL_INDEX = OPERATION_REFERENCE_INDEX + 1;
    public static final int LABEL_QUALIFIER_INDEX = LABEL_INDEX + 1;
    public static final int THIS_EXPRESSION_INDEX = LABEL_QUALIFIER_INDEX + 1;
    public static final int SUPER_EXPRESSION_INDEX = THIS_EXPRESSION_INDEX + 1;
    public static final int BINARY_EXPRESSION_INDEX = SUPER_EXPRESSION_INDEX + 1;
    public static final int BINARY_WITH_TYPE_INDEX = BINARY_EXPRESSION_INDEX + 1;
    public static final int IS_EXPRESSION_INDEX = BINARY_WITH_TYPE_INDEX + 1;
    public static final int PREFIX_EXPRESSION_INDEX = IS_EXPRESSION_INDEX + 1;
    public static final int POSTFIX_EXPRESSION_INDEX = PREFIX_EXPRESSION_INDEX + 1;
    public static final int LABELED_EXPRESSION_INDEX = POSTFIX_EXPRESSION_INDEX + 1;
    public static final int CALL_EXPRESSION_INDEX = LABELED_EXPRESSION_INDEX + 1;
    public static final int ARRAY_ACCESS_EXPRESSION_INDEX = CALL_EXPRESSION_INDEX + 1;
    public static final int INDICES_INDEX = ARRAY_ACCESS_EXPRESSION_INDEX + 1;
    public static final int CALLABLE_REFERENCE_EXPRESSION_INDEX = INDICES_INDEX + 1;
    public static final int SAFE_ACCESS_EXPRESSION_INDEX = CALLABLE_REFERENCE_EXPRESSION_INDEX + 1;
    public static final int OBJECT_LITERAL_INDEX = SAFE_ACCESS_EXPRESSION_INDEX + 1;
    public static final int WHEN_INDEX = OBJECT_LITERAL_INDEX + 1;
    public static final int WHEN_ENTRY_INDEX = WHEN_INDEX + 1;
    public static final int WHEN_ENTRY_GUARD_INDEX = WHEN_ENTRY_INDEX + 1;
    public static final int WHEN_CONDITION_IN_RANGE_INDEX = WHEN_ENTRY_GUARD_INDEX + 1;
    public static final int WHEN_CONDITION_IS_PATTERN_INDEX = WHEN_CONDITION_IN_RANGE_INDEX + 1;
    public static final int WHEN_CONDITION_EXPRESSION_INDEX = WHEN_CONDITION_IS_PATTERN_INDEX + 1;

    public static final IFileElementType KT_FILE = new IFileElementType(KotlinLanguage.INSTANCE);

    public static final IElementType DESTRUCTURING_DECLARATION = new KtNodeType("DESTRUCTURING_DECLARATION", KtDestructuringDeclaration.class);
    public static final IElementType DESTRUCTURING_DECLARATION_ENTRY = new KtNodeType("DESTRUCTURING_DECLARATION_ENTRY", KtDestructuringDeclarationEntry.class);
    public static final IElementType SCRIPT_INITIALIZER     = new KtNodeType("SCRIPT_INITIALIZER", KtScriptInitializer.class);
    public static final IElementType PROPERTY_DELEGATE                  = new KtNodeType("PROPERTY_DELEGATE", KtPropertyDelegate.class);
    public static final IElementType CONSTRUCTOR_DELEGATION_CALL = new KtNodeType.KtLeftBoundNodeType("CONSTRUCTOR_DELEGATION_CALL", KtConstructorDelegationCall.class);
    public static final IElementType CONSTRUCTOR_DELEGATION_REFERENCE = new KtNodeType.KtLeftBoundNodeType("CONSTRUCTOR_DELEGATION_REFERENCE", KtConstructorDelegationReferenceExpression.class);

    public static final IElementType PARENTHESIZED             = new KtNodeType("PARENTHESIZED", KtParenthesizedExpression.class);
    public static final IElementType RETURN                    = new KtNodeType("RETURN", KtReturnExpression.class);
    public static final IElementType THROW                     = new KtNodeType("THROW", KtThrowExpression.class);
    public static final IElementType CONTINUE                  = new KtNodeType("CONTINUE", KtContinueExpression.class);
    public static final IElementType BREAK                     = new KtNodeType("BREAK", KtBreakExpression.class);
    public static final IElementType IF                        = new KtNodeType("IF", KtIfExpression.class);
    public static final IElementType CONDITION                 = new KtNodeType("CONDITION", KtContainerNode.class);
    public static final IElementType THEN                      = new KtNodeType("THEN", KtContainerNodeForControlStructureBody.class);
    public static final IElementType ELSE                      = new KtNodeType("ELSE", KtContainerNodeForControlStructureBody.class);
    public static final IElementType TRY                       = new KtNodeType("TRY", KtTryExpression.class);
    public static final IElementType CATCH                     = new KtNodeType("CATCH", KtCatchClause.class);
    public static final IElementType FINALLY                   = new KtNodeType("FINALLY", KtFinallySection.class);
    public static final IElementType FOR                       = new KtNodeType("FOR", KtForExpression.class);
    public static final IElementType WHILE                     = new KtNodeType("WHILE", KtWhileExpression.class);
    public static final IElementType DO_WHILE                  = new KtNodeType("DO_WHILE", KtDoWhileExpression.class);
    public static final IElementType LOOP_RANGE                = new KtNodeType("LOOP_RANGE", KtContainerNode.class);
    public static final IElementType BODY                      = new KtNodeType("BODY", KtContainerNodeForControlStructureBody.class);

    public static final IElementType BLOCK                     = new BlockExpressionElementType();

    public static final IElementType LAMBDA_EXPRESSION         = new LambdaExpressionElementType();

    public static final IElementType FUNCTION_LITERAL          = new KtNodeType("FUNCTION_LITERAL", KtFunctionLiteral.class);
    public static final IElementType ANNOTATED_EXPRESSION      = new KtNodeType("ANNOTATED_EXPRESSION", KtAnnotatedExpression.class);

    public static final IElementType OPERATION_REFERENCE       = new KtNodeType("OPERATION_REFERENCE", KtOperationReferenceExpression.class);
    public static final IElementType LABEL                     = new KtNodeType("LABEL", KtLabelReferenceExpression.class);

    public static final IElementType LABEL_QUALIFIER           = new KtNodeType("LABEL_QUALIFIER", KtContainerNode.class);

    public static final IElementType THIS_EXPRESSION           = new KtNodeType("THIS_EXPRESSION", KtThisExpression.class);
    public static final IElementType SUPER_EXPRESSION          = new KtNodeType("SUPER_EXPRESSION", KtSuperExpression.class);
    public static final IElementType BINARY_EXPRESSION         = new KtNodeType("BINARY_EXPRESSION", KtBinaryExpression.class);
    public static final IElementType BINARY_WITH_TYPE          = new KtNodeType("BINARY_WITH_TYPE", KtBinaryExpressionWithTypeRHS.class);
    public static final IElementType IS_EXPRESSION             = new KtNodeType("IS_EXPRESSION", KtIsExpression.class);
    public static final IElementType PREFIX_EXPRESSION         = new KtNodeType("PREFIX_EXPRESSION", KtPrefixExpression.class);
    public static final IElementType POSTFIX_EXPRESSION        = new KtNodeType("POSTFIX_EXPRESSION", KtPostfixExpression.class);
    public static final IElementType LABELED_EXPRESSION        = new KtNodeType("LABELED_EXPRESSION", KtLabeledExpression.class);
    public static final IElementType CALL_EXPRESSION           = new KtNodeType("CALL_EXPRESSION", KtCallExpression.class);
    public static final IElementType ARRAY_ACCESS_EXPRESSION   = new KtNodeType("ARRAY_ACCESS_EXPRESSION", KtArrayAccessExpression.class);
    public static final IElementType INDICES                   = new KtNodeType("INDICES", KtContainerNode.class);
    public static final IElementType CALLABLE_REFERENCE_EXPRESSION = new KtNodeType("CALLABLE_REFERENCE_EXPRESSION", KtCallableReferenceExpression.class);
    public static final IElementType SAFE_ACCESS_EXPRESSION    = new KtNodeType("SAFE_ACCESS_EXPRESSION", KtSafeQualifiedExpression.class);

    public static final IElementType OBJECT_LITERAL            = new KtNodeType("OBJECT_LITERAL", KtObjectLiteralExpression.class);

    public static final IElementType WHEN                      = new KtNodeType("WHEN", KtWhenExpression.class);
    public static final IElementType WHEN_ENTRY                = new KtNodeType("WHEN_ENTRY", KtWhenEntry.class);
    public static final IElementType WHEN_ENTRY_GUARD          = new KtNodeType("WHEN_ENTRY_GUARD", KtWhenEntryGuard.class);

    public static final IElementType WHEN_CONDITION_IN_RANGE   = new KtNodeType("WHEN_CONDITION_IN_RANGE", KtWhenConditionInRange.class);
    public static final IElementType WHEN_CONDITION_IS_PATTERN = new KtNodeType("WHEN_CONDITION_IS_PATTERN", KtWhenConditionIsPattern.class);
    public static final IElementType WHEN_CONDITION_EXPRESSION = new KtNodeType("WHEN_CONDITION_WITH_EXPRESSION", KtWhenConditionWithExpression.class);

    static {
        ElementTypeChecker.checkExplicitStaticIndexesMatchImplicit(KtNodeTypes.class);
    }
}
