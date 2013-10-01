package org.jetbrains.jet.lang.resolve.calls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.resolve.calls.context.BasicCallResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResultsImpl;

public class CompositeExtension implements CallResolverExtension {
    private final CallResolverExtension[] delegates = new CallResolverExtension[]{
            new NeedSyntheticCallResolverExtension(), new TypeParameterAsReifiedCheck()};

    @Override
    public <F extends CallableDescriptor> void run(
            @NotNull OverloadResolutionResultsImpl<F> results,
            @NotNull BasicCallResolutionContext context
    ) {
        for (CallResolverExtension delegate : delegates) {
            delegate.run(results, context);
        }
    }
}
