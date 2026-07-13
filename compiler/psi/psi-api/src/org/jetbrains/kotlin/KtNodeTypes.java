/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.psi.*;

public interface KtNodeTypes {
    @NotNull IFileElementType FILE = KtStubBasedElementTypes.FILE;

    @NotNull IFileElementType KT_FILE = new IFileElementType(KotlinLanguage.INSTANCE);

    @NotNull IElementType CLASS     = KtStubBasedElementTypes.CLASS;
    @NotNull IElementType FUN       = KtStubBasedElementTypes.FUNCTION;
    @NotNull IElementType PROPERTY  = KtStubBasedElementTypes.PROPERTY;
    @NotNull IElementType DESTRUCTURING_DECLARATION = KtStubBasedElementTypes.DESTRUCTURING_DECLARATION;
    @NotNull IElementType DESTRUCTURING_DECLARATION_ENTRY = new KtNodeType("DESTRUCTURING_DECLARATION_ENTRY", KtDestructuringDeclarationEntry::new);

    @NotNull IElementType OBJECT_DECLARATION = KtStubBasedElementTypes.OBJECT_DECLARATION;
    @NotNull IElementType TYPEALIAS = KtStubBasedElementTypes.TYPEALIAS;
    @NotNull IElementType COMPANION_BLOCK = KtStubBasedElementTypes.COMPANION_BLOCK;

    @NotNull IElementType ENUM_ENTRY             = KtStubBasedElementTypes.ENUM_ENTRY;
    @NotNull IElementType CLASS_INITIALIZER      = KtStubBasedElementTypes.CLASS_INITIALIZER;
    @NotNull IElementType SCRIPT_INITIALIZER     = KtStubBasedElementTypes.SCRIPT_INITIALIZER;
    @NotNull IElementType SECONDARY_CONSTRUCTOR  = KtStubBasedElementTypes.SECONDARY_CONSTRUCTOR;
    @NotNull IElementType PRIMARY_CONSTRUCTOR    = KtStubBasedElementTypes.PRIMARY_CONSTRUCTOR;
    @NotNull IElementType CONTEXT_RECEIVER       = KtStubBasedElementTypes.CONTEXT_RECEIVER;
    @NotNull IElementType CONTEXT_PARAMETER_LIST = KtStubBasedElementTypes.CONTEXT_PARAMETER_LIST;

    /**
     * @deprecated Use {@link #CONTEXT_PARAMETER_LIST} instead.
     * This constant has been renamed to align the name with the context parameters feature.
     */
    @Deprecated
    @NotNull IElementType CONTEXT_RECEIVER_LIST = CONTEXT_PARAMETER_LIST;

    @NotNull IElementType TYPE_PARAMETER_LIST                = KtStubBasedElementTypes.TYPE_PARAMETER_LIST;
    @NotNull IElementType TYPE_PARAMETER                     = KtStubBasedElementTypes.TYPE_PARAMETER;
    @NotNull IElementType SUPER_TYPE_LIST                    = KtStubBasedElementTypes.SUPER_TYPE_LIST;
    @NotNull IElementType DELEGATED_SUPER_TYPE_ENTRY         = KtStubBasedElementTypes.DELEGATED_SUPER_TYPE_ENTRY;
    @NotNull IElementType SUPER_TYPE_CALL_ENTRY              = KtStubBasedElementTypes.SUPER_TYPE_CALL_ENTRY;
    @NotNull IElementType SUPER_TYPE_ENTRY                   = KtStubBasedElementTypes.SUPER_TYPE_ENTRY;
    @NotNull IElementType PROPERTY_DELEGATE                  = new KtNodeType("PROPERTY_DELEGATE", KtPropertyDelegate::new);
    @NotNull IElementType CONSTRUCTOR_CALLEE                 = KtStubBasedElementTypes.CONSTRUCTOR_CALLEE;
    @NotNull IElementType VALUE_PARAMETER_LIST               = KtStubBasedElementTypes.VALUE_PARAMETER_LIST;
    @NotNull IElementType VALUE_PARAMETER                    = KtStubBasedElementTypes.VALUE_PARAMETER;

