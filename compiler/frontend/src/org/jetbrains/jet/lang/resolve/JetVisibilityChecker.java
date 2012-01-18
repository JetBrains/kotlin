package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;

/**
 * @author abreslav
 */
public class JetVisibilityChecker {
    /**
     * @param locationOwner owner of the call site
     * @param subject the descriptor whose visibility is being checked
     * @return <code>true</code> iff subject is visible locationOwner
     */
    public boolean isVisible(@NotNull DeclarationDescriptor locationOwner, @NotNull DeclarationDescriptor subject) {
        // TODO : stub implementation
        return true;
    }
}
