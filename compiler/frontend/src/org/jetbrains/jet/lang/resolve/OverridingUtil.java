package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;

import java.util.Collection;
import java.util.Set;

/**
 * @author abreslav
 */
public class OverridingUtil {

    public static Set<CallableDescriptor> getEffectiveMembers(@NotNull ClassDescriptor classDescriptor) {
        Collection<DeclarationDescriptor> allDescriptors = classDescriptor.getDefaultType().getMemberScope().getAllDescriptors();
        Set<CallableDescriptor> allMembers = Sets.newLinkedHashSet();
        for (DeclarationDescriptor descriptor : allDescriptors) {
            assert !(descriptor instanceof ConstructorDescriptor);
            if (descriptor instanceof CallableDescriptor) {
                CallableDescriptor callableDescriptor = (CallableDescriptor) descriptor;
                allMembers.add(callableDescriptor);
            }
        }
        return filterOverrides(allMembers);
    }

    public static <D extends CallableDescriptor> Set<D> filterOverrides(Set<D> candidateSet) {
        Set<D> candidates = Sets.newLinkedHashSet(candidateSet);
        for (D descriptor : candidateSet) {
            candidates.removeAll(descriptor.getOverriddenDescriptors());
        }
        return candidates;
    }

    public static <Descriptor extends CallableDescriptor> boolean overrides(@NotNull Descriptor f, @NotNull Descriptor g) {
        Set<CallableDescriptor> overriddenDescriptors = Sets.newHashSet();
        getAllOverriddenDescriptors(f.getOriginal(), overriddenDescriptors);
        CallableDescriptor originalG = g.getOriginal();
        for (CallableDescriptor overriddenFunction : overriddenDescriptors) {
            if (originalG.equals(overriddenFunction.getOriginal())) return true;
        }
        return false;
    }

    private static void getAllOverriddenDescriptors(@NotNull CallableDescriptor current, @NotNull Set<CallableDescriptor> overriddenDescriptors) {
        if (overriddenDescriptors.contains(current)) return;
        for (CallableDescriptor descriptor : current.getOriginal().getOverriddenDescriptors()) {
            getAllOverriddenDescriptors(descriptor, overriddenDescriptors);
            overriddenDescriptors.add(descriptor);
        }
    }
}
