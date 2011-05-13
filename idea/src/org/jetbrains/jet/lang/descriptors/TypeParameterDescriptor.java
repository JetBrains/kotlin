package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public class TypeParameterDescriptor extends DeclarationDescriptorImpl implements ClassifierDescriptor {
    public static TypeParameterDescriptor createWithDefaultBound(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<Annotation> annotations,
            @NotNull Variance variance,
            @NotNull String name,
            int index) {
        TypeParameterDescriptor typeParameterDescriptor = createForFurtherModification(containingDeclaration, annotations, variance, name, index);
        typeParameterDescriptor.addUpperBound(JetStandardClasses.getDefaultBound());
        return typeParameterDescriptor;
    }

    public static TypeParameterDescriptor createForFurtherModification(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<Annotation> annotations,
            @NotNull Variance variance,
            @NotNull String name,
            int index) {
        return new TypeParameterDescriptor(containingDeclaration, annotations, variance, name, index);
    }

    private final int index;
    private final Variance variance;
    private final Set<JetType> upperBounds;
    private final TypeConstructor typeConstructor;
    private JetType boundsAsType;
    private JetType type;

    private TypeParameterDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<Annotation> annotations,
            @NotNull Variance variance,
            @NotNull String name,
            int index) {
        super(containingDeclaration, annotations, name);
        this.index = index;
        this.variance = variance;
        this.upperBounds = Sets.newLinkedHashSet();
        // TODO: Should we actually pass the annotations on to the type constructor?
        this.typeConstructor = new TypeConstructorImpl(
                this,
                annotations,
                false,
                "&" + name,
                Collections.<TypeParameterDescriptor>emptyList(),
                upperBounds);
    }

    public Variance getVariance() {
        return variance;
    }

    public void addUpperBound(@NotNull JetType bound) {
        upperBounds.add(bound);
    }

    public Set<JetType> getUpperBounds() {
        return upperBounds;
    }

    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }

    @Override
    public String toString() {
        return typeConstructor.toString();
    }

    @NotNull
    public JetType getBoundsAsType() {
        if (boundsAsType == null) {
            assert upperBounds != null;
            assert upperBounds.size() > 0;
            boundsAsType = upperBounds.size() == 1 ? upperBounds.iterator().next() : TypeUtils.intersect(JetTypeChecker.INSTANCE, upperBounds);
            if (boundsAsType == null) {
                boundsAsType = JetStandardClasses.getNothingType(); // TODO : some error message?
            }
        }
        return boundsAsType;
    }

    @NotNull
    @Override
    public TypeParameterDescriptor substitute(TypeSubstitutor substitutor) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitTypeParameterDescriptor(this, data);
    }

    @NotNull
    @Override
    public JetType getDefaultType() {
        if (type == null) {
            type = new JetTypeImpl(
                            Collections.<Annotation>emptyList(),
                            getTypeConstructor(),
                            TypeUtils.hasNullableBound(this),
                            Collections.<TypeProjection>emptyList(),
                            getBoundsAsType().getMemberScope());
        }
        return type;
    }

    public int getIndex() {
        return index;
    }
}