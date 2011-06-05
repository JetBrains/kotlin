package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.JetScope;
import org.jetbrains.jet.lang.resolve.SubstitutingScope;
import org.jetbrains.jet.lang.types.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class ClassDescriptorImpl extends DeclarationDescriptorImpl implements ClassDescriptor {
    private TypeConstructor typeConstructor;

    private JetScope memberDeclarations;
    private FunctionGroup constructors;
    private ConstructorDescriptor primaryConstructor;

    public ClassDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<Annotation> annotations,
            @NotNull String name) {
        super(containingDeclaration, annotations, name);
    }

    public final ClassDescriptorImpl initialize(boolean sealed,
                                                @NotNull List<TypeParameterDescriptor> typeParameters,
                                                @NotNull Collection<JetType> superclasses,
                                                @NotNull JetScope memberDeclarations,
                                                @NotNull FunctionGroup constructors,
                                                @Nullable ConstructorDescriptor primaryConstructor) {
        this.typeConstructor = new TypeConstructorImpl(this, getAnnotations(), sealed, getName(), typeParameters, superclasses);
        this.memberDeclarations = memberDeclarations;
        this.constructors = constructors;
        this.primaryConstructor = primaryConstructor;
        assert !constructors.isEmpty() || primaryConstructor == null;
        return this;
    }

    @Override
    @NotNull
    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }

    @Override
    @NotNull
    public JetScope getMemberScope(List<TypeProjection> typeArguments) {
        assert typeArguments.size() == typeConstructor.getParameters().size() : typeArguments;
        if (typeConstructor.getParameters().isEmpty()) {
            return  memberDeclarations;
        }
        Map<TypeConstructor, TypeProjection> substitutionContext = TypeUtils.buildSubstitutionContext(typeConstructor.getParameters(), typeArguments);
        return new SubstitutingScope(memberDeclarations, TypeSubstitutor.create(substitutionContext));
    }

    @NotNull
    @Override
    public JetType getDefaultType() {
        return TypeUtils.makeUnsubstitutedType(this, memberDeclarations);
    }

    @NotNull
    @Override
    public FunctionGroup getConstructors(List<TypeProjection> typeArguments) {
        assert typeArguments.size() == getTypeConstructor().getParameters().size() : "Argument list length mismatch for " + getName();
        if (typeArguments.size() == 0) {
            return constructors;
        }
        Map<TypeConstructor, TypeProjection> substitutionContext = TypeUtils.buildSubstitutionContext(getTypeConstructor().getParameters(), typeArguments);
        return new LazySubstitutingFunctionGroup(TypeSubstitutor.create(substitutionContext), constructors);
    }

    @NotNull
    @Override
    public ClassDescriptor substitute(TypeSubstitutor substitutor) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public JetType getClassObjectType() {
        return null;
    }

    @Override
    public boolean isClassObjectAValue() {
        return true;
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitClassDescriptor(this, data);
    }

    @Override
    public ConstructorDescriptor getUnsubstitutedPrimaryConstructor() {
        return primaryConstructor;
    }

    @Override
    public boolean hasConstructors() {
        return !constructors.isEmpty();
    }
}