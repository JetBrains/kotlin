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

package org.jetbrains.jet.test.util;

import com.google.common.collect.Lists;
import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class DescriptorValidator {

    public static void validate(DeclarationDescriptor... descriptors) {
        validate(ValidationVisitor.FORBID_ERROR_TYPES, Arrays.asList(descriptors));
    }

    public static void validate(@NotNull ValidationVisitor validationStrategy, DeclarationDescriptor... descriptors) {
        validate(validationStrategy, Arrays.asList(descriptors));
    }

    public static void validate(@NotNull ValidationVisitor validator, @NotNull Collection<DeclarationDescriptor> descriptors) {
        DiagnosticCollectorForTests collector = new DiagnosticCollectorForTests();
        for (DeclarationDescriptor descriptor : descriptors) {
            validate(validator, descriptor, collector);
        }
        collector.done();
    }

    public static void validate(
            @NotNull ValidationVisitor validator,
            @NotNull DeclarationDescriptor descriptor,
            @NotNull DiagnosticCollector collector
    ) {
        RecursiveDescriptorProcessor.process(descriptor, collector, validator);
    }

    private static void report(@NotNull DiagnosticCollector collector, @NotNull DeclarationDescriptor descriptor, @NotNull String message) {
        collector.report(new ValidationDiagnostic(descriptor, message));
    }

    public interface DiagnosticCollector {
        void report(@NotNull ValidationDiagnostic diagnostic);
    }

    public static class ValidationVisitor implements DeclarationDescriptorVisitor<Boolean, DiagnosticCollector> {
        public static final ValidationVisitor FORBID_ERROR_TYPES = new ValidationVisitor(false);
        public static final ValidationVisitor ALLOW_ERROR_TYPES = new ValidationVisitor(true);

        private final boolean allowErrorTypes;

        private ValidationVisitor(boolean allowErrorTypes) {
            this.allowErrorTypes = allowErrorTypes;
        }

        private static void validateScope(@NotNull JetScope scope, @NotNull DiagnosticCollector collector) {
            for (DeclarationDescriptor descriptor : scope.getAllDescriptors()) {
                descriptor.accept(new ScopeValidatorVisitor(collector), scope);
            }
        }

        private void validateType(
                @NotNull DeclarationDescriptor descriptor,
                @Nullable JetType type,
                @NotNull DiagnosticCollector collector
        ) {
            if (type == null) {
                report(collector, descriptor, "No type");
                return;
            }

            if (!allowErrorTypes && type.isError()) {
                report(collector, descriptor, "Error type: " + type);
                return;
            }

            validateScope(type.getMemberScope(), collector);
        }

        private void validateReturnType(CallableDescriptor descriptor, DiagnosticCollector collector) {
            validateType(descriptor, descriptor.getReturnType(), collector);
        }

        private static void validateTypeParameters(DiagnosticCollector collector, List<TypeParameterDescriptor> parameters) {
            for (int i = 0; i < parameters.size(); i++) {
                TypeParameterDescriptor typeParameterDescriptor = parameters.get(i);
                if (typeParameterDescriptor.getIndex() != i) {
                    report(collector, typeParameterDescriptor, "Incorrect index: " + typeParameterDescriptor.getIndex() + " but must be " + i);
                }
            }
        }

        private static void validateValueParameters(DiagnosticCollector collector, List<ValueParameterDescriptor> parameters) {
            for (int i = 0; i < parameters.size(); i++) {
                ValueParameterDescriptor valueParameterDescriptor = parameters.get(i);
                if (valueParameterDescriptor.getIndex() != i) {
                    report(collector, valueParameterDescriptor, "Incorrect index: " + valueParameterDescriptor.getIndex() + " but must be " + i);
                }
            }
        }

        private void validateTypes(
                DeclarationDescriptor descriptor,
                DiagnosticCollector collector,
                Collection<JetType> types
        ) {
            for (JetType type : types) {
                validateType(descriptor, type, collector);
            }
        }

        private void validateCallable(CallableDescriptor descriptor, DiagnosticCollector collector) {
            validateReturnType(descriptor, collector);
            validateTypeParameters(collector, descriptor.getTypeParameters());
            validateValueParameters(collector, descriptor.getValueParameters());
        }

        private static <T> void assertEquals(
                DeclarationDescriptor descriptor,
                DiagnosticCollector collector,
                String name,
                T expected,
                T actual
        ) {
            if (!expected.equals(actual)) {
                report(collector, descriptor, "Wrong " + name + ": " + actual + " must be " + expected);
            }
        }

        private static <T> void assertEqualTypes(
                DeclarationDescriptor descriptor,
                DiagnosticCollector collector,
                String name,
                JetType expected,
                JetType actual
        ) {
            if (expected.isError() && actual.isError()) {
                assertEquals(descriptor, collector, name, expected.toString(), actual.toString());
            }
            else if (!expected.equals(actual)) {
                report(collector, descriptor, "Wrong " + name + ": " + actual + " must be " + expected);
            }
        }

        private static void validateAccessor(
                PropertyDescriptor descriptor,
                DiagnosticCollector collector,
                PropertyAccessorDescriptor accessor,
                String name
        ) {
            // TODO: fix the discrepancies in descriptor construction and enable these checks
            //assertEquals(accessor, collector, name + " visibility", descriptor.getVisibility(), accessor.getVisibility());
            //assertEquals(accessor, collector, name + " modality", descriptor.getModality(), accessor.getModality());
            assertEquals(accessor, collector, "corresponding property", descriptor, accessor.getCorrespondingProperty());
        }

        @Override
        public Boolean visitPackageFragmentDescriptor(
                PackageFragmentDescriptor descriptor, DiagnosticCollector collector
        ) {
            validateScope(descriptor.getMemberScope(), collector);
            return true;
        }

        @Override
        public Boolean visitPackageViewDescriptor(
                PackageViewDescriptor descriptor, DiagnosticCollector collector
        ) {
            validateScope(descriptor.getMemberScope(), collector);
            return true;
        }

        @Override
        public Boolean visitVariableDescriptor(
                VariableDescriptor descriptor, DiagnosticCollector collector
        ) {
            validateReturnType(descriptor, collector);
            return true;
        }

        @Override
        public Boolean visitFunctionDescriptor(
                FunctionDescriptor descriptor, DiagnosticCollector collector
        ) {
            validateCallable(descriptor, collector);
            return true;
        }

        @Override
        public Boolean visitTypeParameterDescriptor(
                TypeParameterDescriptor descriptor, DiagnosticCollector collector
        ) {
            validateTypes(descriptor, collector, descriptor.getUpperBounds());

            validateType(descriptor, descriptor.getDefaultType(), collector);

            return true;
        }

        @Override
        public Boolean visitClassDescriptor(
                ClassDescriptor descriptor, DiagnosticCollector collector
        ) {
            validateTypeParameters(collector, descriptor.getTypeConstructor().getParameters());
            validateTypes(descriptor, collector, descriptor.getTypeConstructor().getSupertypes());

            validateType(descriptor, descriptor.getDefaultType(), collector);

            validateScope(descriptor.getUnsubstitutedInnerClassesScope(), collector);

            List<ConstructorDescriptor> primary = Lists.newArrayList();
            for (ConstructorDescriptor constructorDescriptor : descriptor.getConstructors()) {
                if (constructorDescriptor.isPrimary()) {
                    primary.add(constructorDescriptor);
                }
            }
            if (primary.size() > 1) {
                report(collector, descriptor, "Many primary constructors: " + primary);
            }

            ConstructorDescriptor primaryConstructor = descriptor.getUnsubstitutedPrimaryConstructor();
            if (primaryConstructor != null) {
                if (!descriptor.getConstructors().contains(primaryConstructor)) {
                    report(collector, primaryConstructor,
                           "Primary constructor not in getConstructors() result: " + descriptor.getConstructors());
                }
            }

            return true;
        }

        @Override
        public Boolean visitModuleDeclaration(
                ModuleDescriptor descriptor, DiagnosticCollector collector
        ) {
            return true;
        }

        @Override
        public Boolean visitConstructorDescriptor(
                ConstructorDescriptor constructorDescriptor, DiagnosticCollector collector
        ) {
            visitFunctionDescriptor(constructorDescriptor, collector);

            assertEqualTypes(constructorDescriptor, collector,
                         "return type",
                         constructorDescriptor.getContainingDeclaration().getDefaultType(),
                         constructorDescriptor.getReturnType());

            return true;
        }

        @Override
        public Boolean visitScriptDescriptor(
                ScriptDescriptor scriptDescriptor, DiagnosticCollector collector
        ) {
            return true;
        }

        @Override
        public Boolean visitPropertyDescriptor(
                PropertyDescriptor descriptor, DiagnosticCollector collector
        ) {
            validateCallable(descriptor, collector);

            PropertyGetterDescriptor getter = descriptor.getGetter();
            if (getter != null) {
                assertEqualTypes(getter, collector, "getter return type", descriptor.getType(), getter.getReturnType());
                validateAccessor(descriptor, collector, getter, "getter");
            }

            PropertySetterDescriptor setter = descriptor.getSetter();
            if (setter != null) {
                assertEquals(setter, collector, "setter parameter count", 1, setter.getValueParameters().size());
                assertEqualTypes(setter, collector, "setter parameter type", descriptor.getType(), setter.getValueParameters().get(0).getType());
                assertEquals(setter, collector, "corresponding property", descriptor, setter.getCorrespondingProperty());
            }

            return true;
        }

        @Override
        public Boolean visitValueParameterDescriptor(
                ValueParameterDescriptor descriptor, DiagnosticCollector collector
        ) {
            return visitVariableDescriptor(descriptor, collector);
        }

        @Override
        public Boolean visitPropertyGetterDescriptor(
                PropertyGetterDescriptor descriptor, DiagnosticCollector collector
        ) {
            return visitFunctionDescriptor(descriptor, collector);
        }

        @Override
        public Boolean visitPropertySetterDescriptor(
                PropertySetterDescriptor descriptor, DiagnosticCollector collector
        ) {
            return visitFunctionDescriptor(descriptor, collector);
        }

        @Override
        public Boolean visitReceiverParameterDescriptor(
                ReceiverParameterDescriptor descriptor, DiagnosticCollector collector
        ) {
            validateType(descriptor, descriptor.getType(), collector);

            if (!descriptor.getValue().exists()) {
                report(collector, descriptor, "Receiver value does not exist: " + descriptor.getValue());
            }
            return true;
        }

    }

    private static class ScopeValidatorVisitor implements DeclarationDescriptorVisitor<Void, JetScope> {
        private final DiagnosticCollector collector;

        public ScopeValidatorVisitor(DiagnosticCollector collector) {
            this.collector = collector;
        }

        private void report(DeclarationDescriptor expected, String message) {
            DescriptorValidator.report(collector, expected, message);
        }

        private void assertFound(
                @NotNull JetScope scope,
                @NotNull DeclarationDescriptor expected,
                @Nullable DeclarationDescriptor found,
                boolean shouldBeSame
        ) {
            if (found == null) {
                report(expected, "Not found in " + scope);
            }
            if (shouldBeSame ? expected != found : !expected.equals(found)) {
                report(expected, "Lookup error in " + scope + ": " + found);
            }
        }

        private void assertFound(
                @NotNull JetScope scope,
                @NotNull DeclarationDescriptor expected,
                @NotNull Collection<? extends DeclarationDescriptor> found
        ) {
            if (!found.contains(expected)) {
                report(expected, "Not found in " + scope + ": " + found);
            }
        }

        @Override
        public Void visitPackageFragmentDescriptor(
                PackageFragmentDescriptor descriptor, JetScope scope
        ) {
            return null;
        }

        @Override
        public Void visitPackageViewDescriptor(
                PackageViewDescriptor descriptor, JetScope scope
        ) {
            assertFound(scope, descriptor, scope.getPackage(descriptor.getName()), false);
            return null;
        }

        @Override
        public Void visitVariableDescriptor(
                VariableDescriptor descriptor, JetScope scope
        ) {
            assertFound(scope, descriptor, scope.getProperties(descriptor.getName()));
            return null;
        }

        @Override
        public Void visitFunctionDescriptor(
                FunctionDescriptor descriptor, JetScope scope
        ) {
            assertFound(scope, descriptor, scope.getFunctions(descriptor.getName()));
            return null;
        }

        @Override
        public Void visitTypeParameterDescriptor(
                TypeParameterDescriptor descriptor, JetScope scope
        ) {
            assertFound(scope, descriptor, scope.getClassifier(descriptor.getName()), true);
            return null;
        }

        @Override
        public Void visitClassDescriptor(
                ClassDescriptor descriptor, JetScope scope
        ) {
            assertFound(scope, descriptor, scope.getClassifier(descriptor.getName()), true);
            return null;
        }

        @Override
        public Void visitModuleDeclaration(
                ModuleDescriptor descriptor, JetScope scope
        ) {
            report(descriptor, "Module found in scope: " + scope);
            return null;
        }

        @Override
        public Void visitConstructorDescriptor(
                ConstructorDescriptor descriptor, JetScope scope
        ) {
            report(descriptor, "Constructor found in scope: " + scope);
            return null;
        }

        @Override
        public Void visitScriptDescriptor(
                ScriptDescriptor descriptor, JetScope scope
        ) {
            report(descriptor, "Script found in scope: " + scope);
            return null;
        }

        @Override
        public Void visitPropertyDescriptor(
                PropertyDescriptor descriptor, JetScope scope
        ) {
            return visitVariableDescriptor(descriptor, scope);
        }

        @Override
        public Void visitValueParameterDescriptor(
                ValueParameterDescriptor descriptor, JetScope scope
        ) {
            return visitVariableDescriptor(descriptor, scope);
        }

        @Override
        public Void visitPropertyGetterDescriptor(
                PropertyGetterDescriptor descriptor, JetScope scope
        ) {
            report(descriptor, "Getter found in scope: " + scope);
            return null;
        }

        @Override
        public Void visitPropertySetterDescriptor(
                PropertySetterDescriptor descriptor, JetScope scope
        ) {
            report(descriptor, "Setter found in scope: " + scope);
            return null;
        }

        @Override
        public Void visitReceiverParameterDescriptor(
                ReceiverParameterDescriptor descriptor, JetScope scope
        ) {
            report(descriptor, "Receiver parameter found in scope: " + scope);
            return null;
        }
    }

    public static class ValidationDiagnostic {

        private final DeclarationDescriptor descriptor;
        private final String message;
        private final Throwable stackTrace;

        private ValidationDiagnostic(@NotNull DeclarationDescriptor descriptor, @NotNull String message) {
            this.descriptor = descriptor;
            this.message = message;
            this.stackTrace = new Throwable();
        }

        @NotNull
        public DeclarationDescriptor getDescriptor() {
            return descriptor;
        }

        @NotNull
        public String getMessage() {
            return message;
        }

        @NotNull
        public Throwable getStackTrace() {
            return stackTrace;
        }

        public void printStackTrace(@NotNull PrintStream out) {
            out.println(descriptor);
            out.println(message);
            stackTrace.printStackTrace(out);
        }

        @Override
        public String toString() {
            return descriptor + " > " + message;
        }
    }

    private static class DiagnosticCollectorForTests implements DiagnosticCollector {
        private boolean errorsFound = false;

        @Override
        public void report(@NotNull ValidationDiagnostic diagnostic) {
            diagnostic.printStackTrace(System.err);
            errorsFound = true;
        }

        public void done() {
            if (errorsFound) {
                Assert.fail("Descriptor validation failed (see messages above)");
            }
        }
    }

    private DescriptorValidator() {}
}
