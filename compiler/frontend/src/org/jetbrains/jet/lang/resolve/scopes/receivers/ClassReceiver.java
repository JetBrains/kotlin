package org.jetbrains.jet.lang.resolve.scopes.receivers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;

/**
 * @author abreslav
 */
public class ClassReceiver extends ImplicitReceiverDescriptor {

    public ClassReceiver(ClassDescriptor classDescriptor) {
        super(classDescriptor, classDescriptor.getDefaultType());
    }

    @Override
    public <R, D> R accept(@NotNull ReceiverDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitClassReceiver(this, data);
    }
}
