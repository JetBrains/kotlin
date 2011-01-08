/*
 * @author max
 */
package org.jetbrains.jet;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.jet.lang.JetLanguage;
import org.jetbrains.jet.lang.psi.*;

public interface JetNodeTypes {
    IFileElementType JET_FILE = new IFileElementType(JetLanguage.INSTANCE);

    JetNodeType NAMESPACE  = new JetNodeType("NAMESPACE", JetNamespace.class);
    JetNodeType CLASS      = new JetNodeType("CLASS", JetClass.class);
    JetNodeType PROPERTY   = new JetNodeType("PROPERTY", JetProperty.class);
    JetNodeType FUN        = new JetNodeType("FUN", JetFunction.class);
    JetNodeType EXTENSION  = new JetNodeType("EXTENSION", JetExtension.class);
    JetNodeType TYPEDEF    = new JetNodeType("TYPEDEF", JetTypedef.class);
    JetNodeType DECOMPOSER = new JetNodeType("DECOMPOSER", JetDecomposer.class);

    JetNodeType CLASS_OBJECT          = new JetNodeType("CLASS_OBJECT", JetClassObject.class);
    JetNodeType CONSTRUCTOR           = new JetNodeType("CONSTRUCTOR", JetConstructor.class);
    JetNodeType ENUM_ENTRY            = new JetNodeType("ENUM_ENTRY", JetEnumEntry.class);
    JetNodeType ANONYMOUS_INITIALIZER = new JetNodeType("ANONYMOUS_INITIALIZER");

    JetNodeType TYPE_PARAMETER_LIST                 = new JetNodeType("TYPE_PARAMETER_LIST", JetTypeParameterList.class);
    JetNodeType TYPE_PARAMETER                      = new JetNodeType("TYPE_PARAMETER", JetTypeParameter.class);
    JetNodeType DELEGATION_SPECIFIER_LIST           = new JetNodeType("DELEGATION_SPECIFIER_LIST", JetDelegationSpecifierList.class);
    JetNodeType DELEGATOR_BY                        = new JetNodeType("DELEGATOR_BY", JetDelegatorByExpressionSpecifier.class);
    JetNodeType DELEGATOR_SUPER_CALL                = new JetNodeType("DELEGATOR_SUPER_CALL", JetDelegatorToSuperCall.class);
    JetNodeType DELEGATOR_SUPER_CLASS               = new JetNodeType("DELEGATOR_SUPER_CLASS", JetDelegatorToSuperClass.class);
    JetNodeType VALUE_PARAMETER_LIST                = new JetNodeType("VALUE_PARAMETER_LIST", JetParameterList.class);
    JetNodeType VALUE_PARAMETER                     = new JetNodeType("VALUE_PARAMETER", JetParameter.class);
    // TODO: Not sure if we really need separate PSI nodes for the class parameters?
    JetNodeType PRIMARY_CONSTRUCTOR_PARAMETERS_LIST = new JetNodeType("PRIMARY_CONSTRUCTOR_PARAMETERS_LIST", JetParameterList.class);
    JetNodeType PRIMARY_CONSTRUCTOR_PARAMETER       = new JetNodeType("PRIMARY_CONSTRUCTOR_PARAMETER", JetParameter.class);
    JetNodeType NAMED_ARGUMENT                      = new JetNodeType("NAMED_ARGUMENT");
    JetNodeType CLASS_BODY                          = new JetNodeType("CLASS_BODY", JetClassBody.class);
    JetNodeType IMPORT_DIRECTIVE                    = new JetNodeType("IMPORT_DIRECTIVE", JetImportDirective.class);
    JetNodeType IMPORTED                            = new JetNodeType("IMPORTED");
    JetNodeType NAMESPACE_BODY                      = new JetNodeType("NAMESPACE_BODY", JetNamespaceBody.class);
    JetNodeType MODIFIER_LIST                       = new JetNodeType("MODIFIER_LIST", JetModifierList.class);
    JetNodeType ATTRIBUTE_ANNOTATION                = new JetNodeType("ATTRIBUTE_ANNOTATION", JetAttributeAnnotation.class);
    JetNodeType ATTRIBUTE                           = new JetNodeType("ATTRIBUTE", JetAttribute.class);
    JetNodeType USER_TYPE                           = new JetNodeType("USER_TYPE");
    JetNodeType TYPE_ARGUMENT_LIST                  = new JetNodeType("TYPE_ARGUMENT_LIST", JetTypeArgumentList.class);
    JetNodeType VALUE_ARGUMENT_LIST                 = new JetNodeType("VALUE_ARGUMENT_LIST", JetArgumentList.class);
    JetNodeType VALUE_ARGUMENT                      = new JetNodeType("VALUE_ARGUMENT", JetArgument.class);
    JetNodeType TYPE_REFERENCE                      = new JetNodeType("TYPE_REFERENCE", JetTypeReference.class);
    JetNodeType LABELED_TUPLE_ENTRY                 = new JetNodeType("LABELED_TUPLE_ENTRY");
    JetNodeType TUPLE_TYPE                          = new JetNodeType("TUPLE_TYPE");
    JetNodeType FUNCTION_TYPE                       = new JetNodeType("FUNCTION_TYPE");
    JetNodeType SELF_TYPE                           = new JetNodeType("SELF_TYPE");
    JetNodeType DECOMPOSER_PROPERTY_LIST            = new JetNodeType("DECOMPOSER_PROPERTY_LIST");
    // TODO: review
    JetNodeType RECEIVER_TYPE_ATTRIBUTES            = new JetNodeType("RECEIVER_TYPE_ATTRIBUTES");
    JetNodeType PROPERTY_ACCESSOR                   = new JetNodeType("PROPERTY_ACCESSOR");
    JetNodeType INITIALIZER_LIST                    = new JetNodeType("INITIALIZER_LIST");
    JetNodeType THIS_CALL                           = new JetNodeType("THIS_CALL");
    JetNodeType TYPE_CONSTRAINT_LIST                = new JetNodeType("TYPE_CONSTRAINT_LIST");
    JetNodeType TYPE_CONSTRAINT                     = new JetNodeType("TYPE_CONSTRAINT");

