package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.util.lazy.LazyValue;
import org.jetbrains.jet.util.lazy.ReenteringLazyValueComputationException;

import java.util.List;

import static org.jetbrains.jet.lang.resolve.BindingContext.DEFERRED_TYPE;
import static org.jetbrains.jet.lang.resolve.BindingContext.DeferredTypeKey.DEFERRED_TYPE_KEY;

/**
 * @author abreslav
 */
public class DeferredType implements JetType {
    
    public static DeferredType create(BindingTrace trace, LazyValue<JetType> lazyValue) {
        DeferredType deferredType = new DeferredType(lazyValue);
        trace.record(DEFERRED_TYPE, DEFERRED_TYPE_KEY, deferredType);
        return deferredType;
    }
    
    private final LazyValue<JetType> lazyValue;

    private DeferredType(LazyValue<JetType> lazyValue) {
        this.lazyValue = lazyValue;
    }

    public boolean isComputed() {
        return lazyValue.isComputed();
    }

    public JetType getActualType() {
        return lazyValue.get();
    }

    @Override
    @NotNull
    public JetScope getMemberScope() {
        return getActualType().getMemberScope();
    }

    @Override
    @NotNull
    public TypeConstructor getConstructor() {
        return getActualType().getConstructor();
    }

    @Override
    @NotNull
    public List<TypeProjection> getArguments() {
        return getActualType().getArguments();
    }

    @Override
    public boolean isNullable() {
        return getActualType().isNullable();
    }

    @Override
    public List<AnnotationDescriptor> getAnnotations() {
        return getActualType().getAnnotations();
    }

    @Override
    public String toString() {
        try {
            if (lazyValue.isComputed()) {
                return getActualType().toString();
            } else {
                return "<Not computed yet>";
            }
        }
        catch (ReenteringLazyValueComputationException e) {
            return "<Failed to compute this type>";
        }
    }

    @Override
    public boolean equals(Object obj) {
        return getActualType().equals(obj);
    }

    @Override
    public int hashCode() {
        return getActualType().hashCode();
    }
}
