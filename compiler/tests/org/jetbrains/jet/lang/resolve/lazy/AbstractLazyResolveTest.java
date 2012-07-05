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
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerForJvm;
import org.jetbrains.jet.lang.BuiltinsScopeExtensionMode;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.TopDownAnalysisParameters;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.junit.After;
import org.junit.BeforeClass;

import java.util.Collections;

/**
 * @author abreslav
 */
public abstract class AbstractLazyResolveTest {
    private final Disposable rootDisposable = new Disposable() {
        @Override
        public void dispose() {
        }
    };

    protected final JetCoreEnvironment jetCoreEnvironment = new JetCoreEnvironment(rootDisposable,
            CompileCompilerDependenciesTest.compilerConfigurationForTests(ConfigurationKind.JDK_ONLY, true)
    );
    protected final Project project = jetCoreEnvironment.getProject();

    @BeforeClass
    public static void setUp() throws Exception {
        System.setProperty("java.awt.headless", "true");
    }

    @After
    public void tearDown() throws Exception {
        Disposer.dispose(rootDisposable);
    }

    protected InjectorForTopDownAnalyzerForJvm getEagerInjectorForTopDownAnalyzer() {
        ModuleDescriptor eagerModuleForLazy = new ModuleDescriptor(Name.special("<eager module for lazy>"));

        InjectorForTopDownAnalyzerForJvm tdaInjectorForLazy = createInjectorForTDA(eagerModuleForLazy);
        // This line is required fro the 'jet' namespace to be filled in with functions
        tdaInjectorForLazy.getTopDownAnalyzer().analyzeFiles(
                Collections.singletonList(JetPsiFactory.createFile(project, "")), Collections.<AnalyzerScriptParameter>emptyList());
        return tdaInjectorForLazy;
    }

    protected InjectorForTopDownAnalyzerForJvm createInjectorForTDA(ModuleDescriptor module) {
        TopDownAnalysisParameters params = new TopDownAnalysisParameters(
                Predicates.<PsiFile>alwaysTrue(), false, false, Collections.<AnalyzerScriptParameter>emptyList());
        return new InjectorForTopDownAnalyzerForJvm(project, params, new BindingTraceContext(), module, BuiltinsScopeExtensionMode.ALL);
    }

}
