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
            return Type.INT_TYPE;
        }
        if (jetType.equals(standardLibrary.getLongType())) {
            return Type.LONG_TYPE;
        }
        if (jetType.equals(standardLibrary.getShortType())) {
            return Type.SHORT_TYPE;
        }
        if (jetType.equals(standardLibrary.getByteType())) {
            return Type.BYTE_TYPE;
        }
        if (jetType.equals(standardLibrary.getBooleanType())) {
            return Type.BOOLEAN_TYPE;
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

        if (descriptor instanceof ClassDescriptor) {
            return Type.getObjectType(getFQName(descriptor).replace('.', '/'));
        }

        throw new UnsupportedOperationException("Unknown type " + jetType);
    }

    private static String getFQName(DeclarationDescriptor descriptor) {
        DeclarationDescriptor container = descriptor.getContainingDeclaration();
        if (container != null && !(container instanceof ModuleDescriptor)) {
            return getFQName(container) + "." + descriptor.getName();
        }

        return descriptor.getName();
    }
}
