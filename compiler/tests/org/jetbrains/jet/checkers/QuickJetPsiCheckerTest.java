package org.jetbrains.jet.checkers;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.TextRange;
import junit.framework.Test;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetLiteFixture;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.ImportingStrategy;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacade;
import org.jetbrains.jet.lang.resolve.java.JavaDefaultImports;

import java.util.List;

/**
 * @author abreslav
 */
public class QuickJetPsiCheckerTest extends JetLiteFixture {
    private String name;
    
    public QuickJetPsiCheckerTest(@NonNls String dataPath, String name) {
        super(dataPath);
        this.name = name;
    }

    @Override
    public String getName() {
        return "test" + name;
    }

    @Override
    public void runTest() throws Exception {
        String expectedText = loadFile(name + ".jet");
        List<CheckerTestUtil.DiagnosedRange> diagnosedRanges = Lists.newArrayList();
        String clearText = CheckerTestUtil.parseDiagnosedRanges(expectedText, diagnosedRanges);

        createAndCheckPsiFile(name, clearText);
        JetFile jetFile = (JetFile) myFile;
        boolean loadStdlib = !expectedText.contains("-STDLIB");
        boolean importJdk = expectedText.contains("+JDK");
        
        ImportingStrategy importingStrategy = importJdk ? JavaDefaultImports.JAVA_DEFAULT_IMPORTS : ImportingStrategy.NONE;
        
        BindingContext bindingContext = AnalyzerFacade.analyzeFileWithCache(AnalyzingUtils.getInstance(importingStrategy, loadStdlib), jetFile, AnalyzerFacade.SINGLE_DECLARATION_PROVIDER);

        CheckerTestUtil.diagnosticsDiff(diagnosedRanges, bindingContext.getDiagnostics(), new CheckerTestUtil.DiagnosticDiffCallbacks() {
            @Override
            public void missingDiagnostic(String type, int expectedStart, int expectedEnd) {
                String message = "Missing " + type + DiagnosticUtils.atLocation(myFile, new TextRange(expectedStart, expectedEnd));
                System.err.println(message);
            }

            @Override
            public void unexpectedDiagnostic(String type, int actualStart, int actualEnd) {
                String message = "Unexpected " + type + DiagnosticUtils.atLocation(myFile, new TextRange(actualStart, actualEnd));
                System.err.println(message);
            }
        });

        String actualText = CheckerTestUtil.addDiagnosticMarkersToText(jetFile, bindingContext).toString();

        assertEquals(expectedText, actualText);
        
//        convert(new File(myFullDataPath + "/../../checker/"), new File(myFullDataPath));
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
                return new QuickJetPsiCheckerTest(dataPath, name);
            }
        });
    }
}
