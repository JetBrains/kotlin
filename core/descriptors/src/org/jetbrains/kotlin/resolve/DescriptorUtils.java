/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve;

import kotlin.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.name.SpecialNames;
import org.jetbrains.kotlin.resolve.scopes.FilteringScope;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.LazyType;
import org.jetbrains.kotlin.types.TypeConstructor;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;

import java.util.*;

import static org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.*;
import static org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER;

public class DescriptorUtils {
    public static final Name ENUM_VALUES = Name.identifier("values");
    public static final Name ENUM_VALUE_OF = Name.identifier("valueOf");

    private DescriptorUtils() {
    }

    @Nullable
    public static ReceiverParameterDescriptor getDispatchReceiverParameterIfNeeded(@NotNull DeclarationDescriptor containingDeclaration) {
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
     * Descriptor may be local itself or have a local ancestor
     */
    public static boolean isLocal(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor current = descriptor;
        while (current instanceof MemberDescriptor) {
            if (isAnonymousObject(current) || ((DeclarationDescriptorWithVisibility) current).getVisibility() == Visibilities.LOCAL) {
                return true;
            }
            current = current.getContainingDeclaration();
        }
        return false;
    }

    @NotNull
    public static FqNameUnsafe getFqName(@NotNull DeclarationDescriptor descriptor) {
        FqName safe = getFqNameSafeIfPossible(descriptor);
        return safe != null ? safe.toUnsafe() : getFqNameUnsafe(descriptor);
    }

    @NotNull
    public static FqName getFqNameSafe(@NotNull DeclarationDescriptor descriptor) {
        FqName safe = getFqNameSafeIfPossible(descriptor);
        return safe != null ? safe : getFqNameUnsafe(descriptor).toSafe();
    }


    @Nullable
    private static FqName getFqNameSafeIfPossible(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof ModuleDescriptor || ErrorUtils.isError(descriptor)) {
            return FqName.ROOT;
        }

        if (descriptor instanceof PackageViewDescriptor) {
            return ((PackageViewDescriptor) descriptor).getFqName();
        }
        else if (descriptor instanceof PackageFragmentDescriptor) {
            return ((PackageFragmentDescriptor) descriptor).getFqName();
        }

        return null;
    }

    @NotNull
    private static FqNameUnsafe getFqNameUnsafe(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        assert containingDeclaration != null : "Not package/module descriptor doesn't have containing declaration: " + descriptor;
        return getFqName(containingDeclaration).child(descriptor.getName());
    }

    @NotNull
    public static FqName getFqNameFromTopLevelClass(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        Name name = descriptor.getName();
        if (!(containingDeclaration instanceof ClassDescriptor)) {
            return FqName.topLevel(name);
        }
        return getFqNameFromTopLevelClass(containingDeclaration).child(name);
    }

    public static boolean isTopLevelDeclaration(@NotNull DeclarationDescriptor descriptor) {
        return descriptor.getContainingDeclaration() instanceof PackageFragmentDescriptor;
    }

    public static boolean isExtension(@NotNull CallableDescriptor descriptor) {
        return (descriptor.getExtensionReceiverParameter() != null);
    }

    public static boolean isOverride(@NotNull CallableMemberDescriptor descriptor) {
        return !descriptor.getOverriddenDescriptors().isEmpty();
    }

    /**
     * @return true iff this is a top-level declaration or a class member with no expected "this" object (e.g. static members in Java,
     * values() and valueOf() methods of enum classes, etc.)
     */
    public static boolean isStaticDeclaration(@NotNull CallableDescriptor descriptor) {
        if (descriptor instanceof ConstructorDescriptor) return false;

        DeclarationDescriptor container = descriptor.getContainingDeclaration();
        return container instanceof PackageFragmentDescriptor ||
               (container instanceof ClassDescriptor && descriptor.getDispatchReceiverParameter() == null);
    }

    // WARNING! Don't use this method in JVM backend, use JvmCodegenUtil.isCallInsideSameModuleAsDeclared() instead.
    // The latter handles compilation against compiled part of our module correctly.
    public static boolean areInSameModule(@NotNull DeclarationDescriptor first, @NotNull DeclarationDescriptor second) {
        return getContainingModule(first).equals(getContainingModule(second));
    }

