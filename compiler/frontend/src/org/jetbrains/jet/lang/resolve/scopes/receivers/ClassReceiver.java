package org.jetbrains.jet.lang.resolve.scopes.receivers;

import org.jetbrains.jet.lang.descriptors.ClassDescriptor;

/**
 * @author abreslav
 */
public class ClassReceiver extends ImplicitReceiverDescriptor {

    public ClassReceiver(ClassDescriptor classDescriptor) {
        super(classDescriptor, classDescriptor.getDefaultType());
    }
}
