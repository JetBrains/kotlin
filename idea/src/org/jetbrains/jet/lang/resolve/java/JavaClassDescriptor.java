package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.JetScope;
import org.jetbrains.jet.lang.resolve.SubstitutingScope;
import org.jetbrains.jet.lang.types.*;

import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class JavaClassDescriptor extends MutableDeclarationDescriptor implements ClassDescriptor {

    private TypeConstructor typeConstructor;
    private JavaClassMembersScope unsubstitutedMemberScope;
    private final WritableFunctionGroup constructors = new WritableFunctionGroup("<init>");

    public JavaClassDescriptor(DeclarationDescriptor containingDeclaration) {
        super(containingDeclaration);
    }

    public void setTypeConstructor(TypeConstructor typeConstructor) {
        this.typeConstructor = typeConstructor;
    }

    public void setUnsubstitutedMemberScope(JavaClassMembersScope memberScope) {
        this.unsubstitutedMemberScope = memberScope;
    }

    public void addConstructor(ConstructorDescriptor constructorDescriptor) {
        this.constructors.addFunction(constructorDescriptor);
    }

    private TypeSubstitutor createTypeSubstitutor(List<TypeProjection> typeArguments) {
        List<TypeParameterDescriptor> parameters = getTypeConstructor().getParameters();
        Map<TypeConstructor, TypeProjection> context = TypeUtils.buildSubstitutionContext(parameters, typeArguments);
        return TypeSubstitutor.create(context);
    }

    @NotNull
    @Override
    public JetScope getMemberScope(List<TypeProjection> typeArguments) {
        assert typeArguments.size() == typeConstructor.getParameters().size();

        if (typeArguments.isEmpty()) return unsubstitutedMemberScope;

        TypeSubstitutor substitutor = createTypeSubstitutor(typeArguments);
        return new SubstitutingScope(unsubstitutedMemberScope, substitutor);
    }

    @NotNull
    @Override
    public FunctionGroup getConstructors(List<TypeProjection> typeArguments) {
        assert typeArguments.size() == typeConstructor.getParameters().size();
        if (typeArguments.isEmpty()) return constructors;
        return new LazySubstitutingFunctionGroup(createTypeSubstitutor(typeArguments), constructors);
    }

    @Override
    public ConstructorDescriptor getUnsubstitutedPrimaryConstructor() {
        return null;
    }

    @Override
    public boolean hasConstructors() {
        return constructors.isEmpty();
    }

    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }

    @NotNull
    @Override
    public JetType getDefaultType() {
        return TypeUtils.makeUnsubstitutedType(this, unsubstitutedMemberScope);
    }

    @NotNull
    @Override
    public ClassDescriptor substitute(TypeSubstitutor substitutor) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitClassDescriptor(this, data);
    }
}
