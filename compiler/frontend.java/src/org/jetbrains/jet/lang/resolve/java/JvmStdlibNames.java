package org.jetbrains.jet.lang.resolve.java;

import org.objectweb.asm.Type;

/**
 * @author Stepan Koltsov
 */
public class JvmStdlibNames {

    public static final JvmClassName JET_VALUE_PARAMETER = new JvmClassName("jet.runtime.typeinfo.JetValueParameter");

    public static final String JET_VALUE_PARAMETER_NAME_FIELD = "name";
    public static final String JET_VALUE_PARAMETER_HAS_DEFAULT_VALUE_FIELD = "hasDefaultValue";
    public static final String JET_VALUE_PARAMETER_NULLABLE_FIELD = "nullable";
    public static final String JET_VALUE_PARAMETER_TYPE_FIELD = "type";
    public static final String JET_VALUE_PARAMETER_RECEIVER_FIELD = "receiver";


    public static final JvmClassName JET_TYPE_PARAMETER = new JvmClassName("jet.runtime.typeinfo.JetTypeParameter");

    public static final String JET_TYPE_PARAMETER_NAME_FIELD = "name";


    public static final JvmClassName JET_METHOD = new JvmClassName("jet.runtime.typeinfo.JetMethod");

    public static final String JET_METHOD_NULLABLE_RETURN_TYPE_FIELD = "nullableReturnType";
    public static final String JET_METHOD_RETURN_TYPE_FIELD = "returnType";
    public static final String JET_METHOD_TYPE_PARAMETERS_FIELD = "typeParameters";

    public static final JvmClassName JET_PROPERTY = new JvmClassName("jet.runtime.typeinfo.JetProperty");
    

    public static final JvmClassName JET_CLASS = new JvmClassName("jet.runtime.typeinfo.JetClass");
    
    public static final String JET_CLASS_SIGNATURE = "signature";
    

    public static final JvmClassName JET_OBJECT = new JvmClassName("jet.JetObject");
    
    public static final String JET_OBJECT_GET_TYPEINFO_METHOD = "getTypeInfo";


    private JvmStdlibNames() {
    }
}
