package org.jetbrains.jet.lang.resolve.lazy;/*
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

import com.google.common.collect.Lists;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.jet.CompileCompilerDependenciesTest;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.java.CompilerSpecialMode;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author abreslav
 */
public class BasicLazyResolveTest {

    private static final Name FOO = Name.identifier("foo");

    private final Disposable rootDisposable = new Disposable() {
        @Override
        public void dispose() {
        }
    };

    @After
    public void tearDown() throws Exception {
        Disposer.dispose(rootDisposable);
    }

    @Test
    public void test() {
        JetCoreEnvironment jetCoreEnvironment =
                new JetCoreEnvironment(rootDisposable, CompileCompilerDependenciesTest.compilerDependenciesForTests(CompilerSpecialMode.REGULAR, true));

        ModuleDescriptor root = new ModuleDescriptor(Name.special("<root>"));

        Project project = jetCoreEnvironment.getProject();

        ResolveSession session = new ResolveSession(project, root, new FileBasedDeclarationProviderFactory(Arrays.asList(
                JetPsiFactory.createFile(project, "package p; class C {fun f() {}}"),
                JetPsiFactory.createFile(project, "package p; open class G<T> {open fun f(): T {} fun a() {}}"),
                JetPsiFactory.createFile(project, "package p; class G2<E> : G<E> { fun g() : E {} override fun f() : T {}}"),
                JetPsiFactory.createFile(project, "package p; fun foo() {}"),
                JetPsiFactory.createFile(project, "package p; fun foo(a: C) {}")
        )));

        NamespaceDescriptor packageDescriptor = session.getPackageDescriptor(Name.identifier("p"));
        assertNotNull(packageDescriptor);
        assertEquals(Name.identifier("p"), packageDescriptor.getName());
        assertNull(packageDescriptor.getMemberScope().getNamespace(FOO));

        ClassifierDescriptor classifier = packageDescriptor.getMemberScope().getClassifier(Name.identifier("C"));
        assertTrue(ClassDescriptor.class.isInstance(classifier));
        ClassDescriptor classDescriptor = (ClassDescriptor) classifier;
        assertNotNull(classifier);
        assertEquals(classifier.getContainingDeclaration(), packageDescriptor);
        assertEquals(Name.identifier("C"), classifier.getName());
        assertEquals(Visibilities.INTERNAL, classDescriptor.getVisibility());
        TypeConstructor typeConstructor = classifier.getTypeConstructor();
        assertTrue(typeConstructor.isSealed());
        assertTrue(typeConstructor.getParameters().isEmpty());
        assertEquals("[Any]", typeConstructor.getSupertypes().toString());

        List<FunctionDescriptor> fooFunctions = Lists.newArrayList(packageDescriptor.getMemberScope().getFunctions(FOO));
        assertEquals(2, fooFunctions.size());
        FunctionDescriptor foo1 = fooFunctions.get(0);
        assertEquals(FOO, foo1.getName());
        assertEquals(0, foo1.getValueParameters().size());
        assertEquals(packageDescriptor, foo1.getContainingDeclaration());

        FunctionDescriptor foo2 = fooFunctions.get(1);
        assertEquals(FOO, foo2.getName());
        assertEquals(1, foo2.getValueParameters().size());
        assertEquals(classDescriptor, foo2.getValueParameters().get(0).getType().getConstructor().getDeclarationDescriptor());
        assertEquals(packageDescriptor, foo2.getContainingDeclaration());

        ClassifierDescriptor genericClassifier = packageDescriptor.getMemberScope().getClassifier(Name.identifier("G"));
        assertNotNull(genericClassifier);
        ClassDescriptor genericClass = (ClassDescriptor) genericClassifier;
        assertEquals(Modality.OPEN, genericClass.getModality());
        assertEquals(1, genericClassifier.getTypeConstructor().getParameters().size());
        TypeParameterDescriptor typeParameterDescriptor_T = genericClassifier.getTypeConstructor().getParameters().get(0);
        assertEquals(JetStandardClasses.getNullableAnyType(),
                     typeParameterDescriptor_T.getUpperBoundsAsType());
        assertEquals("G<T>", genericClassifier.getDefaultType().toString());
        assertEquals(typeParameterDescriptor_T.getDefaultType(), genericClassifier.getDefaultType().getMemberScope().getFunctions(Name.identifier("f")).iterator().next().getReturnType());

        ClassifierDescriptor genericClassifier2 = packageDescriptor.getMemberScope().getClassifier(Name.identifier("G2"));
        assertNotNull(genericClassifier2);
        assertEquals(1, genericClassifier2.getTypeConstructor().getSupertypes().size());
        assertEquals("G<E>", genericClassifier2.getTypeConstructor().getSupertypes().iterator().next().toString());
        assertEquals(1, genericClassifier2.getDefaultType().getMemberScope().getFunctions(Name.identifier("g")).size());
        assertEquals(1, genericClassifier2.getDefaultType().getMemberScope().getFunctions(Name.identifier("a")).size());
        assertEquals(1, genericClassifier2.getDefaultType().getMemberScope().getFunctions(Name.identifier("f")).iterator().next().getOverriddenDescriptors().size());
    }
}