package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetTupleType;
import org.jetbrains.jet.lang.resolve.JetScope;
import org.jetbrains.jet.lang.resolve.LazyScopeAdapter;
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
    private JetType boundsAsType;
    private final TypeConstructor typeConstructor;
    private JetType defaultType;
    private final Set<JetType> classObjectUpperBounds = Sets.newLinkedHashSet();
    private JetType classObjectBoundsAsType;

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
        upperBounds.add(bound); // TODO : Duplicates?
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
        return getContainingDeclaration() + "::" + typeConstructor.toString();
    }

    @NotNull
    public JetType getBoundsAsType() {
        if (boundsAsType == null) {
            assert upperBounds != null;
            assert upperBounds.size() > 0;
            boundsAsType = TypeUtils.intersect(JetTypeChecker.INSTANCE, upperBounds);
            if (boundsAsType == null) {
                boundsAsType = JetStandardClasses.getNothingType();
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
        if (defaultType == null) {
            defaultType = new JetTypeImpl(
                            Collections.<Annotation>emptyList(),
                            getTypeConstructor(),
                            TypeUtils.hasNullableBound(this),
                            Collections.<TypeProjection>emptyList(),
                            new LazyScopeAdapter(new LazyValue<JetScope>() {
                                @Override
                                protected JetScope compute() {
                                    return getBoundsAsType().getMemberScope();
                                }
                            }));
        }
        return defaultType;
    }

    @Override
    public JetType getClassObjectType() {
        if (classObjectUpperBounds.isEmpty()) return null;

        if (classObjectBoundsAsType == null) {
            classObjectBoundsAsType = TypeUtils.intersect(JetTypeChecker.INSTANCE, classObjectUpperBounds);
            assert classObjectBoundsAsType != null; // TODO : Error message
        }
        return classObjectBoundsAsType;
    }

    @Override
    public boolean isClassObjectAValue() {
        return true;
    }

    public void addClassObjectBound(@NotNull JetType bound) {
        classObjectUpperBounds.add(bound); // TODO : Duplicates?
    }

    public int getIndex() {
        return index;
    }
}