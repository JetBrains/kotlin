/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import com.google.common.collect.Lists;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
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
import org.junit.ComparisonFailure;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Stepan Koltsov
 */
public class NamespaceComparator {
    private static final DescriptorRenderer RENDERER = new DescriptorRendererBuilder()
            .setWithDefinedIn(false)
            .setExcludedAnnotationClasses(Arrays.asList(new FqName(ExpectedLoadErrorsUtil.ANNOTATION_CLASS_NAME)))
            .setVerbose(true).build();

    public static void compareNamespaces(
            @NotNull NamespaceDescriptor expectedNamespace,
            @NotNull NamespaceDescriptor actualNamespace,
            @NotNull Configuration configuration,
            @NotNull File txtFile
    ) {
        String serializedFormWithDemarkedNames = assertNamespacesEqual(expectedNamespace, actualNamespace, configuration);
        // The serializer puts "!" in front of the name because it allows for speedy sorting of members
        // see MemberComparator.normalize()
        String serialized = serializedFormWithDemarkedNames.replace("!", "");
        try {
            if (!txtFile.exists()) {
                FileUtil.writeToFile(txtFile, serialized);
                Assert.fail("Expected data file did not exist. Generating: " + txtFile);
            }
            String expected = FileUtil.loadFile(txtFile, true);

            // compare with hardcopy: make sure nothing is lost in output
            Assert.assertEquals(expected, serialized);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String assertNamespacesEqual(
            NamespaceDescriptor expectedNamespace,
            NamespaceDescriptor actualNamespace,
            @NotNull Configuration configuration
    ) {
        NamespaceComparator comparator = new NamespaceComparator(configuration);
        String serialized = comparator.doCompareNamespaces(expectedNamespace, actualNamespace);
        return serialized;
    }

    public static final Configuration DONT_INCLUDE_METHODS_OF_OBJECT = new Configuration(false, false, Predicates.<FqNameUnsafe>alwaysTrue());
    public static final Configuration RECURSIVE = new Configuration(false, true, Predicates.<FqNameUnsafe>alwaysTrue());

    public static class Configuration {

        private final boolean checkPrimaryConstructors;
        private final boolean includeObject;
        private final Predicate<FqNameUnsafe> recurseIntoPackage;

        public Configuration(
                boolean checkPrimaryConstructors,
                boolean includeObject,
                Predicate<FqNameUnsafe> recurseIntoPackage
        ) {
            this.checkPrimaryConstructors = checkPrimaryConstructors;
            this.includeObject = includeObject;
            this.recurseIntoPackage = recurseIntoPackage;
        }

        public Configuration filterRecusion(@NotNull Predicate<FqNameUnsafe> recurseIntoPackage) {
            return new Configuration(checkPrimaryConstructors, includeObject, recurseIntoPackage);
        }

        public Configuration checkPrimaryConstructors(boolean checkPrimaryConstructors) {
            return new Configuration(checkPrimaryConstructors, includeObject, recurseIntoPackage);
        }
    }

    private final Configuration conf;

    private NamespaceComparator(@NotNull Configuration conf) {
        this.conf = conf;
    }

    private String doCompareNamespaces(@NotNull NamespaceDescriptor expectedNamespace, @NotNull NamespaceDescriptor actualNamespace) {
        String expectedSerialized = recursiveSerialize(expectedNamespace);
        String actualSerialized = recursiveSerialize(actualNamespace);

        Assert.assertEquals(expectedSerialized, actualSerialized);
        return actualSerialized;
    }

    private String recursiveSerialize(@NotNull DeclarationDescriptor declarationDescriptor) {
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
            if (topLevel) {
                printer.println();
                printer.println();
            }
            else {
                printer.printlnWithNoIndent(" {").pushIndent();
            }

            List<DeclarationDescriptor> subDescriptors = Lists.newArrayList();

            if (descriptor instanceof ClassDescriptor) {
                ClassDescriptor klass = (ClassDescriptor) descriptor;
                JetScope memberScope = klass.getDefaultType().getMemberScope();

                subDescriptors.addAll(klass.getConstructors());
                subDescriptors.addAll(memberScope.getAllDescriptors());
                subDescriptors.addAll(memberScope.getObjectDescriptors());
                ContainerUtil.addIfNotNull(subDescriptors, klass.getClassObjectDescriptor());
            }
            else if (descriptor instanceof NamespaceDescriptor) {
                JetScope memberScope = ((NamespaceDescriptor) descriptor).getMemberScope();
                subDescriptors.addAll(memberScope.getAllDescriptors());
                subDescriptors.addAll(memberScope.getObjectDescriptors());
            }
            else {
                throw new IllegalStateException("Should be class or namespace: " + descriptor.getClass());
            }

            Collections.sort(subDescriptors, MemberComparator.INSTANCE);

            for (DeclarationDescriptor subDescriptor : subDescriptors) {
                if (!conf.includeObject) {
                    // TODO regexp check? oh dear
                    if (subDescriptor.getName().getName().matches("equals|hashCode|finalize|wait|notify(All)?|toString|clone|getClass")) {
                        continue;
                    }
                }
                if (subDescriptor instanceof NamespaceDescriptor && !conf.recurseIntoPackage.apply(DescriptorUtils.getFQName(subDescriptor))) {
                    continue;
                }
                appendDeclarationRecursively(subDescriptor, printer, false);
            }

            if (!topLevel) {
                printer.popIndent().println("}");
            }
        }
        else {
            printer.printlnWithNoIndent();
        }
    }


    private static <T extends Comparable<T>> List<T> sorted(Collection<T> items) {
        List<T> r = new ArrayList<T>(items);
        Collections.sort(r);
        return r;
    }

    private static class DeferredAssertions {
        private final List<AssertionError> assertionFailures = Lists.newArrayList();

        private void assertTrue(String message, Boolean b) {
            try {
                Assert.assertTrue(message, b);
            }
            catch (AssertionError e) {
                assertionFailures.add(e);
            }
        }

        private void assertTrue(Boolean b) {
            try {
                Assert.assertTrue(b);
            }
            catch (AssertionError e) {
                assertionFailures.add(e);
            }
        }

        private void assertNotNull(Object a) {
            try {
                Assert.assertNotNull(a);
            }
            catch (AssertionError e) {
                assertionFailures.add(e);
            }
        }

        private void assertEquals(Object a, Object b) {
            try {
                Assert.assertEquals(a, b);
            }
            catch (AssertionError e) {
                assertionFailures.add(e);
            }
        }

        private void assertEquals(String message, Object a, Object b) {
            try {
                Assert.assertEquals(message, a, b);
            }
            catch (AssertionError e) {
                assertionFailures.add(e);
            }
        }

        public void throwFailures() {
            StringBuilder expected = new StringBuilder();
            StringBuilder actual = new StringBuilder();
            for (AssertionError failure : assertionFailures) {
                if (failure instanceof ComparisonFailure) {
                    ComparisonFailure comparisonFailure = (ComparisonFailure) failure;
                    expected.append(comparisonFailure.getExpected());
                    actual.append(comparisonFailure.getActual());
                }
                else {
                    throw failure;
                }
            }
            Assert.assertEquals(expected.toString(), actual.toString());
        }

        public boolean failed() {
            return !assertionFailures.isEmpty();
        }
    }
}
