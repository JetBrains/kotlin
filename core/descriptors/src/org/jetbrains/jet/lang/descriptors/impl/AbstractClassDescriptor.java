package org.jetbrains.jet.lang.descriptors.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptorVisitor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.SubstitutingScope;
import org.jetbrains.jet.lang.types.*;

import java.util.List;
import java.util.Map;

public abstract class AbstractClassDescriptor implements ClassDescriptor {
    protected final Name name;
    protected volatile JetType defaultType;

    public AbstractClassDescriptor(@NotNull Name name) {
        this.name = name;
    }

    @NotNull
    @Override
    public Name getName() {
        return name;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getOriginal() {
        return this;
    }

    protected abstract JetScope getScopeForMemberLookup();

    @NotNull
    @Override
    public JetScope getMemberScope(List<TypeProjection> typeArguments) {
        assert typeArguments.size() == getTypeConstructor().getParameters().size() : "Illegal number of type arguments: expected "
                                                                                     + getTypeConstructor().getParameters().size() + " but was " + typeArguments.size()
                                                                                     + " for " + getTypeConstructor() + " " + getTypeConstructor().getParameters();
        if (typeArguments.isEmpty()) return getScopeForMemberLookup();

        List<TypeParameterDescriptor> typeParameters = getTypeConstructor().getParameters();
        Map<TypeConstructor, TypeProjection> substitutionContext = SubstitutionUtils.buildSubstitutionContext(typeParameters, typeArguments);

        // Unsafe substitutor is OK, because no recursion can hurt us upon a trivial substitution:
        // all the types are written explicitly in the code already, they can not get infinite.
        // One exception is *-projections, but they need to be handled separately anyways.
        TypeSubstitutor substitutor = TypeSubstitutor.createUnsafe(substitutionContext);
        return new SubstitutingScope(getScopeForMemberLookup(), substitutor);
    }

    @NotNull
    @Override
    public ClassDescriptor substitute(@NotNull TypeSubstitutor substitutor) {
        if (substitutor.isEmpty()) {
            return this;
        }
        return new LazySubstitutingClassDescriptor(this, substitutor);
    }

    @NotNull
    @Override
    public JetType getDefaultType() {
        if (defaultType == null) {
            defaultType = TypeUtils.makeUnsubstitutedType(this, getScopeForMemberLookup());
        }
        return defaultType;
    }

    @Override
    public void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor) {
        visitor.visitClassDescriptor(this, null);
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitClassDescriptor(this, data);
    }
}
