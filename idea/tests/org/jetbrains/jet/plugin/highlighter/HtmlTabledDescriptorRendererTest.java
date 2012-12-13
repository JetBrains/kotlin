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

package org.jetbrains.jet.plugin.highlighter;

import com.intellij.openapi.util.Condition;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetLiteFixture;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.util.Collections;
import java.util.List;

public class HtmlTabledDescriptorRendererTest extends JetLiteFixture {
    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_ONLY);
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/htmlTabledRenderer/";
    }

    public void testHtmlTabledRenderer() throws Exception {
        String fileName = "htmlTabledRenderer.kt";
        JetFile psiFile = createPsiFile(null, fileName, loadFile(fileName));

        AnalyzeExhaust analyzeExhaust = AnalyzerFacadeForJVM.analyzeOneFileWithJavaIntegration(psiFile, Collections.<AnalyzerScriptParameter>emptyList());
        BindingContext bindingContext = analyzeExhaust.getBindingContext();

        List<Diagnostic> diagnostics = ContainerUtil.filter(bindingContext.getDiagnostics(), new Condition<Diagnostic>() {
            @Override
            public boolean value(Diagnostic diagnostic) {
                return diagnostic.getFactory() == Errors.TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS;
            }
        });

        assertEquals(diagnostics.size(), 2);

        int index = 1;
        for (Diagnostic diagnostic : diagnostics) {
            String readableDiagnosticHtml = IdeErrorMessages.RENDERER.render(diagnostic).replaceAll(">", ">\n");
            assertSameLinesWithFile(getTestDataPath() + "/diagnostic" + index + ".html", readableDiagnosticHtml);

            index++;
        }
    }
}
