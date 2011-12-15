package org.jetbrains.jet.lang;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

/**
 * @author abreslav
 */
public interface Configuration {
    Configuration EMPTY = new Configuration() {
        @Override
        public void addDefaultImports(@NotNull BindingTrace trace, @NotNull WritableScope rootScope) {
        }

        @Override
        public void extendNamespaceScope(@NotNull BindingTrace trace, @NotNull NamespaceDescriptor namespaceDescriptor, @NotNull WritableScope namespaceMemberScope) {
        }
    };

    void addDefaultImports(@NotNull BindingTrace trace, @NotNull WritableScope rootScope);

    /**
     *
     * This method is called every time a namespace descriptor is created. Use it to add extra descriptors to the namespace, e.g. merge a Java package with a Kotlin one
     */
    void extendNamespaceScope(@NotNull BindingTrace trace, @NotNull NamespaceDescriptor namespaceDescriptor, @NotNull WritableScope namespaceMemberScope);
}
