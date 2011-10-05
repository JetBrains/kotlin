package org.jetbrains.jet.lang.resolve.scopes.receivers;

/**
 * @author abreslav
 */
public class ReceiverDescriptorVisitor<R, D> {
    public R visitNoReceiver(ReceiverDescriptor noReceiver, D data) {
        return null;
    }

    public R visitTransientReceiver(TransientReceiver receiver, D data) {
        return null;
    }

    public R visitExtensionReceiver(ExtensionReceiver receiver, D data) {
        return null;
    }

    public R visitExpressionReceiver(ExpressionReceiver receiver, D data) {
        return null;
    }

    public R visitClassReceiver(ClassReceiver receiver, D data) {
        return null;
    }
}
