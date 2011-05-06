package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author abreslav
 */
public class MutableClassDescriptor extends MutableDeclarationDescriptor implements ClassDescriptor {
    private ConstructorDescriptor primaryConstructor;
    private final WritableFunctionGroup constructors = new WritableFunctionGroup("<init>");
    private final Set<FunctionDescriptor> functions = Sets.newHashSet();
    private final Set<PropertyDescriptor> properties = Sets.newHashSet();
    private final Set<MutableClassDescriptor> classes = Sets.newHashSet();

    private TypeConstructor typeConstructor;

    private final WritableScope classHeaderScope;
    private final WritableScope writableMemberScope;
    private JetScope unsubstitutedMemberScope;

    public MutableClassDescriptor(@NotNull BindingTrace trace, @NotNull DeclarationDescriptor containingDeclaration, @NotNull JetScope outerScope) {
        super(containingDeclaration);
        this.classHeaderScope = new WritableScopeImpl(outerScope, this, trace.getErrorHandler(), null);
        this.writableMemberScope = new WritableScopeImpl(classHeaderScope, this, trace.getErrorHandler(), new DeclarationDescriptorVisitor<Void, WritableScope>() {
            @Override
            public Void visitPropertyDescriptor(PropertyDescriptor descriptor, WritableScope data) {
                properties.add(descriptor);
                return null;
            }

            @Override
            public Void visitFunctionDescriptor(FunctionDescriptor descriptor, WritableScope data) {
                functions.add(descriptor);
                return null;
            }
        });
        this.unsubstitutedMemberScope = this.writableMemberScope;
    }

    public MutableClassDescriptor(@NotNull DeclarationDescriptor containingDeclaration) {
        super(containingDeclaration);
        this.classHeaderScope = null;
        this.writableMemberScope = null;
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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

    public void addConstructor(@NotNull ConstructorDescriptor constructorDescriptor) {
        assert constructorDescriptor.getContainingDeclaration() == this;
        constructors.addFunction(constructorDescriptor);
    }

    public void addProperty(@NotNull PropertyDescriptor propertyDescriptor) {
        properties.add(propertyDescriptor);
    }

    @NotNull
    public Set<PropertyDescriptor> getProperties() {
        return properties;
    }

    public void addFunction(@NotNull FunctionDescriptor functionDescriptor) {
        functions.add(functionDescriptor);
    }

    @NotNull
    public Set<FunctionDescriptor> getFunctions() {
        return functions;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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

    @Override
    public boolean hasConstructors() {
        return !constructors.isEmpty();
    }
}
