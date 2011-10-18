package org.jetbrains.jet.lang.resolve.scopes.receivers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author abreslav
 */
public class ClassReceiver implements ThisReceiverDescriptor {

    private final ClassDescriptor classDescriptor;

    public ClassReceiver(@NotNull ClassDescriptor classDescriptor) {
        this.classDescriptor = classDescriptor;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @NotNull
    @Override
    public JetType getType() {
        return classDescriptor.getDefaultType();
    }

    @NotNull
    @Override
    public DeclarationDescriptor getDeclarationDescriptor() {
        return classDescriptor;
    }

    @Override
    public <R, D> R accept(@NotNull ReceiverDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitClassReceiver(this, data);
    }
}
