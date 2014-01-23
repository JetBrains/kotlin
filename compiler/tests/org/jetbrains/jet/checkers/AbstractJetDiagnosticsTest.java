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
import com.google.common.collect.ImmutableMap;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallWithTrace;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AbstractJetDiagnosticsTest extends BaseDiagnosticsTest {

    @Override
    protected void analyzeAndCheck(File testDataFile, List<TestFile> testFiles) {
        List<JetFile> jetFiles = getJetFiles(testFiles);

        CliLightClassGenerationSupport support = CliLightClassGenerationSupport.getInstanceForCli(getProject());

        BindingContext bindingContext = AnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                getProject(), jetFiles, support.getTrace(),
                Collections.<AnalyzerScriptParameter>emptyList(), Predicates.<PsiFile>alwaysTrue(), false, support.getModule()).getBindingContext();

        boolean ok = true;

        StringBuilder actualText = new StringBuilder();
        for (TestFile testFile : testFiles) {
            ok &= testFile.getActualText(bindingContext, actualText);
        }

        JetTestUtils.assertEqualsToFile(testDataFile, actualText.toString());

        assertTrue("Diagnostics mismatch. See the output above", ok);

        checkAllResolvedCallsAreCompleted(jetFiles, bindingContext);
    }

    private static void checkAllResolvedCallsAreCompleted(@NotNull List<JetFile> jetFiles, @NotNull BindingContext bindingContext) {
        for (JetFile file : jetFiles) {
            if (!AnalyzingUtils.getSyntaxErrorRanges(file).isEmpty()) {
                return;
            }
        }

        ImmutableMap<JetElement,ResolvedCall<? extends CallableDescriptor>> resolvedCallsEntries =
                bindingContext.getSliceContents(BindingContext.RESOLVED_CALL);
        for (Map.Entry<JetElement, ResolvedCall<? extends CallableDescriptor>> entry : resolvedCallsEntries.entrySet()) {
            JetElement element = entry.getKey();
            ResolvedCall<? extends CallableDescriptor> resolvedCall = entry.getValue();

            DiagnosticUtils.LineAndColumn lineAndColumn =
                    DiagnosticUtils.getLineAndColumnInPsiFile(element.getContainingFile(), element.getTextRange());

            assertTrue("Resolved call for '" + element.getText() + "'" + lineAndColumn + " in not completed",
                       ((ResolvedCallWithTrace<? extends CallableDescriptor>)resolvedCall).isCompleted());
        }
    }
}
