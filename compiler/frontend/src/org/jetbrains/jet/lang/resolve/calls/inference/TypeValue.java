package org.jetbrains.jet.lang.resolve.calls.inference;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.Variance;

import java.util.Set;

/**
* @author abreslav
*/
public class TypeValue implements BoundsOwner {
    private final Set<TypeValue> upperBounds = Sets.newLinkedHashSet();
    private final Set<TypeValue> lowerBounds = Sets.newLinkedHashSet();

    private final Variance positionVariance;
    private final TypeParameterDescriptor typeParameterDescriptor; // Null for known types
    private final JetType originalType;
    private JetType value; // For an unknown — the value found by constraint resolution, for a known — just it's value

    // Unknown type
    public TypeValue(@NotNull TypeParameterDescriptor typeParameterDescriptor, @NotNull Variance positionVariance) {
        this.positionVariance = positionVariance;
        this.typeParameterDescriptor = typeParameterDescriptor;
        this.originalType = typeParameterDescriptor.getDefaultType();
    }

    // Known type
    public TypeValue(@NotNull JetType knownType) {
        this.positionVariance = null;
        this.typeParameterDescriptor = null;
        this.originalType = knownType;
    }

    public boolean isKnown() {
        return typeParameterDescriptor == null;
    }

    public TypeParameterDescriptor getTypeParameterDescriptor() {
        return typeParameterDescriptor;
    }

    @Override
    @NotNull
    public Set<TypeValue> getUpperBounds() {
        return upperBounds;
    }

    @Override
    @NotNull
    public Set<TypeValue> getLowerBounds() {
        return lowerBounds;
    }

    @NotNull
    public JetType getType() {
        return value;
    }

    @NotNull
    public JetType getOriginalType() {
        return originalType;
    }

    public void addUpperBound(@NotNull TypeValue bound) {
        upperBounds.add(bound);
    }

    public void addLowerBound(@NotNull TypeValue bound) {
        lowerBounds.add(bound);
    }

    public void setValue(@NotNull JetType value) {
        this.value = value;
    }

    public boolean hasValue() {
        return value != null;
    }
}
