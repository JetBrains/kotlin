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

package org.jetbrains.kotlin.types;

import kotlin.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.*;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.platform.PlatformToKotlinClassMap;
import org.jetbrains.kotlin.resolve.ImportPath;
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.types.error.ErrorSimpleFunctionDescriptorImpl;
import org.jetbrains.kotlin.utils.Printer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static kotlin.KotlinPackage.joinToString;

public class ErrorUtils {

    private static final ModuleDescriptor ERROR_MODULE;
    static {
        ERROR_MODULE = new ModuleDescriptorImpl(
                Name.special("<ERROR MODULE>"),
                Collections.<ImportPath>emptyList(),
                PlatformToKotlinClassMap.EMPTY
        );
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
            for (JetType upperBound : parameter.getUpperBounds()) {
                if (containsErrorType(upperBound)) {
                    return true;
                }
            }
        }

        return false;
    }


    public static class ErrorScope implements JetScope {
        private final String debugMessage;

        private ErrorScope(@NotNull String debugMessage) {
            this.debugMessage = debugMessage;
        }

        @Override
        public ClassifierDescriptor getClassifier(@NotNull Name name) {
            return createErrorClass(name.asString());
        }

        @NotNull
        @Override
        public Set<VariableDescriptor> getProperties(@NotNull Name name) {
            return ERROR_PROPERTY_GROUP;
        }

        @Override
        public VariableDescriptor getLocalVariable(@NotNull Name name) {
            return ERROR_PROPERTY;
        }

        @Override
        public PackageViewDescriptor getPackage(@NotNull Name name) {
            return null;
        }

        @NotNull
        @Override
        public List<ReceiverParameterDescriptor> getImplicitReceiversHierarchy() {
            return Collections.emptyList();
        }

        @NotNull
        @Override
        public Set<FunctionDescriptor> getFunctions(@NotNull Name name) {
            return Collections.<FunctionDescriptor>singleton(createErrorFunction(this));
        }

        @NotNull
        @Override
        public DeclarationDescriptor getContainingDeclaration() {
            return ERROR_MODULE;
        }

        @NotNull
        @Override
        public Collection<DeclarationDescriptor> getDeclarationsByLabel(@NotNull Name labelName) {
            return Collections.emptyList();
        }

        @NotNull
        @Override
        public Collection<DeclarationDescriptor> getDescriptors(
                @NotNull DescriptorKindFilter kindFilter, @NotNull Function1<? super Name, ? extends Boolean> nameFilter
        ) {
            return Collections.emptyList();
        }

        @NotNull
        @Override
        public Collection<DeclarationDescriptor> getAllDescriptors() {
            return Collections.emptyList();
        }

        @NotNull
        @Override
        public Collection<DeclarationDescriptor> getOwnDeclaredDescriptors() {
            return Collections.emptyList();
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

    private static class ThrowingScope implements JetScope {
        private final String debugMessage;

        private ThrowingScope(@NotNull String message) {
            debugMessage = message;
        }

        @Nullable
        @Override
        public ClassifierDescriptor getClassifier(@NotNull Name name) {
            throw new IllegalStateException();
        }

        @Nullable
        @Override
        public PackageViewDescriptor getPackage(@NotNull Name name) {
            throw new IllegalStateException();
        }

        @NotNull
        @Override
        public Collection<VariableDescriptor> getProperties(@NotNull Name name) {
            throw new IllegalStateException();
        }

        @Nullable
        @Override
        public VariableDescriptor getLocalVariable(@NotNull Name name) {
            throw new IllegalStateException();
        }

        @NotNull
        @Override
        public Collection<FunctionDescriptor> getFunctions(@NotNull Name name) {
            throw new IllegalStateException();
        }

        @NotNull
        @Override
        public DeclarationDescriptor getContainingDeclaration() {
            return ERROR_MODULE;
        }

        @NotNull
        @Override
        public Collection<DeclarationDescriptor> getDeclarationsByLabel(@NotNull Name labelName) {
            throw new IllegalStateException();
        }

        @NotNull
        @Override
        public Collection<DeclarationDescriptor> getDescriptors(
                @NotNull DescriptorKindFilter kindFilter, @NotNull Function1<? super Name, ? extends Boolean> nameFilter
        ) {
            throw new IllegalStateException();
        }

        @NotNull
        @Override
        public Collection<DeclarationDescriptor> getAllDescriptors() {
            throw new IllegalStateException();
        }

        @NotNull
        @Override
        public List<ReceiverParameterDescriptor> getImplicitReceiversHierarchy() {
            throw new IllegalStateException();
        }

        @NotNull
        @Override
        public Collection<DeclarationDescriptor> getOwnDeclaredDescriptors() {
            throw new IllegalStateException();
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

    private static final ErrorClassDescriptor ERROR_CLASS = new ErrorClassDescriptor(null);

    private static class ErrorClassDescriptor extends ClassDescriptorImpl {
        public ErrorClassDescriptor(@Nullable String name) {
            super(getErrorModule(), Name.special(name == null ? "<ERROR CLASS>" : "<ERROR CLASS: " + name + ">"),
                  Modality.OPEN, Collections.<JetType>emptyList(), SourceElement.NO_SOURCE);

            ConstructorDescriptorImpl errorConstructor = ConstructorDescriptorImpl.create(this, Annotations.EMPTY, true, SourceElement.NO_SOURCE);
            errorConstructor.initialize(Collections.<TypeParameterDescriptor>emptyList(), Collections.<ValueParameterDescriptor>emptyList(),
                                        Visibilities.INTERNAL);
            JetScope memberScope = createErrorScope(getName().asString());
            errorConstructor.setReturnType(
                    new ErrorTypeImpl(
                            TypeConstructorImpl.createForClass(
                                    this, Annotations.EMPTY, false,
                                    getName().asString(),
                                    Collections.<TypeParameterDescriptorImpl>emptyList(),
                                    Collections.singleton(KotlinBuiltIns.getInstance().getAnyType())
                            ),
                            memberScope
                    )
            );

            initialize(memberScope, Collections.<ConstructorDescriptor>singleton(errorConstructor), errorConstructor);
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
        public JetScope getMemberScope(@NotNull List<? extends TypeProjection> typeArguments) {
            return createErrorScope("Error scope for class " + getName() + " with arguments: " + typeArguments);
        }
    }

    @NotNull
    public static ClassDescriptor createErrorClass(@NotNull String debugMessage) {
        return new ErrorClassDescriptor(debugMessage);
    }

    @NotNull
    public static JetScope createErrorScope(@NotNull String debugMessage) {
        return createErrorScope(debugMessage, false);
    }

    @NotNull
    public static JetScope createErrorScope(@NotNull String debugMessage, boolean throwExceptions) {
        if (throwExceptions) {
            return new ThrowingScope(debugMessage);
        }
        return new ErrorScope(debugMessage);
    }

    private static final JetType ERROR_PROPERTY_TYPE = createErrorType("<ERROR PROPERTY TYPE>");
    private static final VariableDescriptor ERROR_PROPERTY = createErrorProperty();

    private static final Set<VariableDescriptor> ERROR_PROPERTY_GROUP = Collections.singleton(ERROR_PROPERTY);

    @NotNull
    private static PropertyDescriptorImpl createErrorProperty() {
        PropertyDescriptorImpl descriptor = PropertyDescriptorImpl.create(
                ERROR_CLASS,
                Annotations.EMPTY,
                Modality.OPEN,
                Visibilities.INTERNAL,
                true,
                Name.special("<ERROR PROPERTY>"),
                CallableMemberDescriptor.Kind.DECLARATION,
                SourceElement.NO_SOURCE
        );
        descriptor.setType(ERROR_PROPERTY_TYPE,
                           Collections.<TypeParameterDescriptor>emptyList(),
                           ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER,
                           (JetType) null
        );

        return descriptor;
    }

    @NotNull
    private static SimpleFunctionDescriptor createErrorFunction(@NotNull ErrorScope ownerScope) {
        ErrorSimpleFunctionDescriptorImpl function = new ErrorSimpleFunctionDescriptorImpl(ERROR_CLASS, ownerScope);
        function.initialize(
                null,
                ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER,
                Collections.<TypeParameterDescriptorImpl>emptyList(), // TODO
                Collections.<ValueParameterDescriptor>emptyList(), // TODO
                createErrorType("<ERROR FUNCTION RETURN TYPE>"),
                Modality.OPEN,
                Visibilities.INTERNAL
        );
        return function;
    }

    @NotNull
    public static JetType createErrorType(@NotNull String debugMessage) {
        return createErrorTypeWithArguments(debugMessage, Collections.<TypeProjection>emptyList());
    }

    @NotNull
    public static JetType createErrorTypeWithCustomDebugName(@NotNull String debugName) {
        return new ErrorTypeImpl(createErrorTypeConstructorWithCustomDebugName(debugName), createErrorScope(debugName));
    }

    @NotNull
    public static JetType createErrorTypeWithArguments(@NotNull String debugMessage, @NotNull List<TypeProjection> arguments) {
        return new ErrorTypeImpl(createErrorTypeConstructor(debugMessage), createErrorScope(debugMessage), arguments);
    }

    @NotNull
    public static TypeConstructor createErrorTypeConstructor(@NotNull String debugMessage) {
        return createErrorTypeConstructorWithCustomDebugName("[ERROR : " + debugMessage + "]");
    }

    @NotNull
    private static TypeConstructor createErrorTypeConstructorWithCustomDebugName(@NotNull String debugName) {
        return TypeConstructorImpl.createForClass(ERROR_CLASS, Annotations.EMPTY, false, debugName,
                                Collections.<TypeParameterDescriptorImpl>emptyList(),
                                Collections.singleton(KotlinBuiltIns.getInstance().getAnyType()));
    }

    public static boolean containsErrorType(@Nullable JetType type) {
        if (type == null) return false;
        if (type.isError()) return true;
        for (TypeProjection projection : type.getArguments()) {
            if (!projection.isStarProjection() && containsErrorType(projection.getType())) return true;
        }
        return false;
    }

    public static boolean isError(@NotNull DeclarationDescriptor candidate) {
        return isErrorClass(candidate) || isErrorClass(candidate.getContainingDeclaration()) || candidate == ERROR_MODULE;
    }

    private static boolean isErrorClass(@Nullable DeclarationDescriptor candidate) {
        return candidate instanceof ErrorClassDescriptor;
    }

    @NotNull
    public static TypeParameterDescriptor createErrorTypeParameter(int index, @NotNull String debugMessage) {
        return TypeParameterDescriptorImpl.createWithDefaultBound(
                ERROR_CLASS,
                Annotations.EMPTY,
                false,
                Variance.INVARIANT,
                Name.special("<ERROR: " + debugMessage + ">"),
                index
        );
    }

    private static class ErrorTypeImpl implements JetType {
        private final TypeConstructor constructor;
        private final JetScope memberScope;
        private final List<TypeProjection> arguments;

        private ErrorTypeImpl(
                @NotNull TypeConstructor constructor,
                @NotNull JetScope memberScope,
                @NotNull List<TypeProjection> arguments
        ) {
            this.constructor = constructor;
            this.memberScope = memberScope;
            this.arguments = arguments;
        }

        private ErrorTypeImpl(@NotNull TypeConstructor constructor, @NotNull JetScope memberScope) {
            this(constructor, memberScope, Collections.<TypeProjection>emptyList());
        }

        @NotNull
        @Override
        public TypeConstructor getConstructor() {
            return constructor;
        }

        @NotNull
        @Override
        public List<TypeProjection> getArguments() {
            return arguments;
        }

        @Override
        public boolean isMarkedNullable() {
            return false;
        }

        @NotNull
        @Override
        public JetScope getMemberScope() {
            return memberScope;
        }

        @Override
        public boolean isError() {
            return true;
        }

        @NotNull
        @Override
        public Annotations getAnnotations() {
            return Annotations.EMPTY;
        }

        @Nullable
        @Override
        public <T extends TypeCapability> T getCapability(@NotNull Class<T> capabilityClass) {
            return null;
        }

        @Override
        public String toString() {
            return constructor.toString() + (arguments.isEmpty() ? "" : joinToString(arguments, ", ", "<", ">", -1, "..."));
        }
    }

    @NotNull
    public static ModuleDescriptor getErrorModule() {
        return ERROR_MODULE;
    }

    public static boolean isUninferredParameter(@Nullable JetType type) {
        return type != null && type.getConstructor() instanceof UninferredParameterTypeConstructor;
    }

    public static boolean containsUninferredParameter(@Nullable JetType type) {
        return TypeUtils.containsSpecialType(type, new Function1<JetType, Boolean>() {
            @Override
            public Boolean invoke(JetType argumentType) {
                return isUninferredParameter(argumentType);
            }
        });
    }

    @NotNull
    public static JetType createUninferredParameterType(@NotNull TypeParameterDescriptor typeParameterDescriptor) {
        return new ErrorTypeImpl(
                new UninferredParameterTypeConstructor(typeParameterDescriptor),
                createErrorScope("Scope for error type for not inferred parameter: " + typeParameterDescriptor.getName()));
    }

    public static class UninferredParameterTypeConstructor implements TypeConstructor {
        private final TypeParameterDescriptor typeParameterDescriptor;
        private final TypeConstructor errorTypeConstructor;

        public UninferredParameterTypeConstructor(@NotNull TypeParameterDescriptor descriptor) {
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
        public Collection<JetType> getSupertypes() {
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
        public Annotations getAnnotations() {
            return errorTypeConstructor.getAnnotations();
        }
    }

    private ErrorUtils() {}
}