    @NotNull IElementType CLASS_BODY                         = KtStubBasedElementTypes.CLASS_BODY;
    @NotNull IElementType IMPORT_LIST                        = KtStubBasedElementTypes.IMPORT_LIST;
    @NotNull IElementType FILE_ANNOTATION_LIST               = KtStubBasedElementTypes.FILE_ANNOTATION_LIST;
    @NotNull IElementType IMPORT_DIRECTIVE                   = KtStubBasedElementTypes.IMPORT_DIRECTIVE;
    @NotNull IElementType IMPORT_ALIAS                       = KtStubBasedElementTypes.IMPORT_ALIAS;
    @NotNull IElementType MODIFIER_LIST                      = KtStubBasedElementTypes.MODIFIER_LIST;
    @NotNull IElementType ANNOTATION                         = KtStubBasedElementTypes.ANNOTATION;
    @NotNull IElementType ANNOTATION_ENTRY                   = KtStubBasedElementTypes.ANNOTATION_ENTRY;
    @NotNull IElementType ANNOTATION_TARGET                  = KtStubBasedElementTypes.ANNOTATION_TARGET;

    @NotNull IElementType TYPE_ARGUMENT_LIST                 = KtStubBasedElementTypes.TYPE_ARGUMENT_LIST;
    @NotNull IElementType VALUE_ARGUMENT_LIST                = KtStubBasedElementTypes.VALUE_ARGUMENT_LIST;
    @NotNull IElementType VALUE_ARGUMENT                     = KtStubBasedElementTypes.VALUE_ARGUMENT;
    @NotNull IElementType CONTRACT_EFFECT_LIST               = KtStubBasedElementTypes.CONTRACT_EFFECT_LIST;
    @NotNull IElementType CONTRACT_EFFECT                    = KtStubBasedElementTypes.CONTRACT_EFFECT;
    @NotNull IElementType LAMBDA_ARGUMENT                    = KtStubBasedElementTypes.LAMBDA_ARGUMENT;
    @NotNull IElementType VALUE_ARGUMENT_NAME                = KtStubBasedElementTypes.VALUE_ARGUMENT_NAME;
    @NotNull IElementType TYPE_REFERENCE                     = KtStubBasedElementTypes.TYPE_REFERENCE;

    @NotNull IElementType USER_TYPE                = KtStubBasedElementTypes.USER_TYPE;
    @NotNull IElementType DYNAMIC_TYPE             = KtStubBasedElementTypes.DYNAMIC_TYPE;
    @NotNull IElementType FUNCTION_TYPE            = KtStubBasedElementTypes.FUNCTION_TYPE;
    @NotNull IElementType FUNCTION_TYPE_RECEIVER   = KtStubBasedElementTypes.FUNCTION_TYPE_RECEIVER;
    @NotNull IElementType NULLABLE_TYPE            = KtStubBasedElementTypes.NULLABLE_TYPE;
    @NotNull IElementType INTERSECTION_TYPE        = KtStubBasedElementTypes.INTERSECTION_TYPE;
    @NotNull IElementType TYPE_PROJECTION          = KtStubBasedElementTypes.TYPE_PROJECTION;

    @NotNull IElementType PROPERTY_ACCESSOR       = KtStubBasedElementTypes.PROPERTY_ACCESSOR;
    @NotNull IElementType BACKING_FIELD           = KtStubBasedElementTypes.BACKING_FIELD;
    @NotNull IElementType INITIALIZER_LIST        = KtStubBasedElementTypes.INITIALIZER_LIST;
    @NotNull IElementType TYPE_CONSTRAINT_LIST    = KtStubBasedElementTypes.TYPE_CONSTRAINT_LIST;
    @NotNull IElementType TYPE_CONSTRAINT         = KtStubBasedElementTypes.TYPE_CONSTRAINT;

    @NotNull IElementType CONSTRUCTOR_DELEGATION_CALL = new KtNodeType.KtLeftBoundNodeType("CONSTRUCTOR_DELEGATION_CALL", KtConstructorDelegationCall::new);
    @NotNull IElementType CONSTRUCTOR_DELEGATION_REFERENCE = new KtNodeType.KtLeftBoundNodeType("CONSTRUCTOR_DELEGATION_REFERENCE", KtConstructorDelegationReferenceExpression::new);

