package org.jetbrains.jet.lang.modules;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.ClassDescriptor;
import org.jetbrains.jet.lang.types.ExtensionDescriptor;
import org.jetbrains.jet.lang.types.MethodDescriptor;
import org.jetbrains.jet.lang.types.PropertyDescriptor;

/**
 * @author abreslav
 */
public interface MemberDomain {
    @Nullable
    MethodDescriptor getMethods(String name);

    @Nullable
    ClassDescriptor getClass(String name);

    @Nullable
    PropertyDescriptor getProperty(String name);

    @Nullable
    ExtensionDescriptor getExtension(String name);
}
