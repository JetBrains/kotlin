package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collections;

/**
 * @author abreslav
 */
public class VariableAsFunctionDescriptor extends FunctionDescriptorImpl {
    public static VariableAsFunctionDescriptor create(@NotNull VariableDescriptor variableDescriptor) {
        JetType outType = variableDescriptor.getOutType();
        assert outType != null;
        assert JetStandardClasses.isFunctionType(outType);
        VariableAsFunctionDescriptor result = new VariableAsFunctionDescriptor(variableDescriptor);
        result.initialize(JetStandardClasses.getReceiverType(outType), Collections.<TypeParameterDescriptor>emptyList(), JetStandardClasses.getValueParameters(result, outType), JetStandardClasses.getReturnType(outType));
        return result;
    }

    private final VariableDescriptor variableDescriptor;

    private VariableAsFunctionDescriptor(VariableDescriptor variableDescriptor) {
        super(variableDescriptor.getContainingDeclaration(), Collections.<Annotation>emptyList(), variableDescriptor.getName());
//        super(variableDescriptor.getContainingDeclaration(), Collections.<Annotation>emptyList(), variableDescriptor.getName());
        this.variableDescriptor = variableDescriptor;
    }

    public VariableDescriptor getVariableDescriptor() {
        return variableDescriptor;
    }
}
