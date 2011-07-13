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
    private static final IntrinsicMethod INV = new Inv();

    private final JetStandardLibrary myStdLib;
    private final Map<DeclarationDescriptor, IntrinsicMethod> myMethods = new HashMap<DeclarationDescriptor, IntrinsicMethod>();

    public IntrinsicMethods(JetStandardLibrary stdlib) {
        myStdLib = stdlib;
        List<String> primitiveCastMethods = ImmutableList.of("dbl", "flt", "lng", "int", "chr", "sht", "byt");
        for (String method : primitiveCastMethods) {
            declareIntrinsicProperty("Number", method, NUMBER_CAST);
        }
        declareIntrinsicProperty("Array", "size", ARRAY_SIZE);

        List<String> primitiveNumberTypes = ImmutableList.of("Boolean", "Byte", "Char", "Short", "Int", "Float", "Long", "Double");
        for (String primitiveNumberType : primitiveNumberTypes) {
            declareIntrinsicFunction(primitiveNumberType, "minus", 0, UNARY_MINUS);
            declareIntrinsicFunction(primitiveNumberType, "inv", 0, INV);
        }
    }

    private void declareIntrinsicProperty(String className, String methodName, IntrinsicMethod implementation) {
        final JetScope numberScope = getClassMemberScope(className);
        final VariableDescriptor variable = numberScope.getVariable(methodName);
        myMethods.put(variable.getOriginal(), implementation);
    }

    private void declareIntrinsicFunction(String className, String functionName, int arity, IntrinsicMethod implementation) {
        JetScope memberScope = getClassMemberScope(className);
        final FunctionGroup minus = memberScope.getFunctionGroup(functionName);
        for (FunctionDescriptor descriptor : minus.getFunctionDescriptors()) {
            if (descriptor.getValueParameters().size() == arity) {
                myMethods.put(descriptor, implementation);
            }
        }
    }

    private JetScope getClassMemberScope(String className) {
        final ClassDescriptor descriptor = (ClassDescriptor) myStdLib.getLibraryScope().getClassifier(className);
        final List<TypeParameterDescriptor> typeParameterDescriptors = descriptor.getTypeConstructor().getParameters();
        List<TypeProjection> typeParameters = new ArrayList<TypeProjection>();
        for (TypeParameterDescriptor typeParameterDescriptor : typeParameterDescriptors) {
            typeParameters.add(new TypeProjection(JetStandardClasses.getAnyType()));
        }
        return descriptor.getMemberScope(typeParameters);
    }

    public IntrinsicMethod getIntrinsic(DeclarationDescriptor descriptor) {
        return myMethods.get(descriptor.getOriginal());
    }

}
