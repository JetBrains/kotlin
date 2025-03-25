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
package org.jetbrains.kotlin

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

object KtNodeTypes {
    @JvmField
    val KT_FILE: IFileElementType = IFileElementType(KotlinLanguage.INSTANCE)

    val CLASS: IElementType = KtStubElementTypes.CLASS
    val FUN: IElementType = KtStubElementTypes.FUNCTION

    @JvmField
    val PROPERTY: IElementType = KtStubElementTypes.PROPERTY

    @JvmField
    val DESTRUCTURING_DECLARATION: IElementType = KtNodeType("DESTRUCTURING_DECLARATION", KtDestructuringDeclaration::class.java)

    @JvmField
    val DESTRUCTURING_DECLARATION_ENTRY: IElementType =
        KtNodeType("DESTRUCTURING_DECLARATION_ENTRY", KtDestructuringDeclarationEntry::class.java)

    @JvmField
    val OBJECT_DECLARATION: IElementType = KtStubElementTypes.OBJECT_DECLARATION
    val TYPEALIAS: IElementType = KtStubElementTypes.TYPEALIAS

    val ENUM_ENTRY: IElementType = KtStubElementTypes.ENUM_ENTRY
    val CLASS_INITIALIZER: IElementType = KtStubElementTypes.CLASS_INITIALIZER
    val SCRIPT_INITIALIZER: IElementType = KtNodeType("SCRIPT_INITIALIZER", KtScriptInitializer::class.java)
    val SECONDARY_CONSTRUCTOR: IElementType = KtStubElementTypes.SECONDARY_CONSTRUCTOR
    val PRIMARY_CONSTRUCTOR: IElementType = KtStubElementTypes.PRIMARY_CONSTRUCTOR
    val CONTEXT_RECEIVER: IElementType = KtStubElementTypes.CONTEXT_RECEIVER
    val CONTEXT_RECEIVER_LIST: IElementType = KtStubElementTypes.CONTEXT_RECEIVER_LIST

    @JvmField
    val TYPE_PARAMETER_LIST: IElementType = KtStubElementTypes.TYPE_PARAMETER_LIST
    val TYPE_PARAMETER: IElementType = KtStubElementTypes.TYPE_PARAMETER
    val SUPER_TYPE_LIST: IElementType = KtStubElementTypes.SUPER_TYPE_LIST
    val DELEGATED_SUPER_TYPE_ENTRY: IElementType = KtStubElementTypes.DELEGATED_SUPER_TYPE_ENTRY
    val SUPER_TYPE_CALL_ENTRY: IElementType = KtStubElementTypes.SUPER_TYPE_CALL_ENTRY
    val SUPER_TYPE_ENTRY: IElementType = KtStubElementTypes.SUPER_TYPE_ENTRY

    @JvmField
    val PROPERTY_DELEGATE: IElementType = KtNodeType("PROPERTY_DELEGATE", KtPropertyDelegate::class.java)
    val CONSTRUCTOR_CALLEE: IElementType = KtStubElementTypes.CONSTRUCTOR_CALLEE

    @JvmField
    val VALUE_PARAMETER_LIST: IElementType = KtStubElementTypes.VALUE_PARAMETER_LIST

    @JvmField
    val VALUE_PARAMETER: IElementType = KtStubElementTypes.VALUE_PARAMETER

    val CLASS_BODY: IElementType = KtStubElementTypes.CLASS_BODY
    val IMPORT_LIST: IElementType = KtStubElementTypes.IMPORT_LIST
    val FILE_ANNOTATION_LIST: IElementType = KtStubElementTypes.FILE_ANNOTATION_LIST
    val IMPORT_DIRECTIVE: IElementType = KtStubElementTypes.IMPORT_DIRECTIVE
    val IMPORT_ALIAS: IElementType = KtStubElementTypes.IMPORT_ALIAS

    @JvmField
    val MODIFIER_LIST: IElementType = KtStubElementTypes.MODIFIER_LIST

