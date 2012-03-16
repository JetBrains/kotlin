/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetFunction;
import org.jetbrains.jet.lang.psi.JetSecondaryConstructor;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;

import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @NotNull
    public static FqNameUnsafe getFQName(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();

        if (descriptor instanceof ModuleDescriptor || containingDeclaration instanceof ModuleDescriptor) {
            return FqName.ROOT.toUnsafe();
        }

        if (containingDeclaration == null) {
            if (descriptor instanceof NamespaceDescriptor) {
                // TODO: namespace must always have parent
                if (descriptor.getName().equals("jet")) {
                    return FqNameUnsafe.topLevel("jet");
                }
                if (descriptor.getName().equals("<java_root>")) {
                    return FqName.ROOT.toUnsafe();
                }
            }
            throw new IllegalStateException("descriptor is not module descriptor and has null containingDeclaration: " + containingDeclaration);
        }

        return getFQName(containingDeclaration).child(descriptor.getName());
    }

    public static boolean isTopLevelFunction(@NotNull SimpleFunctionDescriptor functionDescriptor) {
        return functionDescriptor.getContainingDeclaration() instanceof NamespaceDescriptor;
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

    @NotNull
    public static JetType getFunctionExpectedReturnType(@NotNull FunctionDescriptor descriptor, @NotNull JetElement function) {
        JetType expectedType;
        if (function instanceof JetFunction) {
            if (((JetFunction) function).getReturnTypeRef() != null || ((JetFunction) function).hasBlockBody()) {
                expectedType = descriptor.getReturnType();
            }
            else {
                expectedType = TypeUtils.NO_EXPECTED_TYPE;
            }
        }
        else if (function instanceof JetSecondaryConstructor) {
            expectedType = JetStandardClasses.getUnitType();
        }
        else {
            expectedType = descriptor.getReturnType();
        }
        return expectedType != null ? expectedType : TypeUtils.NO_EXPECTED_TYPE;
    }

    public static boolean isObject(@NotNull ClassifierDescriptor classifier) {
        if (classifier instanceof ClassDescriptor) {
            ClassDescriptor clazz = (ClassDescriptor) classifier;
            return clazz.getKind() == ClassKind.OBJECT || clazz.getKind() == ClassKind.ENUM_ENTRY;
        } else if (classifier instanceof TypeParameterDescriptor) {
            return false;
        } else {
            throw new IllegalStateException("unknown classifier: " + classifier);
        }
    }
}
