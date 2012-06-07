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
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiFile;
import com.intellij.util.Function;
import org.jetbrains.jet.CompileCompilerDependenciesTest;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.cli.jvm.compiler.NamespaceComparator;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerForJvm;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.TopDownAnalysisParameters;
import org.jetbrains.jet.lang.resolve.java.CompilerDependencies;
import org.jetbrains.jet.lang.resolve.java.CompilerSpecialMode;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.junit.After;
import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public abstract class AbstractLazyResolveComparingTest {
    private final Disposable rootDisposable = new Disposable() {
        @Override
        public void dispose() {
        }
    };

    private final CompilerDependencies
            compilerDependencies = CompileCompilerDependenciesTest.compilerDependenciesForTests(CompilerSpecialMode.REGULAR, true);
    private final JetCoreEnvironment jetCoreEnvironment = new JetCoreEnvironment(rootDisposable, compilerDependencies);
    private final Project project = jetCoreEnvironment.getProject();

    @BeforeClass
    public static void setUp() throws Exception {
        System.setProperty("java.awt.headless", "true");
    }

    @After
    public void tearDown() throws Exception {
        Disposer.dispose(rootDisposable);
    }

    protected void doTest(String testFileName, Function<Pair<ModuleDescriptor, ModuleDescriptor>, Pair<NamespaceDescriptor, NamespaceDescriptor>> transform) throws IOException {
        TopDownAnalysisParameters params = new TopDownAnalysisParameters(
                Predicates.<PsiFile>alwaysTrue(), false, false, Collections.<AnalyzerScriptParameter>emptyList());
        ModuleDescriptor module = new ModuleDescriptor(Name.special("<test module>"));
        InjectorForTopDownAnalyzerForJvm
                injector = new InjectorForTopDownAnalyzerForJvm(project, params, new BindingTraceContext(), module, compilerDependencies);


        List<JetFile> files = JetTestUtils
                .createTestFiles(testFileName, FileUtil.loadFile(new File(testFileName)), new JetTestUtils.TestFileFactory<JetFile>() {
                    @Override
                    public JetFile create(String fileName, String text) {
                        return JetPsiFactory.createFile(project, fileName, text);
                    }
                });

        injector.getTopDownAnalyzer().analyzeFiles(files, Collections.<AnalyzerScriptParameter>emptyList());

        ModuleDescriptor lazyModule = new ModuleDescriptor(Name.special("<lazy module>"));

        ResolveSession session = new ResolveSession(project, lazyModule, new FileBasedDeclarationProviderFactory(files));

        Pair<NamespaceDescriptor, NamespaceDescriptor> namespacesToCompare = transform.fun(Pair.create(module, lazyModule));

        NamespaceComparator.compareNamespaces(namespacesToCompare.first, namespacesToCompare.second,
                                              true,
                                              new File(FileUtil.getNameWithoutExtension(testFileName) + ".txt"));
    }

    protected void doTest(String testFileName) throws Exception {
        doTest(testFileName, new Function<Pair<ModuleDescriptor, ModuleDescriptor>, Pair<NamespaceDescriptor, NamespaceDescriptor>>() {
            @Override
            public Pair<NamespaceDescriptor, NamespaceDescriptor> fun(Pair<ModuleDescriptor, ModuleDescriptor> pair) {
                return Pair.create(pair.first.getRootNamespace(), pair.second.getRootNamespace());
            }
        });
    }

    protected void doTestSinglePackage(String testFileName) throws Exception {
        doTest(testFileName, new Function<Pair<ModuleDescriptor, ModuleDescriptor>, Pair<NamespaceDescriptor, NamespaceDescriptor>>() {
            @Override
            public Pair<NamespaceDescriptor, NamespaceDescriptor> fun(Pair<ModuleDescriptor, ModuleDescriptor> pair) {
                ModuleDescriptor expectedModule = pair.first;
                ModuleDescriptor actualModule = pair.second;
                NamespaceDescriptor actual = theOnlySubPackage(actualModule.getRootNamespace());
                NamespaceDescriptor expected = expectedModule.getRootNamespace().getMemberScope().getNamespace(actual.getName());
                return Pair.create(expected, actual);
            }
        });
    }

    private NamespaceDescriptor theOnlySubPackage(NamespaceDescriptor namespace) {
        return (NamespaceDescriptor) namespace.getMemberScope().getAllDescriptors().iterator().next();
    }
}