    @JvmField
    val ANNOTATION: IElementType = KtStubElementTypes.ANNOTATION
    val ANNOTATION_ENTRY: IElementType = KtStubElementTypes.ANNOTATION_ENTRY
    val ANNOTATION_TARGET: IElementType = KtStubElementTypes.ANNOTATION_TARGET

    @JvmField
    val TYPE_ARGUMENT_LIST: IElementType = KtStubElementTypes.TYPE_ARGUMENT_LIST

    @JvmField
    val VALUE_ARGUMENT_LIST: IElementType = KtStubElementTypes.VALUE_ARGUMENT_LIST
    val VALUE_ARGUMENT: IElementType = KtStubElementTypes.VALUE_ARGUMENT
    val CONTRACT_EFFECT_LIST: IElementType = KtStubElementTypes.CONTRACT_EFFECT_LIST
    val CONTRACT_EFFECT: IElementType = KtStubElementTypes.CONTRACT_EFFECT

    @JvmField
    val LAMBDA_ARGUMENT: IElementType = KtStubElementTypes.LAMBDA_ARGUMENT
    val VALUE_ARGUMENT_NAME: IElementType = KtStubElementTypes.VALUE_ARGUMENT_NAME

    @JvmField
    val TYPE_REFERENCE: IElementType = KtStubElementTypes.TYPE_REFERENCE

    val USER_TYPE: IElementType = KtStubElementTypes.USER_TYPE
    val DYNAMIC_TYPE: IElementType = KtStubElementTypes.DYNAMIC_TYPE
    val FUNCTION_TYPE: IElementType = KtStubElementTypes.FUNCTION_TYPE
    val FUNCTION_TYPE_RECEIVER: IElementType = KtStubElementTypes.FUNCTION_TYPE_RECEIVER
    val NULLABLE_TYPE: IElementType = KtStubElementTypes.NULLABLE_TYPE
    val INTERSECTION_TYPE: IElementType = KtStubElementTypes.INTERSECTION_TYPE
    val TYPE_PROJECTION: IElementType = KtStubElementTypes.TYPE_PROJECTION

    val PROPERTY_ACCESSOR: IElementType = KtStubElementTypes.PROPERTY_ACCESSOR
    val BACKING_FIELD: IElementType = KtStubElementTypes.BACKING_FIELD
    val INITIALIZER_LIST: IElementType = KtStubElementTypes.INITIALIZER_LIST

    @JvmField
    val TYPE_CONSTRAINT_LIST: IElementType = KtStubElementTypes.TYPE_CONSTRAINT_LIST
    val TYPE_CONSTRAINT: IElementType = KtStubElementTypes.TYPE_CONSTRAINT

    val CONSTRUCTOR_DELEGATION_CALL: IElementType =
        KtNodeType.KtLeftBoundNodeType("CONSTRUCTOR_DELEGATION_CALL", KtConstructorDelegationCall::class.java)
    val CONSTRUCTOR_DELEGATION_REFERENCE: IElementType =
        KtNodeType.KtLeftBoundNodeType("CONSTRUCTOR_DELEGATION_REFERENCE", KtConstructorDelegationReferenceExpression::class.java)

    @JvmField
    val NULL: IElementType = KtStubElementTypes.NULL

    @JvmField
    val BOOLEAN_CONSTANT: IElementType = KtStubElementTypes.BOOLEAN_CONSTANT

    @JvmField
    val FLOAT_CONSTANT: IElementType = KtStubElementTypes.FLOAT_CONSTANT

    @JvmField
    val CHARACTER_CONSTANT: IElementType = KtStubElementTypes.CHARACTER_CONSTANT

    @JvmField
    val INTEGER_CONSTANT: IElementType = KtStubElementTypes.INTEGER_CONSTANT