    @Nullable
    public static <D extends DeclarationDescriptor> D getParentOfType(
            @Nullable DeclarationDescriptor descriptor,
            @NotNull Class<D> aClass
    ) {
        return getParentOfType(descriptor, aClass, true);
    }

    @Nullable
    public static <D extends DeclarationDescriptor> D getParentOfType(
            @Nullable DeclarationDescriptor descriptor,
            @NotNull Class<D> aClass,
            boolean strict
    ) {
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

    @NotNull
    public static ModuleDescriptor getContainingModule(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof PackageViewDescriptor) {
            return ((PackageViewDescriptor) descriptor).getModule();
        }
        ModuleDescriptor module = getParentOfType(descriptor, ModuleDescriptor.class, false);
        assert module != null : "Descriptor without a containing module: " + descriptor;
        return module;
    }

    @Nullable
    public static ClassDescriptor getContainingClass(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor containing = descriptor.getContainingDeclaration();
        while (containing != null) {
            if (containing instanceof ClassDescriptor && !isClassObject(containing)) {
                return (ClassDescriptor) containing;
            }
            containing = containing.getContainingDeclaration();
        }
        return null;
    }

    public static boolean isAncestor(
            @Nullable DeclarationDescriptor ancestor,
            @NotNull DeclarationDescriptor declarationDescriptor,
            boolean strict
    ) {
        if (ancestor == null) return false;
        DeclarationDescriptor descriptor = strict ? declarationDescriptor.getContainingDeclaration() : declarationDescriptor;
        while (descriptor != null) {
            if (ancestor == descriptor) return true;
            descriptor = descriptor.getContainingDeclaration();
        }
        return false;
    }

    public static boolean isSubclass(@NotNull ClassDescriptor subClass, @NotNull ClassDescriptor superClass) {
        return isSubtypeOfClass(subClass.getDefaultType(), superClass.getOriginal());
    }

