/*
 * @author max
 */
package org.jetbrains.jet;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import org.jetbrains.jet.lang.JetLanguage;

public interface JetNodeTypes {
    IFileElementType JET_FILE = new IFileElementType(JetLanguage.INSTANCE);

    JetNodeType NAMESPACE = new JetNodeType("NAMESPACE");
    JetNodeType CLASS = new JetNodeType("CLASS");
    JetNodeType PROPERTY = new JetNodeType("PROPERTY");
    JetNodeType FUN = new JetNodeType("FUN");
    JetNodeType EXTENSION = new JetNodeType("EXTENSION");
    JetNodeType TYPEDEF = new JetNodeType("TYPEDEF");
    JetNodeType DECOMPOSER = new JetNodeType("DECOMPOSER");
    JetNodeType TYPE_PARAMETER_LIST = new JetNodeType("TYPE_PARAMETER_LIST");
    JetNodeType TYPE_PARAMETER = new JetNodeType("TYPE_PARAMETER");
    JetNodeType PRIMARY_CONSTRUCTOR_PARAMETERS_LIST = new JetNodeType("PRIMARY_CONSTRUCTOR_PARAMETERS_LIST");
    JetNodeType PRIMARY_CONSTRUCTOR_PARAMETER = new JetNodeType("PRIMARY_CONSTRUCTOR_PARAMETER");
    JetNodeType DELEGATION_SPECIFIER_LIST = new JetNodeType("DELEGATION_SPECIFIER_LIST");
    JetNodeType DELEGATION_SPECIFIER = new JetNodeType("DELEGATION_SPECIFIER");
    JetNodeType DELEGATOR_BY = new JetNodeType("DELEGATOR_BY");
    JetNodeType DELEGATOR_SUPER_CALL = new JetNodeType("DELEGATOR_SUPER_CALL");
    JetNodeType DELEGATOR_SUPER_CLASS = new JetNodeType("DELEGATOR_SUPER_CLASS");
    JetNodeType VALUE_PARAMETER_LIST = new JetNodeType("VALUE_PARAMETER_LIST");
    JetNodeType NAMED_ARGUMENT = new JetNodeType("NAMED_ARGUMENT");
    JetNodeType CLASS_BODY = new JetNodeType("CLASS_BODY");
    JetNodeType IMPORT_DIRECTIVE = new JetNodeType("IMPORT_DIRECTIVE");
    JetNodeType IMPORTED = new JetNodeType("IMPORTED");
    JetNodeType NAMESPACE_BODY = new JetNodeType("NAMESPACE_BODY");
    JetNodeType MODIFIER_LIST = new JetNodeType("MODIFIER_LIST");
    JetNodeType ATTRIBUTE_ANNOTATION = new JetNodeType("ATTRIBUTE_ANNOTATION");
    JetNodeType ATTRIBUTE = new JetNodeType("ATTRIBUTE");
    JetNodeType USER_TYPE = new JetNodeType("USER_TYPE");
    JetNodeType TYPE_ARGUMENT_LIST = new JetNodeType("TYPE_ARGUMENT_LIST");
    JetNodeType VALUE_ARGUMENT_LIST = new JetNodeType("VALUE_ARGUMENT_LIST");
    JetNodeType VALUE_ARGUMENT = new JetNodeType("VALUE_ARGUMENT");
    JetNodeType TYPE_REFERENCE = new JetNodeType("TYPE_REFERENCE");
    JetNodeType LABELED_TUPLE_ENTRY = new JetNodeType("LABELED_TUPLE_ENTRY");
    JetNodeType TUPLE_TYPE = new JetNodeType("TUPLE_TYPE");
    JetNodeType VALUE_PARAMETER = new JetNodeType("VALUE_PARAMETER");
    JetNodeType FUNCTION_TYPE = new JetNodeType("FUNCTION_TYPE");
    JetNodeType DECOMPOSER_PROPERTY_LIST = new JetNodeType("DECOMPOSER_PROPERTY_LIST");
    // TODO: review
    JetNodeType RECEIVER_TYPE_ATTRIBUTES = new JetNodeType("RECEIVER_TYPE_ATTRIBUTES");
    JetNodeType PROPERTY_ACCESSOR = new JetNodeType("PROPERTY_ACCESSOR");
    JetNodeType CONSTRUCTOR = new JetNodeType("CONSTRUCTOR");
    JetNodeType INITIALIZER_LIST = new JetNodeType("INITIALIZER_LIST");
    JetNodeType THIS_CALL = new JetNodeType("THIS_CALL");
    JetNodeType BLOCK = new JetNodeType("BLOCK");
    JetNodeType CLASS_OBJECT = new JetNodeType("CLASS_OBJECT");
    JetNodeType TYPE_CONSTRAINT_LIST = new JetNodeType("TYPE_CONSTRAINT_LIST");
    JetNodeType TYPE_CONSTRAINT = new JetNodeType("TYPE_CONSTRAINT");
    JetNodeType ENUM_ENTRY = new JetNodeType("ENUM_ENTRY");
    JetNodeType NULL = new JetNodeType("NULL");
    JetNodeType BOOLEAN_CONSTANT = new JetNodeType("BOOLEAN_CONSTANT");
    JetNodeType FLOAT_CONSTANT = new JetNodeType("FLOAT_CONSTANT");
    JetNodeType CHARACTER_CONSTANT = new JetNodeType("CHARACTER_CONSTANT");
    JetNodeType STRING_CONSTANT = new JetNodeType("STRING_CONSTANT");
    JetNodeType INTEGER_CONSTANT = new JetNodeType("INTEGER_CONSTANT");
    JetNodeType SUPERTYE_QUALIFIER = new JetNodeType("SUPERTYE_QUALIFIER");
    JetNodeType LONG_CONSTANT = new JetNodeType("LONG_CONSTANT");
    JetNodeType TUPLE = new JetNodeType("TUPLE");
    JetNodeType TYPEOF = new JetNodeType("TYPEOF");
    JetNodeType NEW = new JetNodeType("NEW");
    JetNodeType RETURN = new JetNodeType("RETURN");
    JetNodeType THROW = new JetNodeType("THROW");
    JetNodeType CONTINUE = new JetNodeType("CONTINUE");
    JetNodeType BREAK = new JetNodeType("BREAK");
    JetNodeType IF = new JetNodeType("IF");
    JetNodeType CONDITION = new JetNodeType("CONDITION");
    JetNodeType THEN = new JetNodeType("THEN");
    JetNodeType ELSE = new JetNodeType("ELSE");
    JetNodeType TRY = new JetNodeType("TRY");
    JetNodeType CATCH = new JetNodeType("CATCH");
    JetNodeType FINALLY = new JetNodeType("FINALLY");
    JetNodeType FOR = new JetNodeType("FOR");
    JetNodeType WHILE = new JetNodeType("WHILE");
    JetNodeType DO_WHILE = new JetNodeType("DO_WHILE");
    JetNodeType LOOP_PARAMETER = new JetNodeType("LOOP_PARAMETER");
    JetNodeType LOOP_RANGE = new JetNodeType("LOOP_RANGE");
    JetNodeType BODY = new JetNodeType("BODY");
    JetNodeType RECEIVER_TYPE = new JetNodeType("RECEIVER_TYPE");
    JetNodeType FUNCTION_LITERAL = new JetNodeType("FUNCTION_LITERAL");
    JetNodeType ANNOTATED_EXPRESSION = new JetNodeType("ANNOTATED_EXPRESSION");
//    JetNodeType SIMPLE_NAME = new JetNodeType("SIMPLE_NAME");
    JetNodeType BINARY_EXPRESSION = new JetNodeType("BINARY_EXPRESSION");
    JetNodeType PREFIX_EXPRESSION = new JetNodeType("PREFIX_EXPRESSION");
    JetNodeType POSTFIX_EXPRESSION = new JetNodeType("POSTFIX_EXPRESSION");
    JetNodeType CALL_EXPRESSION = new JetNodeType("CALL_EXPRESSION");
    JetNodeType ARRAY_ACCESS_EXPRESSION = new JetNodeType("ARRAY_ACCESS_EXPRESSION");
    JetNodeType INDICES = new JetNodeType("INDICES");
    JetNodeType DOT_QIALIFIED_EXPRESSION = new JetNodeType("DOT_QIALIFIED_EXPRESSION");
    JetNodeType HASH_QIALIFIED_EXPRESSION = new JetNodeType("HASH_QIALIFIED_EXPRESSION");
    JetNodeType SAFE_ACCESS_EXPRESSION = new JetNodeType("SAFE_ACCESS_EXPRESSION");
    JetNodeType MATCH_ENTRY = new JetNodeType("MATCH_ENTRY");
    JetNodeType PATTERN = new JetNodeType("PATTERN");
    JetNodeType TUPLE_PATTERN = new JetNodeType("TUPLE_PATTERN");

    IElementType NAMESPACE_NAME = new JetNodeType("NAMESPACE_NAME");
}
