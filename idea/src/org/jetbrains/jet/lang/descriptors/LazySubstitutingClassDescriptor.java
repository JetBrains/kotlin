package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
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
    private TypeConstructor typeConstructor;
    private final TypeSubstitutor substitutor;

    public LazySubstitutingClassDescriptor(ClassDescriptor descriptor, TypeSubstitutor substitutor) {
        this.original = descriptor;
        this.substitutor = substitutor;
    }

    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        TypeConstructor originalTypeConstructor = original.getTypeConstructor();
        if (substitutor.isEmpty()) {
            return originalTypeConstructor;
        }

        if (typeConstructor == null) {
            List<TypeParameterDescriptor> parameters = Lists.newArrayList();
            for (TypeParameterDescriptor parameterDescriptor : originalTypeConstructor.getParameters()) {
                parameters.add(parameterDescriptor.substitute(substitutor));
            }
            Collection<JetType> supertypes = Lists.newArrayList();
            for (JetType supertype : originalTypeConstructor.getSupertypes()) {
                supertypes.add(substitutor.substitute(supertype, Variance.INVARIANT));
            }

            typeConstructor = new TypeConstructorImpl(
                    this,
                    originalTypeConstructor.getAnnotations(),
                    originalTypeConstructor.isSealed(),
                    originalTypeConstructor.toString(),
                    parameters,
                    supertypes
            );
        }

        return typeConstructor;
    }

    @NotNull
    @Override
    public JetScope getMemberScope(List<TypeProjection> typeArguments) {
        JetScope memberScope = original.getMemberScope(typeArguments);
        if (substitutor.isEmpty()) {
            return memberScope;
        }
        return new SubstitutingScope(memberScope, substitutor); // TODO : compose substitutors
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
    public List<Annotation> getAnnotations() {
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
