package org.jetbrains.jet.lang.resolve.scopes.receivers;

import org.jetbrains.jet.lang.descriptors.CallableDescriptor;

/**
 * @author abreslav
 */
public class ExtensionCallableReceiver extends ImplicitReceiverDescriptor {

    public ExtensionCallableReceiver(CallableDescriptor callableDescriptor) {
        super(callableDescriptor, callableDescriptor.getReceiverType());
    }
}
