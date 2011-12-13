package org.jetbrains.jet.lang.resolve.calls.inference;

import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.resolve.calls.ResolutionDebugInfo;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Map;
import java.util.Set;

import static org.jetbrains.jet.lang.resolve.calls.ResolutionDebugInfo.*;

/**
 * @author abreslav
 */
public class DebugConstraintResolutionListener implements ConstraintResolutionListener {

    private final ResolutionDebugInfo.Data debugInfo;

    public DebugConstraintResolutionListener(@NotNull ResolutionDebugInfo.Data debugInfo) {
        this.debugInfo = debugInfo;
    }

    @Override
    public void constraintsForUnknown(TypeParameterDescriptor typeParameterDescriptor, BoundsOwner typeValue) {
        if (!ResolutionDebugInfo.isResolutionDebugEnabled()) return;
        Map<TypeParameterDescriptor, BoundsOwner> map = debugInfo.get(BOUNDS_FOR_UNKNOWNS);
        if (map == null) {
            map = Maps.newLinkedHashMap();
            debugInfo.set(BOUNDS_FOR_UNKNOWNS, map);
        }
        map.put(typeParameterDescriptor, typeValue);
    }

    @Override
    public void constraintsForKnownType(JetType type, BoundsOwner typeValue) {
        if (!ResolutionDebugInfo.isResolutionDebugEnabled()) return;
        Map<JetType,BoundsOwner> map = debugInfo.get(BOUNDS_FOR_KNOWNS);
        if (map == null) {
            map = Maps.newLinkedHashMap();
            debugInfo.set(BOUNDS_FOR_KNOWNS, map);
        }
        map.put(type, typeValue);
    }

    @Override
    public void done(ConstraintSystemSolution solution, Set<TypeParameterDescriptor> typeParameterDescriptors) {
        if (!ResolutionDebugInfo.isResolutionDebugEnabled()) return;
        debugInfo.set(SOLUTION, solution);
        debugInfo.set(UNKNOWNS, typeParameterDescriptors);
    }

    @Override
    public void log(Object... messageFragments) {
        if (!ResolutionDebugInfo.isResolutionDebugEnabled()) return;
        StringBuilder stringBuilder = debugInfo.get(LOG);
        if (stringBuilder == null) {
            stringBuilder = new StringBuilder();
            debugInfo.set(LOG, stringBuilder);
        }
        for (Object m : messageFragments) {
            stringBuilder.append(m);
        }
        stringBuilder.append("\n");
    }

    @Override
    public void error(Object... messageFragments) {
        if (!ResolutionDebugInfo.isResolutionDebugEnabled()) return;
        StringBuilder stringBuilder = debugInfo.get(ERRORS);
        if (stringBuilder == null) {
            stringBuilder = new StringBuilder();
            debugInfo.set(ERRORS, stringBuilder);
        }
        for (Object m : messageFragments) {
            stringBuilder.append(m);
        }
        stringBuilder.append("\n");
    }
}
