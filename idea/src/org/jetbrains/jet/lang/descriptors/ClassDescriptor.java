package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.List;

/**
 * @author abreslav
 */
public interface ClassDescriptor extends ClassifierDescriptor {

    @NotNull
    JetScope getMemberScope(List<TypeProjection> typeArguments);

    /**
     * @return the superclass for a class descriptor, and the required class fro a trait descriptor
     */
    @NotNull
    JetType getSuperclassType();

    @NotNull
    FunctionGroup getConstructors();

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

    boolean isObject();

    @NotNull
    ClassModifiers getModifiers();
}
