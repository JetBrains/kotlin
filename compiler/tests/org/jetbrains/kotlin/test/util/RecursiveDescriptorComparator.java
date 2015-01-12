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

package org.jetbrains.kotlin.test.util;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.jvm.compiler.ExpectedLoadErrorsUtil;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.renderer.DescriptorRendererBuilder;
import org.jetbrains.kotlin.renderer.NameShortness;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.MemberComparator;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.test.JetTestUtils;
import org.jetbrains.kotlin.utils.Printer;
import org.junit.Assert;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.resolve.DescriptorUtils.isEnumEntry;
import static org.jetbrains.kotlin.test.util.DescriptorValidator.ValidationVisitor.errorTypesForbidden;

public class RecursiveDescriptorComparator {
    private static final DescriptorRenderer DEFAULT_RENDERER = new DescriptorRendererBuilder()
            .setWithDefinedIn(false)
            .setExcludedAnnotationClasses(Arrays.asList(new FqName(ExpectedLoadErrorsUtil.ANNOTATION_CLASS_NAME)))
            .setOverrideRenderingPolicy(DescriptorRenderer.OverrideRenderingPolicy.RENDER_OPEN_OVERRIDE)
            .setIncludePropertyConstant(true)
            .setNameShortness(NameShortness.FULLY_QUALIFIED)
            .setVerbose(true).build();

    public static final Configuration DONT_INCLUDE_METHODS_OF_OBJECT = new Configuration(false, false, false,
                                                                                         Predicates.<DeclarationDescriptor>alwaysTrue(),
                                                                                         errorTypesForbidden(), DEFAULT_RENDERER);
    public static final Configuration RECURSIVE = new Configuration(false, false, true,
                                                                    Predicates.<DeclarationDescriptor>alwaysTrue(),
                                                                    errorTypesForbidden(), DEFAULT_RENDERER);

    public static final Configuration RECURSIVE_ALL = new Configuration(true, true, true,
                                                                        Predicates.<DeclarationDescriptor>alwaysTrue(),
                                                                        errorTypesForbidden(), DEFAULT_RENDERER);

    public static final Predicate<DeclarationDescriptor> SKIP_BUILT_INS_PACKAGES = new Predicate<DeclarationDescriptor>() {
        @Override
        public boolean apply(DeclarationDescriptor descriptor) {
            if (descriptor instanceof PackageViewDescriptor) {
                return !KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME.equals(((PackageViewDescriptor) descriptor).getFqName());
            }
            return true;
        }
    };

    private static final ImmutableSet<String> KOTLIN_ANY_METHOD_NAMES = ImmutableSet.of("equals", "hashCode", "toString");

    private final Configuration conf;

    public RecursiveDescriptorComparator(@NotNull Configuration conf) {
        this.conf = conf;
    }

    public String serializeRecursively(@NotNull DeclarationDescriptor declarationDescriptor) {
        StringBuilder result = new StringBuilder();
        appendDeclarationRecursively(declarationDescriptor, DescriptorUtils.getContainingModule(declarationDescriptor), new Printer(result, 1), true);
        return result.toString();
    }

