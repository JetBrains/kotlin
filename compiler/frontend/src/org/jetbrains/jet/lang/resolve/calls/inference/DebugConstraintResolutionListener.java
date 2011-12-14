package org.jetbrains.jet.lang.resolve.calls.inference;

import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.resolve.calls.ResolutionDebugInfo;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Map;
import java.util.Set;

import static org.jetbrains.jet.lang.resolve.calls.ResolutionDebugInfo.*;

/**
 * @author abreslav
 */
public class DebugConstraintResolutionListener implements ConstraintResolutionListener {

    private final ResolutionDebugInfo.Data debugInfo;
    private final ResolvedCall<? extends CallableDescriptor> candidateCall;

    public DebugConstraintResolutionListener(@NotNull ResolvedCall<? extends CallableDescriptor> candidateCall, @NotNull ResolutionDebugInfo.Data debugInfo) {
        this.debugInfo = debugInfo;
        this.candidateCall = candidateCall;
    }

    @Override
    public void constraintsForUnknown(TypeParameterDescriptor typeParameterDescriptor, BoundsOwner typeValue) {
        if (!ResolutionDebugInfo.isResolutionDebugEnabled()) return;
        Map<TypeParameterDescriptor, BoundsOwner> map = debugInfo.getByKey(BOUNDS_FOR_UNKNOWNS, candidateCall);
        if (map == null) {
            map = Maps.newLinkedHashMap();
            debugInfo.putByKey(BOUNDS_FOR_UNKNOWNS, candidateCall, map);
        }
        map.put(typeParameterDescriptor, typeValue);
    }

    @Override
    public void constraintsForKnownType(JetType type, BoundsOwner typeValue) {
        if (!ResolutionDebugInfo.isResolutionDebugEnabled()) return;
        Map<JetType,BoundsOwner> map = debugInfo.getByKey(BOUNDS_FOR_KNOWNS, candidateCall);
        if (map == null) {
            map = Maps.newLinkedHashMap();
            debugInfo.putByKey(BOUNDS_FOR_KNOWNS, candidateCall, map);
        }
        map.put(type, typeValue);
    }

    @Override
    public void done(ConstraintSystemSolution solution, Set<TypeParameterDescriptor> typeParameterDescriptors) {
        if (!ResolutionDebugInfo.isResolutionDebugEnabled()) return;
        debugInfo.putByKey(SOLUTION, candidateCall, solution);
        debugInfo.putByKey(UNKNOWNS, candidateCall, typeParameterDescriptors);
    }

    @Override
    public void log(Object... messageFragments) {
        if (!ResolutionDebugInfo.isResolutionDebugEnabled()) return;
        StringBuilder stringBuilder = debugInfo.getByKey(LOG, candidateCall);
        if (stringBuilder == null) {
            stringBuilder = new StringBuilder();
            debugInfo.putByKey(LOG, candidateCall, stringBuilder);
        }
        for (Object m : messageFragments) {
            stringBuilder.append(m);
        }
        stringBuilder.append("\n");
    }

    @Override
    public void error(Object... messageFragments) {
        if (!ResolutionDebugInfo.isResolutionDebugEnabled()) return;
        StringBuilder stringBuilder = debugInfo.getByKey(ERRORS, candidateCall);
        if (stringBuilder == null) {
            stringBuilder = new StringBuilder();
            debugInfo.putByKey(ERRORS, candidateCall, stringBuilder);
        }
        for (Object m : messageFragments) {
            stringBuilder.append(m);
        }
        stringBuilder.append("\n");
    }
}
