/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.codeInsight;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.InTextDirectivesUtils;
import org.jetbrains.jet.checkers.DebugInfoUtil;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.JetLightCodeInsightFixtureTestCase;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;

import java.io.File;
import java.util.List;

public abstract class AbstractInsertImportOnPasteTest extends JetLightCodeInsightFixtureTestCase {
    private static final String BASE_PATH = PluginTestCaseBase.getTestDataPathBase() + "/copyPaste/imports";
    private static final String DEFAULT_TO_FILE_TEXT = "package to\n\n<caret>";
    private static final String ALLOW_UNRESOLVED_DIRECTIVE = "// ALLOW_UNRESOLVED";

    private int savedState;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        savedState = CodeInsightSettings.getInstance().ADD_IMPORTS_ON_PASTE;
        CodeInsightSettings.getInstance().ADD_IMPORTS_ON_PASTE = CodeInsightSettings.YES;
    }

    @Override
    protected void tearDown() throws Exception {
        CodeInsightSettings.getInstance().ADD_IMPORTS_ON_PASTE = savedState;
        super.tearDown();
    }

    public void doTestCut(String path) throws Exception {
        doTestAction(IdeActions.ACTION_CUT, path);
    }

    public void doTestCopy(String path) throws Exception {
        doTestAction(IdeActions.ACTION_COPY, path);
    }

    private void doTestAction(@NotNull String cutOrCopy, String path) throws Exception {
        myFixture.setTestDataPath(BASE_PATH);
        File testFile = new File(path);
        String testFileName = testFile.getName();

        configureByDependencyIfExists(testFileName.replace(".kt", ".dependency.kt"));
        configureByDependencyIfExists(testFileName.replace(".kt", ".dependency.java"));
        myFixture.configureByFile(testFileName);
        myFixture.performEditorAction(cutOrCopy);

        String toFileName = testFileName.replace(".kt", ".to.kt");
        JetFile toFile = configureToFile(toFileName);
        performNotWriteEditorAction(IdeActions.ACTION_PASTE);

        myFixture.checkResultByFile(testFileName.replace(".kt", ".expected.kt"));

        if (!InTextDirectivesUtils.isDirectiveDefined(FileUtil.loadFile(testFile, true), ALLOW_UNRESOLVED_DIRECTIVE)) {
            checkNoUnresolvedReferences(toFile);
        }
    }

    @NotNull
    private JetFile configureToFile(@NotNull String toFileName) {
        if (new File(BASE_PATH + "/" + toFileName).exists()) {
            return (JetFile) myFixture.configureByFile(toFileName);
        }
        else {
            return (JetFile) myFixture.configureByText(toFileName, DEFAULT_TO_FILE_TEXT);
        }
    }

    private static void checkNoUnresolvedReferences(@NotNull final JetFile file) {
        BindingContext bindingContext = ResolvePackage.analyzeFully(file);
        for (Diagnostic diagnostic : bindingContext.getDiagnostics()) {
            if (Errors.UNRESOLVED_REFERENCE_DIAGNOSTICS.contains(diagnostic.getFactory())) {
                List<TextRange> textRanges = diagnostic.getTextRanges();
                String diagnosticText = DefaultErrorMessages.render(diagnostic);
                if (diagnostic.getPsiFile() == file) {
                    fail(diagnostic.getFactory().getName() + ": " + diagnosticText + " "
                         + DiagnosticUtils.atLocation(file, textRanges.get(0)));
                }
            }
        }
        DebugInfoUtil.markDebugAnnotations(file, bindingContext, new DebugInfoUtil.DebugInfoReporter() {
            @Override
            public void preProcessReference(@NotNull JetReferenceExpression expression) {
                ResolvePackage.analyze(expression);
            }

            @Override
            public void reportElementWithErrorType(@NotNull JetReferenceExpression expression) {
                //do nothing
            }

            @Override
            public void reportMissingUnresolved(@NotNull JetReferenceExpression expression) {
                // this may happen if incorrect psi transformations are done
                fail(expression.getText() + " is unresolved but not marked " + DiagnosticUtils.atLocation(file, expression.getTextRange()));
            }

            @Override
            public void reportUnresolvedWithTarget(
                    @NotNull JetReferenceExpression expression, @NotNull String target
            ) {
                //do nothing
            }
        });
    }

    private void configureByDependencyIfExists(@NotNull String dependencyFileName) throws Exception {
        File file = new File(BASE_PATH + "/" + dependencyFileName);
        if (file.exists()) {
            if (dependencyFileName.endsWith(".java")) {
                //allow test framework to put it under right directory
                myFixture.addClass(FileUtil.loadFile(file, true));
            }
            else {
                myFixture.configureByFile(dependencyFileName);
            }
        }
    }

    @Override
    protected String getTestDataPath() {
        return BASE_PATH;
    }
}