    @NotNull IElementType NULL               = KtStubBasedElementTypes.NULL;
    @NotNull IElementType BOOLEAN_CONSTANT   = KtStubBasedElementTypes.BOOLEAN_CONSTANT;
    @NotNull IElementType FLOAT_CONSTANT     = KtStubBasedElementTypes.FLOAT_CONSTANT;
    @NotNull IElementType CHARACTER_CONSTANT = KtStubBasedElementTypes.CHARACTER_CONSTANT;
    @NotNull IElementType INTEGER_CONSTANT   = KtStubBasedElementTypes.INTEGER_CONSTANT;

    @NotNull IElementType STRING_TEMPLATE               = KtStubBasedElementTypes.STRING_TEMPLATE;
    @NotNull IElementType LONG_STRING_TEMPLATE_ENTRY    = KtStubBasedElementTypes.LONG_STRING_TEMPLATE_ENTRY;
    @NotNull IElementType SHORT_STRING_TEMPLATE_ENTRY   = KtStubBasedElementTypes.SHORT_STRING_TEMPLATE_ENTRY;
    @NotNull IElementType LITERAL_STRING_TEMPLATE_ENTRY = KtStubBasedElementTypes.LITERAL_STRING_TEMPLATE_ENTRY;
    @NotNull IElementType ESCAPE_STRING_TEMPLATE_ENTRY  = KtStubBasedElementTypes.ESCAPE_STRING_TEMPLATE_ENTRY;
    @NotNull IElementType STRING_INTERPOLATION_PREFIX   = KtStubBasedElementTypes.STRING_INTERPOLATION_PREFIX;

    @NotNull IElementType PARENTHESIZED             = new KtNodeType("PARENTHESIZED", KtParenthesizedExpression::new);
    @NotNull IElementType RETURN                    = new KtNodeType("RETURN", KtReturnExpression::new);
    @NotNull IElementType THROW                     = new KtNodeType("THROW", KtThrowExpression::new);
    @NotNull IElementType CONTINUE                  = new KtNodeType("CONTINUE", KtContinueExpression::new);
    @NotNull IElementType BREAK                     = new KtNodeType("BREAK", KtBreakExpression::new);
    @NotNull IElementType IF                        = new KtNodeType("IF", KtIfExpression::new);
    @NotNull IElementType CONDITION                 = new KtNodeType("CONDITION", KtContainerNode::new);
    @NotNull IElementType THEN                      = new KtNodeType("THEN", KtContainerNodeForControlStructureBody::new);
    @NotNull IElementType ELSE                      = new KtNodeType("ELSE", KtContainerNodeForControlStructureBody::new);
    @NotNull IElementType TRY                       = new KtNodeType("TRY", KtTryExpression::new);
    @NotNull IElementType CATCH                     = new KtNodeType("CATCH", KtCatchClause::new);
    @NotNull IElementType FINALLY                   = new KtNodeType("FINALLY", KtFinallySection::new);
    @NotNull IElementType FOR                       = new KtNodeType("FOR", KtForExpression::new);
    @NotNull IElementType WHILE                     = new KtNodeType("WHILE", KtWhileExpression::new);
    @NotNull IElementType DO_WHILE                  = new KtNodeType("DO_WHILE", KtDoWhileExpression::new);
    @NotNull IElementType LOOP_RANGE                = new KtNodeType("LOOP_RANGE", KtContainerNode::new);
    @NotNull IElementType BODY                      = new KtNodeType("BODY", KtContainerNodeForControlStructureBody::new);

    @NotNull IElementType BLOCK                     = KtStubBasedElementTypes.BLOCK;

    @NotNull IElementType LAMBDA_EXPRESSION         = KtStubBasedElementTypes.LAMBDA_EXPRESSION;

    @NotNull IElementType FUNCTION_LITERAL          = new KtNodeType("FUNCTION_LITERAL", KtFunctionLiteral::new);
    @NotNull IElementType ANNOTATED_EXPRESSION      = new KtNodeType("ANNOTATED_EXPRESSION", KtAnnotatedExpression::new);

    @NotNull IElementType REFERENCE_EXPRESSION     = KtStubBasedElementTypes.REFERENCE_EXPRESSION;
    @NotNull IElementType ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION = KtStubBasedElementTypes.ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION;
    @NotNull IElementType OPERATION_REFERENCE       = new KtNodeType("OPERATION_REFERENCE", KtOperationReferenceExpression::new);
    @NotNull IElementType LABEL                     = new KtNodeType("LABEL", KtLabelReferenceExpression::new);

    @NotNull IElementType LABEL_QUALIFIER           = new KtNodeType("LABEL_QUALIFIER", KtContainerNode::new);

