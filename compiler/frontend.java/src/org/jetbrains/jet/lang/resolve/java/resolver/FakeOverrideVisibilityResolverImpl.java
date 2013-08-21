package org.jetbrains.jet.lang.resolve.java.resolver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.OverridingUtil;

import javax.inject.Inject;

public class FakeOverrideVisibilityResolverImpl implements FakeOverrideVisibilityResolver {
    private BindingTrace trace;

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }

    @Override
    public void resolveUnknownVisibilityForMember(@NotNull CallableMemberDescriptor descriptor) {
        OverridingUtil.resolveUnknownVisibilityForMember(null, descriptor, trace);
    }
}
