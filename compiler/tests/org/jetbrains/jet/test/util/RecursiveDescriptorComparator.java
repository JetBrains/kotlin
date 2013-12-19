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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.jvm.compiler.ExpectedLoadErrorsUtil;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.MemberComparator;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.renderer.DescriptorRenderer;
import org.jetbrains.jet.renderer.DescriptorRendererBuilder;
import org.jetbrains.jet.utils.Printer;
import org.junit.Assert;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.test.util.DescriptorValidator.ValidationVisitor.FORBID_ERROR_TYPES;

public class RecursiveDescriptorComparator {
    private static final DescriptorRenderer DEFAULT_RENDERER = new DescriptorRendererBuilder()
            .setWithDefinedIn(false)
            .setExcludedAnnotationClasses(Arrays.asList(new FqName(ExpectedLoadErrorsUtil.ANNOTATION_CLASS_NAME)))
            .setOverrideRenderingPolicy(DescriptorRenderer.OverrideRenderingPolicy.RENDER_OPEN_OVERRIDE)
            .setVerbose(true).build();

    public static final Configuration DONT_INCLUDE_METHODS_OF_OBJECT = new Configuration(false, false, false, 
                                                                                         Predicates.<FqName>alwaysTrue(),
                                                                                         FORBID_ERROR_TYPES, DEFAULT_RENDERER);
    public static final Configuration RECURSIVE = new Configuration(false, false, true, 
                                                                    Predicates.<FqName>alwaysTrue(),
                                                                    FORBID_ERROR_TYPES, DEFAULT_RENDERER);

    public static final Configuration RECURSIVE_ALL = new Configuration(true, true, true, 
                                                                        Predicates.<FqName>alwaysTrue(),
                                                                        FORBID_ERROR_TYPES, DEFAULT_RENDERER);

    private static final ImmutableSet<String> JAVA_OBJECT_METHOD_NAMES = ImmutableSet.of(
            "equals", "hashCode", "finalize", "wait", "notify", "notifyAll", "toString", "clone", "getClass");

    private final Configuration conf;

    private RecursiveDescriptorComparator(@NotNull Configuration conf) {
        this.conf = conf;
    }

    private String serializeRecursively(@NotNull DeclarationDescriptor declarationDescriptor) {
        StringBuilder result = new StringBuilder();
        appendDeclarationRecursively(declarationDescriptor, new Printer(result, 1), true);
        return result.toString();
    }

    private void appendDeclarationRecursively(@NotNull DeclarationDescriptor descriptor, @NotNull Printer printer, boolean topLevel) {
        if ((descriptor instanceof ClassOrNamespaceDescriptor || descriptor instanceof PackageViewDescriptor) && !topLevel) {
            printer.println();
        }

        boolean isPrimaryConstructor = descriptor instanceof ConstructorDescriptor && ((ConstructorDescriptor) descriptor).isPrimary();
        printer.print(isPrimaryConstructor && conf.checkPrimaryConstructors ? "/*primary*/ " : "", conf.renderer.render(descriptor));

        if (descriptor instanceof ClassOrNamespaceDescriptor || descriptor instanceof PackageViewDescriptor) {
            if (!topLevel) {
                printer.printlnWithNoIndent(" {").pushIndent();
            }
            else {
                printer.println();
                printer.println();
            }

            if (descriptor instanceof ClassDescriptor) {
                ClassDescriptor klass = (ClassDescriptor) descriptor;
                appendSubDescriptors(klass.getDefaultType().getMemberScope(), getConstructorsAndClassObject(klass), printer);
            }
            else if (descriptor instanceof PackageFragmentDescriptor) {
                appendSubDescriptors(((PackageFragmentDescriptor) descriptor).getMemberScope(),
                                     Collections.<DeclarationDescriptor>emptyList(), printer);
            }
            else if (descriptor instanceof PackageViewDescriptor) {
                appendSubDescriptors(((PackageViewDescriptor) descriptor).getMemberScope(),
                                     Collections.<DeclarationDescriptor>emptyList(), printer);
            }

            if (!topLevel) {
                printer.popIndent().println("}");
            }
        }
        else if (conf.checkPropertyAccessors && descriptor instanceof PropertyDescriptor) {
            printer.printlnWithNoIndent();
            printer.pushIndent();
            PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;
            PropertyGetterDescriptor getter = propertyDescriptor.getGetter();
            if (getter != null) {
                printer.println(conf.renderer.render(getter));
            }

            PropertySetterDescriptor setter = propertyDescriptor.getSetter();
            if (setter != null) {
                printer.println(conf.renderer.render(setter));
            }

            printer.popIndent();
        }
        else {
            printer.printlnWithNoIndent();
        }
    }

    @NotNull
    private static List<DeclarationDescriptor> getConstructorsAndClassObject(@NotNull ClassDescriptor klass) {
        List<DeclarationDescriptor> constructorsAndClassObject = Lists.newArrayList();
        constructorsAndClassObject.addAll(klass.getConstructors());
        ContainerUtil.addIfNotNull(constructorsAndClassObject, klass.getClassObjectDescriptor());
        return constructorsAndClassObject;
    }

    private boolean shouldSkip(@NotNull DeclarationDescriptor subDescriptor) {
        return subDescriptor.getContainingDeclaration() instanceof ClassDescriptor
                && subDescriptor instanceof FunctionDescriptor
                && JAVA_OBJECT_METHOD_NAMES.contains(subDescriptor.getName().asString())
                && !conf.includeMethodsOfJavaObject
            ||
                subDescriptor instanceof PackageViewDescriptor
                && !conf.recurseIntoPackage.apply(((PackageViewDescriptor) subDescriptor).getFqName());
    }

