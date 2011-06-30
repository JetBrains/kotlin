package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import java.util.Collections;
import java.util.List;

/**
 * @author yole
 */
public class ConstructorFrameMap extends FrameMap {
    private int myOuterThisIndex = -1;
    private int myDelegateThisIndex = -1;
    private int myFirstTypeParameter = -1;
    private int myTypeParameterCount = 0;
    private int myFirstExplicitArgument = 0;
    private Type[] myExplicitArgTypes;

    public ConstructorFrameMap(JetTypeMapper typeMapper, @Nullable ConstructorDescriptor descriptor, OwnerKind kind) {
        Type[] argTypes = new Type[0];
        if (descriptor != null) {
            Method method = typeMapper.mapConstructorSignature(descriptor, kind);
            argTypes = method.getArgumentTypes();
        }

        enterTemp(); // this
        ClassDescriptor classDescriptor = null;
        if (descriptor != null) {
            classDescriptor = descriptor.getContainingDeclaration();
            if (classDescriptor.getContainingDeclaration() instanceof ClassDescriptor) {
                myOuterThisIndex = enterTemp();   // outer class instance
                myFirstExplicitArgument++;
            }
        }
        if (kind == OwnerKind.DELEGATING_IMPLEMENTATION) {
            myDelegateThisIndex = enterTemp();   // $this
            myFirstExplicitArgument++;
        }

        if (myFirstExplicitArgument > 0) {
            int explicitArgCount = argTypes.length - myFirstExplicitArgument;
            myExplicitArgTypes = new Type[explicitArgCount];
            System.arraycopy(argTypes, myFirstExplicitArgument, myExplicitArgTypes, 0, explicitArgCount);
        }
        else {
            myExplicitArgTypes = argTypes;
        }

        List<ValueParameterDescriptor> paramDescrs = descriptor != null
                ? descriptor.getValueParameters()
                : Collections.<ValueParameterDescriptor>emptyList();
        for (int i = 0; i < paramDescrs.size(); i++) {
            ValueParameterDescriptor parameter = paramDescrs.get(i);
            enter(parameter, myExplicitArgTypes[i].getSize());
        }


        if (classDescriptor != null) {
            myTypeParameterCount = classDescriptor.getTypeConstructor().getParameters().size();
            if (kind == OwnerKind.IMPLEMENTATION) {
                if (myTypeParameterCount > 0) {
                    myFirstTypeParameter = enterTemp();
                    for (int i = 1; i < myTypeParameterCount; i++) {
                        enterTemp();
                    }
                }
            }
        }
    }

    public int getOuterThisIndex() {
        return myOuterThisIndex;
    }

    public int getDelegateThisIndex() {
        return myDelegateThisIndex;
    }

    public int getFirstTypeParameter() {
        return myFirstTypeParameter;
    }

    public int getTypeParameterCount() {
        return myTypeParameterCount;
    }

    public Type[] getExplicitArgTypes() {
        return myExplicitArgTypes;
    }
}
