package org.jetbrains.jet.lang.types;

/**
 * @author abreslav
 */
public class TypeUtils {
    public static Type makeNullable(Type type) {
        if (type.isNullable()) {
            return type;
        }
        return new TypeImpl(type.getAttributes(), type.getConstructor(), true, type.getArguments(), type.getMemberDomain());
    }
}