    private void appendDeclarationRecursively(
            @NotNull DeclarationDescriptor descriptor,
            @NotNull ModuleDescriptor module,
            @NotNull Printer printer,
            boolean topLevel
    ) {
        if (!isEnumEntry(descriptor) &&
            (descriptor instanceof ClassOrPackageFragmentDescriptor || descriptor instanceof PackageViewDescriptor) && !topLevel) {
            printer.println();
        }

        boolean isPrimaryConstructor = descriptor instanceof ConstructorDescriptor && ((ConstructorDescriptor) descriptor).isPrimary();
        printer.print(isPrimaryConstructor && conf.checkPrimaryConstructors ? "/*primary*/ " : "", conf.renderer.render(descriptor));

        if (descriptor instanceof ClassOrPackageFragmentDescriptor || descriptor instanceof PackageViewDescriptor) {
            if (!topLevel) {
                printer.printlnWithNoIndent(" {").pushIndent();
            }
            else {
                printer.println();
                printer.println();
            }

            if (descriptor instanceof ClassDescriptor) {
                ClassDescriptor klass = (ClassDescriptor) descriptor;
                appendSubDescriptors(descriptor, module,
                                     klass.getDefaultType().getMemberScope(), getConstructorsAndClassObject(klass), printer);
                JetScope staticScope = klass.getStaticScope();
                if (!staticScope.getAllDescriptors().isEmpty()) {
                    printer.println();
                    printer.println("// Static members");
                    appendSubDescriptors(descriptor, module, staticScope, Collections.<DeclarationDescriptor>emptyList(), printer);
                }
            }
            else if (descriptor instanceof PackageFragmentDescriptor) {
                appendSubDescriptors(descriptor, module,
                                     ((PackageFragmentDescriptor) descriptor).getMemberScope(),
                                     Collections.<DeclarationDescriptor>emptyList(), printer);
            }
            else if (descriptor instanceof PackageViewDescriptor) {
                appendSubDescriptors(descriptor, module,
                                     ((PackageViewDescriptor) descriptor).getMemberScope(),
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

        if (isEnumEntry(descriptor)) {
            printer.println();
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
        boolean isFunctionFromAny = subDescriptor.getContainingDeclaration() instanceof ClassDescriptor
                                    && subDescriptor instanceof FunctionDescriptor
                                    && KOTLIN_ANY_METHOD_NAMES.contains(subDescriptor.getName().asString());
        return (isFunctionFromAny && !conf.includeMethodsOfKotlinAny) || !conf.recursiveFilter.apply(subDescriptor);
    }

    private void appendSubDescriptors(
            @NotNull DeclarationDescriptor descriptor,
            @NotNull ModuleDescriptor module,
            @NotNull JetScope memberScope,
            @NotNull Collection<DeclarationDescriptor> extraSubDescriptors,
            @NotNull Printer printer
    ) {
        if (!module.equals(DescriptorUtils.getContainingModule(descriptor))) {
            printer.println(String.format("// -- Module: %s --", DescriptorUtils.getContainingModule(descriptor).getName()));
            return;
        }

        List<DeclarationDescriptor> subDescriptors = Lists.newArrayList();

        subDescriptors.addAll(memberScope.getAllDescriptors());
        subDescriptors.addAll(extraSubDescriptors);

        Collections.sort(subDescriptors, MemberComparator.INSTANCE);

        for (DeclarationDescriptor subDescriptor : subDescriptors) {
            if (!shouldSkip(subDescriptor)) {
                appendDeclarationRecursively(subDescriptor, module, printer, false);
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

    public static void compareDescriptors(
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
        DescriptorValidator.validate(configuration.validationStrategy, expected);
        DescriptorValidator.validate(configuration.validationStrategy, actual);
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
        private final boolean includeMethodsOfKotlinAny;
        private final Predicate<DeclarationDescriptor> recursiveFilter;
        private final DescriptorRenderer renderer;

        private final DescriptorValidator.ValidationVisitor validationStrategy;

        public Configuration(
                boolean checkPrimaryConstructors,
                boolean checkPropertyAccessors,
                boolean includeMethodsOfKotlinAny,
                Predicate<DeclarationDescriptor> recursiveFilter,
                DescriptorValidator.ValidationVisitor validationStrategy,
                DescriptorRenderer renderer
        ) {
            this.checkPrimaryConstructors = checkPrimaryConstructors;
            this.checkPropertyAccessors = checkPropertyAccessors;
            this.includeMethodsOfKotlinAny = includeMethodsOfKotlinAny;
            this.recursiveFilter = recursiveFilter;
            this.validationStrategy = validationStrategy;
            this.renderer = renderer;
        }

        public Configuration filterRecursion(@NotNull Predicate<DeclarationDescriptor> stepIntoFilter) {
            return new Configuration(checkPrimaryConstructors, checkPropertyAccessors, includeMethodsOfKotlinAny, stepIntoFilter,
                                     validationStrategy.withStepIntoFilter(stepIntoFilter), renderer);
        }

        public Configuration checkPrimaryConstructors(boolean checkPrimaryConstructors) {
            return new Configuration(checkPrimaryConstructors, checkPropertyAccessors, includeMethodsOfKotlinAny, recursiveFilter,
                                     validationStrategy, renderer);
        }

        public Configuration checkPropertyAccessors(boolean checkPropertyAccessors) {
            return new Configuration(checkPrimaryConstructors, checkPropertyAccessors, includeMethodsOfKotlinAny, recursiveFilter,
                                     validationStrategy, renderer);
        }

        public Configuration includeMethodsOfKotlinAny(boolean includeMethodsOfKotlinAny) {
            return new Configuration(checkPrimaryConstructors, checkPropertyAccessors, includeMethodsOfKotlinAny, recursiveFilter,
                                     validationStrategy, renderer);
        }

        public Configuration withValidationStrategy(@NotNull DescriptorValidator.ValidationVisitor validationStrategy) {
            return new Configuration(checkPrimaryConstructors, checkPropertyAccessors, includeMethodsOfKotlinAny, recursiveFilter,
                                     validationStrategy, renderer);
        }

        public Configuration withRenderer(@NotNull DescriptorRenderer renderer) {
            return new Configuration(checkPrimaryConstructors, checkPropertyAccessors, includeMethodsOfKotlinAny, recursiveFilter,
                                     validationStrategy, renderer);
        }
    }
}
