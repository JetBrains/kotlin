package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.jet.lang.resolve.AbstractScopeAdapter;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.TraceBasedRedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.*;
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
    private final Set<ConstructorDescriptor> constructors = Sets.newLinkedHashSet();
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
    private final WritableScope scopeForInitializers; //contains members + primary constructor value parameters + map for backing fields

    private MutableClassDescriptor classObjectDescriptor;
    private JetType classObjectType;
    private JetType defaultType;
    private final ClassKind kind;
    private JetType superclassType;
    private ClassReceiver implicitReceiver;

    public MutableClassDescriptor(@NotNull BindingTrace trace, @NotNull DeclarationDescriptor containingDeclaration, @NotNull JetScope outerScope, ClassKind kind) {
        super(containingDeclaration);
        TraceBasedRedeclarationHandler redeclarationHandler = new TraceBasedRedeclarationHandler(trace);
        this.scopeForMemberLookup = new WritableScopeImpl(JetScope.EMPTY, this, redeclarationHandler).setDebugName("MemberLookup").changeLockLevel(WritableScope.LockLevel.BOTH);
        this.scopeForSupertypeResolution = new WritableScopeImpl(outerScope, this, redeclarationHandler).setDebugName("SupertypeResolution").changeLockLevel(WritableScope.LockLevel.BOTH);
        this.scopeForMemberResolution = new WritableScopeImpl(scopeForSupertypeResolution, this, redeclarationHandler).setDebugName("MemberResolution").changeLockLevel(WritableScope.LockLevel.BOTH);
        this.scopeForInitializers = new WritableScopeImpl(scopeForMemberResolution, this, redeclarationHandler).setDebugName("Initializers").changeLockLevel(WritableScope.LockLevel.BOTH);
        this.kind = kind;
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ClassObjectStatus setClassObjectDescriptor(@NotNull MutableClassDescriptor classObjectDescriptor) {
        if (this.classObjectDescriptor != null) return ClassObjectStatus.DUPLICATE;
        if (!isStatic(this.getContainingDeclaration())) {
            return ClassObjectStatus.NOT_ALLOWED;
        }
        assert classObjectDescriptor.getKind() == ClassKind.OBJECT;
        this.classObjectDescriptor = classObjectDescriptor;

        // Members of the class object are accessible from the class
        // The scope must be lazy, because classObjectDescriptor may not by fully built yet
        scopeForMemberResolution.importScope(new AbstractScopeAdapter() {
            @NotNull
            @Override
            protected JetScope getWorkerScope() {
                return MutableClassDescriptor.this.classObjectDescriptor.getDefaultType().getMemberScope();
            }

            @NotNull
            @Override
            public ReceiverDescriptor getImplicitReceiver() {
                return MutableClassDescriptor.this.classObjectDescriptor.getImplicitReceiver();
            }
        }
        );
        return ClassObjectStatus.OK;
    }

    private static boolean isStatic(DeclarationDescriptor declarationDescriptor) {
        if (declarationDescriptor instanceof NamespaceDescriptor) {
            return true;
        } else if (declarationDescriptor instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
            return classDescriptor.getKind() == ClassKind.OBJECT || classDescriptor.getKind() == ClassKind.ENUM_CLASS;
        } else {
            return false;
        }
    }

    @Nullable
    public MutableClassDescriptor getClassObjectDescriptor() {
        return classObjectDescriptor;
    }

    public void setPrimaryConstructor(@NotNull ConstructorDescriptor constructorDescriptor, BindingTrace trace) {
        assert this.primaryConstructor == null : "Primary constructor assigned twice " + this;
        this.primaryConstructor = constructorDescriptor;
        addConstructor(constructorDescriptor, trace);
    }

    @Override
    @Nullable
    public ConstructorDescriptor getUnsubstitutedPrimaryConstructor() {
        return primaryConstructor;
    }

    public void addConstructor(@NotNull ConstructorDescriptor constructorDescriptor, BindingTrace trace) {
        assert constructorDescriptor.getContainingDeclaration() == this;
        constructors.add(constructorDescriptor);
        if (defaultType != null) {
            ((ConstructorDescriptorImpl) constructorDescriptor).setReturnType(getDefaultType());
        }
        if (constructorDescriptor.isPrimary()) {
            for (ValueParameterDescriptor valueParameterDescriptor : constructorDescriptor.getValueParameters()) {
                JetParameter parameter = (JetParameter) trace.getBindingContext().get(BindingContext.DESCRIPTOR_TO_DECLARATION, valueParameterDescriptor);
                assert parameter != null;
                if (parameter.getValOrVarNode() == null || !constructorDescriptor.isPrimary()) {
                    scopeForInitializers.addVariableDescriptor(valueParameterDescriptor);
                }
            }
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
        scopeForSupertypeResolution.changeLockLevel(WritableScope.LockLevel.READING);
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
    public Set<ConstructorDescriptor> getConstructors() {
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
    public JetScope getScopeForInitializers() {
        return scopeForInitializers;
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
    
    public void lockScopes() {
        scopeForSupertypeResolution.changeLockLevel(WritableScope.LockLevel.READING);
        scopeForMemberLookup.changeLockLevel(WritableScope.LockLevel.READING);
        scopeForMemberResolution.changeLockLevel(WritableScope.LockLevel.READING);
        scopeForInitializers.changeLockLevel(WritableScope.LockLevel.READING);
        if (classObjectDescriptor != null) {
            classObjectDescriptor.lockScopes();
        }
    }

}
