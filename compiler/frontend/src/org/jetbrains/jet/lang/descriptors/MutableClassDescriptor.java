package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.SubstitutingScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ClassReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.util.*;

/**
 * @author abreslav
 */
public class MutableClassDescriptor extends MutableDeclarationDescriptor implements ClassDescriptor, NamespaceLike {
    private ConstructorDescriptor primaryConstructor;
    private final Set<FunctionDescriptor> constructors = Sets.newLinkedHashSet();
    private final Set<CallableMemberDescriptor> callableMembers = Sets.newHashSet();
    private final Set<PropertyDescriptor> properties = Sets.newHashSet();
    private final Set<FunctionDescriptor> functions = Sets.newHashSet();
    private List<TypeParameterDescriptor> typeParameters = Lists.newArrayList();
    private Collection<JetType> supertypes = Lists.newArrayList();

    private Modality modality;
    private Visibility visibility;
    private TypeConstructor typeConstructor;
    private final WritableScope scopeForMemberResolution;
    private final WritableScope scopeForMemberLookup;
    // This scope contains type parameters but does not contain inner classes
    private final WritableScope scopeForSupertypeResolution;
    private MutableClassDescriptor classObjectDescriptor;
    private JetType classObjectType;
    private JetType defaultType;
    private final ClassKind kind;
    private JetType superclassType;
    private ClassReceiver implicitReceiver;

//    public MutableClassDescriptor(@NotNull BindingTrace trace, @NotNull DeclarationDescriptor containingDeclaration, @NotNull JetScope outerScope) {
//        this(trace, containingDeclaration, outerScope, ClassKind.CLASS);
//    }

    public MutableClassDescriptor(@NotNull BindingTrace trace, @NotNull DeclarationDescriptor containingDeclaration, @NotNull JetScope outerScope, ClassKind kind) {
        super(containingDeclaration);
        TraceBasedRedeclarationHandler redeclarationHandler = new TraceBasedRedeclarationHandler(trace);
        this.scopeForMemberLookup = new WritableScopeImpl(JetScope.EMPTY, this, redeclarationHandler).setDebugName("MemberLookup");
        this.scopeForSupertypeResolution = new WritableScopeImpl(outerScope, this, redeclarationHandler).setDebugName("SupertypeResolution");
        this.scopeForMemberResolution = new WritableScopeImpl(scopeForSupertypeResolution, this, redeclarationHandler).setDebugName("MemberResolution");
        this.kind = kind;
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ClassObjectStatus setClassObjectDescriptor(@NotNull MutableClassDescriptor classObjectDescriptor) {
        if (this.classObjectDescriptor != null) return ClassObjectStatus.DUPLICATE;
        assert classObjectDescriptor.getKind() == ClassKind.OBJECT;
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
//        assert constructorDescriptor.getTypeParameters().size() == getTypeConstructor().getValueParameters().size();
        constructors.add(constructorDescriptor);
        if (defaultType != null) {
//            constructorDescriptor.getTypeParameters().addAll(typeParameters);
            ((ConstructorDescriptorImpl) constructorDescriptor).setReturnType(getDefaultType());
        }
    }

    @Override
    public void addPropertyDescriptor(@NotNull PropertyDescriptor propertyDescriptor) {
        properties.add(propertyDescriptor);
        callableMembers.add(propertyDescriptor);
        scopeForMemberLookup.addVariableDescriptor(propertyDescriptor);
        scopeForMemberResolution.addVariableDescriptor(propertyDescriptor);
    }

    @Override
    public void addFunctionDescriptor(@NotNull FunctionDescriptor functionDescriptor) {
        functions.add(functionDescriptor);
        callableMembers.add(functionDescriptor);
        scopeForMemberLookup.addFunctionDescriptor(functionDescriptor);
        scopeForMemberResolution.addFunctionDescriptor(functionDescriptor);
    }

    @NotNull
    public Set<FunctionDescriptor> getFunctions() {
        return functions;
    }

    @NotNull
    public Set<PropertyDescriptor> getProperties() {
        return properties;
    }

    @NotNull
    public Set<CallableMemberDescriptor> getCallableMembers() {
        return callableMembers;
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
        scopeForMemberResolution.addClassifierDescriptor(classDescriptor);
    }

    public void addSupertype(@NotNull JetType supertype) {
        if (!ErrorUtils.isErrorType(supertype)) {
            supertypes.add(supertype);
        }
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

    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }

    public void createTypeConstructor() {
        assert typeConstructor == null : typeConstructor;
        this.typeConstructor = new TypeConstructorImpl(
                this,
                Collections.<AnnotationDescriptor>emptyList(), // TODO : pass annotations from the class?
                !modality.isOverridable(),
                getName(),
                typeParameters,
                supertypes);
        scopeForMemberResolution.setImplicitReceiver(new ClassReceiver(this));
        for (FunctionDescriptor functionDescriptor : constructors) {
            ((ConstructorDescriptorImpl) functionDescriptor).setReturnType(getDefaultType());
        }
    }

    public void addSupertypesToScopeForMemberLookup() {
        for (JetType supertype : supertypes) {
            scopeForMemberLookup.importScope(supertype.getMemberScope());
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
    public Set<FunctionDescriptor> getConstructors() {
        return constructors;
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
        if (substitutor.isEmpty()) {
            return this;
        }
        return new LazySubstitutingClassDescriptor(this, substitutor);
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

    @NotNull
    @Override
    public ClassKind getKind() {
        return kind;
    }

    @NotNull
    @Override
    public JetType getSuperclassType() {
        return superclassType;
    }

    public void setSuperclassType(@NotNull JetType superclassType) {
        this.superclassType = superclassType;
    }


    public void setModality(Modality modality) {
        this.modality = modality;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    @Override
    @NotNull
    public Modality getModality() {
        return modality;
    }

    @NotNull
    @Override
    public Visibility getVisibility() {
        return visibility;
    }

    @Override
    public String toString() {
        return DescriptorRenderer.TEXT.render(this) + "[" + getClass().getCanonicalName() + "@" + System.identityHashCode(this) + "]";
    }

    public Collection<JetType> getSupertypes() {
        return supertypes;
    }

    @NotNull
    @Override
    public ReceiverDescriptor getImplicitReceiver() {
        if (implicitReceiver == null) {
            implicitReceiver = new ClassReceiver(this);
        }
        return implicitReceiver;
    }
}
