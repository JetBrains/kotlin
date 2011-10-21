package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public interface DeclarationDescriptorWithVisibility extends DeclarationDescriptor {
    @NotNull
    Visibility getVisibility();
}
