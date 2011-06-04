package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.util.*;

/**
 * @author abreslav
 */
public class MutableClassDescriptor extends MutableDeclarationDescriptor implements ClassDescriptor, NamespaceLike {
    private ConstructorDescriptor primaryConstructor;
    private final WritableFunctionGroup constructors = new WritableFunctionGroup("<init>");
    private final Set<FunctionDescriptor> functions = Sets.newHashSet();
    private final Set<PropertyDescriptor> properties = Sets.newHashSet();
    private List<TypeParameterDescriptor> typeParameters = Lists.newArrayList();
    private Collection<JetType> supertypes = Lists.newArrayList();

    private boolean open;
    private TypeConstructor typeConstructor;
    private final WritableScope scopeForMemberResolution;
    private final WritableScope scopeForMemberLookup;
    // This scope contains type parameters but does not contain inner classes
    private final WritableScope scopeForSupertypeResolution;
    private MutableClassDescriptor classObjectDescriptor;
    private JetType classObjectType;
    private JetType defaultType;

    public MutableClassDescriptor(@NotNull BindingTrace trace, @NotNull DeclarationDescriptor containingDeclaration, @NotNull JetScope outerScope) {
        super(containingDeclaration);
        this.scopeForMemberLookup = new WritableScopeImpl(JetScope.EMPTY, this, trace.getErrorHandler()).setDebugName("MemberLookup");
        this.scopeForSupertypeResolution = new WritableScopeImpl(outerScope, this, trace.getErrorHandler()).setDebugName("SupertypeResolution");
        this.scopeForMemberResolution = new WritableScopeImpl(new ChainedScope(this, scopeForMemberLookup, scopeForSupertypeResolution), this, trace.getErrorHandler()).setDebugName("MemberResolution");
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ClassObjectStatus setClassObjectDescriptor(@NotNull MutableClassDescriptor classObjectDescriptor) {
        if (this.classObjectDescriptor != null) return ClassObjectStatus.DUPLICATE;
        this.classObjectDescriptor = classObjectDescriptor;
        return ClassObjectStatus.OK;
    }

    @Nullable
    public MutableClassDescriptor getClassObjectDescriptor() {
        return classObjectDescriptor;
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

    public void addConstructor(@NotNull ConstructorDescriptor constructorDescriptor) {
        assert constructorDescriptor.getContainingDeclaration() == this;
        constructors.addFunction(constructorDescriptor);
        if (defaultType != null) {
            ((ConstructorDescriptorImpl) constructorDescriptor).setReturnType(getDefaultType());
        }
    }

    @Override
    public void addPropertyDescriptor(@NotNull PropertyDescriptor propertyDescriptor) {
        properties.add(propertyDescriptor);
        scopeForMemberLookup.addVariableDescriptor(propertyDescriptor);
    }

    @NotNull
    public Set<PropertyDescriptor> getProperties() {
        return properties;
    }

    @Override
    public void addFunctionDescriptor(@NotNull FunctionDescriptor functionDescriptor) {
        functions.add(functionDescriptor);
        scopeForMemberLookup.addFunctionDescriptor(functionDescriptor);
    }

    @NotNull
    public Set<FunctionDescriptor> getFunctions() {
        return functions;
    }

    @Override
    public NamespaceDescriptorImpl getNamespace(String name) {
        throw new UnsupportedOperationException("Classes do not define namespaces");
    }

    @Override
    public void addNamespace(@NotNull NamespaceDescriptor namespaceDescriptor) {
        throw new UnsupportedOperationException("Classes do not define namespaces");
    }

    @Override
    public void addClassifierDescriptor(@NotNull MutableClassDescriptor classDescriptor) {
        scopeForMemberLookup.addClassifierDescriptor(classDescriptor);
    }

    public void addSupertype(@NotNull JetType supertype) {
        scopeForMemberLookup.importScope(supertype.getMemberScope());
        supertypes.add(supertype);
    }

    public void setTypeParameterDescriptors(List<TypeParameterDescriptor> typeParameters) {
        for (TypeParameterDescriptor typeParameterDescriptor : typeParameters) {
            this.typeParameters.add(typeParameterDescriptor);
            scopeForSupertypeResolution.addTypeParameterDescriptor(typeParameterDescriptor);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void setName(@NotNull String name) {
        super.setName(name);
        scopeForMemberResolution.addLabeledDeclaration(this);
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }

    public void createTypeConstructor() {
        assert typeConstructor == null : typeConstructor;
        this.typeConstructor = new TypeConstructorImpl(
                this,
                Collections.<Annotation>emptyList(), // TODO : pass annotations from the class?
                !open,
                getName(),
                typeParameters,
                supertypes);
        scopeForMemberResolution.setThisType(getDefaultType());
        for (FunctionDescriptor functionDescriptor : constructors.getFunctionDescriptors()) {
            ((ConstructorDescriptorImpl) functionDescriptor).setReturnType(getDefaultType());
        }
    }

    @NotNull
    @Override
    public JetScope getMemberScope(List<TypeProjection> typeArguments) {
        assert typeArguments.size() == typeConstructor.getParameters().size();
        if (typeArguments.isEmpty()) return scopeForMemberLookup;

        List<TypeParameterDescriptor> typeParameters = getTypeConstructor().getParameters();
        Map<TypeConstructor, TypeProjection> substitutionContext = TypeUtils.buildSubstitutionContext(typeParameters, typeArguments);
        return new SubstitutingScope(scopeForMemberLookup, TypeSubstitutor.create(substitutionContext));
    }

    @NotNull
    @Override
    public JetType getDefaultType() {
        if (defaultType == null) {
            defaultType = TypeUtils.makeUnsubstitutedType(this, scopeForMemberLookup);
        }
        return defaultType;
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
    public JetScope getScopeForSupertypeResolution() {
        return scopeForSupertypeResolution;
    }

    @NotNull
    public JetScope getScopeForMemberLookup() {
        return scopeForMemberLookup;
    }

    @NotNull
    public JetScope getScopeForMemberResolution() {
        return scopeForMemberResolution;
    }

    @NotNull
    @Override
    public ClassDescriptor substitute(TypeSubstitutor substitutor) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public JetType getClassObjectType() {
        if (classObjectType == null && classObjectDescriptor != null) {
            classObjectType = classObjectDescriptor.getDefaultType();
        }
        return classObjectType;
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
    public boolean hasConstructors() {
        return !constructors.isEmpty();
    }

    @Override
    public String toString() {
        return DescriptorRenderer.TEXT_FOR_DEBUG.render(this) + "[" + getClass().getCanonicalName() + "@" + System.identityHashCode(this) + "]";
    }
}