    // TODO: Not sure if we need separate NT for each kind of constants
    JetNodeType NULL               = new JetNodeType("NULL", JetConstantExpression.class);
    JetNodeType BOOLEAN_CONSTANT   = new JetNodeType("BOOLEAN_CONSTANT", JetConstantExpression.class);
    JetNodeType FLOAT_CONSTANT     = new JetNodeType("FLOAT_CONSTANT", JetConstantExpression.class);
    JetNodeType CHARACTER_CONSTANT = new JetNodeType("CHARACTER_CONSTANT", JetConstantExpression.class);
    JetNodeType STRING_CONSTANT    = new JetNodeType("STRING_CONSTANT", JetConstantExpression.class);
    JetNodeType INTEGER_CONSTANT   = new JetNodeType("INTEGER_CONSTANT", JetConstantExpression.class);
    JetNodeType LONG_CONSTANT      = new JetNodeType("LONG_CONSTANT", JetConstantExpression.class);

    JetNodeType TUPLE                     = new JetNodeType("TUPLE", JetTupleExpression.class);
    JetNodeType TYPEOF                    = new JetNodeType("TYPEOF", JetTypeofExpression.class);
    JetNodeType NEW                       = new JetNodeType("NEW", JetNewExpression.class);
    JetNodeType RETURN                    = new JetNodeType("RETURN", JetReturnExpression.class);
    JetNodeType THROW                     = new JetNodeType("THROW", JetThrowExpression.class);
    JetNodeType CONTINUE                  = new JetNodeType("CONTINUE", JetContinueExpression.class);
    JetNodeType BREAK                     = new JetNodeType("BREAK", JetBreakExpression.class);
    JetNodeType IF                        = new JetNodeType("IF", JetIfExpression.class);
    JetNodeType CONDITION                 = new JetNodeType("CONDITION");   //TODO: discuss, this can be omitted
    JetNodeType THEN                      = new JetNodeType("THEN");        //TODO: discuss, this can be omitted
    JetNodeType ELSE                      = new JetNodeType("ELSE");        //TODO: discuss, this can be omitted
    JetNodeType TRY                       = new JetNodeType("TRY", JetTryExpression.class);
    JetNodeType CATCH                     = new JetNodeType("CATCH", JetCatchSection.class);
    JetNodeType FINALLY                   = new JetNodeType("FINALLY", JetFinallySection.class);
    JetNodeType FOR                       = new JetNodeType("FOR", JetForExpression.class);
    JetNodeType WHILE                     = new JetNodeType("WHILE", JetWhileExpression.class);
    JetNodeType DO_WHILE                  = new JetNodeType("DO_WHILE", JetDoWhileExpression.class);
    JetNodeType LOOP_PARAMETER            = new JetNodeType("LOOP_PARAMETER", JetParameter.class); // TODO: Do we need separate type?
    JetNodeType LOOP_RANGE                = new JetNodeType("LOOP_RANGE");  //TODO: discuss, this can be omitted
    JetNodeType BODY                      = new JetNodeType("BODY");        //TODO: discuss, this can be omitted
    JetNodeType RECEIVER_TYPE             = new JetNodeType("RECEIVER_TYPE");
    JetNodeType BLOCK                     = new JetNodeType("BLOCK", JetBlockExpression.class);
    JetNodeType FUNCTION_LITERAL          = new JetNodeType("FUNCTION_LITERAL", JetFunctionLiteralExpression.class);
    JetNodeType ANNOTATED_EXPRESSION      = new JetNodeType("ANNOTATED_EXPRESSION", JetAnnotatedExpression.class);
    JetNodeType REFERENCE_EXPRESSION      = new JetNodeType("REFERENCE_EXPRESSION", JetReferenceExpression.class);
    JetNodeType THIS_EXPRESSION           = new JetNodeType("THIS_EXPRESSION", JetThisExpression.class);
    JetNodeType BINARY_EXPRESSION         = new JetNodeType("BINARY_EXPRESSION", JetBinaryExpression.class);
    JetNodeType PREFIX_EXPRESSION         = new JetNodeType("PREFIX_EXPRESSION", JetPrefixExpression.class);
    JetNodeType POSTFIX_EXPRESSION        = new JetNodeType("POSTFIX_EXPRESSION", JetPostfixExpression.class);
    JetNodeType CALL_EXPRESSION           = new JetNodeType("CALL_EXPRESSION", JetCallExpression.class);
    JetNodeType ARRAY_ACCESS_EXPRESSION   = new JetNodeType("ARRAY_ACCESS_EXPRESSION", JetArrayAccessExpression.class);
    JetNodeType INDICES                   = new JetNodeType("INDICES"); //TODO: discuss, this can be omitted
    JetNodeType DOT_QIALIFIED_EXPRESSION  = new JetNodeType("DOT_QIALIFIED_EXPRESSION", JetDotQualifiedExpression.class);
    JetNodeType HASH_QIALIFIED_EXPRESSION = new JetNodeType("HASH_QIALIFIED_EXPRESSION", JetHashQualifiedExpression.class);
    JetNodeType SAFE_ACCESS_EXPRESSION    = new JetNodeType("SAFE_ACCESS_EXPRESSION", JetSafeQualifiedExpression.class);
    JetNodeType MATCH_ENTRY               = new JetNodeType("MATCH_ENTRY");
    JetNodeType PATTERN                   = new JetNodeType("PATTERN");
    JetNodeType TUPLE_PATTERN             = new JetNodeType("TUPLE_PATTERN");
    JetNodeType OBJECT_LITERAL            = new JetNodeType("OBJECT_LITERAL", JetObjectLiteralExpression.class);
    JetNodeType ROOT_NAMESPACE            = new JetNodeType("ROOT_NAMESPACE", JetRootNamespaceExpression.class);

    IElementType NAMESPACE_NAME = new JetNodeType("NAMESPACE_NAME");

    TokenSet DECLARATIONS = TokenSet.create(
            NAMESPACE, CLASS, PROPERTY, FUN, EXTENSION,
            TYPEDEF, DECOMPOSER, CLASS_OBJECT, CONSTRUCTOR, ENUM_ENTRY, VALUE_PARAMETER);
}
