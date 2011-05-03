package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.types.*;

import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class MutableClassDescriptor extends MutableDeclarationDescriptor implements ClassDescriptor {
    private final WritableScope classHeaderScope;
    private final WritableScope writableMemberScope;
    private final WritableFunctionGroup constructors = new WritableFunctionGroup("<init>");
    private ConstructorDescriptor primaryConstructor;

    private TypeConstructor typeConstructor;
    private JetScope unsubstitutedMemberScope;

    public MutableClassDescriptor(@NotNull JetSemanticServices semanticServices, @NotNull DeclarationDescriptor containingDeclaration, @NotNull JetScope outerScope) {
        super(containingDeclaration);
        this.classHeaderScope = semanticServices.createWritableScope(outerScope, this);
        this.writableMemberScope = semanticServices.createWritableScope(classHeaderScope, this);
        this.unsubstitutedMemberScope = this.writableMemberScope;
    }

    public MutableClassDescriptor(@NotNull DeclarationDescriptor containingDeclaration) {
        super(containingDeclaration);
        this.classHeaderScope = null;
        this.writableMemberScope = null;
    }

    public void setUnsubstitutedMemberScope(@NotNull JetScope unsubstitutedMemberScope) {
        assert writableMemberScope == null;
        this.unsubstitutedMemberScope = unsubstitutedMemberScope;
    }

    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }

    public void setTypeConstructor(@NotNull TypeConstructor typeConstructor) {
        this.typeConstructor = typeConstructor;
    }

    @NotNull
    @Override
    public JetScope getMemberScope(List<TypeProjection> typeArguments) {
        assert typeArguments.size() == typeConstructor.getParameters().size();
        assert unsubstitutedMemberScope != null;
        if (typeArguments.isEmpty()) return unsubstitutedMemberScope;

        List<TypeParameterDescriptor> typeParameters = getTypeConstructor().getParameters();
        Map<TypeConstructor, TypeProjection> substitutionContext = TypeUtils.buildSubstitutionContext(typeParameters, typeArguments);
        return new SubstitutingScope(unsubstitutedMemberScope, TypeSubstitutor.create(substitutionContext));
    }

    @NotNull
    @Override
    public JetType getDefaultType() {
        return TypeUtils.makeUnsubstitutedType(this, unsubstitutedMemberScope);
    }

    public void addConstructor(@NotNull ConstructorDescriptor constructorDescriptor) {
        assert constructorDescriptor.getContainingDeclaration() == this;
        constructors.addFunction(constructorDescriptor);
    }

    @NotNull
    @Override
    public FunctionGroup getConstructors(List<TypeProjection> typeArguments) {
        // TODO : Duplicates ClassDescriptorImpl
        assert typeArguments.size() == getTypeConstructor().getParameters().size();

        if (typeArguments.size() == 0) {
            return constructors;
        }
        Map<TypeConstructor, TypeProjection> substitutionContext = TypeUtils.buildSubstitutionContext(getTypeConstructor().getParameters(), typeArguments);
        return new LazySubstitutingFunctionGroup(TypeSubstitutor.create(substitutionContext), constructors);
    }

    @NotNull
    public WritableScope getWritableUnsubstitutedMemberScope() {
        return writableMemberScope;
    }

    @NotNull
    public WritableScope getClassHeaderScope() {
        return classHeaderScope;
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

    public void setPrimaryConstructor(@NotNull ConstructorDescriptor constructorDescriptor) {
        assert this.primaryConstructor == null : "Primary constructor assigned twice " + this;
        this.primaryConstructor = constructorDescriptor;
        addConstructor(constructorDescriptor);
    }

    @Override
    @Nullable
    public ConstructorDescriptor getUnsubstitutedPrimaryConstructor() {
        return primaryConstructor;
    }

    @Override
    public boolean hasConstructors() {
        return !constructors.isEmpty();
    }
}
