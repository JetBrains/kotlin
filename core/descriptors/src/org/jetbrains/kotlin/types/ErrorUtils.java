/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.types;

import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.DefaultBuiltIns;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.kotlin.incremental.components.LookupLocation;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.types.error.ErrorSimpleFunctionDescriptorImpl;
import org.jetbrains.kotlin.utils.Printer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptySet;
import static kotlin.collections.CollectionsKt.emptyList;

public class ErrorUtils {
    private static final ModuleDescriptor ERROR_MODULE;

    static {
        ERROR_MODULE = new ModuleDescriptor() {
            @Nullable
            @Override
            public <T> T getCapability(@NotNull Capability<T> capability) {
                return null;
            }

            @NotNull
            @Override
            public Annotations getAnnotations() {
                return Annotations.Companion.getEMPTY();
            }

            @NotNull
            @Override
            public Collection<FqName> getSubPackagesOf(@NotNull FqName fqName, @NotNull Function1<? super Name, Boolean> nameFilter) {
                return emptyList();
            }

            @NotNull
            @Override
            public Name getName() {
                return Name.special("<ERROR MODULE>");
            }

            @NotNull
            @Override
            public PackageViewDescriptor getPackage(@NotNull FqName fqName) {
                throw new IllegalStateException("Should not be called!");
            }

            @NotNull
            @Override
            public List<ModuleDescriptor> getAllDependencyModules() {
                return emptyList();
            }

            @NotNull
            @Override
            public Set<ModuleDescriptor> getAllImplementingModules() {
                return emptySet();
            }

            @Override
            public <R, D> R accept(@NotNull DeclarationDescriptorVisitor<R, D> visitor, D data) {
                return null;
            }

            @Override
            public void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor) {
            }

            @Override
            public boolean shouldSeeInternalsOf(@NotNull ModuleDescriptor targetModule) {
                return false;
            }

            @NotNull
            @Override
            public DeclarationDescriptor getOriginal() {
                return this;
            }

            @Nullable
            @Override
            public DeclarationDescriptor getContainingDeclaration() {
                return null;
            }

            @NotNull
            @Override
            public KotlinBuiltIns getBuiltIns() {
                return DefaultBuiltIns.getInstance();
            }

            @Override
            public boolean isValid() {
                return false;
            }

            @Override
            public void assertValid() {
                throw new IllegalStateException("ERROR_MODULE is not a valid module");
            }
        };
    }

    public static boolean containsErrorType(@NotNull CallableDescriptor callableDescriptor) {
        if (callableDescriptor instanceof FunctionDescriptor) {
            return containsErrorType((FunctionDescriptor) callableDescriptor);
        }
        else {
            return containsErrorType(callableDescriptor.getReturnType());
        }
    }

    public static boolean containsErrorType(@NotNull FunctionDescriptor function) {
        if (containsErrorType(function.getReturnType())) {
            return true;
        }
        ReceiverParameterDescriptor receiverParameter = function.getExtensionReceiverParameter();
        if (receiverParameter != null && containsErrorType(receiverParameter.getType())) {
            return true;
        }
        for (ValueParameterDescriptor parameter : function.getValueParameters()) {
            if (containsErrorType(parameter.getType())) {
                return true;
            }
        }
        for (TypeParameterDescriptor parameter : function.getTypeParameters()) {
            for (KotlinType upperBound : parameter.getUpperBounds()) {
                if (containsErrorType(upperBound)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static class ErrorScope implements MemberScope {
        private final String debugMessage;

        private ErrorScope(@NotNull String debugMessage) {
            this.debugMessage = debugMessage;
        }

        @Nullable
        @Override
        public ClassifierDescriptor getContributedClassifier(@NotNull Name name, @NotNull LookupLocation location) {
            return createErrorClass(name.asString());
        }

        @NotNull
        @Override
        // TODO: Convert to Kotlin or add @JvmWildcard to MemberScope declarations
        // method is covariantly overridden in Kotlin, but collections in Java are invariant
        @SuppressWarnings({"unchecked"})
        public Set getContributedVariables(@NotNull Name name, @NotNull LookupLocation location) {
            return ERROR_PROPERTY_GROUP;
        }

        @NotNull
        @Override
        // TODO: Convert to Kotlin or add @JvmWildcard to MemberScope declarations
        // method is covariantly overridden in Kotlin, but collections in Java are invariant
        @SuppressWarnings({"unchecked"})
        public Set getContributedFunctions(@NotNull Name name, @NotNull LookupLocation location) {
            return Collections.singleton(createErrorFunction(this));
        }

        @NotNull
        @Override
        public Set<Name> getFunctionNames() {
            return emptySet();
        }

        @NotNull
        @Override
        public Set<Name> getVariableNames() {
            return emptySet();
        }

        @NotNull
        @Override
        public Collection<DeclarationDescriptor> getContributedDescriptors(
                @NotNull DescriptorKindFilter kindFilter, @NotNull Function1<? super Name, Boolean> nameFilter
        ) {
            return Collections.emptyList();
        }

        @Override
        public boolean definitelyDoesNotContainName(@NotNull Name name) {
            return false;
        }

        @Override
        public String toString() {
            return "ErrorScope{" + debugMessage + '}';
        }

        @Override
        public void printScopeStructure(@NotNull Printer p) {
            p.println(getClass().getSimpleName(), ": ", debugMessage);
        }
    }

    private static class ThrowingScope implements MemberScope {
        private final String debugMessage;

        private ThrowingScope(@NotNull String message) {
            debugMessage = message;
        }

        @Nullable
        @Override
        public ClassifierDescriptor getContributedClassifier(@NotNull Name name, @NotNull LookupLocation location) {
            throw new IllegalStateException(debugMessage+", required name: " + name);
        }

        @NotNull
        @Override
        @SuppressWarnings({"unchecked"}) // KT-9898 Impossible implement kotlin interface from java
        public Collection getContributedVariables(@NotNull Name name, @NotNull LookupLocation location) {
            throw new IllegalStateException(debugMessage+", required name: " + name);
        }

        @NotNull
        @Override
        // TODO: Convert to Kotlin or add @JvmWildcard to MemberScope declarations
        // method is covariantly overridden in Kotlin, but collections in Java are invariant
        @SuppressWarnings({"unchecked"})
        public Collection getContributedFunctions(@NotNull Name name, @NotNull LookupLocation location) {
            throw new IllegalStateException(debugMessage+", required name: " + name);
        }

        @NotNull
        @Override
        public Collection<DeclarationDescriptor> getContributedDescriptors(
                @NotNull DescriptorKindFilter kindFilter, @NotNull Function1<? super Name, Boolean> nameFilter
        ) {
            throw new IllegalStateException(debugMessage);
        }

        @NotNull
        @Override
        public Set<Name> getFunctionNames() {
            throw new IllegalStateException();
        }

        @NotNull
        @Override
        public Set<Name> getVariableNames() {
            throw new IllegalStateException();
        }

        @Override
        public boolean definitelyDoesNotContainName(@NotNull Name name) {
            return false;
        }

        @Override
        public String toString() {
            return "ThrowingScope{" + debugMessage + '}';
        }

        @Override
        public void printScopeStructure(@NotNull Printer p) {
            p.println(getClass().getSimpleName(), ": ", debugMessage);
        }
    }

    private static final ErrorClassDescriptor ERROR_CLASS = new ErrorClassDescriptor(Name.special("<ERROR CLASS>"));

    private static class ErrorClassDescriptor extends ClassDescriptorImpl {
        public ErrorClassDescriptor(@NotNull Name name) {
            super(getErrorModule(), name,
                  Modality.OPEN, ClassKind.CLASS, Collections.<KotlinType>emptyList(), SourceElement.NO_SOURCE,
                  /* isExternal = */ false
            );

            ClassConstructorDescriptorImpl
                    errorConstructor = ClassConstructorDescriptorImpl.create(this, Annotations.Companion.getEMPTY(), true, SourceElement.NO_SOURCE);
            errorConstructor.initialize(Collections.<ValueParameterDescriptor>emptyList(),
                                        Visibilities.INTERNAL);
            MemberScope memberScope = createErrorScope(getName().asString());
            errorConstructor.setReturnType(
                    new ErrorType(
                            createErrorTypeConstructorWithCustomDebugName("<ERROR>", this),
                            memberScope
                    )
            );

            initialize(memberScope, Collections.<ClassConstructorDescriptor>singleton(errorConstructor), errorConstructor);
        }

        @NotNull
        @Override
        public ClassDescriptor substitute(@NotNull TypeSubstitutor substitutor) {
            return this;
        }

        @Override
        public String toString() {
            return getName().asString();
        }

        @NotNull
        @Override
        public MemberScope getMemberScope(@NotNull List<? extends TypeProjection> typeArguments) {
            return createErrorScope("Error scope for class " + getName() + " with arguments: " + typeArguments);
        }

        @NotNull
        @Override
        public MemberScope getMemberScope(@NotNull TypeSubstitution typeSubstitution) {
            return createErrorScope("Error scope for class " + getName() + " with arguments: " + typeSubstitution);
        }
    }

    @NotNull
    public static ClassDescriptor createErrorClass(@NotNull String debugMessage) {
        return new ErrorClassDescriptor(Name.special("<ERROR CLASS: " + debugMessage + ">"));
    }

    @NotNull
    public static ClassDescriptor createErrorClassWithExactName(@NotNull Name name) {
        return new ErrorClassDescriptor(name);
    }

    @NotNull
    public static MemberScope createErrorScope(@NotNull String debugMessage) {
        return createErrorScope(debugMessage, false);
    }

    @NotNull
    public static MemberScope createErrorScope(@NotNull String debugMessage, boolean throwExceptions) {
        if (throwExceptions) {
            return new ThrowingScope(debugMessage);
        }
        return new ErrorScope(debugMessage);
    }

    // Do not move it into AbstractTypeConstructor.Companion because of cycle in initialization(see KT-13264)
    public static final SimpleType ERROR_TYPE_FOR_LOOP_IN_SUPERTYPES = createErrorType("<LOOP IN SUPERTYPES>");

    private static final KotlinType ERROR_PROPERTY_TYPE = createErrorType("<ERROR PROPERTY TYPE>");
    private static final PropertyDescriptor ERROR_PROPERTY = createErrorProperty();

    private static final Set<PropertyDescriptor> ERROR_PROPERTY_GROUP = Collections.singleton(ERROR_PROPERTY);

    @NotNull
    private static PropertyDescriptorImpl createErrorProperty() {
        PropertyDescriptorImpl descriptor = PropertyDescriptorImpl.create(
                ERROR_CLASS,
                Annotations.Companion.getEMPTY(),
                Modality.OPEN,
                Visibilities.PUBLIC,
                true,
                Name.special("<ERROR PROPERTY>"),
                CallableMemberDescriptor.Kind.DECLARATION,
                SourceElement.NO_SOURCE,
                false, false, false, false, false, false
        );
        descriptor.setType(ERROR_PROPERTY_TYPE,
                           Collections.<TypeParameterDescriptor>emptyList(),
                           null,
                           (KotlinType) null
        );

        return descriptor;
    }

    @NotNull
    private static SimpleFunctionDescriptor createErrorFunction(@NotNull ErrorScope ownerScope) {
        ErrorSimpleFunctionDescriptorImpl function = new ErrorSimpleFunctionDescriptorImpl(ERROR_CLASS, ownerScope);
        function.initialize(
                null,
                null,
                Collections.<TypeParameterDescriptorImpl>emptyList(), // TODO
                Collections.<ValueParameterDescriptor>emptyList(), // TODO
                createErrorType("<ERROR FUNCTION RETURN TYPE>"),
                Modality.OPEN,
                Visibilities.PUBLIC
        );
        return function;
    }

    @NotNull
    public static SimpleType createErrorType(@NotNull String debugMessage) {
        return createErrorTypeWithArguments(debugMessage, Collections.<TypeProjection>emptyList());
    }

    @NotNull
    public static SimpleType createErrorTypeWithCustomDebugName(@NotNull String debugName) {
        return createErrorTypeWithCustomConstructor(debugName, createErrorTypeConstructorWithCustomDebugName(debugName));
    }

    @NotNull
    public static SimpleType createErrorTypeWithCustomConstructor(@NotNull String debugName, @NotNull TypeConstructor typeConstructor) {
        return new ErrorType(typeConstructor, createErrorScope(debugName));
    }

    @NotNull
    public static SimpleType createErrorTypeWithArguments(@NotNull String debugMessage, @NotNull List<TypeProjection> arguments) {
        return new ErrorType(createErrorTypeConstructor(debugMessage), createErrorScope(debugMessage), arguments, false);
    }

    @NotNull
    public static SimpleType createUnresolvedType(@NotNull String presentableName, @NotNull List<TypeProjection> arguments) {
        return new UnresolvedType(presentableName, createErrorTypeConstructor(presentableName), createErrorScope(presentableName),
                                  arguments, false);
    }

    @NotNull
    public static TypeConstructor createErrorTypeConstructor(@NotNull String debugMessage) {
        return createErrorTypeConstructorWithCustomDebugName("[ERROR : " + debugMessage + "]", ERROR_CLASS);
    }

    @NotNull
    public static TypeConstructor createErrorTypeConstructorWithCustomDebugName(@NotNull String debugName) {
        return createErrorTypeConstructorWithCustomDebugName(debugName, ERROR_CLASS);
    }

    @NotNull
    private static TypeConstructor createErrorTypeConstructorWithCustomDebugName(
            @NotNull final String debugName, @NotNull final ErrorClassDescriptor errorClass
    ) {
        return new TypeConstructor() {
            @NotNull
            @Override
            public List<TypeParameterDescriptor> getParameters() {
                return emptyList();
            }

            @NotNull
            @Override
            public Collection<KotlinType> getSupertypes() {
                return emptyList();
            }

            @Override
            public boolean isFinal() {
                return false;
            }

            @Override
            public boolean isDenotable() {
                return false;
            }

            @Nullable
            @Override
            public ClassifierDescriptor getDeclarationDescriptor() {
                return errorClass;
            }

            @NotNull
            @Override
            public KotlinBuiltIns getBuiltIns() {
                return DefaultBuiltIns.getInstance();
            }

            @Override
            public String toString() {
                return debugName;
            }
        };
    }

    public static boolean containsErrorType(@Nullable KotlinType type) {
        if (type == null) return false;
        if (KotlinTypeKt.isError(type)) return true;
        for (TypeProjection projection : type.getArguments()) {
            if (!projection.isStarProjection() && containsErrorType(projection.getType())) return true;
        }
        return false;
    }

    public static boolean isError(@Nullable DeclarationDescriptor candidate) {
        if (candidate == null) return false;
        return isErrorClass(candidate) || isErrorClass(candidate.getContainingDeclaration()) || candidate == ERROR_MODULE;
    }

    private static boolean isErrorClass(@Nullable DeclarationDescriptor candidate) {
        return candidate instanceof ErrorClassDescriptor;
    }

    @NotNull
    public static ModuleDescriptor getErrorModule() {
        return ERROR_MODULE;
    }

    public static boolean isUninferredParameter(@Nullable KotlinType type) {
        return type != null && type.getConstructor() instanceof UninferredParameterTypeConstructor;
    }

    public static boolean containsUninferredParameter(@Nullable KotlinType type) {
        return TypeUtils.contains(type, new Function1<UnwrappedType, Boolean>() {
            @Override
            public Boolean invoke(UnwrappedType argumentType) {
                return isUninferredParameter(argumentType);
            }
        });
    }

    @NotNull
    public static KotlinType createUninferredParameterType(@NotNull TypeParameterDescriptor typeParameterDescriptor) {
        return createErrorTypeWithCustomConstructor("Scope for error type for not inferred parameter: " + typeParameterDescriptor.getName(),
                                                    new UninferredParameterTypeConstructor(typeParameterDescriptor));
    }

    public static class UninferredParameterTypeConstructor implements TypeConstructor {
        private final TypeParameterDescriptor typeParameterDescriptor;
        private final TypeConstructor errorTypeConstructor;

        private UninferredParameterTypeConstructor(@NotNull TypeParameterDescriptor descriptor) {
            typeParameterDescriptor = descriptor;
            errorTypeConstructor = createErrorTypeConstructorWithCustomDebugName("CANT_INFER_TYPE_PARAMETER: " + descriptor.getName());
        }

        @NotNull
        public TypeParameterDescriptor getTypeParameterDescriptor() {
            return typeParameterDescriptor;
        }

        @NotNull
        @Override
        public List<TypeParameterDescriptor> getParameters() {
            return errorTypeConstructor.getParameters();
        }

        @NotNull
        @Override
        public Collection<KotlinType> getSupertypes() {
            return errorTypeConstructor.getSupertypes();
        }

        @Override
        public boolean isFinal() {
            return errorTypeConstructor.isFinal();
        }

        @Override
        public boolean isDenotable() {
            return errorTypeConstructor.isDenotable();
        }

        @Nullable
        @Override
        public ClassifierDescriptor getDeclarationDescriptor() {
            return errorTypeConstructor.getDeclarationDescriptor();
        }

        @NotNull
        @Override
        public KotlinBuiltIns getBuiltIns() {
            return DescriptorUtilsKt.getBuiltIns(typeParameterDescriptor);
        }
    }

    private ErrorUtils() {}
}
