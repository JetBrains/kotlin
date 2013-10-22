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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.codegen.RecursionStatus;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.lazy.KotlinTestWithEnvironment;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class AbstractTailRecursionTest extends KotlinTestWithEnvironment {

    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_AND_ANNOTATIONS);
    }

    public void doTest(@NotNull String testFile) throws IOException {
        JetFile file = JetPsiFactory.createFile(getProject(), FileUtil.loadFile(new File(testFile), true));
        List<JetFile> files = new ArrayList<JetFile>(Collections.singleton(file));

        final BindingTrace trace = CliLightClassGenerationSupport.getInstanceForCli(getProject()).getTrace();
        assertNotNull(trace);

        AnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                getProject(), files, trace,
                Collections.<AnalyzerScriptParameter>emptyList(), Predicates.<PsiFile>alwaysTrue(), false).getBindingContext();

        file.acceptChildren(new JetTreeVisitor<Data>() {

            @Override
            public Void visitNamedFunction(@NotNull JetNamedFunction function, @Nullable Data outerData) {
                SimpleFunctionDescriptor descriptor = trace.get(BindingContext.FUNCTION, function);
                assert descriptor != null;

                Data data = new Data(descriptor);
                super.visitNamedFunction(function, data);

                if (data.isTail) {
                    List<JetCallExpression> calls = trace.get(BindingContext.FUNCTION_RECURSIONS, descriptor);
                    if (calls == null) {
                        calls = Collections.emptyList();
                    }

                    List<JetCallExpression> detectedRecursions = new ArrayList<JetCallExpression>(calls);
                    List<JetCallExpression> expectedRecursions = new ArrayList<JetCallExpression>(data.visitedCalls);

                    Collections.sort(detectedRecursions, new CallComparator());
                    Collections.sort(expectedRecursions, new CallComparator());

                    assertEquals(
                            "Bad detected tail recursions list for " + descriptor,
                            Joiner.on(",\n").skipNulls().join(Lists.transform(expectedRecursions, new CallExpressionToText())),
                            Joiner.on(",\n").skipNulls().join(Lists.transform(detectedRecursions, new CallExpressionToText()))
                    );

                    assertEquals(detectedRecursions, expectedRecursions);
                }

                return null;
            }

            @Override
            public Void visitCallExpression(@NotNull JetCallExpression expression, @Nullable Data data) {
                if (data != null && data.isTail) {
                    ResolvedCall<? extends CallableDescriptor> call =
                            trace.get(BindingContext.RESOLVED_CALL, expression.getCalleeExpression());

                    assert call != null;
                    if (data.functionDescriptor.equals(call.getCandidateDescriptor())) {
                        JetValueArgumentList argumentList = expression.getValueArgumentList();
                        assert argumentList != null;

                        checkCall(data.functionDescriptor, expression, argumentList, trace);
                        data.visitedCalls.add(expression);
                    }
                }

                super.visitCallExpression(expression, data);
                return null;
            }
        }, null);
    }

    private static class Data {
        public final SimpleFunctionDescriptor functionDescriptor;
        public final boolean isTail;
        public final List<JetCallExpression> visitedCalls = new ArrayList<JetCallExpression>();

        private Data(@NotNull SimpleFunctionDescriptor descriptor) {
            functionDescriptor = descriptor;
            isTail = KotlinBuiltIns.getInstance().isTailRecursive(descriptor);
        }
    }

    private static void checkCall(
            SimpleFunctionDescriptor functionDescriptor,
            JetCallExpression expression,
            JetValueArgumentList argumentList,
            BindingTrace trace
    ) {
        int size = argumentList.getArguments().size();
        boolean shouldBeTail = size == 0 || isLastArgumentTail(argumentList.getArguments());
        RecursionStatus status = trace.get(BindingContext.TAIL_RECURSION_CALL, expression);
        assertNotNull(status);
        assertEquals("Tail-recursion detection failed for " + functionDescriptor.getName().asString() + " at " + expression.getText(),
                     shouldBeTail, status.isDoGenerateTailRecursion());
    }

    private static boolean isLastArgumentTail(List<JetValueArgument> arguments) {
        JetValueArgument lastArgument = arguments.get(arguments.size() - 1);
        JetExpression expression = lastArgument.getArgumentExpression();
        if (expression instanceof JetStringTemplateExpression) {
            JetStringTemplateEntry[] entries = ((JetStringTemplateExpression) expression).getEntries();
            StringBuilder sb = new StringBuilder();
            for (JetStringTemplateEntry entry : entries) {
                sb.append(entry.getText());
            }

            return !sb.toString().trim().equals("no tail");
        }

        return true;
    }

    private static class CallComparator implements Comparator<JetCallExpression> {
        @Override
        public int compare(@NotNull JetCallExpression o1, @NotNull JetCallExpression o2) {
            return o1.getTextOffset() - o2.getTextOffset();
        }
    }

    private static class CallExpressionToText implements Function<JetCallExpression, String> {
        @Override
        public String apply(JetCallExpression input) {
            if (input == null) return null;
            return ("\"" + input.getText().replace("\"", "\\\"") + "\"");
        }
    }
}
