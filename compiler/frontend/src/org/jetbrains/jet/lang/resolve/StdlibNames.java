package org.jetbrains.jet.lang.resolve;

import org.objectweb.asm.Type;

/**
 * @author Stepan Koltsov
 */
public class StdlibNames {
    
    public static final Type JET_PARAMETER_TYPE = Type.getObjectType("jet/typeinfo/JetParameter");
    public static final String JET_PARAMETER_CLASS = "jet.typeinfo.JetParameter";
    public static final String JET_PARAMETER_DESCRIPTOR = "Ljet/typeinfo/JetParameter;";
    
    public static final String JET_PARAMETER_NAME_FIELD = "name";
    public static final String JET_PARAMETER_HAS_DEFAULT_FIELD = "hasDefault";
    public static final String JET_PARAMETER_NULLABLE_FIELD = "nullable";
    
    
    public static final Type JET_TYPE_PARAMETER_TYPE = Type.getObjectType("jet/typeinfo/JetTypeParameter");
    public static final String JET_TYPE_PARAMETER_CLASS = "jet.typeinfo.JetTypeParameter";
    public static final String JET_TYPE_PARAMETER_DESCRIPTOR = "Ljet/typeinfo/JetTypeParameter;";
    
    public static final String JET_TYPE_PARAMETER_NAME_FIELD = "name";
    
    
    public static final Type JET_METHOD_TYPE = Type.getObjectType("jet/typeinfo/JetMethod");
    public static final String JET_METHOD_CLASS = "jet.typeinfo.JetMethod";
    public static final String JET_METHOD_DESCRIPTOR = "Ljet/typeinfo/JetMethod;";
    
    public static final String JET_METHOD_NULLABLE_RETURN_TYPE_FIELD = "nullableReturnType";
    
}
