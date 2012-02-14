package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.SubstitutingScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ClassReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.util.*;

/**
 * @author Stepan Koltsov
 */
public class MutableClassDescriptorLite extends MutableDeclarationDescriptor implements ClassDescriptor, NamespaceLike {
    private ConstructorDescriptor primaryConstructor;
    private final Set<ConstructorDescriptor> constructors = Sets.newLinkedHashSet();

    private final List<AnnotationDescriptor> annotations = Lists.newArrayList();

    private Map<String, ClassDescriptor> innerClassesAndObjects = Maps.newHashMap();

    private List<TypeParameterDescriptor> typeParameters;
    private Collection<JetType> supertypes = Lists.newArrayList();

    private TypeConstructor typeConstructor;

    private Modality modality;
    private Visibility visibility;

    private MutableClassDescriptorLite classObjectDescriptor;
    private JetType classObjectType;
    private JetType defaultType;
    private final ClassKind kind;

    private JetScope scopeForMemberLookup;

    private ClassReceiver implicitReceiver;

    public MutableClassDescriptorLite(DeclarationDescriptor containingDeclaration, ClassKind kind) {
        super(containingDeclaration);
        this.kind = kind;
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



    @Override
    public ClassObjectStatus setClassObjectDescriptor(@NotNull MutableClassDescriptorLite classObjectDescriptor) {
        if (this.classObjectDescriptor != null) return ClassObjectStatus.DUPLICATE;
        if (!isStatic(this.getContainingDeclaration())) {
            return ClassObjectStatus.NOT_ALLOWED;
        }
        assert classObjectDescriptor.getKind() == ClassKind.OBJECT;
        this.classObjectDescriptor = classObjectDescriptor;

        return ClassObjectStatus.OK;
    }





    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }

    public void setScopeForMemberLookup(JetScope scopeForMemberLookup) {
        this.scopeForMemberLookup = scopeForMemberLookup;
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
        for (FunctionDescriptor functionDescriptor : constructors) {
            ((ConstructorDescriptorImpl) functionDescriptor).setReturnType(getDefaultType());
        }
    }

    public WritableScope getScopeForMemberLookupAsWritableScope() {
        // hack
        return (WritableScope) scopeForMemberLookup;
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
    public Set<ConstructorDescriptor> getConstructors() {
        return constructors;
    }

    @NotNull
    public JetScope getScopeForMemberLookup() {
        return scopeForMemberLookup;
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
    public boolean hasConstructors() {
        return !constructors.isEmpty();
    }

    @NotNull
    @Override
    public ClassKind getKind() {
        return kind;
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

    public Collection<JetType> getSupertypes() {
        return supertypes;
    }

    public void setSupertypes(@NotNull Collection<JetType> supertypes) {
        this.supertypes = supertypes;
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

    public void addConstructor(@NotNull ConstructorDescriptor constructorDescriptor, @Nullable BindingTrace trace) {
        assert constructorDescriptor.getContainingDeclaration() == this;
        constructors.add(constructorDescriptor);
        if (defaultType != null) {
            ((ConstructorDescriptorImpl) constructorDescriptor).setReturnType(getDefaultType());
        }
    }

    @NotNull
    @Override
    public JetType getDefaultType() {
        if (defaultType == null) {
            defaultType = TypeUtils.makeUnsubstitutedType(this, scopeForMemberLookup);
        }
        return defaultType;
    }

    @Override
    @Nullable
    public MutableClassDescriptorLite getClassObjectDescriptor() {
        return classObjectDescriptor;
    }

    @Override
    public void addPropertyDescriptor(@NotNull PropertyDescriptor propertyDescriptor) {
        getScopeForMemberLookupAsWritableScope().addPropertyDescriptor(propertyDescriptor);
    }

    @Override
    public void addFunctionDescriptor(@NotNull NamedFunctionDescriptor functionDescriptor) {
        getScopeForMemberLookupAsWritableScope().addFunctionDescriptor(functionDescriptor);
    }

    @Override
    public void addNamespace(@NotNull NamespaceDescriptor namespaceDescriptor) {
        throw new UnsupportedOperationException("Classes do not define namespaces");
    }

    @Override
    public NamespaceDescriptorImpl getNamespace(String name) {
        throw new UnsupportedOperationException("Classes do not define namespaces");
    }

    @Override
    public void addClassifierDescriptor(@NotNull MutableClassDescriptorLite classDescriptor) {
        getScopeForMemberLookupAsWritableScope().addClassifierDescriptor(classDescriptor);
        innerClassesAndObjects.put(classDescriptor.getName(), classDescriptor);
    }

    @Override
    @Nullable
    public ClassDescriptor getInnerClassOrObject(String name) {
        return innerClassesAndObjects.get(name);
    }

    @Override
    @NotNull
    public Collection<ClassDescriptor> getInnerClassesAndObjects() {
        return innerClassesAndObjects.values();
    }

    @Override
    public void addObjectDescriptor(@NotNull MutableClassDescriptorLite objectDescriptor) {
        getScopeForMemberLookupAsWritableScope().addObjectDescriptor(objectDescriptor);
        innerClassesAndObjects.put(objectDescriptor.getName(), objectDescriptor);
    }

    public void addSupertype(@NotNull JetType supertype) {
        if (!ErrorUtils.isErrorType(supertype)) {
            if (!(supertype.getConstructor().getDeclarationDescriptor() instanceof ClassDescriptor)) {
                throw new IllegalStateException();
            }
            supertypes.add(supertype);
        }
    }

    public void setTypeParameterDescriptors(List<TypeParameterDescriptor> typeParameters) {
        if (this.typeParameters != null) {
            throw new IllegalStateException();
        }
        this.typeParameters = new ArrayList<TypeParameterDescriptor>();
        for (TypeParameterDescriptor typeParameterDescriptor : typeParameters) {
            this.typeParameters.add(typeParameterDescriptor);
        }
    }


    public void lockScopes() {
        getScopeForMemberLookupAsWritableScope().changeLockLevel(WritableScope.LockLevel.READING);
        if (classObjectDescriptor != null) {
            classObjectDescriptor.lockScopes();
        }
    }


    @NotNull
    @Override
    public ReceiverDescriptor getImplicitReceiver() {
        if (implicitReceiver == null) {
            implicitReceiver = new ClassReceiver(this);
        }
        return implicitReceiver;
    }


    @Override
    public String toString() {
        return DescriptorRenderer.TEXT.render(this) + "[" + getClass().getCanonicalName() + "@" + System.identityHashCode(this) + "]";
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitClassDescriptor(this, data);
    }

    @Override
    public List<AnnotationDescriptor> getAnnotations() {
        return annotations;
    }
}