    @NotNull IElementType THIS_EXPRESSION           = new KtNodeType("THIS_EXPRESSION", KtThisExpression::new);
    @NotNull IElementType SUPER_EXPRESSION          = new KtNodeType("SUPER_EXPRESSION", KtSuperExpression::new);
    @NotNull IElementType BINARY_EXPRESSION         = new KtNodeType("BINARY_EXPRESSION", KtBinaryExpression::new);
    @NotNull IElementType BINARY_WITH_TYPE          = new KtNodeType("BINARY_WITH_TYPE", KtBinaryExpressionWithTypeRHS::new);
    @NotNull IElementType IS_EXPRESSION             = new KtNodeType("IS_EXPRESSION", KtIsExpression::new);
    @NotNull IElementType PREFIX_EXPRESSION         = new KtNodeType("PREFIX_EXPRESSION", KtPrefixExpression::new);
    @NotNull IElementType POSTFIX_EXPRESSION        = new KtNodeType("POSTFIX_EXPRESSION", KtPostfixExpression::new);
    @NotNull IElementType LABELED_EXPRESSION        = new KtNodeType("LABELED_EXPRESSION", KtLabeledExpression::new);
    @NotNull IElementType CALL_EXPRESSION           = KtStubBasedElementTypes.CALL_EXPRESSION;
    @NotNull IElementType ARRAY_ACCESS_EXPRESSION   = new KtNodeType("ARRAY_ACCESS_EXPRESSION", KtArrayAccessExpression::new);
    @NotNull IElementType INDICES                   = new KtNodeType("INDICES", KtContainerNode::new);
    @NotNull IElementType DOT_QUALIFIED_EXPRESSION  = KtStubBasedElementTypes.DOT_QUALIFIED_EXPRESSION;
    @NotNull IElementType CALLABLE_REFERENCE_EXPRESSION = new KtNodeType("CALLABLE_REFERENCE_EXPRESSION", KtCallableReferenceExpression::new);
    @NotNull IElementType CLASS_LITERAL_EXPRESSION  = KtStubBasedElementTypes.CLASS_LITERAL_EXPRESSION;
    @NotNull IElementType SAFE_ACCESS_EXPRESSION    = new KtNodeType("SAFE_ACCESS_EXPRESSION", KtSafeQualifiedExpression::new);

    @NotNull IElementType OBJECT_LITERAL            = new KtNodeType("OBJECT_LITERAL", KtObjectLiteralExpression::new);

    @NotNull IElementType WHEN                      = new KtNodeType("WHEN", KtWhenExpression::new);
    @NotNull IElementType WHEN_ENTRY                = new KtNodeType("WHEN_ENTRY", KtWhenEntry::new);
    @NotNull IElementType WHEN_ENTRY_GUARD          = new KtNodeType("WHEN_ENTRY_GUARD", KtWhenEntryGuard::new);

    @NotNull IElementType WHEN_CONDITION_IN_RANGE   = new KtNodeType("WHEN_CONDITION_IN_RANGE", KtWhenConditionInRange::new);
    @NotNull IElementType WHEN_CONDITION_IS_PATTERN = new KtNodeType("WHEN_CONDITION_IS_PATTERN", KtWhenConditionIsPattern::new);
    @NotNull IElementType WHEN_CONDITION_EXPRESSION = new KtNodeType("WHEN_CONDITION_WITH_EXPRESSION", KtWhenConditionWithExpression::new);

    @NotNull IElementType COLLECTION_LITERAL_EXPRESSION = KtStubBasedElementTypes.COLLECTION_LITERAL_EXPRESSION;

    @NotNull IElementType PACKAGE_DIRECTIVE = KtStubBasedElementTypes.PACKAGE_DIRECTIVE;

    @NotNull IElementType SCRIPT = KtStubBasedElementTypes.SCRIPT;

    @NotNull IFileElementType TYPE_CODE_FRAGMENT = KtStubBasedElementTypes.TYPE_CODE_FRAGMENT;
    @NotNull IFileElementType EXPRESSION_CODE_FRAGMENT = KtStubBasedElementTypes.EXPRESSION_CODE_FRAGMENT;
    @NotNull IFileElementType BLOCK_CODE_FRAGMENT = KtStubBasedElementTypes.BLOCK_CODE_FRAGMENT;
}
