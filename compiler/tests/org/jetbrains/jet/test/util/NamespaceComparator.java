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
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.jvm.compiler.ExpectedLoadErrorsUtil;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.MemberComparator;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.renderer.DescriptorRenderer;
import org.jetbrains.jet.renderer.DescriptorRendererBuilder;
import org.jetbrains.jet.utils.Printer;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.test.util.DescriptorValidator.ValidationVisitor.FORBID_ERROR_TYPES;

public class NamespaceComparator {
    public static final Configuration DONT_INCLUDE_METHODS_OF_OBJECT = new Configuration(false, false, false, Predicates.<FqNameUnsafe>alwaysTrue(),
                                                                                         FORBID_ERROR_TYPES);
    public static final Configuration RECURSIVE = new Configuration(false, false, true, Predicates.<FqNameUnsafe>alwaysTrue(), FORBID_ERROR_TYPES);
    public static final Configuration RECURSIVE_ALL = new Configuration(true, true, true, Predicates.<FqNameUnsafe>alwaysTrue(), FORBID_ERROR_TYPES);

    private static final DescriptorRenderer RENDERER = new DescriptorRendererBuilder()
            .setWithDefinedIn(false)
            .setExcludedAnnotationClasses(Arrays.asList(new FqName(ExpectedLoadErrorsUtil.ANNOTATION_CLASS_NAME)))
            .setOverrideRenderingPolicy(DescriptorRenderer.OverrideRenderingPolicy.RENDER_OPEN_OVERRIDE)
            .setVerbose(true).build();

    private static final ImmutableSet<String> JAVA_OBJECT_METHOD_NAMES = ImmutableSet.of(
            "equals", "hashCode", "finalize", "wait", "notify", "notifyAll", "toString", "clone", "getClass");

    private final Configuration conf;

    private NamespaceComparator(@NotNull Configuration conf) {
        this.conf = conf;
    }

    private String serializeRecursively(@NotNull DeclarationDescriptor declarationDescriptor) {
        StringBuilder result = new StringBuilder();
        appendDeclarationRecursively(declarationDescriptor, new Printer(result, 1), true);
        return result.toString();
    }

