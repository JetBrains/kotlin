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

package org.jetbrains.kotlin.test.util;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.jvm.compiler.ExpectedLoadErrorsUtil;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.renderer.*;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.MemberComparator;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.utils.Printer;
import org.junit.Assert;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.resolve.DescriptorUtils.isEnumEntry;
import static org.jetbrains.kotlin.test.util.DescriptorValidator.ValidationVisitor.errorTypesForbidden;

public class RecursiveDescriptorComparator {

    private static final DescriptorRenderer DEFAULT_RENDERER = DescriptorRenderer.Companion.withOptions(
            new Function1<DescriptorRendererOptions, Unit>() {
                @Override
                public Unit invoke(DescriptorRendererOptions options) {
                    options.setWithDefinedIn(false);
                    options.setExcludedAnnotationClasses(Collections.singleton(new FqName(ExpectedLoadErrorsUtil.ANNOTATION_CLASS_NAME)));
                    options.setOverrideRenderingPolicy(OverrideRenderingPolicy.RENDER_OPEN_OVERRIDE);
                    options.setIncludePropertyConstant(true);
                    options.setClassifierNamePolicy(ClassifierNamePolicy.FULLY_QUALIFIED.INSTANCE);
                    options.setVerbose(true);
                    options.setIncludeAnnotationArguments(true);
                    options.setModifiers(DescriptorRendererModifier.ALL);
                    return Unit.INSTANCE;
                }
            }
    );

    public static final Configuration DONT_INCLUDE_METHODS_OF_OBJECT = new Configuration(false, false, false, false,
                                                                                         Predicates.<DeclarationDescriptor>alwaysTrue(),
                                                                                         errorTypesForbidden(), DEFAULT_RENDERER);
    public static final Configuration RECURSIVE = new Configuration(false, false, true, false,
                                                                    Predicates.<DeclarationDescriptor>alwaysTrue(),
                                                                    errorTypesForbidden(), DEFAULT_RENDERER);

    public static final Configuration RECURSIVE_ALL = new Configuration(true, true, true, false,
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
        appendDeclarationRecursively(declarationDescriptor, DescriptorUtils.getContainingModule(declarationDescriptor),
                                     new Printer(result, 1), true);
        return result.toString();
    }

    private void appendDeclarationRecursively(
            @NotNull DeclarationDescriptor descriptor,
            @NotNull ModuleDescriptor module,
            @NotNull Printer printer,
            boolean topLevel
    ) {
        boolean isEnumEntry = isEnumEntry(descriptor);
        boolean isClassOrPackage =
                (descriptor instanceof ClassOrPackageFragmentDescriptor || descriptor instanceof PackageViewDescriptor) && !isEnumEntry;

        if (isClassOrPackage && !topLevel) {
            printer.println();
        }

        boolean isPrimaryConstructor = descriptor instanceof ConstructorDescriptor && ((ConstructorDescriptor) descriptor).isPrimary();
        printer.print(isPrimaryConstructor && conf.checkPrimaryConstructors ? "/*primary*/ " : "", conf.renderer.render(descriptor));

        if (isClassOrPackage) {
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
                                     klass.getDefaultType().getMemberScope(), klass.getConstructors(), printer);
                MemberScope staticScope = klass.getStaticScope();
                if (!DescriptorUtils.getAllDescriptors(staticScope).isEmpty()) {
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

        if (isEnumEntry) {
            printer.println();
        }
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
            @NotNull MemberScope memberScope,
            @NotNull Collection<? extends DeclarationDescriptor> extraSubDescriptors,
            @NotNull Printer printer
    ) {
        if (!conf.renderDeclarationsFromOtherModules && !module.equals(DescriptorUtils.getContainingModule(descriptor))) {
            printer.println(String.format("// -- Module: %s --", DescriptorUtils.getContainingModule(descriptor).getName()));
            return;
        }

        List<DeclarationDescriptor> subDescriptors = Lists.newArrayList();

        subDescriptors.addAll(DescriptorUtils.getAllDescriptors(memberScope));
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
            KotlinTestUtils.assertEqualsToFile(txtFile, actualSerialized);
        }
    }

    public static class Configuration {
        private final boolean checkPrimaryConstructors;
        private final boolean checkPropertyAccessors;
        private final boolean includeMethodsOfKotlinAny;
        private final boolean renderDeclarationsFromOtherModules;
        private final Predicate<DeclarationDescriptor> recursiveFilter;
        private final DescriptorRenderer renderer;
        private final DescriptorValidator.ValidationVisitor validationStrategy;

        public Configuration(
                boolean checkPrimaryConstructors,
                boolean checkPropertyAccessors,
                boolean includeMethodsOfKotlinAny,
                boolean renderDeclarationsFromOtherModules,
                Predicate<DeclarationDescriptor> recursiveFilter,
                DescriptorValidator.ValidationVisitor validationStrategy,
                DescriptorRenderer renderer
        ) {
            this.checkPrimaryConstructors = checkPrimaryConstructors;
            this.checkPropertyAccessors = checkPropertyAccessors;
            this.includeMethodsOfKotlinAny = includeMethodsOfKotlinAny;
            this.renderDeclarationsFromOtherModules = renderDeclarationsFromOtherModules;
            this.recursiveFilter = recursiveFilter;
            this.validationStrategy = validationStrategy;
            this.renderer = renderer;
        }

        public Configuration filterRecursion(@NotNull Predicate<DeclarationDescriptor> stepIntoFilter) {
            return new Configuration(checkPrimaryConstructors, checkPropertyAccessors, includeMethodsOfKotlinAny,
                                     renderDeclarationsFromOtherModules, stepIntoFilter,
                                     validationStrategy.withStepIntoFilter(stepIntoFilter), renderer);
        }

        public Configuration checkPrimaryConstructors(boolean checkPrimaryConstructors) {
            return new Configuration(checkPrimaryConstructors, checkPropertyAccessors, includeMethodsOfKotlinAny,
                                     renderDeclarationsFromOtherModules, recursiveFilter, validationStrategy, renderer);
        }

        public Configuration checkPropertyAccessors(boolean checkPropertyAccessors) {
            return new Configuration(checkPrimaryConstructors, checkPropertyAccessors, includeMethodsOfKotlinAny,
                                     renderDeclarationsFromOtherModules, recursiveFilter, validationStrategy, renderer);
        }

        public Configuration includeMethodsOfKotlinAny(boolean includeMethodsOfKotlinAny) {
            return new Configuration(checkPrimaryConstructors, checkPropertyAccessors, includeMethodsOfKotlinAny,
                                     renderDeclarationsFromOtherModules, recursiveFilter, validationStrategy, renderer);
        }

        public Configuration renderDeclarationsFromOtherModules(boolean renderDeclarationsFromOtherModules) {
            return new Configuration(checkPrimaryConstructors, checkPropertyAccessors, includeMethodsOfKotlinAny,
                                     renderDeclarationsFromOtherModules, recursiveFilter, validationStrategy, renderer);
        }

        public Configuration withValidationStrategy(@NotNull DescriptorValidator.ValidationVisitor validationStrategy) {
            return new Configuration(checkPrimaryConstructors, checkPropertyAccessors, includeMethodsOfKotlinAny,
                                     renderDeclarationsFromOtherModules, recursiveFilter, validationStrategy, renderer);
        }

        public Configuration withRenderer(@NotNull DescriptorRenderer renderer) {
            return new Configuration(checkPrimaryConstructors, checkPropertyAccessors, includeMethodsOfKotlinAny,
                                     renderDeclarationsFromOtherModules, recursiveFilter, validationStrategy, renderer);
        }
    }
}
