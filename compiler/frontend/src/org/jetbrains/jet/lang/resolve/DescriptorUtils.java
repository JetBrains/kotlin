package org.jetbrains.jet.lang.resolve;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetObjectDeclaration;
import org.jetbrains.jet.lang.psi.JetObjectDeclarationName;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;

import java.util.*;

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

            @Nullable
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

    /**
     * The primary case for local extensions is the following:
     *
     * I had a locally declared extension function or a local variable of function type called foo
     * And I called it on my x
     * Now, someone added function foo() to the class of x
     * My code should not change
     *
     * thus
     *
     * local extension prevail over members (and members prevail over all non-local extensions)
     */
    public static boolean isLocal(DeclarationDescriptor containerOfTheCurrentLocality, DeclarationDescriptor candidate) {
        if (candidate instanceof ValueParameterDescriptor) {
            return true;
        }
        DeclarationDescriptor parent = candidate.getContainingDeclaration();
        if (!(parent instanceof FunctionDescriptor)) {
            return false;
        }
        FunctionDescriptor functionDescriptor = (FunctionDescriptor) parent;
        DeclarationDescriptor current = containerOfTheCurrentLocality;
        while (current != null) {
            if (current == functionDescriptor) {
                return true;
            }
            current = current.getContainingDeclaration();
        }
        return false;
    }

    public static String getFQName(DeclarationDescriptor descriptor) {
        DeclarationDescriptor container = descriptor.getContainingDeclaration();
        if (container != null && !(container instanceof ModuleDescriptor)) {
            String baseName = getFQName(container);
            if (!baseName.isEmpty()) return baseName + "." + descriptor.getName();
        }

        return descriptor.getName();
    }

    @Nullable
    public static <D extends DeclarationDescriptor> D getParentOfType(@Nullable DeclarationDescriptor descriptor, @NotNull Class<D> aClass) {
        return getParentOfType(descriptor, aClass, true);
    }    
    
    @Nullable
    public static <D extends DeclarationDescriptor> D getParentOfType(@Nullable DeclarationDescriptor descriptor, @NotNull Class<D> aClass, boolean strict) {
        if (descriptor == null) return null;
        if (strict) {
            descriptor = descriptor.getContainingDeclaration();
        }
        while (descriptor != null) {
            if (aClass.isInstance(descriptor)) {
                //noinspection unchecked
                return (D) descriptor;
            }
            descriptor = descriptor.getContainingDeclaration();
        }
        return null;
    }
    
    public static boolean isAncestor(@Nullable DeclarationDescriptor ancestor, @NotNull DeclarationDescriptor declarationDescriptor, boolean strict) {
        if (ancestor == null) return false;
        DeclarationDescriptor descriptor = strict ? declarationDescriptor.getContainingDeclaration() : declarationDescriptor;
        while (descriptor != null) {
            if (ancestor == descriptor) return true;
            descriptor = descriptor.getContainingDeclaration();
        }
        return false;
    }

    @Nullable
    public static VariableDescriptor filterNonExtensionProperty(Set<VariableDescriptor> variables) {
        for (VariableDescriptor variable : variables) {
            if (!variable.getReceiverParameter().exists()) {
                return variable;
            }
        }
        return null;
    }

    @Nullable
    public static ClassDescriptor getObjectIfObjectOrClassObjectDescriptor(ClassDescriptor descriptor) {
        if ((descriptor).getKind() == ClassKind.OBJECT) {
            return descriptor;
        }
        return descriptor.getClassObjectDescriptor();
    }
}
