package org.jetbrains.jet.lang.resolve.calls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.resolve.calls.context.BasicCallResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResultsImpl;

import java.util.List;

public class CompositeExtension implements CallResolverExtension {

    private final List<CallResolverExtension> extensions;

    public CompositeExtension(@NotNull List<CallResolverExtension> extensions) {
        this.extensions = extensions;
    }

    @Override
    public <F extends CallableDescriptor> void run(
            @NotNull ResolvedCall<F> resolvedCall,
            @NotNull BasicCallResolutionContext context
    ) {
        for (CallResolverExtension resolverExtension : extensions) {
            resolverExtension.run(resolvedCall, context);
        }
    }
}
