/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.builtins.UnsignedTypes;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotated;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl;
import org.jetbrains.kotlin.incremental.components.LookupLocation;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.name.*;
import org.jetbrains.kotlin.resolve.constants.ConstantValue;
import org.jetbrains.kotlin.resolve.constants.StringValue;
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;
import org.jetbrains.kotlin.types.error.ErrorUtils;

import java.util.*;

import static org.jetbrains.kotlin.builtins.KotlinBuiltIns.isAny;
import static org.jetbrains.kotlin.builtins.KotlinBuiltIns.isNullableAny;
import static org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.*;
import static org.jetbrains.kotlin.descriptors.Modality.ABSTRACT;
import static org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt.getBuiltIns;

public class DescriptorUtils {
    // This JVM-specific class FQ name is declared here only because it's used in MainFunctionDetector which is in frontend
    public static final FqName JVM_NAME = new FqName("kotlin.jvm.JvmName");

    private DescriptorUtils() {
    }

    @Nullable
    public static ReceiverParameterDescriptor getDispatchReceiverParameterIfNeeded(@NotNull DeclarationDescriptor containingDeclaration) {
        if (containingDeclaration instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;
            return classDescriptor.getThisAsReceiverParameter();
        }
        return null;
    }

    /**
     * Descriptor may be local itself or have a local ancestor
     */
    public static boolean isLocal(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor current = descriptor;
        while (current != null) {
            if (isAnonymousObject(current) || isDescriptorWithLocalVisibility(current)) {
                return true;
            }
            current = current.getContainingDeclaration();
        }
        return false;
    }

