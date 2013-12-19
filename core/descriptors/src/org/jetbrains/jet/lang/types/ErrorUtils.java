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

package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.PropertyDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.name.LabelName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.error.ErrorClassDescriptor;
import org.jetbrains.jet.lang.types.error.ErrorSimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.utils.Printer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
        ReceiverParameterDescriptor receiverParameter = function.getReceiverParameter();
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
            return ERROR_CLASS;
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
        public Collection<DeclarationDescriptor> getDeclarationsByLabel(@NotNull LabelName labelName) {
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
        public Collection<DeclarationDescriptor> getDeclarationsByLabel(@NotNull LabelName labelName) {
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

    private static final ErrorClassDescriptor ERROR_CLASS = new ErrorClassDescriptor("");

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
    private static final VariableDescriptor ERROR_PROPERTY = new PropertyDescriptorImpl(
            ERROR_CLASS,
            Collections.<AnnotationDescriptor>emptyList(),
            Modality.OPEN,
            Visibilities.INTERNAL,
            true,
            null,
            ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER,
            Name.special("<ERROR PROPERTY>"),
            ERROR_PROPERTY_TYPE,
            CallableMemberDescriptor.Kind.DECLARATION);
    private static final Set<VariableDescriptor> ERROR_PROPERTY_GROUP = Collections.singleton(ERROR_PROPERTY);

    @NotNull
    private static SimpleFunctionDescriptor createErrorFunction(@NotNull ErrorScope ownerScope) {
        ErrorSimpleFunctionDescriptorImpl function = new ErrorSimpleFunctionDescriptorImpl(ownerScope);
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
        return new ErrorTypeImpl(createErrorTypeConstructor(debugMessage), createErrorScope(debugMessage));
    }

    @NotNull
    public static JetType createErrorTypeWithCustomDebugName(@NotNull String debugName) {
        return new ErrorTypeImpl(createErrorTypeConstructorWithCustomDebugName(debugName), createErrorScope(debugName));
    }

    @NotNull
    public static TypeConstructor createErrorTypeConstructor(@NotNull String debugMessage) {
        return createErrorTypeConstructorWithCustomDebugName("[ERROR : " + debugMessage + "]");
    }

    @NotNull
    private static TypeConstructor createErrorTypeConstructorWithCustomDebugName(@NotNull String debugName) {
        return new TypeConstructorImpl(ERROR_CLASS, Collections.<AnnotationDescriptor>emptyList(), false, debugName,
                                Collections.<TypeParameterDescriptorImpl>emptyList(),
                                Collections.singleton(KotlinBuiltIns.getInstance().getAnyType()));
    }

    @NotNull
    public static ClassDescriptor getErrorClass() {
        return ERROR_CLASS;
    }

    public static boolean containsErrorType(@Nullable JetType type) {
        if (type == null) return false;
        if (type instanceof NamespaceType) return false;
        if (type.isError()) return true;
        for (TypeProjection projection : type.getArguments()) {
            if (containsErrorType(projection.getType())) return true;
        }
        return false;
    }

    public static boolean isError(@NotNull DeclarationDescriptor candidate) {
        return isErrorClass(candidate) || isErrorClass(candidate.getContainingDeclaration()) || candidate == ERROR_MODULE;
    }

    private static boolean isErrorClass(@Nullable DeclarationDescriptor candidate) {
        return candidate instanceof ErrorClassDescriptor;
    }

    private static class ErrorTypeImpl implements JetType {
        private final TypeConstructor constructor;
        private final JetScope memberScope;

        private ErrorTypeImpl(@NotNull TypeConstructor constructor, @NotNull JetScope memberScope) {
            this.constructor = constructor;
            this.memberScope = memberScope;
        }

        @NotNull
        @Override
        public TypeConstructor getConstructor() {
            return constructor;
        }

        @NotNull
        @Override
        public List<TypeProjection> getArguments() {
            return Collections.emptyList();
        }

        @Override
        public boolean isNullable() {
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
        public List<AnnotationDescriptor> getAnnotations() {
            return Collections.emptyList();
        }

        @Override
        public String toString() {
            return constructor.toString();
        }
    }

    @NotNull
    public static ModuleDescriptor getErrorModule() {
        return ERROR_MODULE;
    }

    private ErrorUtils() {}
}
