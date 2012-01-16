package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.SubstitutingScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ClassReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.*;

import java.util.*;

/**
 * @author abreslav
 */
public class ClassDescriptorImpl extends DeclarationDescriptorImpl implements ClassDescriptor {
    private TypeConstructor typeConstructor;

    private JetScope memberDeclarations;
    private Set<ConstructorDescriptor> constructors;
    private ConstructorDescriptor primaryConstructor;
    private ReceiverDescriptor implicitReceiver;

    public ClassDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<AnnotationDescriptor> annotations,
            @NotNull String name) {
        super(containingDeclaration, annotations, name);
    }

    public final ClassDescriptorImpl initialize(boolean sealed,
                                            @NotNull List<TypeParameterDescriptor> typeParameters,
                                            @NotNull Collection<JetType> supertypes,
                                            @NotNull JetScope memberDeclarations,
                                            @NotNull Set<ConstructorDescriptor> constructors,
                                            @Nullable ConstructorDescriptor primaryConstructor) {
        return initialize(sealed, typeParameters, supertypes, memberDeclarations, constructors, primaryConstructor, getClassType(supertypes));
    }

    public final ClassDescriptorImpl initialize(boolean sealed,
                                                @NotNull List<TypeParameterDescriptor> typeParameters,
                                                @NotNull Collection<JetType> supertypes,
                                                @NotNull JetScope memberDeclarations,
                                                @NotNull Set<ConstructorDescriptor> constructors,
                                                @Nullable ConstructorDescriptor primaryConstructor,
                                                @Nullable JetType superclassType) {
        this.typeConstructor = new TypeConstructorImpl(this, getAnnotations(), sealed, getName(), typeParameters, supertypes);
        this.memberDeclarations = memberDeclarations;
        this.constructors = constructors;
        this.primaryConstructor = primaryConstructor;
        return this;
    }

    @NotNull
    private JetType getClassType(@NotNull Collection<JetType> types) {
        for (JetType type : types) {
            ClassDescriptor classDescriptor = TypeUtils.getClassDescriptor(type);
            if (classDescriptor != null) {
                return type;
            }
        }
        return JetStandardClasses.getAnyType();
    }

    public void setPrimaryConstructor(@NotNull ConstructorDescriptor primaryConstructor) {
        this.primaryConstructor = primaryConstructor;
    }

    @Override
    @NotNull
    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }

    @Override
    @NotNull
    public JetScope getMemberScope(List<TypeProjection> typeArguments) {
        assert typeArguments.size() == typeConstructor.getParameters().size() : typeArguments;
        if (typeConstructor.getParameters().isEmpty()) {
            return  memberDeclarations;
        }
        Map<TypeConstructor, TypeProjection> substitutionContext = TypeUtils.buildSubstitutionContext(typeConstructor.getParameters(), typeArguments);
        return new SubstitutingScope(memberDeclarations, TypeSubstitutor.create(substitutionContext));
    }

    @NotNull
    @Override
    public JetType getDefaultType() {
        return TypeUtils.makeUnsubstitutedType(this, memberDeclarations);
    }

    @NotNull
    @Override
    public Set<ConstructorDescriptor> getConstructors() {
        return constructors;
    }

    @NotNull
    @Override
    public ClassDescriptor substitute(TypeSubstitutor substitutor) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public JetType getClassObjectType() {
        return null;
    }

    @Override
    public ClassDescriptor getClassObjectDescriptor() {
        return null;
    }

    @NotNull
    @Override
    public ClassKind getKind() {
        return ClassKind.CLASS;
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
    public ConstructorDescriptor getUnsubstitutedPrimaryConstructor() {
        return primaryConstructor;
    }

    @Override
    public boolean hasConstructors() {
        return !constructors.isEmpty();
    }

    @Override
    @NotNull
    public Modality getModality() {
        return Modality.FINAL;
    }

    @NotNull
    @Override
    public Visibility getVisibility() {
        return Visibility.PUBLIC;
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
    public ClassDescriptor getInnerClassOrObject(String name) {
        return null;
    }

    @NotNull
    @Override
    public Collection<ClassDescriptor> getInnerClassesAndObjects() {
        return Collections.emptyList();
    }
}
