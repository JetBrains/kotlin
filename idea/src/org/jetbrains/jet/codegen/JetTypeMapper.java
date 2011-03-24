package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.types.*;
import org.objectweb.asm.Type;

/**
 * @author yole
 */
public class JetTypeMapper {
    private final JetStandardLibrary standardLibrary;

    public JetTypeMapper(JetStandardLibrary standardLibrary) {
        this.standardLibrary = standardLibrary;
    }

    public Type mapType(final JetType jetType) {
        if (jetType.equals(JetStandardClasses.getUnitType())) {
            return Type.VOID_TYPE;
        }
        if (jetType.equals(standardLibrary.getIntType())) {
            return Type.getType(Integer.class);
        }
        if (jetType.equals(standardLibrary.getLongType())) {
            return Type.getType(Long.class);
        }
        if (jetType.equals(standardLibrary.getStringType())) {
            return Type.getType(String.class);
        }

        DeclarationDescriptor descriptor = jetType.getConstructor().getDeclarationDescriptor();
        if (standardLibrary.getArray().equals(descriptor)) {
            if (jetType.getArguments().size() != 1) {
                throw new UnsupportedOperationException("arrays must have one type argument");
            }
            TypeProjection memberType = jetType.getArguments().get(0);
            Type elementType = mapType(memberType.getType());
            return Type.getType("[" + elementType.getDescriptor());
        }
        if (JetStandardClasses.getAny().equals(descriptor)) {
            return Type.getType(Object.class);
        }
        throw new UnsupportedOperationException("Unknown type " + jetType);
    }
}
