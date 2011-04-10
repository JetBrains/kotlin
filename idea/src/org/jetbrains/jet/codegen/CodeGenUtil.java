package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.types.ClassDescriptor;
import org.jetbrains.jet.resolve.DescriptorUtil;

/**
 * @author max
 */
public class CodeGenUtil {
    public static  String getInternalInterfaceName(ClassDescriptor descriptor) {
        return DescriptorUtil.getFQName(descriptor).replace('.', '/');
    }

    public static String getInternalImplementationName(ClassDescriptor descriptor) {
        return getInternalInterfaceName(descriptor) + "$$Impl";
    }

    public static String getInternalDelegatingImplementationName(ClassDescriptor descriptor) {
        return getInternalInterfaceName(descriptor) + "$$DImpl";
    }
}
