package org.jetbrains.jet.plugin.codeInsight;

import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.MutableClassDescriptor;
import org.jetbrains.jet.lang.resolve.OverrideResolver;

import java.util.HashSet;
import java.util.Set;

/**
 * @author yole
 */
public class OverrideMethodsHandler extends OverrideImplementMethodsHandler {
    protected Set<CallableMemberDescriptor> collectMethodsToGenerate(MutableClassDescriptor descriptor) {
        final Set<CallableMemberDescriptor> superMethods = OverrideResolver.collectSuperMethods(descriptor).keySet();
        for (CallableMemberDescriptor member : descriptor.getCallableMembers()) {
            superMethods.removeAll(member.getOverriddenDescriptors());
        }
        Set<CallableMemberDescriptor> result = new HashSet<CallableMemberDescriptor>();
        for (CallableMemberDescriptor superMethod : superMethods) {
            if (superMethod.getModality().isOverridable()) {
                result.add(superMethod);
            }
        }
        return result;
    }

    protected String getChooserTitle() {
        return "Override Members";
    }
}
