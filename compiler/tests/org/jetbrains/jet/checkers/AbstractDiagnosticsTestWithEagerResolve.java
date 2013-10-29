/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.checkers;

import com.google.common.base.Predicates;
import com.intellij.psi.PsiFile;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;

import java.io.File;
import java.util.Collections;
import java.util.List;

public abstract class AbstractDiagnosticsTestWithEagerResolve extends AbstractJetDiagnosticsTest {

    @Override
    protected void analyzeAndCheck(File testDataFile, List<TestFile> testFiles) {
        List<JetFile> jetFiles = getJetFiles(testFiles);

        BindingTrace trace = CliLightClassGenerationSupport.getInstanceForCli(getProject()).getTrace();

        BindingContext bindingContext = AnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                getProject(), jetFiles, trace,
                Collections.<AnalyzerScriptParameter>emptyList(), Predicates.<PsiFile>alwaysTrue(), false).getBindingContext();

        boolean ok = true;

        StringBuilder actualText = new StringBuilder();
        for (TestFile testFile : testFiles) {
            ok &= testFile.getActualText(bindingContext, actualText);
        }

        JetTestUtils.assertEqualsToFile(testDataFile, actualText.toString());

        assertTrue("Diagnostics mismatch. See the output above", ok);
    }
}
