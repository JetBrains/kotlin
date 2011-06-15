/*
 * @author max
 */
package org.jetbrains.jet;

import com.intellij.psi.tree.IFileElementType;
import org.jetbrains.jet.plugin.JetLanguage;
import org.jetbrains.jet.lang.psi.*;

public interface JetNodeTypes {
    IFileElementType JET_FILE = new IFileElementType(JetLanguage.INSTANCE);

    JetNodeType NAMESPACE  = new JetNodeType("NAMESPACE", JetNamespace.class);
    JetNodeType CLASS      = new JetNodeType("CLASS", JetClass.class);
    JetNodeType PROPERTY   = new JetNodeType("PROPERTY", JetProperty.class);
    JetNodeType FUN        = new JetNodeType("FUN", JetFunction.class);
    JetNodeType EXTENSION  = new JetNodeType("EXTENSION", JetExtension.class);
    JetNodeType TYPEDEF    = new JetNodeType("TYPEDEF", JetTypedef.class);
    JetNodeType OBJECT_DECLARATION = new JetNodeType("OBJECT_DECLARATION", JetObjectDeclaration.class);
    JetNodeType OBJECT_DECLARATION_NAME = new JetNodeType("OBJECT_DECLARATION_NAME", JetObjectDeclarationName.class);

    JetNodeType CLASS_OBJECT          = new JetNodeType("CLASS_OBJECT", JetClassObject.class);
    JetNodeType CONSTRUCTOR           = new JetNodeType("CONSTRUCTOR", JetConstructor.class);
    JetNodeType ENUM_ENTRY            = new JetNodeType("ENUM_ENTRY", JetEnumEntry.class);
    JetNodeType ANONYMOUS_INITIALIZER = new JetNodeType("ANONYMOUS_INITIALIZER", JetClassInitializer.class);

    JetNodeType TYPE_PARAMETER_LIST                 = new JetNodeType("TYPE_PARAMETER_LIST", JetTypeParameterList.class);
    JetNodeType TYPE_PARAMETER                      = new JetNodeType("TYPE_PARAMETER", JetTypeParameter.class);
    JetNodeType DELEGATION_SPECIFIER_LIST           = new JetNodeType("DELEGATION_SPECIFIER_LIST", JetDelegationSpecifierList.class);
    JetNodeType DELEGATOR_BY                        = new JetNodeType("DELEGATOR_BY", JetDelegatorByExpressionSpecifier.class);
    JetNodeType DELEGATOR_SUPER_CALL                = new JetNodeType("DELEGATOR_SUPER_CALL", JetDelegatorToSuperCall.class);
    JetNodeType DELEGATOR_SUPER_CLASS               = new JetNodeType("DELEGATOR_SUPER_CLASS", JetDelegatorToSuperClass.class);
    JetNodeType VALUE_PARAMETER_LIST                = new JetNodeType("VALUE_PARAMETER_LIST", JetParameterList.class);
    JetNodeType VALUE_PARAMETER                     = new JetNodeType("VALUE_PARAMETER", JetParameter.class);

