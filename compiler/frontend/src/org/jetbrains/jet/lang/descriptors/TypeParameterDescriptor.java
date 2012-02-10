package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.LazyScopeAdapter;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.resolve.DescriptorRenderer;
import org.jetbrains.jet.util.lazy.LazyValue;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public class TypeParameterDescriptor extends DeclarationDescriptorImpl implements ClassifierDescriptor {
    public static TypeParameterDescriptor createWithDefaultBound(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<AnnotationDescriptor> annotations,
            boolean reified,
            @NotNull Variance variance,
            @NotNull String name,
            int index) {
        TypeParameterDescriptor typeParameterDescriptor = createForFurtherModification(containingDeclaration, annotations, reified, variance, name, index);
        typeParameterDescriptor.addUpperBound(JetStandardClasses.getDefaultBound());
        typeParameterDescriptor.setInitialized();
        return typeParameterDescriptor;
    }

    public static TypeParameterDescriptor createForFurtherModification(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<AnnotationDescriptor> annotations,
            boolean reified,
            @NotNull Variance variance,
            @NotNull String name,
            int index) {
        return new TypeParameterDescriptor(containingDeclaration, annotations, reified, variance, name, index);
    }

    // 0-based
    private final int index;
    private final Variance variance;
    private final Set<JetType> upperBounds;
    private JetType upperBoundsAsType;
    private final TypeConstructor typeConstructor;
    private JetType defaultType;
    private final Set<JetType> classObjectUpperBounds = Sets.newLinkedHashSet();
    private JetType classObjectBoundsAsType;

    private final boolean reified;

    private boolean initialized = false;

    private TypeParameterDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<AnnotationDescriptor> annotations,
            boolean reified,
            @NotNull Variance variance,
            @NotNull String name,
            int index) {
        super(containingDeclaration, annotations, name);
        this.index = index;
        this.variance = variance;
        this.upperBounds = Sets.newLinkedHashSet();
        this.reified = reified;
        // TODO: Should we actually pass the annotations on to the type constructor?
        this.typeConstructor = new TypeConstructorImpl(
                this,
                annotations,
                false,
                name,
                Collections.<TypeParameterDescriptor>emptyList(),
                upperBounds);
    }

    public void setInitialized() {
        if (initialized) {
            throw new IllegalStateException();
        }
        initialized = true;
    }

    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException();
        }
    }

    private void checkUninitialized() {
        if (initialized) {
            throw new IllegalStateException();
        }
    }

    public boolean isReified() {
        checkInitialized();
        return reified;
    }

    public Variance getVariance() {
        checkInitialized();
        return variance;
    }

    public void addUpperBound(@NotNull JetType bound) {
        checkUninitialized();
        doAddUpperBound(bound);
    }

    private void doAddUpperBound(JetType bound) {
        upperBounds.add(bound); // TODO : Duplicates?
    }

    public void addDefaultUpperBound() {
        checkUninitialized();

        if (upperBounds.isEmpty()) {
            doAddUpperBound(JetStandardClasses.getDefaultBound());
        }
    }

    @NotNull
    public Set<JetType> getUpperBounds() {
        checkInitialized();
        return upperBounds;
    }

    @NotNull
    public JetType getUpperBoundsAsType() {
        checkInitialized();
        if (upperBoundsAsType == null) {
            assert upperBounds != null : "Upper bound list is null in " + getName();
            assert upperBounds.size() > 0 : "Upper bound list is empty in " + getName();
            upperBoundsAsType = TypeUtils.intersect(JetTypeChecker.INSTANCE, upperBounds);
            if (upperBoundsAsType == null) {
                upperBoundsAsType = JetStandardClasses.getNothingType();
            }
        }
        return upperBoundsAsType;
    }

    @NotNull
    public Set<JetType> getLowerBounds() {
        //checkInitialized();
        return Collections.singleton(JetStandardClasses.getNothingType());
    }

    @NotNull
    public JetType getLowerBoundsAsType() {
        checkInitialized();
        return JetStandardClasses.getNothingType();
    }
    
    
    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        //checkInitialized();
        return typeConstructor;
    }

    @Override
    public String toString() {
        try {
            return DescriptorRenderer.TEXT.render(this);
        } catch (Exception e) {
            return this.getClass().getName() + "@" + System.identityHashCode(this);
        }
    }

    @NotNull
    @Override
    @Deprecated // Use the static method TypeParameterDescriptor.substitute()
    public TypeParameterDescriptor substitute(TypeSubstitutor substitutor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        checkInitialized();
        return visitor.visitTypeParameterDescriptor(this, data);
    }

    @NotNull
    @Override
    public JetType getDefaultType() {
        //checkInitialized();
        if (defaultType == null) {
            defaultType = new JetTypeImpl(
                            Collections.<AnnotationDescriptor>emptyList(),
                            getTypeConstructor(),
                            TypeUtils.hasNullableLowerBound(this),
                            Collections.<TypeProjection>emptyList(),
                            new LazyScopeAdapter(new LazyValue<JetScope>() {
                                @Override
                                protected JetScope compute() {
                                    return getUpperBoundsAsType().getMemberScope();
                                }
                            }));
        }
        return defaultType;
    }

    @Override
    public JetType getClassObjectType() {
        checkInitialized();
        if (classObjectUpperBounds.isEmpty()) return null;

        if (classObjectBoundsAsType == null) {
            classObjectBoundsAsType = TypeUtils.intersect(JetTypeChecker.INSTANCE, classObjectUpperBounds);
            if (classObjectBoundsAsType == null) {
                classObjectBoundsAsType = JetStandardClasses.getNothingType();
            }
        }
        return classObjectBoundsAsType;
    }

    @Override
    public boolean isClassObjectAValue() {
        return true;
    }

    public void addClassObjectBound(@NotNull JetType bound) {
        checkUninitialized();
        classObjectUpperBounds.add(bound); // TODO : Duplicates?
    }

    public int getIndex() {
        checkInitialized();
        return index;
    }
    
    @NotNull
    public TypeParameterDescriptor copy(@NotNull DeclarationDescriptor newOwner) {
        TypeParameterDescriptor copy = new TypeParameterDescriptor(newOwner, Lists.newArrayList(getAnnotations()), reified, variance, getName(), index);
        copy.upperBounds.addAll(this.upperBounds);
        copy.initialized = this.initialized;
        return copy;
    }
}
