package org.jetbrains.jet.lang.types;

import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;

import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class DescriptorSubstitutor {

    @NotNull
    public static TypeSubstitutor substituteTypeParameters(
            @NotNull List<TypeParameterDescriptor> typeParameters,
            @NotNull final TypeSubstitutor originalSubstitutor,
            @NotNull DeclarationDescriptor newContainingDeclaration,
            @NotNull List<TypeParameterDescriptor> result) {
        final Map<TypeConstructor, TypeProjection> mutableSubstitution = Maps.newHashMap();
        TypeSubstitutor substitutor = TypeSubstitutor.create(new TypeSubstitutor.TypeSubstitution() {

            @Override
            public TypeProjection get(TypeConstructor key) {
                if (originalSubstitutor.inRange(key)) {
                    return originalSubstitutor.getSubstitution().get(key);
                }
                return mutableSubstitution.get(key);
            }

            @Override
            public boolean isEmpty() {
                return originalSubstitutor.isEmpty() && mutableSubstitution.isEmpty();
            }
        });

        for (TypeParameterDescriptor descriptor : typeParameters) {
            TypeParameterDescriptor substituted = TypeParameterDescriptor.createForFurtherModification(
                    newContainingDeclaration,
                    descriptor.getAnnotations(),
                    descriptor.isReified(),
                    descriptor.getVariance(),
                    descriptor.getName(),
                    descriptor.getIndex());

            mutableSubstitution.put(descriptor.getTypeConstructor(), new TypeProjection(substituted.getDefaultType()));

            for (JetType upperBound : descriptor.getUpperBounds()) {
                substituted.getUpperBounds().add(substitutor.substitute(upperBound, Variance.INVARIANT));
            }

            result.add(substituted);
        }

        return substitutor;
    }

}
