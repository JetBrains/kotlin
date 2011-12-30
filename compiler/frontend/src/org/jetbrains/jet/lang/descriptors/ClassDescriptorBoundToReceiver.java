package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.ImportsResolver;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author svtk
 */
public class ClassDescriptorBoundToReceiver implements ClassDescriptor {

    private ClassDescriptor classDescriptor;
    private DeclarationDescriptor receiver;

    public ClassDescriptorBoundToReceiver(ClassDescriptor classDescriptor, DeclarationDescriptor receiver) {
        this.classDescriptor = classDescriptor;
        this.receiver = receiver;
    }

    public ClassDescriptor getClassDescriptor() {
        return classDescriptor;
    }

    public DeclarationDescriptor getReceiver() {
        return receiver;
    }

    @NotNull
    @Override
    public JetScope getMemberScope(List<TypeProjection> typeArguments) {
        return classDescriptor.getMemberScope(typeArguments);
    }

    @NotNull
    @Override
    public JetType getSuperclassType() {
        return classDescriptor.getSuperclassType();
    }

    @NotNull
    @Override
    public Set<ConstructorDescriptor> getConstructors() {
        return (Set)Sets.newHashSet(ImportsResolver.ImportResolver.addBoundToReceiver((Collection)classDescriptor.getConstructors(), receiver));
    }

    @Override
    public ConstructorDescriptor getUnsubstitutedPrimaryConstructor() {
        return classDescriptor.getUnsubstitutedPrimaryConstructor();
    }

    @Override
    public boolean hasConstructors() {
        return classDescriptor.hasConstructors();
    }

    @NotNull
    @Override
    public DeclarationDescriptor getOriginal() {
        return classDescriptor.getOriginal();
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return classDescriptor.getContainingDeclaration();
    }

    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        return classDescriptor.getTypeConstructor();
    }

    @NotNull
    @Override
    public JetType getDefaultType() {
        return classDescriptor.getDefaultType();
    }

    @NotNull
    @Override
    public ClassDescriptor substitute(TypeSubstitutor substitutor) {
        return classDescriptor.substitute(substitutor);
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return classDescriptor.accept(visitor, data);
    }

    @Override
    public void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor) {
        classDescriptor.acceptVoid(visitor);
    }

    @Override
    public JetType getClassObjectType() {
        return classDescriptor.getClassObjectType();
    }

    @Override
    public boolean isClassObjectAValue() {
        return classDescriptor.isClassObjectAValue();
    }

    @NotNull
    @Override
    public ClassKind getKind() {
        return classDescriptor.getKind();
    }

    @NotNull
    @Override
    public Modality getModality() {
        return classDescriptor.getModality();
    }

    @NotNull
    @Override
    public Visibility getVisibility() {
        return classDescriptor.getVisibility();
    }

    @NotNull
    @Override
    public ReceiverDescriptor getImplicitReceiver() {
        return ReceiverDescriptor.NO_RECEIVER;
    }

    @Override
    public List<AnnotationDescriptor> getAnnotations() {
        return classDescriptor.getAnnotations();
    }

    @NotNull
    @Override
    public String getName() {
        return classDescriptor.getName();
    }
}