    val STRING_TEMPLATE: IElementType = KtStubElementTypes.STRING_TEMPLATE
    val LONG_STRING_TEMPLATE_ENTRY: IElementType = KtStubElementTypes.LONG_STRING_TEMPLATE_ENTRY
    val SHORT_STRING_TEMPLATE_ENTRY: IElementType = KtStubElementTypes.SHORT_STRING_TEMPLATE_ENTRY
    @JvmField
    val LITERAL_STRING_TEMPLATE_ENTRY: IElementType = KtStubElementTypes.LITERAL_STRING_TEMPLATE_ENTRY
    val ESCAPE_STRING_TEMPLATE_ENTRY: IElementType = KtStubElementTypes.ESCAPE_STRING_TEMPLATE_ENTRY

    val PARENTHESIZED: IElementType = KtNodeType("PARENTHESIZED", KtParenthesizedExpression::class.java)
    val RETURN: IElementType = KtNodeType("RETURN", KtReturnExpression::class.java)
    val THROW: IElementType = KtNodeType("THROW", KtThrowExpression::class.java)
    val CONTINUE: IElementType = KtNodeType("CONTINUE", KtContinueExpression::class.java)
    val BREAK: IElementType = KtNodeType("BREAK", KtBreakExpression::class.java)
    val IF: IElementType = KtNodeType("IF", KtIfExpression::class.java)

    @JvmField
    val CONDITION: IElementType = KtNodeType("CONDITION", KtContainerNode::class.java)

    @JvmField
    val THEN: IElementType = KtNodeType("THEN", KtContainerNodeForControlStructureBody::class.java)

    @JvmField
    val ELSE: IElementType = KtNodeType("ELSE", KtContainerNodeForControlStructureBody::class.java)
    val TRY: IElementType = KtNodeType("TRY", KtTryExpression::class.java)

    @JvmField
    val CATCH: IElementType = KtNodeType("CATCH", KtCatchClause::class.java)

    @JvmField
    val FINALLY: IElementType = KtNodeType("FINALLY", KtFinallySection::class.java)
    val FOR: IElementType = KtNodeType("FOR", KtForExpression::class.java)
    val WHILE: IElementType = KtNodeType("WHILE", KtWhileExpression::class.java)
    val DO_WHILE: IElementType = KtNodeType("DO_WHILE", KtDoWhileExpression::class.java)

    @JvmField
    val LOOP_RANGE: IElementType = KtNodeType("LOOP_RANGE", KtContainerNode::class.java)

    @JvmField
    val BODY: IElementType = KtNodeType("BODY", KtContainerNodeForControlStructureBody::class.java)

    @JvmField
    val BLOCK: IElementType = BlockExpressionElementType()

    @JvmField
    val LAMBDA_EXPRESSION: IElementType = LambdaExpressionElementType()

    @JvmField
    val FUNCTION_LITERAL: IElementType = KtNodeType("FUNCTION_LITERAL", KtFunctionLiteral::class.java)
    val ANNOTATED_EXPRESSION: IElementType = KtNodeType("ANNOTATED_EXPRESSION", KtAnnotatedExpression::class.java)

    @JvmField
    val REFERENCE_EXPRESSION: IElementType = KtStubElementTypes.REFERENCE_EXPRESSION
    val ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION: IElementType = KtStubElementTypes.ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION

    @JvmField
    val OPERATION_REFERENCE: IElementType = KtNodeType("OPERATION_REFERENCE", KtOperationReferenceExpression::class.java)
    val LABEL: IElementType = KtNodeType("LABEL", KtLabelReferenceExpression::class.java)

    @JvmField
    val LABEL_QUALIFIER: IElementType = KtNodeType("LABEL_QUALIFIER", KtContainerNode::class.java)

