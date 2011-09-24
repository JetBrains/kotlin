package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * @author abreslav
 */
public interface CallableMemberDescriptor extends CallableDescriptor, MemberDescriptor {
    @NotNull
    @Override
    Set<? extends CallableMemberDescriptor> getOverriddenDescriptors();
}
