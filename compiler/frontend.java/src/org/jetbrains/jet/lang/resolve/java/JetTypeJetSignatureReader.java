package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.JetTypeImpl;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.rt.signature.JetSignatureExceptionsAdapter;
import org.jetbrains.jet.rt.signature.JetSignatureVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Stepan Koltsov
 */
public abstract class JetTypeJetSignatureReader extends JetSignatureExceptionsAdapter {
    
    private final JavaSemanticServices javaSemanticServices;
    private final JavaDescriptorResolver javaDescriptorResolver;
    private final JetStandardLibrary jetStandardLibrary;
    private final TypeVariableResolver typeVariableResolver;

    public JetTypeJetSignatureReader(JavaSemanticServices javaSemanticServices, JetStandardLibrary jetStandardLibrary, TypeVariableResolver typeVariableResolver) {
        this.javaSemanticServices = javaSemanticServices;
        this.javaDescriptorResolver = javaSemanticServices.getDescriptorResolver();
        this.jetStandardLibrary = jetStandardLibrary;
        this.typeVariableResolver = typeVariableResolver;
    }
    
    
    private JetType getPrimitiveType(char descriptor, boolean nullable) {
        if (!nullable) {
            switch (descriptor) {
                case 'Z':
                    return jetStandardLibrary.getBooleanType();
                case 'C':
                    return jetStandardLibrary.getCharType();
                case 'B':
                    return jetStandardLibrary.getByteType();
                case 'S':
                    return jetStandardLibrary.getShortType();
                case 'I':
                    return jetStandardLibrary.getIntType();
                case 'F':
                    return jetStandardLibrary.getFloatType();
                case 'J':
                    return jetStandardLibrary.getLongType();
                case 'D':
                    return jetStandardLibrary.getDoubleType();
                case 'V':
                    return JetStandardClasses.getUnitType();
            }
        } else {
            switch (descriptor) {
                case 'Z':
                    return jetStandardLibrary.getNullableBooleanType();
                case 'C':
                    return jetStandardLibrary.getNullableCharType();
                case 'B':
                    return jetStandardLibrary.getNullableByteType();
                case 'S':
                    return jetStandardLibrary.getNullableShortType();
                case 'I':
                    return jetStandardLibrary.getNullableIntType();
                case 'F':
                    return jetStandardLibrary.getNullableFloatType();
                case 'J':
                    return jetStandardLibrary.getNullableLongType();
                case 'D':
                    return jetStandardLibrary.getNullableDoubleType();
                case 'V':
                    throw new IllegalStateException("incorrect signature: nullable void");
            }
        }
        throw new IllegalStateException("incorrect signature");
    }

    @Override
    public void visitBaseType(char descriptor, boolean nullable) {
        done(getPrimitiveType(descriptor, nullable));
    }


    private ClassDescriptor classDescriptor;
    private boolean nullable;
    private List<TypeProjection> typeArguments;

    @Override
    public void visitClassType(String name, boolean nullable) {
        String ourName = name.replace('/', '.');
        
        this.classDescriptor = this.javaSemanticServices.getTypeTransformer().getPrimitiveWrappersClassDescriptorMap().get(ourName);

        if (this.classDescriptor == null) {
            this.classDescriptor = javaDescriptorResolver.resolveClass(ourName);
        }

        if (this.classDescriptor == null) {
            throw new IllegalStateException("class not found by name: " + ourName); // TODO: wrong exception
        }
        this.nullable = nullable;
        this.typeArguments = new ArrayList<TypeProjection>();
    }

    private static Variance parseVariance(char wildcard) {
        switch (wildcard) {
            case '=': return Variance.INVARIANT;
            case '+': return Variance.OUT_VARIANCE;
            case '-': return Variance.IN_VARIANCE;
            default: throw new IllegalStateException();
        }
    }

    @Override
    public JetSignatureVisitor visitTypeArgument(final char wildcard) {
        return new JetTypeJetSignatureReader(javaSemanticServices, jetStandardLibrary, typeVariableResolver) {

            @Override
            protected void done(@NotNull JetType jetType) {
                typeArguments.add(new TypeProjection(parseVariance(wildcard), jetType));
            }
        };
    }

    @Override
    public JetSignatureVisitor visitArrayType(final boolean nullable) {
        return new JetTypeJetSignatureReader(javaSemanticServices, jetStandardLibrary, typeVariableResolver) {
            @Override
            public void visitBaseType(char descriptor, boolean nullable) {
                JetType primitiveType = getPrimitiveType(descriptor, nullable);
                JetType arrayType;
                if (!nullable) {
                    arrayType = jetStandardLibrary.getPrimitiveArrayType(primitiveType);
                } else {
                    arrayType = TypeUtils.makeNullableAsSpecified(jetStandardLibrary.getArrayType(primitiveType), nullable);
                }
                JetTypeJetSignatureReader.this.done(arrayType);
            }

            @Override
            protected void done(@NotNull JetType jetType) {
                JetType arrayType = TypeUtils.makeNullableAsSpecified(jetStandardLibrary.getArrayType(jetType), nullable);
                JetTypeJetSignatureReader.this.done(arrayType);
            }
        };
    }

    @Override
    public void visitTypeVariable(String name, boolean nullable) {
        JetType r = TypeUtils.makeNullableAsSpecified(typeVariableResolver.getTypeVariable(name).getDefaultType(), nullable);
        done(r);
    }

    @Override
    public void visitEnd() {
        JetType jetType = new JetTypeImpl(
                Collections.<AnnotationDescriptor>emptyList(),
                classDescriptor.getTypeConstructor(),
                nullable,
                typeArguments,
                ErrorUtils.getErrorScope());
        done(jetType);
    }
    
    protected abstract void done(@NotNull JetType jetType);
}