    val THIS_EXPRESSION: IElementType = KtNodeType("THIS_EXPRESSION", KtThisExpression::class.java)
    val SUPER_EXPRESSION: IElementType = KtNodeType("SUPER_EXPRESSION", KtSuperExpression::class.java)
    val BINARY_EXPRESSION: IElementType = KtNodeType("BINARY_EXPRESSION", KtBinaryExpression::class.java)
    val BINARY_WITH_TYPE: IElementType = KtNodeType("BINARY_WITH_TYPE", KtBinaryExpressionWithTypeRHS::class.java)
    val IS_EXPRESSION: IElementType = KtNodeType("IS_EXPRESSION", KtIsExpression::class.java)
    val PREFIX_EXPRESSION: IElementType = KtNodeType("PREFIX_EXPRESSION", KtPrefixExpression::class.java)
    val POSTFIX_EXPRESSION: IElementType = KtNodeType("POSTFIX_EXPRESSION", KtPostfixExpression::class.java)
    val LABELED_EXPRESSION: IElementType = KtNodeType("LABELED_EXPRESSION", KtLabeledExpression::class.java)
    val CALL_EXPRESSION: IElementType = KtNodeType("CALL_EXPRESSION", KtCallExpression::class.java)
    val ARRAY_ACCESS_EXPRESSION: IElementType = KtNodeType("ARRAY_ACCESS_EXPRESSION", KtArrayAccessExpression::class.java)

    @JvmField
    val INDICES: IElementType = KtNodeType("INDICES", KtContainerNode::class.java)

    @JvmField
    val DOT_QUALIFIED_EXPRESSION: IElementType = KtStubElementTypes.DOT_QUALIFIED_EXPRESSION
    val CALLABLE_REFERENCE_EXPRESSION: IElementType =
        KtNodeType("CALLABLE_REFERENCE_EXPRESSION", KtCallableReferenceExpression::class.java)
    val CLASS_LITERAL_EXPRESSION: IElementType = KtStubElementTypes.CLASS_LITERAL_EXPRESSION

    @JvmField
    val SAFE_ACCESS_EXPRESSION: IElementType = KtNodeType("SAFE_ACCESS_EXPRESSION", KtSafeQualifiedExpression::class.java)

    val OBJECT_LITERAL: IElementType = KtNodeType("OBJECT_LITERAL", KtObjectLiteralExpression::class.java)

    val WHEN: IElementType = KtNodeType("WHEN", KtWhenExpression::class.java)

    @JvmField
    val WHEN_ENTRY: IElementType = KtNodeType("WHEN_ENTRY", KtWhenEntry::class.java)
    val WHEN_ENTRY_GUARD: IElementType = KtNodeType("WHEN_ENTRY_GUARD", KtWhenEntryGuard::class.java)

    val WHEN_CONDITION_IN_RANGE: IElementType = KtNodeType("WHEN_CONDITION_IN_RANGE", KtWhenConditionInRange::class.java)
    val WHEN_CONDITION_IS_PATTERN: IElementType = KtNodeType("WHEN_CONDITION_IS_PATTERN", KtWhenConditionIsPattern::class.java)

    @JvmField
    val WHEN_CONDITION_EXPRESSION: IElementType =
        KtNodeType("WHEN_CONDITION_WITH_EXPRESSION", KtWhenConditionWithExpression::class.java)

    val COLLECTION_LITERAL_EXPRESSION: IElementType = KtStubElementTypes.COLLECTION_LITERAL_EXPRESSION

    val PACKAGE_DIRECTIVE: IElementType = KtStubElementTypes.PACKAGE_DIRECTIVE

    val SCRIPT: IElementType = KtStubElementTypes.SCRIPT

    val TYPE_CODE_FRAGMENT: IFileElementType = KtStubElementTypes.TYPE_CODE_FRAGMENT

    @JvmField
    val EXPRESSION_CODE_FRAGMENT: IFileElementType = KtStubElementTypes.EXPRESSION_CODE_FRAGMENT

    @JvmField
    val BLOCK_CODE_FRAGMENT: IFileElementType = KtStubElementTypes.BLOCK_CODE_FRAGMENT
}