    JetNodeType CLASS_BODY                          = new JetNodeType("CLASS_BODY", JetClassBody.class);
    JetNodeType IMPORT_DIRECTIVE                    = new JetNodeType("IMPORT_DIRECTIVE", JetImportDirective.class);
    JetNodeType NAMESPACE_BODY                      = new JetNodeType("NAMESPACE_BODY", JetNamespaceBody.class);
    JetNodeType MODIFIER_LIST                       = new JetNodeType("MODIFIER_LIST", JetModifierList.class);
    JetNodeType PRIMARY_CONSTRUCTOR_MODIFIER_LIST   = new JetNodeType("PRIMARY_CONSTRUCTOR_MODIFIER_LIST", JetModifierList.class);
    JetNodeType ATTRIBUTE_ANNOTATION                = new JetNodeType("ATTRIBUTE_ANNOTATION", JetAttributeAnnotation.class);
    JetNodeType ATTRIBUTE                           = new JetNodeType("ATTRIBUTE", JetAttribute.class);
    JetNodeType TYPE_ARGUMENT_LIST                  = new JetNodeType("TYPE_ARGUMENT_LIST", JetTypeArgumentList.class);
    JetNodeType VALUE_ARGUMENT_LIST                 = new JetNodeType("VALUE_ARGUMENT_LIST", JetArgumentList.class);
    JetNodeType VALUE_ARGUMENT                      = new JetNodeType("VALUE_ARGUMENT", JetArgument.class);
    JetNodeType TYPE_REFERENCE                      = new JetNodeType("TYPE_REFERENCE", JetTypeReference.class);
    JetNodeType LABELED_TUPLE_ENTRY                 = new JetNodeType("LABELED_TUPLE_ENTRY");
    JetNodeType LABELED_TUPLE_TYPE_ENTRY            = new JetNodeType("LABELED_TUPLE_TYPE_ENTRY");

    JetNodeType USER_TYPE     = new JetNodeType("USER_TYPE", JetUserType.class);
    JetNodeType TUPLE_TYPE    = new JetNodeType("TUPLE_TYPE", JetTupleType.class);
    JetNodeType FUNCTION_TYPE = new JetNodeType("FUNCTION_TYPE", JetFunctionType.class);
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
    JetNodeType STRING_CONSTANT    = new JetNodeType("STRING_CONSTANT", JetConstantExpression.class);
    JetNodeType INTEGER_CONSTANT   = new JetNodeType("INTEGER_CONSTANT", JetConstantExpression.class);
    JetNodeType LONG_CONSTANT      = new JetNodeType("LONG_CONSTANT", JetConstantExpression.class);

    JetNodeType TUPLE                     = new JetNodeType("TUPLE", JetTupleExpression.class);
    JetNodeType PARENTHESIZED             = new JetNodeType("PARENTHESIZED", JetParenthesizedExpression.class);
    JetNodeType TYPEOF                    = new JetNodeType("TYPEOF", JetTypeofExpression.class);
//    JetNodeType NEW                       = new JetNodeType("NEW", JetNewExpression.class);
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
    JetNodeType FUNCTION_LITERAL          = new JetNodeType("FUNCTION_LITERAL", JetFunctionLiteralExpression.class);
    JetNodeType ANNOTATED_EXPRESSION      = new JetNodeType("ANNOTATED_EXPRESSION", JetAnnotatedExpression.class);

    JetNodeType REFERENCE_EXPRESSION      = new JetNodeType("REFERENCE_EXPRESSION", JetSimpleNameExpression.class);
    JetNodeType OPERATION_REFERENCE       = new JetNodeType("OPERATION_REFERENCE", JetSimpleNameExpression.class);
    JetNodeType LABEL_REFERENCE           = new JetNodeType("LABEL_REFERENCE", JetSimpleNameExpression.class);

    JetNodeType LABEL_QUALIFIER           = new JetNodeType("LABEL_QUALIFIER", JetContainerNode.class);

