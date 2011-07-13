package org.jetbrains.jet.codegen.intrinsics;

import com.google.common.collect.ImmutableList;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.JetScope;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lang.types.TypeProjection;

import java.util.*;

/**
 * @author yole
 */
public class IntrinsicMethods {
    private static final IntrinsicMethod UNARY_MINUS = new UnaryMinus();
    private static final IntrinsicMethod NUMBER_CAST = new NumberCast();
    private static final IntrinsicMethod ARRAY_SIZE = new ArraySize();

    private final Map<DeclarationDescriptor, IntrinsicMethod> myMethods = new HashMap<DeclarationDescriptor, IntrinsicMethod>();

    public IntrinsicMethods(JetStandardLibrary stdlib) {
        List<String> primitiveCastMethods = ImmutableList.of("dbl", "flt", "lng", "int", "chr", "sht", "byt");
        for (String method : primitiveCastMethods) {
            declareIntrinsicProperty(stdlib, "Number", method, NUMBER_CAST);
        }
        declareIntrinsicProperty(stdlib, "Array", "size", ARRAY_SIZE);

        List<ClassDescriptor> primitiveNumberTypes = ImmutableList.of(
                stdlib.getBoolean(),
                stdlib.getByte(),
                stdlib.getChar(),
                stdlib.getShort(),
                stdlib.getInt(),
                stdlib.getFloat(),
                stdlib.getLong(),
                stdlib.getDouble()
        );

        for (ClassDescriptor primitiveNumberType : primitiveNumberTypes) {
            final JetScope memberScope = primitiveNumberType.getMemberScope(Collections.<TypeProjection>emptyList());

            final FunctionGroup minus = memberScope.getFunctionGroup("minus");
            for (FunctionDescriptor descriptor : minus.getFunctionDescriptors()) {
                if (descriptor.getValueParameters().isEmpty()) {
                    myMethods.put(descriptor, UNARY_MINUS);
                }
            }


        }
    }

    private void declareIntrinsicProperty(JetStandardLibrary stdlib, String className, String methodName, IntrinsicMethod implementation) {
        final ClassDescriptor descriptor = (ClassDescriptor) stdlib.getLibraryScope().getClassifier(className);
        final List<TypeParameterDescriptor> typeParameterDescriptors = descriptor.getTypeConstructor().getParameters();
        List<TypeProjection> typeParameters = new ArrayList<TypeProjection>();
        for (TypeParameterDescriptor typeParameterDescriptor : typeParameterDescriptors) {
            typeParameters.add(new TypeProjection(JetStandardClasses.getAnyType()));
        }
        final JetScope numberScope = descriptor.getMemberScope(typeParameters);
        final VariableDescriptor variable = numberScope.getVariable(methodName);
        myMethods.put(variable.getOriginal(), implementation);
    }

    public IntrinsicMethod getIntrinsic(DeclarationDescriptor descriptor) {
        return myMethods.get(descriptor.getOriginal());
    }

}
