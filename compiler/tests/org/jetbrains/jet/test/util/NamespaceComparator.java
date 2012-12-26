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
import com.google.common.collect.Sets;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.PropertyCodegen;
import org.jetbrains.jet.jvm.compiler.ExpectedLoadErrorsUtil;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.MemberComparator;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
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
        comparator.deferred.throwFailures();
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
    private final DeferredAssertions deferred = new DeferredAssertions();

    private NamespaceComparator(@NotNull Configuration conf) {
        this.conf = conf;
    }

    private String doCompareNamespaces(@NotNull NamespaceDescriptor expectedNamespace, @NotNull NamespaceDescriptor actualNamespace) {
        StringBuilder sb = new StringBuilder();
        deferred.assertEquals(expectedNamespace.getName(), actualNamespace.getName());

        sb.append("namespace " + expectedNamespace.getName() + "\n");

        //deferred.assertTrue("namespace " + expectedNamespace.getName() + " is empty", !expectedNamespace.getMemberScope().getAllDescriptors().isEmpty());

        Set<Name> classifierNames = Sets.newHashSet();
        Set<Name> propertyNames = Sets.newHashSet();
        Set<Name> functionNames = Sets.newHashSet();
        Set<Name> objectNames = Sets.newHashSet();

        for (DeclarationDescriptor ad : expectedNamespace.getMemberScope().getAllDescriptors()) {
            if (ad instanceof ClassifierDescriptor) {
                classifierNames.add(ad.getName());
            }
            else if (ad instanceof PropertyDescriptor) {
                propertyNames.add(ad.getName());
            }
            else if (ad instanceof FunctionDescriptor) {
                functionNames.add(ad.getName());
            }
            else if (ad instanceof NamespaceDescriptor) {
                if (conf.recurseIntoPackage.apply(DescriptorUtils.getFQName(ad))) {
                    NamespaceDescriptor namespaceDescriptorA = (NamespaceDescriptor) ad;
                    NamespaceDescriptor namespaceDescriptorB = actualNamespace.getMemberScope().getNamespace(namespaceDescriptorA.getName());
                    //deferred.assertNotNull("Namespace not found: " + namespaceDescriptorA.getQualifiedName(), namespaceDescriptorB);
                    if (namespaceDescriptorB == null) {
                        System.err.println("Namespace not found: " + namespaceDescriptorA.getQualifiedName());
                    }
                    else {
                        String comparison = doCompareNamespaces(namespaceDescriptorA, namespaceDescriptorB);
                        sb.append("// <namespace name=\"" + namespaceDescriptorA.getName() + "\">\n");
                        sb.append(comparison);
                        sb.append("// </namespace name=\"" + namespaceDescriptorA.getName() + "\">\n");
                    }
                }
            }
            else {
                throw new AssertionError("unknown member: " + ad);
            }
        }

        for (ClassDescriptor objectDescriptor : expectedNamespace.getMemberScope().getObjectDescriptors()) {
            objectNames.add(objectDescriptor.getName());
        }

        for (Name name : sorted(classifierNames)) {
            ClassifierDescriptor ca = expectedNamespace.getMemberScope().getClassifier(name);
            ClassifierDescriptor cb = actualNamespace.getMemberScope().getClassifier(name);
            deferred.assertTrue("Classifier not found in " + expectedNamespace + ": " + name, ca != null);
            deferred.assertTrue("Classifier not found in " + actualNamespace + ": " + name, cb != null);
            if (ca != null && cb != null) {
                compareClassifiers(ca, cb, sb);
            }
        }

        for (Name name : sorted(objectNames)) {
            ClassifierDescriptor ca = expectedNamespace.getMemberScope().getObjectDescriptor(name);
            ClassifierDescriptor cb = actualNamespace.getMemberScope().getObjectDescriptor(name);
            deferred.assertTrue("Object not found in " + expectedNamespace + ": " + name, ca != null);
            deferred.assertTrue("Object not found in " + actualNamespace + ": " + name, cb != null);
            if (ca != null && cb != null) {
                compareClassifiers(ca, cb, sb);
            }
        }

        for (Name name : sorted(propertyNames)) {
            Collection<VariableDescriptor> pa = expectedNamespace.getMemberScope().getProperties(name);
            Collection<VariableDescriptor> pb = actualNamespace.getMemberScope().getProperties(name);
            compareDeclarationSets("Properties in package " + expectedNamespace, pa, pb, sb);

            deferred.assertTrue(actualNamespace.getMemberScope().getFunctions(Name.identifier(PropertyCodegen.getterName(name))).isEmpty());
            deferred.assertTrue(actualNamespace.getMemberScope().getFunctions(Name.identifier(PropertyCodegen.setterName(name))).isEmpty());
        }

        for (Name name : sorted(functionNames)) {
            Collection<FunctionDescriptor> fa = expectedNamespace.getMemberScope().getFunctions(name);
            Collection<FunctionDescriptor> fb = actualNamespace.getMemberScope().getFunctions(name);
            compareDeclarationSets("Functions in package " + expectedNamespace, fa, fb, sb);
        }

        return sb.toString();
    }

    private void compareDeclarationSets(String message,
            Collection<? extends DeclarationDescriptor> a,
            Collection<? extends DeclarationDescriptor> b,
            @NotNull StringBuilder sb) {
        String at = serializedDeclarationSets(a);
        String bt = serializedDeclarationSets(b);
        deferred.assertEquals(message, at, bt);
        sb.append(at);
    }

    private String serializedDeclarationSets(Collection<? extends DeclarationDescriptor> ds) {
        List<DeclarationDescriptor> members = Lists.newArrayList(ds);

        Collections.sort(members, MemberComparator.INSTANCE);

        List<String> strings = new ArrayList<String>();
        for (DeclarationDescriptor d : members) {
            strings.add(RENDERER.render(d));
        }


        StringBuilder r = new StringBuilder();
        for (String string : strings) {
            r.append(string);
            r.append("\n");
        }
        return r.toString();
    }

    private void compareClassifiers(@NotNull ClassifierDescriptor a, @NotNull ClassifierDescriptor b, @NotNull StringBuilder sb) {
        String as = serializeClassifier(a);
        String bs = serializeClassifier(b);

        deferred.assertEquals(as, bs);
        sb.append(as);
    }

    private String serializeClassifier(@NotNull ClassifierDescriptor classifier) {
        StringBuilder result = new StringBuilder();
        serializeDeclarationRecursively(classifier, new Printer(result));
        return result.toString();
    }

    private void serializeDeclarationRecursively(@NotNull DeclarationDescriptor descriptor, @NotNull Printer printer) {
        if (descriptor instanceof ClassDescriptor) {
            printer.println();
        }

        boolean isPrimaryConstructor = descriptor instanceof ConstructorDescriptor && ((ConstructorDescriptor) descriptor).isPrimary();
        printer.print(isPrimaryConstructor && conf.checkPrimaryConstructors ? "/*primary*/ " : "", RENDERER.render(descriptor));

        if (descriptor instanceof ClassDescriptor) {
            printer.printlnWithNoIndent(" {");
            printer.pushIndent();

            ClassDescriptor klass = (ClassDescriptor) descriptor;
            JetScope memberScope = klass.getDefaultType().getMemberScope();

            List<DeclarationDescriptor> subDescriptors = Lists.newArrayList();
            subDescriptors.addAll(klass.getConstructors());
            subDescriptors.addAll(memberScope.getAllDescriptors());
            subDescriptors.addAll(memberScope.getObjectDescriptors());
            ContainerUtil.addIfNotNull(subDescriptors, klass.getClassObjectDescriptor());

            Collections.sort(subDescriptors, MemberComparator.INSTANCE);

            for (DeclarationDescriptor subDescriptor : subDescriptors) {
                if (!conf.includeObject) {
                    // TODO regexp check? oh dear
                    if (subDescriptor.getName().getName().matches("equals|hashCode|finalize|wait|notify(All)?|toString|clone|getClass")) {
                        continue;
                    }
                }
                serializeDeclarationRecursively(subDescriptor, printer);
            }

            printer.popIndent();
            printer.println("}");
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
