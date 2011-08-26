package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.JetScope;
import org.jetbrains.jet.lang.resolve.SubstitutingScope;
import org.jetbrains.jet.lang.types.*;

import java.util.Collection;
import java.util.List;

/**
 * @author abreslav
 */
public class LazySubstitutingClassDescriptor implements ClassDescriptor {

    private final ClassDescriptor original;
    private final TypeSubstitutor originalSubstitutor;
    private TypeSubstitutor newSubstitutor;
    private List<TypeParameterDescriptor> typeParameters;
    private TypeConstructor typeConstructor;

    public LazySubstitutingClassDescriptor(ClassDescriptor descriptor, TypeSubstitutor substitutor) {
        this.original = descriptor;
        this.originalSubstitutor = substitutor;
    }

    private TypeSubstitutor getSubstitutor() {
        if (newSubstitutor == null) {
            if (originalSubstitutor.isEmpty()) {
                newSubstitutor = originalSubstitutor;
            }
            else {
                typeParameters = Lists.newArrayList();
                newSubstitutor = DescriptorSubstitutor.substituteTypeParameters(original.getTypeConstructor().getParameters(), originalSubstitutor, this, typeParameters);
            }
        }
        return newSubstitutor;
    }
    
    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        TypeConstructor originalTypeConstructor = original.getTypeConstructor();
        if (originalSubstitutor.isEmpty()) {
            return originalTypeConstructor;
        }

        if (typeConstructor == null) {
            TypeSubstitutor substitutor = getSubstitutor();

            Collection<JetType> supertypes = Lists.newArrayList();
            for (JetType supertype : originalTypeConstructor.getSupertypes()) {
                supertypes.add(substitutor.substitute(supertype, Variance.INVARIANT));
            }

            typeConstructor = new TypeConstructorImpl(
                    this,
                    originalTypeConstructor.getAnnotations(),
                    originalTypeConstructor.isSealed(),
                    originalTypeConstructor.toString(),
                    typeParameters,
                    supertypes
            );
        }

        return typeConstructor;
    }

    @NotNull
    @Override
    public JetScope getMemberScope(List<TypeProjection> typeArguments) {
        JetScope memberScope = original.getMemberScope(typeArguments);
        if (originalSubstitutor.isEmpty()) {
            return memberScope;
        }
        return new SubstitutingScope(memberScope, getSubstitutor());
    }

    @NotNull
    @Override
    public JetType getDefaultType() {
        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    @Override
    public FunctionGroup getConstructors() {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public ConstructorDescriptor getUnsubstitutedPrimaryConstructor() {
        return original.getUnsubstitutedPrimaryConstructor();
    }

    @Override
    public boolean hasConstructors() {
        return original.hasConstructors();
    }

    @Override
    public List<AnnotationDescriptor> getAnnotations() {
        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    @Override
    public String getName() {
        return original.getName();
    }

    @NotNull
    @Override
    public DeclarationDescriptor getOriginal() {
        return original.getOriginal();
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return original.getContainingDeclaration();
    }

    @NotNull
    @Override
    public ClassDescriptor substitute(TypeSubstitutor substitutor) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public JetType getClassObjectType() {
        return original.getClassObjectType();
    }

    @Override
    public boolean isObject() {
        return original.isObject();
    }

    @Override
    public boolean isClassObjectAValue() {
        return original.isClassObjectAValue();
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitClassDescriptor(this, data);
    }

    @Override
    public void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor) {
        throw new UnsupportedOperationException(); // TODO
    }
}
