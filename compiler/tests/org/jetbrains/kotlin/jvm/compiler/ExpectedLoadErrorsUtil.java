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

package org.jetbrains.kotlin.jvm.compiler;

import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies;
import org.jetbrains.kotlin.load.java.JavaBindingContext;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.resolve.scopes.JetScope;

import java.util.*;

import static com.intellij.testFramework.UsefulTestCase.assertNotNull;
import static com.intellij.testFramework.UsefulTestCase.assertSameElements;

public class ExpectedLoadErrorsUtil {
    public static final String ANNOTATION_CLASS_NAME = "org.jetbrains.kotlin.jvm.compiler.annotation.ExpectLoadError";

    public static void checkForLoadErrors(
            @NotNull PackageViewDescriptor packageFromJava,
            @NotNull BindingContext bindingContext
    ) {
        Map<DeclarationDescriptor, List<String>> expectedErrors = getExpectedLoadErrors(packageFromJava);
        Map<DeclarationDescriptor, List<String>> actualErrors = getActualLoadErrors(bindingContext);

        for (DeclarationDescriptor descriptor : ContainerUtil.union(expectedErrors.keySet(), actualErrors.keySet())) {
            List<String> actual = actualErrors.get(descriptor);
            List<String> expected = expectedErrors.get(descriptor);
            String rendered = DescriptorRenderer.FQ_NAMES_IN_TYPES.render(descriptor);

            assertNotNull("Unexpected load error(s):\n" + actual + "\ncontainer:" + rendered, expected);
            assertNotNull("Missing load error(s):\n" + expected + "\ncontainer:" + rendered, actual);

            assertSameElements("Unexpected/missing load error(s)\ncontainer:" + rendered, actual, expected);
        }
    }

    private static Map<DeclarationDescriptor, List<String>> getExpectedLoadErrors(@NotNull PackageViewDescriptor packageFromJava) {
        final Map<DeclarationDescriptor, List<String>> map = new HashMap<DeclarationDescriptor, List<String>>();

        packageFromJava.acceptVoid(new DeclarationDescriptorVisitorEmptyBodies<Void, Void>() {
            @Override
            public Void visitPackageViewDescriptor(PackageViewDescriptor descriptor, Void data) {
                return visitDeclarationRecursively(descriptor, descriptor.getMemberScope());
            }

            @Override
            public Void visitClassDescriptor(ClassDescriptor descriptor, Void data) {
                return visitDeclarationRecursively(descriptor, descriptor.getDefaultType().getMemberScope());
            }

            @Override
            public Void visitFunctionDescriptor(FunctionDescriptor descriptor, Void data) {
                return visitDeclaration(descriptor);
            }

            @Override
            public Void visitPropertyDescriptor(PropertyDescriptor descriptor, Void data) {
                return visitDeclaration(descriptor);
            }

            private Void visitDeclaration(@NotNull DeclarationDescriptor descriptor) {
                AnnotationDescriptor annotation = descriptor.getAnnotations().findAnnotation(new FqName(ANNOTATION_CLASS_NAME));
                if (annotation == null) return null;

                // we expect exactly one annotation argument
                CompileTimeConstant<?> argument = annotation.getAllValueArguments().values().iterator().next();

                String error = (String) argument.getValue();
                //noinspection ConstantConditions
                List<String> errors = Arrays.asList(error.split("\\|"));

                map.put(descriptor.getOriginal(), errors);

                return null;
            }

            private Void visitDeclarationRecursively(@NotNull DeclarationDescriptor descriptor, @NotNull JetScope memberScope) {
                for (DeclarationDescriptor member : memberScope.getAllDescriptors()) {
                    member.acceptVoid(this);
                }

                return visitDeclaration(descriptor);
            }
        });

        return map;
    }

    private static Map<DeclarationDescriptor, List<String>> getActualLoadErrors(@NotNull BindingContext bindingContext) {
        Map<DeclarationDescriptor, List<String>> result = new HashMap<DeclarationDescriptor, List<String>>();

        Collection<DeclarationDescriptor> descriptors = bindingContext.getKeys(JavaBindingContext.LOAD_FROM_JAVA_SIGNATURE_ERRORS);
        for (DeclarationDescriptor descriptor : descriptors) {
            List<String> errors = bindingContext.get(JavaBindingContext.LOAD_FROM_JAVA_SIGNATURE_ERRORS, descriptor);
            result.put(descriptor.getOriginal(), errors);
        }

        return result;
    }

    private ExpectedLoadErrorsUtil() {
    }
}
