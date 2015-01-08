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

package org.jetbrains.jet.plugin.highlighter;

import com.google.common.collect.Sets;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetLiteFixture;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.analyzer.AnalysisResult;
import org.jetbrains.kotlin.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.lazy.JvmResolveUtil;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.jet.plugin.highlighter.formatHtml.FormatHtmlPackage.formatHtml;

public abstract class AbstractDiagnosticMessageTest extends JetLiteFixture {
    private static final String DIAGNOSTICS_NUMBER_DIRECTIVE = "DIAGNOSTICS_NUMBER";
    private static final String DIAGNOSTICS_DIRECTIVE = "DIAGNOSTICS";
    private static final String MESSAGE_TYPE_DIRECTIVE = "MESSAGE_TYPE";

    private enum MessageType {
        TEXT("TEXT", "txt"), HTML("HTML", "html");

        public final String directive;
        public final String extension;

        MessageType(String directive, String extension) {
            this.directive = directive;
            this.extension = extension;
        }
    }

    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_ONLY);
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/diagnosticMessage/";
    }

    public void doTest(String filePath) throws Exception {
        File file = new File(filePath);
        String fileName = file.getName();

        String fileData = JetTestUtils.doLoadFile(file);
        Map<String,String> directives = JetTestUtils.parseDirectives(fileData);
        int diagnosticNumber = getDiagnosticNumber(directives);
        final Set<DiagnosticFactory<?>> diagnosticFactories = getDiagnosticFactories(directives);
        MessageType messageType = getMessageTypeDirective(directives);

        JetFile psiFile = createPsiFile(null, fileName, loadFile(fileName));
        AnalysisResult analysisResult = JvmResolveUtil.analyzeOneFileWithJavaIntegration(psiFile);
        BindingContext bindingContext = analysisResult.getBindingContext();

        List<Diagnostic> diagnostics = ContainerUtil.filter(bindingContext.getDiagnostics().all(), new Condition<Diagnostic>() {
            @Override
            public boolean value(Diagnostic diagnostic) {
                return diagnosticFactories.contains(diagnostic.getFactory());
            }
        });

        assertEquals("Expected diagnostics number mismatch:", diagnosticNumber, diagnostics.size());

        int index = 1;
        String name = FileUtil.getNameWithoutExtension(fileName);
        for (Diagnostic diagnostic : diagnostics) {
            String readableDiagnosticText;
            String extension;
            if (messageType != MessageType.TEXT && IdeErrorMessages.hasIdeSpecificMessage(diagnostic)) {
                readableDiagnosticText = formatHtml(IdeErrorMessages.render(diagnostic));
                extension = MessageType.HTML.extension;
            }
            else {
                readableDiagnosticText = DefaultErrorMessages.render(diagnostic);
                extension = MessageType.TEXT.extension;
            }
            String errorMessageFileName = name + index;
            String path = getTestDataPath() + "/" + errorMessageFileName + "." + extension;
            String actualText = "<!-- " + errorMessageFileName + " -->\n" + readableDiagnosticText;
            assertSameLinesWithFile(path, actualText);

            index++;
        }
    }

    private static int getDiagnosticNumber(Map<String, String> directives) {
        String diagnosticsNumber = directives.get(DIAGNOSTICS_NUMBER_DIRECTIVE);
        assert diagnosticsNumber != null : DIAGNOSTICS_NUMBER_DIRECTIVE + " should be present.";
        try {
            return Integer.parseInt(diagnosticsNumber);
        }
        catch (NumberFormatException e) {
            throw new AssertionError(DIAGNOSTICS_NUMBER_DIRECTIVE + " should contain number as its value.");
        }
    }

    @NotNull
    private static Set<DiagnosticFactory<?>> getDiagnosticFactories(Map<String, String> directives) {
        String diagnosticsData = directives.get(DIAGNOSTICS_DIRECTIVE);
        assert diagnosticsData != null : DIAGNOSTICS_DIRECTIVE + " should be present.";
        Set<DiagnosticFactory<?>> diagnosticFactories = Sets.newHashSet();
        String[] diagnostics = diagnosticsData.split(" ");
        for (String diagnosticName : diagnostics) {
            String errorMessage = "Can't load diagnostic factory for " + diagnosticName;
            try {
                Field field = Errors.class.getField(diagnosticName);
                Object value = field.get(null);
                if (value instanceof DiagnosticFactory) {
                    diagnosticFactories.add((DiagnosticFactory<?>)value);
                }
                else {
                    throw new AssertionError(errorMessage);
                }
            }
            catch (NoSuchFieldException e) {
                throw new AssertionError(errorMessage);
            }
            catch (IllegalAccessException e) {
                throw new AssertionError(errorMessage);
            }
        }
        return diagnosticFactories;
    }

    @Nullable
    private static MessageType getMessageTypeDirective(Map<String, String> directives) {
        String messageType = directives.get(MESSAGE_TYPE_DIRECTIVE);
        if (messageType == null) return null;
        try {
            return MessageType.valueOf(messageType);
        }
        catch (IllegalArgumentException e) {
            throw new AssertionError(MESSAGE_TYPE_DIRECTIVE + " should be " + MessageType.TEXT.directive + " or " +
                                     MessageType.HTML.directive + ". But was: \"" + messageType + "\".");
        }
    }
}
