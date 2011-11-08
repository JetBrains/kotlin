package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.descriptors.ClassDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author alex.tkachman
 */
public class ObjectOrClosureCodegen {
    protected boolean captureThis;

    public final GenerationState state;
    protected final ExpressionCodegen exprContext;
    protected final CodegenContext context;
    protected ClassBuilder cv = null;
    public String name = null;
    protected Map<DeclarationDescriptor, EnclosedValueDescriptor> closure = new LinkedHashMap<DeclarationDescriptor, EnclosedValueDescriptor>();

    public ObjectOrClosureCodegen(ExpressionCodegen exprContext, CodegenContext context, GenerationState state) {
        this.exprContext = exprContext;
        this.context = context;
        this.state = state;
    }

    public StackValue lookupInContext(DeclarationDescriptor d) {
        if (d instanceof VariableDescriptor) {
            VariableDescriptor vd = (VariableDescriptor) d;

            EnclosedValueDescriptor answer = closure.get(vd);
            if (answer != null) return answer.getInnerValue();

            final int idx = exprContext.lookupLocal(vd);
            if (idx < 0) return null;

            final Type sharedVarType = exprContext.getSharedVarType(vd);
            Type localType = state.getTypeMapper().mapType(vd.getOutType());
            final Type type = sharedVarType != null ? sharedVarType : localType;

            StackValue outerValue = StackValue.local(idx, type);
            final String fieldName = "$" + (closure.size() + 1); // + "$" + vd.getName();
            StackValue innerValue = sharedVarType != null ? StackValue.fieldForSharedVar(localType, name, fieldName) : StackValue.field(type, name, fieldName, false);

            cv.newField(null, Opcodes.ACC_PUBLIC, fieldName, type.getDescriptor(), null, null);

            answer = new EnclosedValueDescriptor(d, innerValue, outerValue);
            closure.put(d, answer);

            return innerValue;
        }

        return null;
    }

    public boolean isCaptureThis() {
        return captureThis;
    }

    public boolean isConst () {
        return !captureThis && closure.isEmpty();
    }
}
