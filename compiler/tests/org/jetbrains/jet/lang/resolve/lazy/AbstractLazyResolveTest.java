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
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzer;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerForJvm;
import org.jetbrains.jet.lang.BuiltinsScopeExtensionMode;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.TopDownAnalysisParameters;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.junit.After;

import java.util.Collections;

/**
 * @author abreslav
 */
public abstract class AbstractLazyResolveTest extends TestCase {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private final Disposable rootDisposable = new Disposable() {
        @Override
        public void dispose() {
        }
    };

    public class JetCoreEnvironmentWithDisposable {
        public final JetCoreEnvironment jetCoreEnvironment;

        public final Project project;

        public JetCoreEnvironmentWithDisposable(@NotNull ConfigurationKind configurationKind) {
            this.jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(rootDisposable, configurationKind);
            this.project = jetCoreEnvironment.getProject();
        }

    }

    protected final JetCoreEnvironmentWithDisposable regularEnvironment = new JetCoreEnvironmentWithDisposable(ConfigurationKind.ALL);

    protected Project getProject() {
        return regularEnvironment.project;
    }

    @After
    public void tearDown() throws Exception {
        Disposer.dispose(rootDisposable);
    }

    protected InjectorForTopDownAnalyzer getEagerInjectorForTopDownAnalyzer(JetCoreEnvironmentWithDisposable environment) {
        ModuleDescriptor eagerModuleForLazy = new ModuleDescriptor(Name.special("<eager module for lazy>"));

        InjectorForTopDownAnalyzer tdaInjectorForLazy = createInjectorForTDA(eagerModuleForLazy, environment);
        // This line is required fro the 'jet' namespace to be filled in with functions
        tdaInjectorForLazy.getTopDownAnalyzer().analyzeFiles(
                Collections.singletonList(JetPsiFactory.createFile(getProject(), "")), Collections.<AnalyzerScriptParameter>emptyList());
        return tdaInjectorForLazy;
    }

    protected InjectorForTopDownAnalyzer createInjectorForTDA(ModuleDescriptor module, JetCoreEnvironmentWithDisposable environment) {
        TopDownAnalysisParameters params = new TopDownAnalysisParameters(
                Predicates.<PsiFile>alwaysTrue(), false, false, Collections.<AnalyzerScriptParameter>emptyList());
        return new InjectorForTopDownAnalyzerForJvm(environment.project, params, new BindingTraceContext(), module, BuiltinsScopeExtensionMode.ALL);
    }

}
