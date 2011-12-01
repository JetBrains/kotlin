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
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.ImportingStrategy;
import org.jetbrains.jet.lang.resolve.java.JavaDefaultImports;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author abreslav
 */
public class JetDiagnosticsTest extends JetLiteFixture {
    private String name;
    public static final Pattern FILE_PATTERN = Pattern.compile("//\\s*FILE:\\s*(.*)$", Pattern.MULTILINE);

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
        }

        public void getActualText(BindingContext bindingContext, StringBuilder actualText) {
            CheckerTestUtil.diagnosticsDiff(diagnosedRanges, CheckerTestUtil.getDiagnosticsIncludingSyntaxErrors(bindingContext, jetFile), new CheckerTestUtil.DiagnosticDiffCallbacks() {
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

        List<TestFile> testFileFiles = createTestFiles(testFileName, expectedText);

        boolean importJdk = expectedText.contains("+JDK");
        ImportingStrategy importingStrategy = importJdk ? JavaDefaultImports.JAVA_DEFAULT_IMPORTS : ImportingStrategy.NONE;

        List<JetDeclaration> namespaces = Lists.newArrayList();
        for (TestFile testFileFile : testFileFiles) {
            namespaces.add(testFileFile.jetFile.getRootNamespace());
        }

        BindingContext bindingContext = AnalyzingUtils.getInstance(importingStrategy).analyzeNamespaces(getProject(), namespaces, Predicates.<PsiFile>alwaysTrue(), JetControlFlowDataTraceFactory.EMPTY);

        StringBuilder actualText = new StringBuilder();
        for (TestFile testFileFile : testFileFiles) {
            testFileFile.getActualText(bindingContext, actualText);
        }

        assertEquals(expectedText, actualText.toString());
    }

    private List<TestFile> createTestFiles(String testFileName, String expectedText) {
        List<TestFile> testFileFiles = Lists.newArrayList();
        Matcher matcher = FILE_PATTERN.matcher(expectedText);
        if (!matcher.find()) {
            // One file
            testFileFiles.add(new TestFile(testFileName, expectedText));
        }
        else {
            int processedChars = 0;
            // Many files
            while (true) {
                String fileName = matcher.group(1);
                int start = matcher.start();
                assertTrue("Characters skipped from " + processedChars + " to " + matcher.start(), start == processedChars);

                boolean nextFileExists = matcher.find();
                int end;
                if (nextFileExists) {
                    end = matcher.start();
                }
                else {
                    end = expectedText.length();
                }
                String fileText = expectedText.substring(start, end);
                processedChars = end;

                testFileFiles.add(new TestFile(fileName, fileText));

                if (!nextFileExists) break;
            }
            assertTrue("Characters skipped from " + processedChars + " to " + (expectedText.length() - 1), processedChars == expectedText.length());
        }
        return testFileFiles;
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
        return JetTestCaseBuilder.suiteForDirectory(JetTestCaseBuilder.getTestDataPathBase(), "/checkerWithErrorTypes/quick", true, new JetTestCaseBuilder.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name) {
                return new JetDiagnosticsTest(dataPath, name);
            }
        });
    }
}
