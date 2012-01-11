package org.jetbrains.jet.lang.resolve.java;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.SubstitutingScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ClassReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.*;

import java.util.*;

/**
 * @author abreslav
 */
public class JavaClassDescriptor extends MutableDeclarationDescriptor implements ClassDescriptor {

    private TypeConstructor typeConstructor;
    private JavaClassMembersScope unsubstitutedMemberScope;
//    private JetType classObjectType;
    private final Set<ConstructorDescriptor> constructors = Sets.newLinkedHashSet();
    private Modality modality;
    private Visibility visibility;
    private JetType superclassType;
    private final ClassKind kind;
    private ClassReceiver implicitReceiver;


    public JavaClassDescriptor(DeclarationDescriptor containingDeclaration, @NotNull ClassKind kind) {
        super(containingDeclaration);
        this.kind = kind;
    }

    public void setTypeConstructor(TypeConstructor typeConstructor) {
        this.typeConstructor = typeConstructor;
    }

    public void setModality(Modality modality) {
        this.modality = modality;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public void setUnsubstitutedMemberScope(JavaClassMembersScope memberScope) {
        this.unsubstitutedMemberScope = memberScope;
    }

//    public void setClassObjectMemberScope(JavaClassMembersScope memberScope) {
//        classObjectType = new JetTypeImpl(
//                new TypeConstructorImpl(
//                        JavaDescriptorResolver.JAVA_CLASS_OBJECT,
//                        Collections.<AnnotationDescriptor>emptyList(),
//                        true,
//                        "Class object emulation for " + getName(),
//                        Collections.<TypeParameterDescriptor>emptyList(),
//                        Collections.<JetType>emptyList()
//                ),
//                memberScope
//        );
//    }

    public void addConstructor(ConstructorDescriptor constructorDescriptor) {
        this.constructors.add(constructorDescriptor);
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
    public JetType getSuperclassType() {
        return superclassType;
    }

    public void setSuperclassType(@NotNull JetType superclassType) {
        this.superclassType = superclassType;
    }

    @NotNull
    @Override
    public Set<ConstructorDescriptor> getConstructors() {
        return constructors;
    }

    @Override
    public ConstructorDescriptor getUnsubstitutedPrimaryConstructor() {
        return null;
    }

    @Override
    public boolean hasConstructors() {
        return !constructors.isEmpty();
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
    public JetType getClassObjectType() {
        return null;
    }

    @Override
    public boolean isClassObjectAValue() {
        return false;
    }

    @NotNull
    @Override
    public ClassKind getKind() {
        return kind;
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
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitClassDescriptor(this, data);
    }

    @Override
    public String toString() {
        return "java class " + typeConstructor;
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
    public ClassDescriptor getInnerClass(String name) {
        return null;
    }

    @NotNull
    @Override
    public Collection<ClassDescriptor> getInnerClasses() {
        return Collections.emptyList();
    }
}
