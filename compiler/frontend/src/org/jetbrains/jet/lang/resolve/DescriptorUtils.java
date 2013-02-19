/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.NamespaceDescriptorParent;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetFunction;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.FilteringScope;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.*;

import static org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER;

public class DescriptorUtils {

    @NotNull
    public static <D extends CallableDescriptor> D substituteBounds(@NotNull final D functionDescriptor) {
        final List<TypeParameterDescriptor> typeParameters = functionDescriptor.getTypeParameters();
        if (typeParameters.isEmpty()) return functionDescriptor;

        // TODO: this does not handle any recursion in the bounds
        @SuppressWarnings("unchecked")
        D substitutedFunction = (D) functionDescriptor.substitute(DescriptorSubstitutor.createUpperBoundsSubstitutor(typeParameters));
        assert substitutedFunction != null : "Substituting upper bounds should always be legal";

        return substitutedFunction;
    }

    public static Modality convertModality(Modality modality, boolean makeNonAbstract) {
        if (makeNonAbstract && modality == Modality.ABSTRACT) return Modality.OPEN;
        return modality;
    }

    @Nullable
    public static ReceiverParameterDescriptor getExpectedThisObjectIfNeeded(@NotNull DeclarationDescriptor containingDeclaration) {
        if (containingDeclaration instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;
            return classDescriptor.getThisAsReceiverParameter();
        }
        else if (containingDeclaration instanceof ScriptDescriptor) {
            ScriptDescriptor scriptDescriptor = (ScriptDescriptor) containingDeclaration;
            return scriptDescriptor.getThisAsReceiverParameter();
        }
        return NO_RECEIVER_PARAMETER;
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
                if (descriptor.getName().equals(Name.identifier("jet"))) {
                    return FqNameUnsafe.topLevel(Name.identifier("jet"));
                }
                if (descriptor.getName().equals(Name.special("<java_root>"))) {
                    return FqName.ROOT.toUnsafe();
                }
            }
            throw new IllegalStateException("descriptor is not module descriptor and has null containingDeclaration: " + containingDeclaration);
        }

        if (containingDeclaration instanceof ClassDescriptor && ((ClassDescriptor) containingDeclaration).getKind() == ClassKind.CLASS_OBJECT) {
            DeclarationDescriptor classOfClassObject = containingDeclaration.getContainingDeclaration();
            assert classOfClassObject != null;
            return getFQName(classOfClassObject).child(descriptor.getName());
        }

        return getFQName(containingDeclaration).child(descriptor.getName());
    }

    public static boolean isTopLevelDeclaration(@NotNull DeclarationDescriptor descriptor) {
        return descriptor.getContainingDeclaration() instanceof NamespaceDescriptor;
    }

    public static boolean isInSameNamespace(@NotNull DeclarationDescriptor first, @NotNull DeclarationDescriptor second) {
        NamespaceDescriptor whatPackage = DescriptorUtils.getParentOfType(first, NamespaceDescriptor.class, false);
        NamespaceDescriptor fromPackage = DescriptorUtils.getParentOfType(second, NamespaceDescriptor.class, false);
        return fromPackage != null && whatPackage != null && whatPackage.equals(fromPackage);
    }

    @Nullable
    public static DeclarationDescriptor findTopLevelParent(@NotNull DeclarationDescriptor declarationDescriptor) {
        DeclarationDescriptor descriptor = declarationDescriptor;
        while (!(descriptor == null || isTopLevelDeclaration(descriptor))) {
            descriptor = descriptor.getContainingDeclaration();
        }
        return descriptor;
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
    public static VariableDescriptor filterNonExtensionProperty(Collection<VariableDescriptor> variables) {
        for (VariableDescriptor variable : variables) {
            if (variable.getReceiverParameter() == null) {
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
        else {
            expectedType = descriptor.getReturnType();
        }
        return expectedType != null ? expectedType : TypeUtils.NO_EXPECTED_TYPE;
    }

    public static boolean isSubclass(@NotNull ClassDescriptor subClass, @NotNull ClassDescriptor superClass) {
        return isSubtypeOfClass(subClass.getDefaultType(), superClass.getOriginal());
    }

    private static boolean isSubtypeOfClass(@NotNull JetType type, @NotNull DeclarationDescriptor superClass) {
        DeclarationDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
        if (descriptor != null && superClass == descriptor.getOriginal()) {
            return true;
        }
        for (JetType superType : type.getConstructor().getSupertypes()) {
            if (isSubtypeOfClass(superType, superClass)) {
                return true;
            }
        }
        return false;
    }

    public static void addSuperTypes(JetType type, Set<JetType> set) {
        set.add(type);

        for (JetType jetType : type.getConstructor().getSupertypes()) {
            addSuperTypes(jetType, set);
        }
    }

    public static boolean isRootNamespace(@NotNull NamespaceDescriptor namespaceDescriptor) {
        return namespaceDescriptor.getContainingDeclaration() instanceof ModuleDescriptor;
    }

    @NotNull
    public static List<DeclarationDescriptor> getPathWithoutRootNsAndModule(@NotNull DeclarationDescriptor descriptor) {
        List<DeclarationDescriptor> path = Lists.newArrayList();
        DeclarationDescriptor current = descriptor;
        while (true) {
            if (current instanceof NamespaceDescriptor && isRootNamespace((NamespaceDescriptor) current)) {
                return Lists.reverse(path);
            }
            path.add(current);
            current = current.getContainingDeclaration();
        }
    }

    public static boolean isClassObject(@NotNull DeclarationDescriptor descriptor) {
        return descriptor instanceof ClassDescriptor
               && ((ClassDescriptor) descriptor).getKind() == ClassKind.CLASS_OBJECT;
    }

    public static boolean isEnumEntry(@NotNull DeclarationDescriptor descriptor) {
        return descriptor instanceof ClassDescriptor
               && ((ClassDescriptor) descriptor).getKind() == ClassKind.ENUM_ENTRY;
    }

    public static boolean isEnumClass(@NotNull DeclarationDescriptor descriptor) {
        return descriptor instanceof ClassDescriptor
               && ((ClassDescriptor) descriptor).getKind() == ClassKind.ENUM_CLASS;
    }

    @NotNull
    public static List<ClassDescriptor> getSuperclassDescriptors(@NotNull ClassDescriptor classDescriptor) {
        Collection<JetType> superclassTypes = classDescriptor.getTypeConstructor().getSupertypes();
        List<ClassDescriptor> superClassDescriptors = new ArrayList<ClassDescriptor>();
        for (JetType type : superclassTypes) {
            ClassDescriptor result = getClassDescriptorForType(type);
            if (isNotAny(result)) {
                superClassDescriptors.add(result);
            }
        }
        return superClassDescriptors;
    }

    @NotNull
    public static ClassDescriptor getClassDescriptorForType(@NotNull JetType type) {
        DeclarationDescriptor superClassDescriptor =
            type.getConstructor().getDeclarationDescriptor();
        assert superClassDescriptor instanceof ClassDescriptor
            : "Superclass descriptor of a type should be of type ClassDescriptor";
        return (ClassDescriptor)superClassDescriptor;
    }

    public static boolean isNotAny(@NotNull DeclarationDescriptor superClassDescriptor) {
        return !superClassDescriptor.equals(KotlinBuiltIns.getInstance().getAny());
    }

    public static boolean inStaticContext(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        if (containingDeclaration instanceof NamespaceDescriptor) {
            return true;
        }
        if (containingDeclaration instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;

            if (classDescriptor.getKind().isObject()) {
                return inStaticContext(classDescriptor.getContainingDeclaration());
            }

        }
        return false;
    }

    public static boolean isIteratorWithoutRemoveImpl(@NotNull ClassDescriptor classDescriptor) {
        ClassDescriptor iteratorOfT = KotlinBuiltIns.getInstance().getIterator();
        JetType iteratorOfAny = TypeUtils.substituteParameters(iteratorOfT, Collections.singletonList(KotlinBuiltIns.getInstance().getAnyType()));
        boolean isIterator = JetTypeChecker.INSTANCE.isSubtypeOf(classDescriptor.getDefaultType(), iteratorOfAny);
        boolean hasRemove = hasMethod(classDescriptor, Name.identifier("remove"));
        return isIterator && !hasRemove;
    }

    private static boolean hasMethod(ClassDescriptor classDescriptor, Name name) {
        Collection<FunctionDescriptor> removeFunctions = classDescriptor.getDefaultType().getMemberScope().getFunctions(name);
        for (FunctionDescriptor function : removeFunctions) {
            if (function.getValueParameters().isEmpty() && function.getTypeParameters().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public static Name getClassObjectName(@NotNull Name className) {
        return getClassObjectName(className.getName());
    }

    @NotNull
    public static Name getClassObjectName(@NotNull String className) {
        return Name.special("<class-object-for-" + className + ">");
    }

    public static boolean isEnumClassObject(@NotNull DeclarationDescriptor classObjectDescriptor) {
        DeclarationDescriptor containingDeclaration = classObjectDescriptor.getContainingDeclaration();
        return ((containingDeclaration instanceof ClassDescriptor) &&
                ((ClassDescriptor) containingDeclaration).getKind() == ClassKind.ENUM_CLASS);
    }

    @NotNull
    public static Visibility getDefaultConstructorVisibility(@NotNull ClassDescriptor classDescriptor) {
        ClassKind classKind = classDescriptor.getKind();
        if (classKind == ClassKind.ENUM_CLASS) {
            return Visibilities.PRIVATE;
        }
        if (classKind.isObject()) {
            return Visibilities.PRIVATE;
        }
        assert classKind == ClassKind.CLASS || classKind == ClassKind.TRAIT || classKind == ClassKind.ANNOTATION_CLASS;
        return Visibilities.PUBLIC;
    }

    public static List<String> getSortedValueArguments(AnnotationDescriptor descriptor) {
        List<String> resultList = Lists.newArrayList();
        for (Map.Entry<ValueParameterDescriptor, CompileTimeConstant<?>> entry : descriptor.getAllValueArguments().entrySet()) {
            resultList.add(entry.getKey().getName().getName() + " = " + entry.getValue().toString());
        }
        Collections.sort(resultList);
        return resultList;
    }

    @Nullable
    public static ClassDescriptor getInnerClassByName(@NotNull ClassDescriptor classDescriptor, @NotNull String innerClassName) {
        ClassifierDescriptor classifier = classDescriptor.getDefaultType().getMemberScope().getClassifier(Name.identifier(innerClassName));
        assert classifier instanceof ClassDescriptor :
                "Inner class " + innerClassName + " in " + classDescriptor + " should be instance of ClassDescriptor, but was: "
                + (classifier == null ? "null" : classifier.getClass());
        return (ClassDescriptor) classifier;
    }

    public static ConstructorDescriptor getConstructorOfDataClass(ClassDescriptor classDescriptor) {
        Collection<ConstructorDescriptor> constructors = classDescriptor.getConstructors();
        assert constructors.size() == 1 : "Data class must have only one constructor: " + constructors;
        return constructors.iterator().next();
    }

    @Nullable
    public static JetType getReceiverParameterType(@Nullable ReceiverParameterDescriptor receiverParameterDescriptor) {
        if (receiverParameterDescriptor == null) {
            return null;
        }
        return receiverParameterDescriptor.getType();
    }

    @NotNull
    public static ReceiverValue safeGetValue(@Nullable ReceiverParameterDescriptor receiverParameterDescriptor) {
        if (receiverParameterDescriptor == null) {
            return ReceiverValue.NO_RECEIVER;
        }
        return receiverParameterDescriptor.getValue();
    }


    public static boolean isExternallyAccessible(PropertyDescriptor propertyDescriptor) {
        return propertyDescriptor.getVisibility() != Visibilities.PRIVATE || isClassObject(propertyDescriptor.getContainingDeclaration())
               || isTopLevelDeclaration(propertyDescriptor);
    }

    @NotNull
    public static JetType getVarargParameterType(@NotNull JetType elementType) {
        return getVarargParameterType(elementType, Variance.INVARIANT);
    }

    @NotNull
    public static JetType getVarargParameterType(@NotNull JetType elementType, @NotNull Variance projectionKind) {
        KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();
        JetType primitiveArrayType = builtIns.getPrimitiveArrayJetTypeByPrimitiveJetType(elementType);
        if (primitiveArrayType != null) {
            return primitiveArrayType;
        }
        else {
            return builtIns.getArrayType(projectionKind, elementType);
        }
    }

    @NotNull
    public static List<JetType> getValueParametersTypes(@NotNull List<ValueParameterDescriptor> valueParameters) {
        List<JetType> parameterTypes = Lists.newArrayList();
        for (ValueParameterDescriptor parameter : valueParameters) {
            parameterTypes.add(parameter.getType());
        }
        return parameterTypes;
    }

    public static boolean isConstructorOfStaticNestedClass(@Nullable CallableDescriptor descriptor) {
        return descriptor instanceof ConstructorDescriptor && isStaticNestedClass(descriptor.getContainingDeclaration());
    }

    /**
     * @return true if descriptor is a class inside another class and does not have access to the outer class
     */
    public static boolean isStaticNestedClass(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor containing = descriptor.getContainingDeclaration();
        return descriptor instanceof ClassDescriptor &&
               containing instanceof ClassDescriptor &&
               !((ClassDescriptor) descriptor).isInner() &&
               !((ClassDescriptor) containing).getKind().isObject();
    }

    @Nullable
    public static ClassDescriptor getContainingClass(@NotNull JetScope scope) {
        DeclarationDescriptor containingDeclaration = scope.getContainingDeclaration();
        return getParentOfType(containingDeclaration, ClassDescriptor.class, false);
    }

    @NotNull
    public static JetScope getStaticNestedClassesScope(@NotNull ClassDescriptor descriptor) {
        JetScope innerClassesScope = descriptor.getUnsubstitutedInnerClassesScope();
        return new FilteringScope(innerClassesScope, new Predicate<DeclarationDescriptor>() {
            @Override
            public boolean apply(@Nullable DeclarationDescriptor descriptor) {
                return descriptor instanceof ClassDescriptor && !((ClassDescriptor) descriptor).isInner();
            }
        });
    }

    @Nullable
    public static ClassDescriptor getClassForCorrespondingJavaNamespace(@NotNull NamespaceDescriptor correspondingNamespace) {
        NamespaceDescriptorParent containingDeclaration = correspondingNamespace.getContainingDeclaration();
        if (!(containingDeclaration instanceof NamespaceDescriptor)) {
            return null;
        }

        NamespaceDescriptor namespaceDescriptor = (NamespaceDescriptor) containingDeclaration;

        ClassifierDescriptor classDescriptor = namespaceDescriptor.getMemberScope().getClassifier(correspondingNamespace.getName());
        if (classDescriptor != null && classDescriptor instanceof ClassDescriptor) {
            return (ClassDescriptor) classDescriptor;
        }

        ClassDescriptor classDescriptorForOuterClass = getClassForCorrespondingJavaNamespace(namespaceDescriptor);
        if (classDescriptorForOuterClass == null) {
            return null;
        }

        ClassifierDescriptor innerClassDescriptor =
                classDescriptorForOuterClass.getUnsubstitutedInnerClassesScope().getClassifier(correspondingNamespace.getName());
        if (innerClassDescriptor instanceof ClassDescriptor) {
            return (ClassDescriptor) innerClassDescriptor;
        }
        return null;
    }

}
