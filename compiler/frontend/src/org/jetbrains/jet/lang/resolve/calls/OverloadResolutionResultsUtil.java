package org.jetbrains.jet.lang.resolve.calls;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;

/**
 * @author svtk
 */
public class OverloadResolutionResultsUtil {
    @NotNull
    public static <D extends CallableDescriptor> OverloadResolutionResults<D> ambiguity(OverloadResolutionResults<D> results1, OverloadResolutionResults<D> results2) {
        Collection<ResolvedCallImpl<D>> resultingCalls = Lists.newArrayList();
        resultingCalls.addAll((Collection<ResolvedCallImpl<D>>) results1.getResultingCalls());
        resultingCalls.addAll((Collection<ResolvedCallImpl<D>>) results2.getResultingCalls());
        return OverloadResolutionResultsImpl.ambiguity(resultingCalls);
    }

    @Nullable
    public static <D extends CallableDescriptor> JetType getResultType(OverloadResolutionResults<D> results) {
        if (results.isSuccess()) {
            return results.getResultingDescriptor().getReturnType();
        }
        return null;
    }
}
