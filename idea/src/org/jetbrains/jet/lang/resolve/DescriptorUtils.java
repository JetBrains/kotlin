package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;

/**
 * @author abreslav
 */
public class DescriptorUtils {
    public static boolean definesItsOwnThis(@NotNull DeclarationDescriptor descriptor) {
        return descriptor.accept(new DeclarationDescriptorVisitor<Boolean, Void>() {
            @Override
            public Boolean visitDeclarationDescriptor(DeclarationDescriptor descriptor, Void data) {
                return false;
            }

            @Override
            public Boolean visitFunctionDescriptor(FunctionDescriptor descriptor, Void data) {
                return descriptor.getReceiverType() != null;
            }

            @Override
            public Boolean visitClassDescriptor(ClassDescriptor descriptor, Void data) {
                return true;
            }

            @Override
            public Boolean visitPropertyDescriptor(PropertyDescriptor descriptor, Void data) {
                return descriptor.getReceiverType() != null;
            }
        }, null);
    }
}
