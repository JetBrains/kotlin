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

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.CompileCompilerDependenciesTest;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.java.CompilerSpecialMode;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
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
        final JetClass jetClass = JetPsiFactory.createClass(project, "package p; class C {fun f() {}}");
        final JetClass genericJetClass = JetPsiFactory.createClass(project, "package p; open class G<T> {fun f(): T {}}");
        final JetClass genericJetClass2 = JetPsiFactory.createClass(project, "package p; class G2<E> : G<E> {}");
        final JetNamedFunction fooFunction1 = JetPsiFactory.createFunction(project, "package p; fun foo() {}");
        final JetNamedFunction fooFunction2 = JetPsiFactory.createFunction(project, "package p; fun foo(a: C) {}");

        ResolveSession session = new ResolveSession(project, root, new DeclarationProviderFactory() {
            @Override
            public DeclarationProvider getPackageMemberDeclarationProvider(@NotNull FqName packageFqName) {
                if (packageFqName.equals(FqName.ROOT)) {
                    return new DeclarationProvider() {
                        @Override
                        public List<JetDeclaration> getAllDeclarations() {
                            return Collections.emptyList();
                        }

                        @NotNull
                        @Override
                        public List<JetNamedFunction> getFunctionDeclarations(@NotNull Name name) {
                            return Collections.emptyList();
                        }

                        @NotNull
                        @Override
                        public List<JetProperty> getPropertyDeclarations(@NotNull Name name) {
                            return Collections.emptyList();
                        }

                        @Override
                        public JetClassOrObject getClassOrObjectDeclaration(@NotNull Name name) {
                            return null;
                        }

                        @Override
                        public boolean isPackageDeclared(@NotNull Name name) {
                            return name.equals(Name.identifier("p"));
                        }
                    };
                }
                if (!packageFqName.equals(FqName.topLevel(Name.identifier("p")))) return null;
                return new DeclarationProvider() {
                    @Override
                    public List<JetDeclaration> getAllDeclarations() {
                        return Arrays.<JetDeclaration>asList(jetClass, genericJetClass, genericJetClass2, fooFunction1, fooFunction2);
                    }

                    @NotNull
                    @Override
                    public List<JetNamedFunction> getFunctionDeclarations(@NotNull Name name) {
                        if (!FOO.equals(name)) return Collections.emptyList();
                        return Arrays.asList(fooFunction1, fooFunction2);
                    }

                    @NotNull
                    @Override
                    public List<JetProperty> getPropertyDeclarations(@NotNull Name name) {
                        return Collections.emptyList();
                    }

                    @Override
                    public JetClassOrObject getClassOrObjectDeclaration(@NotNull Name name) {
                        if (name.equals(Name.identifier("C"))) return jetClass;
                        if (name.equals(Name.identifier("G"))) return genericJetClass;
                        if (name.equals(Name.identifier("G2"))) return genericJetClass2;
                        return null;
                    }

                    @Override
                    public boolean isPackageDeclared(@NotNull Name name) {
                        return false;
                    }
                };
            }

            @NotNull
            @Override
            public ClassMemberDeclarationProvider getClassMemberDeclarationProvider(@NotNull JetClassOrObject jetClassOrObject) {
                final JetClass jetClass = (JetClass) jetClassOrObject;
                return new ClassMemberDeclarationProvider() {
                    @NotNull
                    @Override
                    public JetClassOrObject getOwnerClassOrObject() {
                        return jetClass;
                    }

                    @Override
                    public List<JetDeclaration> getAllDeclarations() {
                        return jetClass.getDeclarations();
                    }

                    private <D, T> List<T> filter(List<D> list, Class<T> t) {
                        //noinspection unchecked
                        return (List) Lists.newArrayList(Collections2.filter(list, Predicates.instanceOf(t)));
                    }

                    @NotNull
                    @Override
                    public List<JetNamedFunction> getFunctionDeclarations(@NotNull Name name) {
                        return filter(jetClass.getDeclarations(), JetNamedFunction.class);
                    }

                    @NotNull
                    @Override
                    public List<JetProperty> getPropertyDeclarations(@NotNull Name name) {
                        return filter(jetClass.getDeclarations(), JetProperty.class);
                    }

                    @Override
                    public JetClassOrObject getClassOrObjectDeclaration(@NotNull Name name) {
                        return null;
                    }

                    @Override
                    public boolean isPackageDeclared(@NotNull Name name) {
                        return false;
                    }
                };
            }
        });

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

        ClassifierDescriptor genericClassifier2 = packageDescriptor.getMemberScope().getClassifier(Name.identifier("G2"));
        assertNotNull(genericClassifier2);
        assertEquals(1, genericClassifier2.getTypeConstructor().getSupertypes().size());
        assertEquals("G<E>", genericClassifier2.getTypeConstructor().getSupertypes().iterator().next().toString());

        ClassifierDescriptor genericClassifier = packageDescriptor.getMemberScope().getClassifier(Name.identifier("G"));
        assertNotNull(genericClassifier);
        assertEquals(1, genericClassifier.getTypeConstructor().getParameters().size());
        TypeParameterDescriptor typeParameterDescriptor_T = genericClassifier.getTypeConstructor().getParameters().get(0);
        assertEquals(JetStandardClasses.getNullableAnyType(),
                     typeParameterDescriptor_T.getUpperBoundsAsType());
        assertEquals("G<T>", genericClassifier.getDefaultType().toString());
        assertEquals(typeParameterDescriptor_T.getDefaultType(), genericClassifier.getDefaultType().getMemberScope().getFunctions(Name.identifier("f")).iterator().next().getReturnType());
    }
}