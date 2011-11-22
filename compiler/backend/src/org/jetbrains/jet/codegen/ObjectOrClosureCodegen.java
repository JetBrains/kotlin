package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.descriptors.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author alex.tkachman
 */
public class ObjectOrClosureCodegen {
    protected boolean captureThis;
    protected Type captureReceiver;

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

    public StackValue lookupInContext(DeclarationDescriptor d, StackValue result) {
        EnclosedValueDescriptor answer = closure.get(d);
        if (answer != null) {
            StackValue innerValue = answer.getInnerValue();
            return result != null ? innerValue : StackValue.composed(result, innerValue);
        }

        if (d instanceof VariableDescriptor && !(d instanceof PropertyDescriptor)) {
            VariableDescriptor vd = (VariableDescriptor) d;

            final int idx = exprContext.lookupLocal(vd);
            if (idx < 0) return null;

            final Type sharedVarType = state.getTypeMapper().getSharedVarType(vd);
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

        if(d instanceof FunctionDescriptor) {
            // we are looking for receiver
            FunctionDescriptor fd = (FunctionDescriptor) d;

            // we generate method
            assert context instanceof CodegenContext.ReceiverContext;

            CodegenContext.ReceiverContext fcontext = (CodegenContext.ReceiverContext) context;

            if(fcontext.getReceiverDescriptor() != fd)
                return null;

            Type type = state.getTypeMapper().mapType(fcontext.getReceiverDescriptor().getReceiverParameter().getType());
            boolean isStatic = fcontext.getContextDescriptor().getContainingDeclaration() instanceof NamespaceDescriptor;
            StackValue outerValue = StackValue.local(isStatic ? 0 : 1, type);
            final String fieldName = "receiver$0";
            StackValue innerValue = StackValue.field(type, name, fieldName, false);

            cv.newField(null, Opcodes.ACC_PUBLIC, fieldName, type.getDescriptor(), null, null);

            answer = new EnclosedValueDescriptor(d, innerValue, outerValue);
            closure.put(d, answer);

            assert captureReceiver == null;
            captureReceiver = type;

            return innerValue;
        }

        return null;
    }

    public boolean isConst () {
        return !captureThis && captureReceiver != null && closure.isEmpty();
    }
}
