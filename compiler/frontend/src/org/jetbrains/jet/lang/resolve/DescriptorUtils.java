package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.*;

import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor.NO_RECEIVER;

/**
 * @author abreslav
 */
public class DescriptorUtils {
    public static boolean definesItsOwnThis(@NotNull DeclarationDescriptor descriptor) {
        return descriptor.accept(new DeclarationDescriptorVisitor<Boolean, Void>() {
            @Override
            public Boolean visitDeclarationDescriptor(DeclarationDescriptor descriptor, Void data) {
                return false;
            }

            @Override
            public Boolean visitFunctionDescriptor(FunctionDescriptor descriptor, Void data) {
                return descriptor.getReceiverParameter().exists();
            }

            @Override
            public Boolean visitClassDescriptor(ClassDescriptor descriptor, Void data) {
                return true;
            }

            @Override
            public Boolean visitPropertyDescriptor(PropertyDescriptor descriptor, Void data) {
                return descriptor.getReceiverParameter().exists();
            }
        }, null);
    }

    @NotNull
    public static <Descriptor extends CallableDescriptor> Descriptor substituteBounds(@NotNull Descriptor functionDescriptor) {
        final List<TypeParameterDescriptor> typeParameters = functionDescriptor.getTypeParameters();
        if (typeParameters.isEmpty()) return functionDescriptor;
        final Map<TypeConstructor, TypeParameterDescriptor> typeConstructors = Maps.newHashMap();
        for (TypeParameterDescriptor typeParameter : typeParameters) {
            typeConstructors.put(typeParameter.getTypeConstructor(), typeParameter);
        }
        //noinspection unchecked
        return (Descriptor) functionDescriptor.substitute(new TypeSubstitutor(TypeSubstitutor.TypeSubstitution.EMPTY) {
            @Override
            public boolean inRange(@NotNull TypeConstructor typeConstructor) {
                return typeConstructors.containsKey(typeConstructor);
            }

            @Override
            public boolean isEmpty() {
                return typeParameters.isEmpty();
            }

            @NotNull
            @Override
            public TypeSubstitution getSubstitution() {
                throw new UnsupportedOperationException();
            }

            @NotNull
            @Override
            public JetType safeSubstitute(@NotNull JetType type, @NotNull Variance howThisTypeIsUsed) {
                JetType substituted = substitute(type, howThisTypeIsUsed);
                if (substituted == null) {
                    return ErrorUtils.createErrorType("Substitution failed");
                }
                return substituted;
            }

            @Override
            public JetType substitute(@NotNull JetType type, @NotNull Variance howThisTypeIsUsed) {
                TypeParameterDescriptor typeParameterDescriptor = typeConstructors.get(type.getConstructor());
                if (typeParameterDescriptor != null) {
                    switch (howThisTypeIsUsed) {
                        case INVARIANT:
                            return type;
                        case IN_VARIANCE:
                            throw new UnsupportedOperationException(); // TODO : lower bounds
                        case OUT_VARIANCE:
                            return typeParameterDescriptor.getDefaultType();
                    }
                }
                return super.substitute(type, howThisTypeIsUsed);
            }
        });
    }
    
    public static List<ValueParameterDescriptor> copyValueParameters(DeclarationDescriptor newOwner, List<ValueParameterDescriptor> parameters) {
        List<ValueParameterDescriptor> result = Lists.newArrayList();
        for (ValueParameterDescriptor parameter : parameters) {
            result.add(parameter.copy(newOwner));
        }
        return result;
    }

    public static List<TypeParameterDescriptor> copyTypeParameters(DeclarationDescriptor newOwner, List<TypeParameterDescriptor> parameters) {
        List<TypeParameterDescriptor> result = Lists.newArrayList();
        for (TypeParameterDescriptor parameter : parameters) {
            result.add(parameter.copy(newOwner));
        }
        return result;
    }
    
    public static Modality convertModality(Modality modality, boolean makeNonAbstract) {
        if (makeNonAbstract && modality == Modality.ABSTRACT) return Modality.OPEN;
        return modality;
    }

    @NotNull
    public static ReceiverDescriptor getExpectedThisObjectIfNeeded(@NotNull DeclarationDescriptor containingDeclaration) {
        if (containingDeclaration instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;
            return classDescriptor.getImplicitReceiver();
        }
        return NO_RECEIVER;
    }
}
