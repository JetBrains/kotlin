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

package org.jetbrains.jet.checkers;

import com.google.common.base.Predicates;
import com.intellij.psi.PsiFile;
import org.jetbrains.jet.lang.BuiltinsScopeExtensionMode;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.test.generator.SimpleTestClassModel;
import org.jetbrains.jet.test.generator.TestGenerator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public abstract class AbstractDiagnosticsTestWithEagerResolve extends AbstractJetDiagnosticsTest {

    @Override
    protected void analyzeAndCheck(File testDataFile, String expectedText, List<TestFile> testFiles) {
        List<JetFile> jetFiles = getJetFiles(testFiles);

        BindingContext bindingContext = AnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                getProject(), jetFiles, Collections.<AnalyzerScriptParameter>emptyList(),
                Predicates.<PsiFile>alwaysTrue(), BuiltinsScopeExtensionMode.ALL).getBindingContext();

        boolean ok = true;

        StringBuilder actualText = new StringBuilder();
        for (TestFile testFile : testFiles) {
            ok &= testFile.getActualText(bindingContext, actualText);
        }

        assertEquals(expectedText, actualText.toString());

        assertTrue("Diagnostics mismatch. See the output above", ok);
    }

    public static void main(String[] args) throws IOException {
        String aPackage = "org.jetbrains.jet.checkers";
        Class<AbstractDiagnosticsTestWithEagerResolve> thisClass = AbstractDiagnosticsTestWithEagerResolve.class;
        new TestGenerator(
                "compiler/tests/",
                aPackage,
                "JetDiagnosticsTestGenerated",
                thisClass,
                Arrays.asList(
                        new SimpleTestClassModel(new File("compiler/testData/diagnostics/tests"), true, "kt", "doTest"),
                        new SimpleTestClassModel(new File("compiler/testData/diagnostics/tests/script"), true, "ktscript", "doTest")
                ),
                thisClass
        ).generateAndSave();
    }

}