    JetNodeType THIS_EXPRESSION           = new JetNodeType("THIS_EXPRESSION", JetThisExpression.class);
    JetNodeType BINARY_EXPRESSION         = new JetNodeType("BINARY_EXPRESSION", JetBinaryExpression.class);
    JetNodeType BINARY_WITH_TYPE          = new JetNodeType("BINARY_WITH_TYPE", JetBinaryExpressionWithTypeRHS.class);
    JetNodeType BINARY_WITH_PATTERN       = new JetNodeType("BINARY_WITH_PATTERN", JetIsExpression.class); // TODO:
    JetNodeType PREFIX_EXPRESSION         = new JetNodeType("PREFIX_EXPRESSION", JetPrefixExpression.class);
    JetNodeType POSTFIX_EXPRESSION        = new JetNodeType("POSTFIX_EXPRESSION", JetPostfixExpression.class);
    JetNodeType CALL_EXPRESSION           = new JetNodeType("CALL_EXPRESSION", JetCallExpression.class);
    JetNodeType ARRAY_ACCESS_EXPRESSION   = new JetNodeType("ARRAY_ACCESS_EXPRESSION", JetArrayAccessExpression.class);
    JetNodeType INDICES                   = new JetNodeType("INDICES", JetContainerNode.class);
    JetNodeType DOT_QUALIFIED_EXPRESSION = new JetNodeType("DOT_QUALIFIED_EXPRESSION", JetDotQualifiedExpression.class);
    JetNodeType HASH_QUALIFIED_EXPRESSION = new JetNodeType("HASH_QUALIFIED_EXPRESSION", JetHashQualifiedExpression.class);
    JetNodeType SAFE_ACCESS_EXPRESSION    = new JetNodeType("SAFE_ACCESS_EXPRESSION", JetSafeQualifiedExpression.class);
    JetNodeType PREDICATE_EXPRESSION       = new JetNodeType("PREDICATE_EXPRESSION", JetPredicateExpression.class);

    JetNodeType OBJECT_LITERAL            = new JetNodeType("OBJECT_LITERAL", JetObjectLiteralExpression.class);
    JetNodeType ROOT_NAMESPACE            = new JetNodeType("ROOT_NAMESPACE", JetRootNamespaceExpression.class);

    JetNodeType EXPRESSION_PATTERN        = new JetNodeType("EXPRESSION_PATTERN", JetExpressionPattern.class);
    JetNodeType TYPE_PATTERN              = new JetNodeType("TYPE_PATTERN", JetTypePattern.class);
    JetNodeType WILDCARD_PATTERN          = new JetNodeType("WILDCARD_PATTERN", JetWildcardPattern.class);
    JetNodeType BINDING_PATTERN           = new JetNodeType("BINDING_PATTERN", JetBindingPattern.class);
    JetNodeType TUPLE_PATTERN             = new JetNodeType("TUPLE_PATTERN", JetTuplePattern.class);
    JetNodeType TUPLE_PATTERN_ENTRY       = new JetNodeType("TUPLE_PATTERN_ENTRY", JetTuplePatternEntry.class);
    JetNodeType DECOMPOSER_PATTERN        = new JetNodeType("DECOMPOSER_PATTERN", JetDecomposerPattern.class);
    JetNodeType DECOMPOSER_ARGUMENT_LIST  = new JetNodeType("DECOMPOSER_ARGUMENT_LIST", JetTuplePattern.class);
    JetNodeType DECOMPOSER_ARGUMENT       = TUPLE_PATTERN_ENTRY;//new JetNodeType("DECOMPOSER_ARGUMENT", JetTuplePatternEntry.class);

    JetNodeType WHEN                      = new JetNodeType("WHEN", JetWhenExpression.class);
    JetNodeType WHEN_ENTRY                = new JetNodeType("WHEN_ENTRY", JetWhenEntry.class);

    JetNodeType WHEN_CONDITION_IN_RANGE   = new JetNodeType("WHEN_CONDITION_IN_RANGE", JetWhenConditionInRange.class);
    JetNodeType WHEN_CONDITION_IS_PATTERN = new JetNodeType("WHEN_CONDITION_IS_PATTERN", JetWhenConditionIsPattern.class);
    JetNodeType WHEN_CONDITION_CALL       = new JetNodeType("WHEN_CONDITION_CALL", JetWhenConditionCall.class);
    JetNodeType WHEN_CONDITION_EXPRESSION = new JetNodeType("WHEN_CONDITION_EXPRESSION", JetWhenConditionWithExpression.class);

    JetNodeType NAMESPACE_NAME = new JetNodeType("NAMESPACE_NAME", JetContainerNode.class);
}
