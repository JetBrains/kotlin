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
    JetNodeType RECEIVER_TYPE_ATTRIBUTES = new JetNodeType("RECEIVER_TYPE_ATTRIBUTES");
    JetNodeType PROPERTY_GETTER = new JetNodeType("PROPERTY_GETTER");
    JetNodeType CONSTRUCTOR = new JetNodeType("CONSTRUCTOR");
    JetNodeType INITIALIZER_LIST = new JetNodeType("INITIALIZER_LIST");
    JetNodeType THIS_CALL = new JetNodeType("THIS_CALL");
    JetNodeType BLOCK = new JetNodeType("BLOCK");
    JetNodeType CLASS_OBJECT = new JetNodeType("CLASS_OBJECT");

    IElementType NAMESPACE_NAME = new JetNodeType("NAMESPACE_NAME");
}
