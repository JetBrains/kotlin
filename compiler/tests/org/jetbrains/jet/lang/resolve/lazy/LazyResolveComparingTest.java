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

package org.jetbrains.jet.lang.resolve.lazy;

import com.google.common.base.Predicates;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiFile;
import org.jetbrains.jet.CompileCompilerDependenciesTest;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.cli.jvm.compiler.NamespaceComparator;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerForJvm;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.TopDownAnalysisParameters;
import org.jetbrains.jet.lang.resolve.java.CompilerDependencies;
import org.jetbrains.jet.lang.resolve.java.CompilerSpecialMode;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class LazyResolveComparingTest {
    private final Disposable rootDisposable = new Disposable() {
        @Override
        public void dispose() {
        }
    };

    private final CompilerDependencies
            compilerDependencies = CompileCompilerDependenciesTest.compilerDependenciesForTests(CompilerSpecialMode.REGULAR, true);
    private final JetCoreEnvironment jetCoreEnvironment = new JetCoreEnvironment(rootDisposable, compilerDependencies);
    private final Project project = jetCoreEnvironment.getProject();
    private final List<JetFile> files = Arrays.asList(
            JetPsiFactory.createFile(project, "class A {}"),
            JetPsiFactory.createFile(project, "package p; class C {fun f() {}}"),
            JetPsiFactory.createFile(project, "package p; open class G<T> {open fun f(): T {} fun a() {}}"),
            JetPsiFactory.createFile(project, "package p; class G2<E> : G<E> { fun g() : E {} override fun f() : T {}}"),
            JetPsiFactory.createFile(project, "package p; fun foo() {}"),
            JetPsiFactory.createFile(project, "package p; fun foo(a: C) {}")
    );

    @BeforeClass
    public static void setUp() throws Exception {
        System.setProperty("java.awt.headless", "true");
    }

    @After
    public void tearDown() throws Exception {
        Disposer.dispose(rootDisposable);
    }

    @Test
    public void testNamespacesAreEqual() throws Exception {
        TopDownAnalysisParameters params = new TopDownAnalysisParameters(
                Predicates.<PsiFile>alwaysTrue(), false, false, Collections.<AnalyzerScriptParameter>emptyList());
        ModuleDescriptor module = new ModuleDescriptor(Name.special("<test module>"));
        InjectorForTopDownAnalyzerForJvm
                injector = new InjectorForTopDownAnalyzerForJvm(project, params, new BindingTraceContext(), module, compilerDependencies);
        injector.getTopDownAnalyzer().analyzeFiles(files, Collections.<AnalyzerScriptParameter>emptyList());
        final BindingTrace trace = injector.getBindingTrace();

        //for (JetFile file : files) {
        //    for (final JetDeclaration declaration : file.getDeclarations()) {
        //        declaration.accept(new JetTreeVisitor<Void>() {
        //            @Override
        //            public Void visitDeclaration(JetDeclaration dcl, Void data) {
        //                DeclarationDescriptor descriptor = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, dcl);
        //                System.out.println(descriptor);
        //                return super.visitDeclaration(dcl, data);
        //            }
        //        }, null);
        //    }
        //}

        ModuleDescriptor lazyModule = new ModuleDescriptor(Name.special("<lazy module>"));

        ResolveSession session = new ResolveSession(project, lazyModule, new FileBasedDeclarationProviderFactory(files));

        NamespaceComparator.compareNamespaces(lazyModule.getRootNamespace(), module.getRootNamespace(), true,
                                              new File("compiler/testData/lazyResolve/simpleClass.txt"));
        //NamespaceComparator.compareNamespaces(lazyModule.getRootNamespace().getMemberScope().getNamespace(Name.identifier("p")),
        //                                      module.getRootNamespace().getMemberScope().getNamespace(Name.identifier("p")), true, new File("log1.txt"));
    }
}
