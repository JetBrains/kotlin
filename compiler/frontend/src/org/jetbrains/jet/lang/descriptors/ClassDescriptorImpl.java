package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.SubstitutingScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ClassReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class ClassDescriptorImpl extends DeclarationDescriptorImpl implements ClassDescriptor {
    private TypeConstructor typeConstructor;

    private JetScope memberDeclarations;
    private FunctionGroup constructors;
    private ConstructorDescriptor primaryConstructor;
    private JetType superclassType;
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
                                            @NotNull FunctionGroup constructors,
                                            @Nullable ConstructorDescriptor primaryConstructor) {
        return initialize(sealed, typeParameters, supertypes, memberDeclarations, constructors, primaryConstructor, getClassType(supertypes));
    }

    public final ClassDescriptorImpl initialize(boolean sealed,
                                                @NotNull List<TypeParameterDescriptor> typeParameters,
                                                @NotNull Collection<JetType> supertypes,
                                                @NotNull JetScope memberDeclarations,
                                                @NotNull FunctionGroup constructors,
                                                @Nullable ConstructorDescriptor primaryConstructor,
                                                @Nullable JetType superclassType) {
        this.typeConstructor = new TypeConstructorImpl(this, getAnnotations(), sealed, getName(), typeParameters, supertypes);
        this.memberDeclarations = memberDeclarations;
        this.constructors = constructors;
        this.primaryConstructor = primaryConstructor;
        this.superclassType = superclassType;
//        assert !constructors.isEmpty() || primaryConstructor == null;
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
    public JetType getSuperclassType() {
        return superclassType;
    }

    @NotNull
    @Override
    public JetType getDefaultType() {
        return TypeUtils.makeUnsubstitutedType(this, memberDeclarations);
    }

    @NotNull
    @Override
    public FunctionGroup getConstructors() {
//        assert typeArguments.size() == getTypeConstructor().getParameters().size() : "Argument list length mismatch for " + getName();
//        if (typeArguments.size() == 0) {
//            return constructors;
//        }
//        Map<TypeConstructor, TypeProjection> substitutionContext = TypeUtils.buildSubstitutionContext(getTypeConstructor().getParameters(), typeArguments);
        return constructors;// LazySubstitutingFunctionGroup(TypeSubstitutor.create(substitutionContext), constructors);
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
    public ReceiverDescriptor getImplicitReceiver() {
        if (implicitReceiver == null) {
            implicitReceiver = new ClassReceiver(this);
        }
        return implicitReceiver;
    }
}
