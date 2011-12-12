package org.jetbrains.jet.lang.resolve.scopes;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Nikolay Krasko
 */
public final class JetScopeUtils {
    private JetScopeUtils() {}

    /**
     * Get receivers in order of locality, so that the closest (the most local) receiver goes first
     * A wrapper for {@link JetScope#getImplicitReceiversHierarchy(java.util.List)}
     *
     * @param scope Scope for getting receivers hierarchy.
     * @return receivers hierarchy.
     */
    @NotNull
    public static Collection<ReceiverDescriptor> getImplicitReceiversHierarchy(@NotNull JetScope scope) {
        List<ReceiverDescriptor> descriptors = Lists.newArrayList();
        scope.getImplicitReceiversHierarchy(descriptors);
        return descriptors;
    }

    /**
     * Get all extension descriptors among visible descriptors for current scope.
     *
     * @param scope Scope for query extensions.
     * @return extension descriptors.
     */
    public static Collection<CallableDescriptor> getAllExtensions(@NotNull JetScope scope) {
        final Set<CallableDescriptor> result = Sets.newHashSet();

        for (DeclarationDescriptor descriptor : scope.getAllDescriptors()) {
            if (descriptor instanceof CallableDescriptor) {
                CallableDescriptor callDescriptor = (CallableDescriptor) descriptor;
                if (callDescriptor.getReceiverParameter().exists()) {
                    result.add(callDescriptor);
                }
            }
        }

        return result;
    }
}