    private void appendSubDescriptors(
            @NotNull JetScope memberScope,
            @NotNull Collection<DeclarationDescriptor> extraSubDescriptors,
            @NotNull Printer printer
    ) {
        List<DeclarationDescriptor> subDescriptors = Lists.newArrayList();

        subDescriptors.addAll(memberScope.getAllDescriptors());
        subDescriptors.addAll(extraSubDescriptors);

        Collections.sort(subDescriptors, MemberComparator.INSTANCE);

        for (DeclarationDescriptor subDescriptor : subDescriptors) {
            if (!shouldSkip(subDescriptor)) {
                appendDeclarationRecursively(subDescriptor, printer, false);
            }
        }
    }

    private static void compareDescriptorWithFile(
            @NotNull DeclarationDescriptor actual,
            @NotNull Configuration configuration,
            @NotNull File txtFile
    ) {
        doCompareDescriptors(null, actual, configuration, txtFile);
    }

    private static void compareDescriptors(
            @NotNull DeclarationDescriptor expected,
            @NotNull DeclarationDescriptor actual,
            @NotNull Configuration configuration,
            @Nullable File txtFile
    ) {
        if (expected == actual) {
            throw new IllegalArgumentException("Don't invoke this method with expected == actual." +
                                               "Invoke compareDescriptorWithFile() instead.");
        }
        doCompareDescriptors(expected, actual, configuration, txtFile);
    }

    public static void validateAndCompareDescriptorWithFile(
            @NotNull DeclarationDescriptor actual,
            @NotNull Configuration configuration,
            @NotNull File txtFile
    ) {
        DescriptorValidator.validate(configuration.validationStrategy, actual);
        compareDescriptorWithFile(actual, configuration, txtFile);
    }

    public static void validateAndCompareDescriptors(
            @NotNull DeclarationDescriptor expected,
            @NotNull DeclarationDescriptor actual,
            @NotNull Configuration configuration,
            @Nullable File txtFile
    ) {
        DescriptorValidator.validate(configuration.validationStrategy, expected, actual);
        compareDescriptors(expected, actual, configuration, txtFile);
    }

    private static void doCompareDescriptors(
            @Nullable DeclarationDescriptor expected,
            @NotNull DeclarationDescriptor actual,
            @NotNull Configuration configuration,
            @Nullable File txtFile
    ) {
        RecursiveDescriptorComparator comparator = new RecursiveDescriptorComparator(configuration);

        String actualSerialized = comparator.serializeRecursively(actual);

        if (expected != null) {
            String expectedSerialized = comparator.serializeRecursively(expected);

            Assert.assertEquals("Expected and actual descriptors differ", expectedSerialized, actualSerialized);
        }

        if (txtFile != null) {
            JetTestUtils.assertEqualsToFile(txtFile, actualSerialized);
        }
    }

    public static class Configuration {
        private final boolean checkPrimaryConstructors;
        private final boolean checkPropertyAccessors;
        private final boolean includeMethodsOfJavaObject;
        private final Predicate<FqName> recurseIntoPackage;
        private final DescriptorRenderer renderer;

        private final DescriptorValidator.ValidationVisitor validationStrategy;

        public Configuration(
                boolean checkPrimaryConstructors,
                boolean checkPropertyAccessors,
                boolean includeMethodsOfJavaObject,
                Predicate<FqName> recurseIntoPackage,
                DescriptorValidator.ValidationVisitor validationStrategy,
                DescriptorRenderer renderer
        ) {
            this.checkPrimaryConstructors = checkPrimaryConstructors;
            this.checkPropertyAccessors = checkPropertyAccessors;
            this.includeMethodsOfJavaObject = includeMethodsOfJavaObject;
            this.recurseIntoPackage = recurseIntoPackage;
            this.validationStrategy = validationStrategy;
            this.renderer = renderer;
        }

        public Configuration filterRecursion(@NotNull Predicate<FqName> recurseIntoPackage) {
            return new Configuration(checkPrimaryConstructors, checkPropertyAccessors, includeMethodsOfJavaObject, recurseIntoPackage,
                                     validationStrategy, renderer);
        }

        public Configuration checkPrimaryConstructors(boolean checkPrimaryConstructors) {
            return new Configuration(checkPrimaryConstructors, checkPropertyAccessors, includeMethodsOfJavaObject, recurseIntoPackage,
                                     validationStrategy, renderer);
        }

        public Configuration checkPropertyAccessors(boolean checkPropertyAccessors) {
            return new Configuration(checkPrimaryConstructors, checkPropertyAccessors, includeMethodsOfJavaObject, recurseIntoPackage,
                                     validationStrategy, renderer);
        }

        public Configuration withValidationStrategy(@NotNull DescriptorValidator.ValidationVisitor validationStrategy) {
            return new Configuration(checkPrimaryConstructors, checkPropertyAccessors, includeMethodsOfJavaObject, recurseIntoPackage,
                                     validationStrategy, renderer);
        }

        public Configuration withRenderer(@NotNull DescriptorRenderer renderer) {
            return new Configuration(checkPrimaryConstructors, checkPropertyAccessors, includeMethodsOfJavaObject, recurseIntoPackage, 
                                     validationStrategy, renderer);
        }
    }
}
