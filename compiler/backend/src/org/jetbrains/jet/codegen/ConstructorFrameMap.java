package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.objectweb.asm.Type;

import java.util.Collections;
import java.util.List;

/**
 * @author yole
 * @author alex.tkachman
 */
public class ConstructorFrameMap extends FrameMap {
    private int myOuterThisIndex = -1;
    private int myTypeInfoIndex  = -1;

    public ConstructorFrameMap(CallableMethod callableMethod, @Nullable ConstructorDescriptor descriptor, ClassDescriptor classDescriptor, OwnerKind kind) {
        enterTemp(); // this
        if (descriptor != null) {
            if (CodegenUtil.hasThis0(classDescriptor)) {
                myOuterThisIndex = enterTemp();   // outer class instance
            }
        }

        if (classDescriptor != null) {
            if (CodegenUtil.requireTypeInfoConstructorArg(classDescriptor.getDefaultType())) {
                myTypeInfoIndex = enterTemp();
            }
        }

        List<Type> explicitArgTypes = callableMethod.getValueParameterTypes();

        List<ValueParameterDescriptor> paramDescrs = descriptor != null
                ? descriptor.getValueParameters()
                : Collections.<ValueParameterDescriptor>emptyList();
        for (int i = 0; i < paramDescrs.size(); i++) {
            ValueParameterDescriptor parameter = paramDescrs.get(i);
            enter(parameter, explicitArgTypes.get(i).getSize());
        }
    }

    public int getOuterThisIndex() {
        return myOuterThisIndex;
    }

    public int getTypeInfoIndex() {
        return myTypeInfoIndex;
    }
}
