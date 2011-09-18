package org.jetbrains.jet.lang.resolve.scopes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;

/**
 * @author abreslav
 */
public interface RedeclarationHandler {
    RedeclarationHandler DO_NOTHING = new RedeclarationHandler() {
        @Override
        public void handleRedeclaration(@NotNull DeclarationDescriptor first, @NotNull DeclarationDescriptor second) {
        }
    };
    RedeclarationHandler THROW_EXCEPTION = new RedeclarationHandler() {
        @Override
        public void handleRedeclaration(@NotNull DeclarationDescriptor first, @NotNull DeclarationDescriptor second) {
            throw new IllegalStateException("Redeclaration: " + first + " and " + second + "(no line info available)");
        }
    };

    void handleRedeclaration(@NotNull DeclarationDescriptor first, @NotNull DeclarationDescriptor second);
}