    public static boolean isDescriptorWithLocalVisibility(DeclarationDescriptor current) {
        return current instanceof DeclarationDescriptorWithVisibility &&
         ((DeclarationDescriptorWithVisibility) current).getVisibility() == DescriptorVisibilities.LOCAL;
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

    @NotNull
    public static ClassId getClassIdForNonLocalClass(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        Name name = descriptor.getName();
        if (containingDeclaration instanceof PackageFragmentDescriptorImpl) {
            FqName packageFqName = ((PackageFragmentDescriptorImpl) containingDeclaration).getFqName();
            return new ClassId(packageFqName, name);
        }
        if (!(containingDeclaration instanceof ClassDescriptor)) {
            return new ClassId(FqName.ROOT, name);
        }
        return getClassIdForNonLocalClass(containingDeclaration).createNestedClassId(name);
    }

    public static boolean isTopLevelDeclaration(@Nullable DeclarationDescriptor descriptor) {
        return descriptor != null && descriptor.getContainingDeclaration() instanceof PackageFragmentDescriptor;
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
    @SuppressWarnings("unchecked")
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
                return (D) descriptor;
            }
            descriptor = descriptor.getContainingDeclaration();
        }
        return null;
    }

    @Nullable
    public static ModuleDescriptor getContainingModuleOrNull(@NotNull KotlinType kotlinType) {
        ClassifierDescriptor descriptor = kotlinType.getConstructor().getDeclarationDescriptor();
        if (descriptor == null) return null;

        return getContainingModuleOrNull(descriptor);
    }

    @NotNull
    public static ModuleDescriptor getContainingModule(@NotNull DeclarationDescriptor descriptor) {
        ModuleDescriptor module = getContainingModuleOrNull(descriptor);
        assert module != null : "Descriptor without a containing module: " + descriptor;
        return module;
    }

    @Nullable
    public static ModuleDescriptor getContainingModuleOrNull(@NotNull DeclarationDescriptor descriptor) {
        while (descriptor != null) {
            if (descriptor instanceof ModuleDescriptor) {
                return (ModuleDescriptor) descriptor;
            }
            if (descriptor instanceof PackageViewDescriptor) {
                return ((PackageViewDescriptor) descriptor).getModule();
            }
            //noinspection ConstantConditions
            descriptor = descriptor.getContainingDeclaration();
        }
        return null;
    }

    @Nullable
    public static ClassDescriptor getContainingClass(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor containing = descriptor.getContainingDeclaration();
        while (containing != null) {
            if (containing instanceof ClassDescriptor && !isCompanionObject(containing)) {
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

    public static boolean isDirectSubclass(@NotNull ClassDescriptor subClass, @NotNull ClassDescriptor superClass) {
        for (KotlinType superType : subClass.getTypeConstructor().getSupertypes()) {
            if (isSameClass(superType, superClass.getOriginal())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSubclass(@NotNull ClassDescriptor subClass, @NotNull ClassDescriptor superClass) {
        return isSubtypeOfClass(subClass.getDefaultType(), superClass.getOriginal());
    }

    private static boolean isSameClass(@NotNull KotlinType type, @NotNull DeclarationDescriptor other) {
        DeclarationDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
        if (descriptor != null) {
            DeclarationDescriptor originalDescriptor = descriptor.getOriginal();
            if (originalDescriptor instanceof ClassifierDescriptor
                && other instanceof ClassifierDescriptor
                && ((ClassifierDescriptor) other).getTypeConstructor().equals(
                    ((ClassifierDescriptor) originalDescriptor).getTypeConstructor())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSubtypeOfClass(@NotNull KotlinType type, @NotNull DeclarationDescriptor superClass) {
        if (isSameClass(type, superClass)) return true;
        for (KotlinType superType : type.getConstructor().getSupertypes()) {
            if (isSubtypeOfClass(superType, superClass)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCompanionObject(@Nullable DeclarationDescriptor descriptor) {
        return isKindOf(descriptor, ClassKind.OBJECT) && ((ClassDescriptor) descriptor).isCompanionObject();
    }

    public static boolean isSealedClass(@Nullable DeclarationDescriptor descriptor) {
        return (isKindOf(descriptor, ClassKind.CLASS) || isKindOf(descriptor, ClassKind.INTERFACE)) && ((ClassDescriptor) descriptor).getModality() == Modality.SEALED;
    }

    public static boolean isAnonymousObject(@NotNull DeclarationDescriptor descriptor) {
        return isClass(descriptor) && descriptor.getName().equals(SpecialNames.NO_NAME_PROVIDED);
    }

    @SuppressWarnings("unused")
    public static boolean isAnonymousFunction(@NotNull DeclarationDescriptor descriptor) {
        return descriptor instanceof SimpleFunctionDescriptor &&
               descriptor.getName().equals(SpecialNames.ANONYMOUS);
    }

    public static boolean isNonCompanionObject(@Nullable DeclarationDescriptor descriptor) {
        return isKindOf(descriptor, ClassKind.OBJECT) && !((ClassDescriptor) descriptor).isCompanionObject();
    }

    public static boolean isObject(@Nullable DeclarationDescriptor descriptor) {
        return isKindOf(descriptor, ClassKind.OBJECT);
    }

    public static boolean isEnumEntry(@NotNull DeclarationDescriptor descriptor) {
        return isKindOf(descriptor, ClassKind.ENUM_ENTRY);
    }

    public static boolean isEnumClass(@Nullable DeclarationDescriptor descriptor) {
        return isKindOf(descriptor, ClassKind.ENUM_CLASS);
    }

    public static boolean isAnnotationClass(@Nullable DeclarationDescriptor descriptor) {
        return isKindOf(descriptor, ClassKind.ANNOTATION_CLASS);
    }

    public static boolean isInterface(@Nullable DeclarationDescriptor descriptor) {
        return isKindOf(descriptor, ClassKind.INTERFACE);
    }

    public static boolean isClass(@Nullable DeclarationDescriptor descriptor) {
        return isKindOf(descriptor, ClassKind.CLASS);
    }

    public static boolean isClassOrEnumClass(@Nullable DeclarationDescriptor descriptor) {
        return isClass(descriptor) || isEnumClass(descriptor);
    }

    private static boolean isKindOf(@Nullable DeclarationDescriptor descriptor, @NotNull ClassKind classKind) {
        return descriptor instanceof ClassDescriptor && ((ClassDescriptor) descriptor).getKind() == classKind;
    }

    public static boolean hasAbstractMembers(@NotNull ClassDescriptor classDescriptor) {
        for (DeclarationDescriptor member : getAllDescriptors(classDescriptor.getDefaultType().getMemberScope())) {
            if (member instanceof CallableMemberDescriptor &&
                ((CallableMemberDescriptor) member).getModality() == ABSTRACT) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public static List<ClassDescriptor> getSuperclassDescriptors(@NotNull ClassDescriptor classDescriptor) {
        Collection<KotlinType> superclassTypes = classDescriptor.getTypeConstructor().getSupertypes();
        List<ClassDescriptor> superClassDescriptors = new ArrayList<ClassDescriptor>();
        for (KotlinType type : superclassTypes) {
            ClassDescriptor result = getClassDescriptorForType(type);
            if (!isAny(result)) {
                superClassDescriptors.add(result);
            }
        }
        return superClassDescriptors;
    }

    @NotNull
    public static KotlinType getSuperClassType(@NotNull ClassDescriptor classDescriptor) {
        Collection<KotlinType> superclassTypes = classDescriptor.getTypeConstructor().getSupertypes();
        for (KotlinType type : superclassTypes) {
            ClassDescriptor superClassDescriptor = getClassDescriptorForType(type);
            if (superClassDescriptor.getKind() != ClassKind.INTERFACE) {
                return type;
            }
        }
        return getBuiltIns(classDescriptor).getAnyType();
    }

    @Nullable
    public static ClassDescriptor getSuperClassDescriptor(@NotNull ClassDescriptor classDescriptor) {
        Collection<KotlinType> superclassTypes = classDescriptor.getTypeConstructor().getSupertypes();
        for (KotlinType type : superclassTypes) {
            ClassDescriptor superClassDescriptor = getClassDescriptorForType(type);
            if (superClassDescriptor.getKind() != ClassKind.INTERFACE) {
                return superClassDescriptor;
            }
        }
        return null;
    }

    @NotNull
    public static ClassDescriptor getClassDescriptorForType(@NotNull KotlinType type) {
        return getClassDescriptorForTypeConstructor(type.getConstructor());
    }

    @NotNull
    public static ClassDescriptor getClassDescriptorForTypeConstructor(@NotNull TypeConstructor typeConstructor) {
        ClassifierDescriptor descriptor = typeConstructor.getDeclarationDescriptor();
        assert descriptor instanceof ClassDescriptor
            : "Classifier descriptor of a type should be of type ClassDescriptor: " + typeConstructor;
        return (ClassDescriptor) descriptor;
    }

    @NotNull
    public static DescriptorVisibility getDefaultConstructorVisibility(
            @NotNull ClassDescriptor classDescriptor,
            boolean freedomForSealedInterfacesSupported
    ) {
        ClassKind classKind = classDescriptor.getKind();
        if (classKind == ClassKind.ENUM_CLASS || classKind.isSingleton()) {
            return DescriptorVisibilities.PRIVATE;
        }
        if (isSealedClass(classDescriptor)) {
            if (freedomForSealedInterfacesSupported) {
                return DescriptorVisibilities.PROTECTED;
            } else {
                return DescriptorVisibilities.PRIVATE;
            }
        }
        if (isAnonymousObject(classDescriptor)) {
            return DescriptorVisibilities.DEFAULT_VISIBILITY;
        }
        assert classKind == ClassKind.CLASS || classKind == ClassKind.INTERFACE || classKind == ClassKind.ANNOTATION_CLASS;
        return DescriptorVisibilities.PUBLIC;
    }

    // TODO: should be internal
    @Nullable
    public static ClassDescriptor getInnerClassByName(@NotNull ClassDescriptor classDescriptor, @NotNull String innerClassName, @NotNull LookupLocation location) {
        ClassifierDescriptor classifier =
                classDescriptor.getDefaultType().getMemberScope().getContributedClassifier(Name.identifier(innerClassName), location);
        assert classifier instanceof ClassDescriptor :
                "Inner class " + innerClassName + " in " + classDescriptor + " should be instance of ClassDescriptor, but was: "
                + (classifier == null ? "null" : classifier.getClass());
        return (ClassDescriptor) classifier;
    }

    @Nullable
    public static KotlinType getReceiverParameterType(@Nullable ReceiverParameterDescriptor receiverParameterDescriptor) {
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
     * of the fake override in the supertypes, this method finds just only one of them.
     * TODO: probably some call-sites of this method are wrong, they should handle all super-declarations
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <D extends CallableMemberDescriptor> D unwrapFakeOverride(@NotNull D descriptor) {
        while (descriptor.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            Collection<? extends CallableMemberDescriptor> overridden = descriptor.getOverriddenDescriptors();
            if (overridden.isEmpty()) {
                throw new IllegalStateException("Fake override should have at least one overridden descriptor: " + descriptor);
            }
            descriptor = (D) overridden.iterator().next();
        }
        return descriptor;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static <D extends DeclarationDescriptorWithVisibility> D unwrapFakeOverrideToAnyDeclaration(@NotNull D descriptor) {
        if (descriptor instanceof CallableMemberDescriptor) {
            return (D) unwrapFakeOverride((CallableMemberDescriptor) descriptor);
        }

        return descriptor;
    }

    public static boolean shouldRecordInitializerForProperty(@NotNull VariableDescriptor variable, @NotNull KotlinType type) {
        if (variable.isVar() || KotlinTypeKt.isError(type)) return false;

        if (TypeUtils.acceptsNullable(type)) return true;

        KotlinBuiltIns builtIns = getBuiltIns(variable);
        return KotlinBuiltIns.isPrimitiveType(type) ||
               KotlinTypeChecker.DEFAULT.equalTypes(builtIns.getStringType(), type) ||
               KotlinTypeChecker.DEFAULT.equalTypes(builtIns.getNumber().getDefaultType(), type) ||
               KotlinTypeChecker.DEFAULT.equalTypes(builtIns.getAnyType(), type) ||
               UnsignedTypes.INSTANCE.isUnsignedType(type);
    }

    public static boolean classCanHaveAbstractFakeOverride(@NotNull ClassDescriptor classDescriptor) {
        return classCanHaveAbstractDeclaration(classDescriptor) || classDescriptor.isExpect();
    }

    public static boolean classCanHaveAbstractDeclaration(@NotNull ClassDescriptor classDescriptor) {
        return classDescriptor.getModality() == Modality.ABSTRACT
               || isSealedClass(classDescriptor)
               || classDescriptor.getKind() == ClassKind.ENUM_CLASS;
    }

    public static boolean classCanHaveOpenMembers(@NotNull ClassDescriptor classDescriptor) {
        return classDescriptor.getModality() != Modality.FINAL || classDescriptor.getKind() == ClassKind.ENUM_CLASS;
    }

    /**
     * @return original (not substituted) descriptors without any duplicates
     */
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
            D descriptor = (D) callableDescriptor.getOriginal();
            collectAllOverriddenDescriptors(descriptor, result);
            result.add(descriptor);
        }
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static <D extends CallableMemberDescriptor> Set<D> getAllOverriddenDeclarations(@NotNull D memberDescriptor) {
        Set<D> result = new HashSet<D>();
        for (CallableMemberDescriptor overriddenDeclaration : memberDescriptor.getOverriddenDescriptors()) {
            CallableMemberDescriptor.Kind kind = overriddenDeclaration.getKind();
            if (kind == DECLARATION) {
                result.add((D) overriddenDeclaration);
            }
            else if (kind == DELEGATION || kind == FAKE_OVERRIDE || kind == SYNTHESIZED) {
                //do nothing
            }
            else {
                throw new AssertionError("Unexpected callable kind " + kind);
            }
            result.addAll(getAllOverriddenDeclarations((D) overriddenDeclaration));
        }
        return result;
    }

    public static boolean isSingletonOrAnonymousObject(@NotNull ClassDescriptor classDescriptor) {
        return classDescriptor.getKind().isSingleton() || isAnonymousObject(classDescriptor);
    }

    public static boolean canHaveDeclaredConstructors(@NotNull ClassDescriptor classDescriptor) {
        return !isSingletonOrAnonymousObject(classDescriptor) && !isInterface(classDescriptor);
    }

    @Nullable
    public static String getJvmName(@NotNull Annotated annotated) {
        return getJvmName(findJvmNameAnnotation(annotated));
    }

    @Nullable
    private static String getJvmName(@Nullable AnnotationDescriptor jvmNameAnnotation) {
        if (jvmNameAnnotation == null) return null;

        Map<Name, ConstantValue<?>> arguments = jvmNameAnnotation.getAllValueArguments();
        if (arguments.isEmpty()) return null;

        ConstantValue<?> name = arguments.values().iterator().next();
        if (!(name instanceof StringValue)) return null;

        return ((StringValue) name).getValue();
    }

    @Nullable
    public static AnnotationDescriptor findJvmNameAnnotation(@NotNull Annotated annotated) {
        return annotated.getAnnotations().findAnnotation(JVM_NAME);
    }

    public static boolean hasJvmNameAnnotation(@NotNull Annotated annotated) {
        return findJvmNameAnnotation(annotated) != null;
    }

    @NotNull
    public static SourceFile getContainingSourceFile(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof PropertySetterDescriptor) {
            descriptor = ((PropertySetterDescriptor) descriptor).getCorrespondingProperty();
        }

        if (descriptor instanceof DeclarationDescriptorWithSource) {
            return ((DeclarationDescriptorWithSource) descriptor).getSource().getContainingFile();
        }

        return SourceFile.NO_SOURCE_FILE;
    }

    @NotNull
    public static Collection<DeclarationDescriptor> getAllDescriptors(@NotNull MemberScope scope) {
        return scope.getContributedDescriptors(DescriptorKindFilter.ALL, MemberScope.Companion.getALL_NAME_FILTER());
    }

    @NotNull
    public static FunctionDescriptor getFunctionByName(@NotNull MemberScope scope, @NotNull Name name) {
        FunctionDescriptor result = getFunctionByNameOrNull(scope, name);

        if (result == null) {
            throw new IllegalStateException("Function not found");
        }

        return result;
    }

    @Nullable
    public static FunctionDescriptor getFunctionByNameOrNull(@NotNull MemberScope scope, @NotNull Name name) {
        for (SimpleFunctionDescriptor d : scope.getContributedFunctions(name, NoLookupLocation.FROM_BACKEND)) {
            if (name.equals(d.getOriginal().getName())) {
                return d;
            }
        }

        return null;
    }

    @NotNull
    public static PropertyDescriptor getPropertyByName(@NotNull MemberScope scope, @NotNull Name name) {
        for (PropertyDescriptor d : scope.getContributedVariables(name, NoLookupLocation.FROM_BACKEND)) {
            if (name.equals(d.getOriginal().getName())) {
                return d;
            }
        }

        throw new IllegalStateException("Property not found");
    }

    @NotNull
    public static CallableMemberDescriptor getDirectMember(@NotNull CallableMemberDescriptor descriptor) {
        return descriptor instanceof PropertyAccessorDescriptor
               ? ((PropertyAccessorDescriptor) descriptor).getCorrespondingProperty()
               : descriptor;
    }

    public static boolean isMethodOfAny(@NotNull CallableMemberDescriptor descriptor) {
        if (!(descriptor instanceof FunctionDescriptor)) return false;

        String name = descriptor.getName().asString();
        List<ValueParameterDescriptor> parameters = descriptor.getValueParameters();
        if (parameters.isEmpty()) {
            return name.equals("hashCode") || name.equals("toString");
        }
        else if (parameters.size() == 1 && name.equals("equals")) {
            return isNullableAny(parameters.get(0).getType());
        }
        return false;
    }
}