    private static boolean isSubtypeOfClass(@NotNull JetType type, @NotNull DeclarationDescriptor superClass) {
        DeclarationDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
        if (descriptor != null) {
            DeclarationDescriptor originalDescriptor = descriptor.getOriginal();
            if (originalDescriptor instanceof ClassifierDescriptor
                     && superClass instanceof ClassifierDescriptor
                     && ((ClassifierDescriptor) superClass).getTypeConstructor().equals(((ClassifierDescriptor) originalDescriptor).getTypeConstructor())) {
                return true;
            }
        }

        for (JetType superType : type.getConstructor().getSupertypes()) {
            if (isSubtypeOfClass(superType, superClass)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFunctionLiteral(@NotNull FunctionDescriptor descriptor) {
        return descriptor instanceof AnonymousFunctionDescriptor;
    }

    public static boolean isClassObject(@Nullable DeclarationDescriptor descriptor) {
        return isKindOf(descriptor, ClassKind.CLASS_OBJECT);
    }

    public static boolean isAnonymousObject(@NotNull DeclarationDescriptor descriptor) {
        return isClass(descriptor) && descriptor.getName().equals(SpecialNames.NO_NAME_PROVIDED);
    }

    public static boolean isObject(@NotNull DeclarationDescriptor descriptor) {
        return isKindOf(descriptor, ClassKind.OBJECT);
    }

    public static boolean isEnumEntry(@NotNull DeclarationDescriptor descriptor) {
        return isKindOf(descriptor, ClassKind.ENUM_ENTRY);
    }

    public static boolean isSingleton(@Nullable DeclarationDescriptor classifier) {
        if (classifier instanceof ClassDescriptor) {
            ClassDescriptor clazz = (ClassDescriptor) classifier;
            return clazz.getKind().isSingleton();
        }
        return false;
    }

    public static boolean isEnumClass(@Nullable DeclarationDescriptor descriptor) {
        return isKindOf(descriptor, ClassKind.ENUM_CLASS);
    }

    public static boolean isAnnotationClass(@Nullable DeclarationDescriptor descriptor) {
        return isKindOf(descriptor, ClassKind.ANNOTATION_CLASS);
    }

    public static boolean isTrait(@Nullable DeclarationDescriptor descriptor) {
        return isKindOf(descriptor, ClassKind.TRAIT);
    }

    public static boolean isClass(@Nullable DeclarationDescriptor descriptor) {
        return isKindOf(descriptor, ClassKind.CLASS);
    }

    public static boolean isKindOf(@Nullable DeclarationDescriptor descriptor, @NotNull ClassKind classKind) {
        return descriptor instanceof ClassDescriptor && ((ClassDescriptor) descriptor).getKind() == classKind;
    }

    @NotNull
    public static List<ClassDescriptor> getSuperclassDescriptors(@NotNull ClassDescriptor classDescriptor) {
        Collection<JetType> superclassTypes = classDescriptor.getTypeConstructor().getSupertypes();
        List<ClassDescriptor> superClassDescriptors = new ArrayList<ClassDescriptor>();
        for (JetType type : superclassTypes) {
            ClassDescriptor result = getClassDescriptorForType(type);
            if (!isAny(result)) {
                superClassDescriptors.add(result);
            }
        }
        return superClassDescriptors;
    }

    @NotNull
    public static ClassDescriptor getClassDescriptorForType(@NotNull JetType type) {
        return getClassDescriptorForTypeConstructor(type.getConstructor());
    }

    @NotNull
    public static ClassDescriptor getClassDescriptorForTypeConstructor(@NotNull TypeConstructor typeConstructor) {
        ClassifierDescriptor descriptor = typeConstructor.getDeclarationDescriptor();
        assert descriptor instanceof ClassDescriptor
            : "Classifier descriptor of a type should be of type ClassDescriptor: " + typeConstructor;
        return (ClassDescriptor) descriptor;
    }

    public static boolean isAny(@NotNull DeclarationDescriptor superClassDescriptor) {
        return superClassDescriptor.equals(KotlinBuiltIns.getInstance().getAny());
    }

    @NotNull
    public static Visibility getDefaultConstructorVisibility(@NotNull ClassDescriptor classDescriptor) {
        ClassKind classKind = classDescriptor.getKind();
        if (classKind == ClassKind.ENUM_CLASS || classKind.isSingleton()) {
            return Visibilities.PRIVATE;
        }
        if (isAnonymousObject(classDescriptor)) {
            return Visibilities.INTERNAL;
        }
        assert classKind == ClassKind.CLASS || classKind == ClassKind.TRAIT || classKind == ClassKind.ANNOTATION_CLASS;
        return Visibilities.PUBLIC;
    }

    @Nullable
    public static ClassDescriptor getInnerClassByName(@NotNull ClassDescriptor classDescriptor, @NotNull String innerClassName) {
        ClassifierDescriptor classifier = classDescriptor.getDefaultType().getMemberScope().getClassifier(Name.identifier(innerClassName));
        assert classifier instanceof ClassDescriptor :
                "Inner class " + innerClassName + " in " + classDescriptor + " should be instance of ClassDescriptor, but was: "
                + (classifier == null ? "null" : classifier.getClass());
        return (ClassDescriptor) classifier;
    }

    @Nullable
    public static JetType getReceiverParameterType(@Nullable ReceiverParameterDescriptor receiverParameterDescriptor) {
        return receiverParameterDescriptor == null ? null : receiverParameterDescriptor.getType();
    }

    /**
     * @return true if descriptor is a class inside another class and does not have access to the outer class
     */
    public static boolean isStaticNestedClass(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor containing = descriptor.getContainingDeclaration();
        return descriptor instanceof ClassDescriptor &&
               containing instanceof ClassDescriptor &&
               !((ClassDescriptor) descriptor).isInner();
    }

    @NotNull
    public static JetScope getStaticNestedClassesScope(@NotNull ClassDescriptor descriptor) {
        JetScope innerClassesScope = descriptor.getUnsubstitutedInnerClassesScope();
        return new FilteringScope(innerClassesScope, new Function1<DeclarationDescriptor, Boolean>() {
            @Override
            public Boolean invoke(DeclarationDescriptor descriptor) {
                return descriptor instanceof ClassDescriptor && !((ClassDescriptor) descriptor).isInner();
            }
        });
    }

    /**
     * @return true iff {@code descriptor}'s first non-class container is a package
     */
    public static boolean isTopLevelOrInnerClass(@NotNull ClassDescriptor descriptor) {
        DeclarationDescriptor containing = descriptor.getContainingDeclaration();
        return isTopLevelDeclaration(descriptor) ||
               containing instanceof ClassDescriptor && isTopLevelOrInnerClass((ClassDescriptor) containing);
    }

    /**
     * Given a fake override, finds any declaration of it in the overridden descriptors. Keep in mind that there may be many declarations
     * of the fake override in the supertypes, this method finds just the only one.
     * TODO: probably all call-sites of this method are wrong, they should handle all super-declarations
     */
    @NotNull
    public static <D extends CallableMemberDescriptor> D unwrapFakeOverride(@NotNull D descriptor) {
        while (descriptor.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            Set<? extends CallableMemberDescriptor> overridden = descriptor.getOverriddenDescriptors();
            if (overridden.isEmpty()) {
                throw new IllegalStateException("Fake override should have at least one overridden descriptor: " + descriptor);
            }
            //noinspection unchecked
            descriptor = (D) overridden.iterator().next();
        }
        return descriptor;
    }

    public static boolean shouldRecordInitializerForProperty(@NotNull VariableDescriptor variable, @NotNull JetType type) {
        if (variable.isVar() || type.isError()) return false;

        if (type instanceof LazyType || type.isMarkedNullable()) return true;

        KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();
        return KotlinBuiltIns.isPrimitiveType(type) ||
               JetTypeChecker.DEFAULT.equalTypes(builtIns.getStringType(), type) ||
               JetTypeChecker.DEFAULT.equalTypes(builtIns.getNumber().getDefaultType(), type) ||
               JetTypeChecker.DEFAULT.equalTypes(builtIns.getAnyType(), type);
    }

    public static boolean classCanHaveAbstractMembers(@NotNull ClassDescriptor classDescriptor) {
        return classDescriptor.getModality() == Modality.ABSTRACT || classDescriptor.getKind() == ClassKind.ENUM_CLASS;
    }

    public static boolean classCanHaveOpenMembers(@NotNull ClassDescriptor classDescriptor) {
        return classDescriptor.getModality() != Modality.FINAL || classDescriptor.getKind() == ClassKind.ENUM_CLASS;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static <D extends CallableDescriptor> Set<D> getAllOverriddenDescriptors(@NotNull D f) {
        Set<D> result = new LinkedHashSet<D>();
        collectAllOverriddenDescriptors((D) f.getOriginal(), result);
        return result;
    }

    private static <D extends CallableDescriptor> void collectAllOverriddenDescriptors(@NotNull D current, @NotNull Set<D> result) {
        if (result.contains(current)) return;
        for (CallableDescriptor callableDescriptor : current.getOriginal().getOverriddenDescriptors()) {
            @SuppressWarnings("unchecked")
            D descriptor = (D) callableDescriptor;
            collectAllOverriddenDescriptors(descriptor, result);
            result.add(descriptor);
        }
    }

    @NotNull
    public static <D extends CallableMemberDescriptor> Set<D> getAllOverriddenDeclarations(@NotNull D memberDescriptor) {
        Set<D> result = new HashSet<D>();
        for (CallableMemberDescriptor overriddenDeclaration : memberDescriptor.getOverriddenDescriptors()) {
            CallableMemberDescriptor.Kind kind = overriddenDeclaration.getKind();
            if (kind == DECLARATION) {
                //noinspection unchecked
                result.add((D) overriddenDeclaration);
            }
            else if (kind == DELEGATION || kind == FAKE_OVERRIDE || kind == SYNTHESIZED) {
                //do nothing
            }
            else {
                throw new AssertionError("Unexpected callable kind " + kind);
            }
            //noinspection unchecked
            result.addAll(getAllOverriddenDeclarations((D) overriddenDeclaration));
        }
        return result;
    }

    public static boolean containsReifiedTypeParameterWithName(@NotNull CallableDescriptor descriptor, @NotNull String name) {
        for (TypeParameterDescriptor typeParameterDescriptor : descriptor.getTypeParameters()) {
            if (typeParameterDescriptor.isReified() && typeParameterDescriptor.getName().asString().equals(name)) return true;
        }

        return false;
    }

    public static boolean containsReifiedTypeParameters(@NotNull CallableDescriptor descriptor) {
        for (TypeParameterDescriptor typeParameterDescriptor : descriptor.getTypeParameters()) {
            if (typeParameterDescriptor.isReified()) return true;
        }

        return false;
    }

}
