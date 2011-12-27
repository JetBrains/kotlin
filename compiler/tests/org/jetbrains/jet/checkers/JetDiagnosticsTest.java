package org.jetbrains.jet.checkers;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import junit.framework.Test;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetLiteFixture;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.lang.Configuration;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacade;

import java.io.File;
import java.util.List;

/**
 * @author abreslav
 */
public class JetDiagnosticsTest extends JetLiteFixture {
    private String name;

    public JetDiagnosticsTest(@NonNls String dataPath, String name) {
        super(dataPath);
        this.name = name;
    }

    @Override
    public String getName() {
        return "test" + name;
    }


    private class TestFile {
        private final List<CheckerTestUtil.DiagnosedRange> diagnosedRanges = Lists.newArrayList();
        private final String expectedText;
        private final String clearText;
        private final JetFile jetFile;

        public TestFile(String fileName, String textWithMarkers) {
            expectedText = textWithMarkers;
            clearText = CheckerTestUtil.parseDiagnosedRanges(expectedText, diagnosedRanges);
            jetFile = createCheckAndReturnPsiFile(fileName, clearText);
            for (CheckerTestUtil.DiagnosedRange diagnosedRange : diagnosedRanges) {
                diagnosedRange.setFile(jetFile);
            }
        }

        public void getActualText(BindingContext bindingContext, StringBuilder actualText) {
            CheckerTestUtil.diagnosticsDiff(diagnosedRanges, CheckerTestUtil.getDiagnosticsIncludingSyntaxErrors(bindingContext, jetFile), new CheckerTestUtil.DiagnosticDiffCallbacks() {
                @NotNull
                @Override
                public PsiFile getFile() {
                    return jetFile;
                }

                @Override
                public void missingDiagnostic(String type, int expectedStart, int expectedEnd) {
                    String message = "Missing " + type + DiagnosticUtils.atLocation(jetFile, new TextRange(expectedStart, expectedEnd));
                    System.err.println(message);
                }

                @Override
                public void unexpectedDiagnostic(String type, int actualStart, int actualEnd) {
                    String message = "Unexpected " + type + DiagnosticUtils.atLocation(jetFile, new TextRange(actualStart, actualEnd));
                    System.err.println(message);
                }
            });

            actualText.append(CheckerTestUtil.addDiagnosticMarkersToText(jetFile, bindingContext, AnalyzingUtils.getSyntaxErrorRanges(jetFile)));
        }

    }

    @Override
    public void runTest() throws Exception {
        String testFileName = name + ".jet";

        String expectedText = loadFile(testFileName);

        List<TestFile> testFileFiles = JetTestUtils.createTestFiles(testFileName, expectedText, new JetTestUtils.TestFileFactory<TestFile>() {
            @Override
            public TestFile create(String fileName, String text) {
                return new TestFile(fileName, text);
            }
        });

        boolean importJdk = expectedText.contains("+JDK");
//        Configuration configuration = importJdk ? JavaBridgeConfiguration.createJavaBridgeConfiguration(getProject()) : Configuration.EMPTY;

        List<JetDeclaration> namespaces = Lists.newArrayList();
        for (TestFile testFileFile : testFileFiles) {
            namespaces.add(testFileFile.jetFile.getRootNamespace());
        }

        BindingContext bindingContext;
        if (importJdk) {
            bindingContext = AnalyzerFacade.analyzeNamespacesWithJavaIntegration(getProject(), namespaces, Predicates.<PsiFile>alwaysTrue(), JetControlFlowDataTraceFactory.EMPTY);
        }
        else {
            bindingContext = AnalyzingUtils.analyzeNamespaces(getProject(), Configuration.EMPTY, namespaces, Predicates.<PsiFile>alwaysTrue(), JetControlFlowDataTraceFactory.EMPTY);
        }

        StringBuilder actualText = new StringBuilder();
        for (TestFile testFileFile : testFileFiles) {
            testFileFile.getActualText(bindingContext, actualText);
        }

        assertEquals(expectedText, actualText.toString());
    }

    //    private void convert(File src, File dest) throws IOException {
//        File[] files = src.listFiles();
//        for (File file : files) {
//            try {
//                if (file.isDirectory()) {
//                    File destDir = new File(dest, file.getName());
//                    destDir.mkdir();
//                    convert(file, destDir);
//                    continue;
//                }
//                if (!file.getName().endsWith(".jet")) continue;
//                String text = doLoadFile(file.getParentFile().getAbsolutePath(), file.getName());
//                Pattern pattern = Pattern.compile("</?(error|warning)>");
//                String clearText = pattern.matcher(text).replaceAll("");
//                createAndCheckPsiFile(name, clearText);
//
//                BindingContext bindingContext = AnalyzingUtils.getInstance(ImportingStrategy.NONE).analyzeFileWithCache((JetFile) myFile);
//                String expectedText = CheckerTestUtil.addDiagnosticMarkersToText(myFile, bindingContext).toString();
//
//                File destFile = new File(dest, file.getName());
//                FileWriter fileWriter = new FileWriter(destFile);
//                fileWriter.write(expectedText);
//                fileWriter.close();
//            }
//            catch (RuntimeException e) {
//                e.printStackTrace();
//            }
//        }
//    }

    public static Test suite() {
        return JetTestCaseBuilder.suiteForDirectory(JetTestCaseBuilder.getTestDataPathBase(), "/diagnostics/tests", true, new JetTestCaseBuilder.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name, @NotNull File file) {
                return new JetDiagnosticsTest(dataPath, name);
            }
        });
    }
}
