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
    private final ClassContext parentContext;

    public ClassContext(DeclarationDescriptor contextType, OwnerKind contextKind, StackValue thisExpression, ClassContext parentContext) {
        this.contextType = contextType;
        this.contextKind = contextKind;
        this.parentContext = parentContext;
    }

    public DeclarationDescriptor getContextType() {
        return contextType;
    }

    public OwnerKind getContextKind() {
        return contextKind;
    }

    public StackValue getThisExpression() {
        int thisIdx = -1;
        if (getContextKind() != OwnerKind.NAMESPACE) {
            thisIdx++;
        }

        if (hasReceiver()) {
            thisIdx++;
        }

        if (thisIdx == -1) {
            throw new RuntimeException("Has no this!" + contextType);
        }

        return StackValue.local(thisIdx, JetTypeMapper.TYPE_OBJECT);
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
        return new ClassContext(descriptor, getContextKind(), StackValue.local(0, JetTypeMapper.TYPE_OBJECT), this);
    }

    public ClassContext intoClosure() {
        return new ClassContext(null, OwnerKind.IMPLEMENTATION, StackValue.local(0, JetTypeMapper.TYPE_OBJECT), this); // TODO!
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
            if (contextKind == OwnerKind.INTERFACE) {
                System.out.println("OOps?!");
            }
            return mapper.jvmType((ClassDescriptor) contextType, contextKind);
        }
        else {
            return JetTypeMapper.TYPE_OBJECT; // TODO?
        }
    }
}
