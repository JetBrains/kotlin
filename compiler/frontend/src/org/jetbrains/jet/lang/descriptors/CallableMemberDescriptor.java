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

    enum Kind {
        DECLARATION,
        FAKE_OVERRIDE,
        DELEGATION,
        ;
        
        public boolean isReal() {
            return this == DECLARATION || this == DELEGATION;
        }
    }

    /**
     * Is this a real function or function projection.
     */
    Kind getKind();

    @NotNull
    CallableMemberDescriptor copy(DeclarationDescriptor newOwner, boolean makeNonAbstract, Kind kind, boolean copyOverrides);
}