    private void appendDeclarationRecursively(@NotNull DeclarationDescriptor descriptor, @NotNull Printer printer, boolean topLevel) {
        if (descriptor instanceof ClassOrNamespaceDescriptor && !topLevel) {
            printer.println();
        }

        boolean isPrimaryConstructor = descriptor instanceof ConstructorDescriptor && ((ConstructorDescriptor) descriptor).isPrimary();
        printer.print(isPrimaryConstructor && conf.checkPrimaryConstructors ? "/*primary*/ " : "", RENDERER.render(descriptor));

        if (descriptor instanceof ClassOrNamespaceDescriptor) {
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
            else if (descriptor instanceof NamespaceDescriptor) {
                appendSubDescriptors(((NamespaceDescriptor) descriptor).getMemberScope(),
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
                printer.println(RENDERER.render(getter));
            }

            PropertySetterDescriptor setter = propertyDescriptor.getSetter();
            if (setter != null) {
                printer.println(RENDERER.render(setter));
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
                subDescriptor instanceof NamespaceDescriptor && !conf.recurseIntoPackage.apply(DescriptorUtils.getFQName(subDescriptor));
    }

    private void appendSubDescriptors(
            @NotNull JetScope memberScope,
            @NotNull Collection<DeclarationDescriptor> extraSubDescriptors,
            @NotNull Printer printer
    ) {
        List<DeclarationDescriptor> subDescriptors = Lists.newArrayList();

        subDescriptors.addAll(memberScope.getAllDescriptors());
        subDescriptors.addAll(memberScope.getObjectDescriptors());
        subDescriptors.addAll(extraSubDescriptors);

        Collections.sort(subDescriptors, MemberComparator.INSTANCE);

        for (DeclarationDescriptor subDescriptor : subDescriptors) {
            if (!shouldSkip(subDescriptor)) {
                appendDeclarationRecursively(subDescriptor, printer, false);
            }
        }
    }

    private static void compareNamespaceWithFile(
            @NotNull NamespaceDescriptor actualNamespace,
            @NotNull Configuration configuration,
            @NotNull File txtFile
    ) {
        doCompareNamespaces(null, actualNamespace, configuration, txtFile);
    }

    private static void compareNamespaces(
            @NotNull NamespaceDescriptor expectedNamespace,
            @NotNull NamespaceDescriptor actualNamespace,
            @NotNull Configuration configuration,
            @Nullable File txtFile
    ) {
        if (expectedNamespace == actualNamespace) {
            throw new IllegalArgumentException("Don't invoke this method with expectedNamespace == actualNamespace." +
                                               "Invoke compareNamespaceWithFile() instead.");
        }
        doCompareNamespaces(expectedNamespace, actualNamespace, configuration, txtFile);
    }

    public static void validateAndCompareNamespaceWithFile(
            @NotNull NamespaceDescriptor actualNamespace,
            @NotNull Configuration configuration,
            @NotNull File txtFile
    ) {
        DescriptorValidator.validate(configuration.validationStrategy, actualNamespace);
        compareNamespaceWithFile(actualNamespace, configuration, txtFile);
    }

    public static void validateAndCompareNamespaces(
            @NotNull NamespaceDescriptor expectedNamespace,
            @NotNull NamespaceDescriptor actualNamespace,
            @NotNull Configuration configuration,
            @Nullable File txtFile
    ) {
        DescriptorValidator.validate(configuration.validationStrategy, expectedNamespace, actualNamespace);
        compareNamespaces(expectedNamespace, actualNamespace, configuration, txtFile);
    }

    private static void doCompareNamespaces(
            @Nullable NamespaceDescriptor expectedNamespace,
            @NotNull NamespaceDescriptor actualNamespace,
            @NotNull Configuration configuration,
            @Nullable File txtFile
    ) {
        NamespaceComparator comparator = new NamespaceComparator(configuration);

        String actualSerialized = comparator.serializeRecursively(actualNamespace);

        if (expectedNamespace != null) {
            String expectedSerialized = comparator.serializeRecursively(expectedNamespace);

            Assert.assertEquals("Expected and actual namespaces differ", expectedSerialized, actualSerialized);
        }

        if (txtFile != null) {
            try {
                if (!txtFile.exists()) {
                    FileUtil.writeToFile(txtFile, actualSerialized);
                    Assert.fail("Expected data file did not exist. Generating: " + txtFile);
                }
                String expected = FileUtil.loadFile(txtFile, true);

                // compare with hard copy: make sure nothing is lost in output
                Assert.assertEquals("Expected and actual namespaces differ from " + txtFile.getName(),
                                    StringUtil.convertLineSeparators(expected),
                                    StringUtil.convertLineSeparators(actualSerialized));
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class Configuration {
        private final boolean checkPrimaryConstructors;
        private final boolean checkPropertyAccessors;
        private final boolean includeMethodsOfJavaObject;
        private final Predicate<FqNameUnsafe> recurseIntoPackage;

        private final DescriptorValidator.ValidationVisitor validationStrategy;

        public Configuration(
                boolean checkPrimaryConstructors,
                boolean checkPropertyAccessors,
                boolean includeMethodsOfJavaObject,
                Predicate<FqNameUnsafe> recurseIntoPackage,
                DescriptorValidator.ValidationVisitor validationStrategy
        ) {
            this.checkPrimaryConstructors = checkPrimaryConstructors;
            this.checkPropertyAccessors = checkPropertyAccessors;
            this.includeMethodsOfJavaObject = includeMethodsOfJavaObject;
            this.recurseIntoPackage = recurseIntoPackage;
            this.validationStrategy = validationStrategy;
        }

        public Configuration filterRecursion(@NotNull Predicate<FqNameUnsafe> recurseIntoPackage) {
            return new Configuration(checkPrimaryConstructors, checkPropertyAccessors, includeMethodsOfJavaObject, recurseIntoPackage,
                                     validationStrategy);
        }

        public Configuration checkPrimaryConstructors(boolean checkPrimaryConstructors) {
            return new Configuration(checkPrimaryConstructors, checkPropertyAccessors, includeMethodsOfJavaObject, recurseIntoPackage,
                                     validationStrategy);
        }

        public Configuration checkPropertyAccessors(boolean checkPropertyAccessors) {
            return new Configuration(checkPrimaryConstructors, checkPropertyAccessors, includeMethodsOfJavaObject, recurseIntoPackage,
                                     validationStrategy);
        }

        public Configuration withValidationStrategy(@NotNull DescriptorValidator.ValidationVisitor validationStrategy) {
            return new Configuration(checkPrimaryConstructors, checkPropertyAccessors, includeMethodsOfJavaObject, recurseIntoPackage,
                                     validationStrategy);
        }
    }
}
