/*
 * @author max
 */
package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.objectweb.asm.Type;

public class ClassContext {
    public static final ClassContext STATIC = new ClassContext(null, OwnerKind.NAMESPACE, null, null);
    private final DeclarationDescriptor contextType;
    private final OwnerKind contextKind;
    private final StackValue thisExpression;
    private final ClassContext parentContext;

    public ClassContext(DeclarationDescriptor contextType, OwnerKind contextKind, StackValue thisExpression, ClassContext parentContext) {
        this.contextType = contextType;
        this.contextKind = contextKind;
        this.thisExpression = thisExpression;
        this.parentContext = parentContext;
    }

    public DeclarationDescriptor getContextDescriptor() {
        return contextType;
    }

    public OwnerKind getContextKind() {
        return contextKind;
    }

    public StackValue getThisExpression() {
        if (thisExpression != null) return thisExpression;
        return parentContext != null ? parentContext.getThisExpression() : null;
    }

    public ClassContext intoNamespace(NamespaceDescriptor descriptor) {
        return new ClassContext(descriptor, OwnerKind.NAMESPACE, null, this);
    }

    public ClassContext intoClass(ClassDescriptor descriptor, OwnerKind kind) {
        final StackValue thisValue;
        if (kind == OwnerKind.DELEGATING_IMPLEMENTATION) {
            thisValue = StackValue.instanceField(JetTypeMapper.jetInterfaceType(descriptor),
                    JetTypeMapper.jetDelegatingImplementationType(descriptor).getInternalName(),
                    "$this");
        }
        else {
            thisValue = StackValue.local(0, JetTypeMapper.TYPE_OBJECT);
        }

        return new ClassContext(descriptor, kind, thisValue, this);
    }

    public ClassContext intoFunction(FunctionDescriptor descriptor) {
        int thisIdx = -1;
        if (getContextKind() != OwnerKind.NAMESPACE) {
            thisIdx++;
        }

        final boolean hasReceiver = descriptor.getReceiverType() != null;
        if (hasReceiver) {
            thisIdx++;
        }

        return new ClassContext(descriptor, getContextKind(), hasReceiver ? StackValue.local(thisIdx, JetTypeMapper.TYPE_OBJECT) : null, this);
    }

    public ClassContext intoClosure(String internalClassName) {
        final DeclarationDescriptor contextDescriptor = getContextClass();
        StackValue outerClass = contextDescriptor instanceof ClassDescriptor
                                ? StackValue.instanceField(JetTypeMapper.jetImplementationType((ClassDescriptor) contextDescriptor), internalClassName, "this$0")
                                : StackValue.local(0, JetTypeMapper.TYPE_OBJECT);
        return new ClassContext(null, OwnerKind.IMPLEMENTATION, outerClass, this);
    }

    public FrameMap prepareFrame() {
        FrameMap frameMap = new FrameMap();

        if (getContextKind() != OwnerKind.NAMESPACE) {
            frameMap.enterTemp();  // 0 slot for this
        }

        if (hasReceiver()) {
            frameMap.enterTemp();  // Next slot for fake this
        }

        return frameMap;
    }

    private JetType receiverType() {
        return contextType instanceof FunctionDescriptor ? ((FunctionDescriptor) contextType).getReceiverType() : null;
    }

    private boolean hasReceiver() {
        return receiverType() != null;
    }

    public ClassContext getParentContext() {
        return parentContext;
    }

    public Type jvmType(JetTypeMapper mapper) {
        if (contextType instanceof ClassDescriptor) {
            return mapper.jvmType((ClassDescriptor) contextType, contextKind);
        }
        else {
            return JetTypeMapper.TYPE_OBJECT; // TODO?
        }
    }

    public DeclarationDescriptor getContextClass() {
        DeclarationDescriptor descriptor = getContextDescriptor();
        if (descriptor == null || descriptor instanceof ClassDescriptor || descriptor instanceof NamespaceDescriptor) return descriptor;

        final ClassContext parent = getParentContext();
        return parent != null ? parent.getContextClass() : null;
    }
}
