package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public interface ClassDescriptor extends ClassifierDescriptor, MemberDescriptor, ClassOrNamespaceDescriptor {

    @NotNull
    JetScope getMemberScope(List<TypeProjection> typeArguments);

    @NotNull
    Set<ConstructorDescriptor> getConstructors();

    @Nullable
    ConstructorDescriptor getUnsubstitutedPrimaryConstructor();

    boolean hasConstructors();

    @Override
    @NotNull
    DeclarationDescriptor getContainingDeclaration();

    /**
     * @return type A&lt;T&gt; for the class A&lt;T&gt;
     */
    @NotNull
    JetType getDefaultType();

    @NotNull
    @Override
    ClassDescriptor substitute(TypeSubstitutor substitutor);

    @Nullable
    JetType getClassObjectType();

    @Nullable
    ClassDescriptor getClassObjectDescriptor();

    @NotNull
    ClassKind getKind();

    @Override
    @NotNull
    Modality getModality();

    @Override
    @NotNull
    Visibility getVisibility();

    @NotNull
    ReceiverDescriptor getImplicitReceiver();

    @Nullable
    ClassDescriptor getInnerClassOrObject(String name);
    
    @NotNull
    Collection<ClassDescriptor> getInnerClassesAndObjects();
}
