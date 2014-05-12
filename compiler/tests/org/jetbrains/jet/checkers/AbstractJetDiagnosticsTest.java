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
import com.google.common.collect.Sets;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.jet.descriptors.serialization.descriptors.MemberFilter;
import org.jetbrains.jet.lang.diagnostics.*;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.Diagnostics;
import org.jetbrains.jet.lang.resolve.calls.model.MutableResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;

public abstract class AbstractJetDiagnosticsTest extends BaseDiagnosticsTest {

    @Override
    protected void analyzeAndCheck(File testDataFile, List<TestFile> testFiles) {
        List<JetFile> jetFiles = getJetFiles(testFiles);

        CliLightClassGenerationSupport support = CliLightClassGenerationSupport.getInstanceForCli(getProject());

        BindingContext bindingContext = AnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                getProject(), jetFiles, support.getTrace(),
                Predicates.<PsiFile>alwaysTrue(), support.getModule(),
                MemberFilter.ALWAYS_TRUE).getBindingContext();

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

        ImmutableMap<JetElement, ResolvedCall<?>> resolvedCallsEntries = bindingContext.getSliceContents(BindingContext.RESOLVED_CALL);
        for (Map.Entry<JetElement, ResolvedCall<?>> entry : resolvedCallsEntries.entrySet()) {
            JetElement element = entry.getKey();
            ResolvedCall<?> resolvedCall = entry.getValue();

            DiagnosticUtils.LineAndColumn lineAndColumn =
                    DiagnosticUtils.getLineAndColumnInPsiFile(element.getContainingFile(), element.getTextRange());

            assertTrue("Resolved call for '" + element.getText() + "'" + lineAndColumn + " is not completed",
                       ((MutableResolvedCall<?>) resolvedCall).isCompleted());
        }

        checkResolvedCallsInDiagnostics(bindingContext);
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    private static void checkResolvedCallsInDiagnostics(BindingContext bindingContext) {
        Set<DiagnosticFactory> diagnosticsStoringResolvedCalls1 = Sets.<DiagnosticFactory>newHashSet(
                OVERLOAD_RESOLUTION_AMBIGUITY, NONE_APPLICABLE, CANNOT_COMPLETE_RESOLVE, UNRESOLVED_REFERENCE_WRONG_RECEIVER,
                ASSIGN_OPERATOR_AMBIGUITY, ITERATOR_AMBIGUITY);
        Set<DiagnosticFactory> diagnosticsStoringResolvedCalls2 = Sets.<DiagnosticFactory>newHashSet(
                COMPONENT_FUNCTION_AMBIGUITY, DELEGATE_SPECIAL_FUNCTION_AMBIGUITY, DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE);
        Diagnostics diagnostics = bindingContext.getDiagnostics();
        for (Diagnostic diagnostic : diagnostics) {
            DiagnosticFactory factory = diagnostic.getFactory();
            if (diagnosticsStoringResolvedCalls1.contains(factory)) {
                assertResolvedCallsAreCompleted(
                        diagnostic, ((DiagnosticWithParameters1<PsiElement, Collection<? extends ResolvedCall<?>>>) diagnostic).getA());

            }
            if (diagnosticsStoringResolvedCalls2.contains(factory)) {
                assertResolvedCallsAreCompleted(
                        diagnostic,
                        ((DiagnosticWithParameters2<PsiElement, Object, Collection<? extends ResolvedCall<?>>>)diagnostic).getB());
            }
        }
    }

    private static void assertResolvedCallsAreCompleted(
            @NotNull Diagnostic diagnostic, @NotNull Collection<? extends ResolvedCall<?>> resolvedCalls
    ) {
        boolean allCallsAreCompleted = true;
        for (ResolvedCall<?> resolvedCall : resolvedCalls) {
            if (!((MutableResolvedCall<?>) resolvedCall).isCompleted()) {
                allCallsAreCompleted = false;
            }
        }

        PsiElement element = diagnostic.getPsiElement();
        DiagnosticUtils.LineAndColumn lineAndColumn =
                DiagnosticUtils.getLineAndColumnInPsiFile(element.getContainingFile(), element.getTextRange());

        assertTrue("Resolved calls stored in " + diagnostic.getFactory().getName() + "\n" +
                   "for '" + element.getText() + "'" + lineAndColumn + " are not completed",
                   allCallsAreCompleted);
    }
}
