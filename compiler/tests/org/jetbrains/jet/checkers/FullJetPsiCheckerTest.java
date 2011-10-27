package org.jetbrains.jet.checkers;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.TextRange;
import junit.framework.Test;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetLiteFixture;
import org.jetbrains.jet.JetTestCaseBase;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacade;

import java.util.List;

/**
 * @author abreslav
 */
public class FullJetPsiCheckerTest extends JetLiteFixture {
    private final String myDataPath;
    private String myName;

    public FullJetPsiCheckerTest(@NonNls String dataPath, String name) {
        myDataPath = dataPath;
        myName = name;
    }


    @Override
    public void runTest() throws Exception {
        String fileName = myName + ".jet";
        String fullPath = myDataPath + "/" + fileName;


        String expectedText = loadFile(fullPath);

        List<CheckerTestUtil.DiagnosedRange> diagnosedRanges = Lists.newArrayList();
        String clearText = CheckerTestUtil.parseDiagnosedRanges(expectedText, diagnosedRanges);

        myFile = createPsiFile(myName, clearText);
        BindingContext bindingContext = AnalyzerFacade.analyzeFileWithCache(myFile);

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

        String actualText = CheckerTestUtil.addDiagnosticMarkersToText(myFile, bindingContext).toString();

        assertEquals(expectedText, actualText);

//        String myFullDataPath = getTestDataPath() + getDataPath();
//        System.out.println("myFullDataPath = " + myFullDataPath);
//        convert(new File(myFullDataPath + "/../../checker/"), new File(myFullDataPath));
    }

/*
    private void convert(File src, File dest) throws IOException {
        File[] files = src.listFiles();
        for (File file : files) {
            try {
                if (file.isDirectory()) {
                    File destDir = new File(dest, file.getName());
                    destDir.mkdir();
                    convert(file, destDir);
                    continue;
                }
                if (!file.getName().endsWith(".jet")) continue;
                String text = loadFile(file.getAbsolutePath());
                Pattern pattern = Pattern.compile("</?(error|warning|info( descr=\"[\\w ]+\")?)>");
                String clearText = pattern.matcher(text).replaceAll("");

                configureFromFileText(file.getName(), clearText);

                BindingContext bindingContext = AnalyzerFacade.analyzeFileWithCache((JetFile) myFile);
                String expectedText = CheckerTestUtil.addDiagnosticMarkersToText(myFile, bindingContext).toString();

                File destFile = new File(dest, file.getName());
                FileWriter fileWriter = new FileWriter(destFile);
                fileWriter.write(expectedText);
                fileWriter.close();
            }
            catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }
*/

    public static Test suite() {
        return JetTestCaseBase.suiteForDirectory(JetTestCaseBase.getTestDataPathBase(), "/checkerWithErrorTypes/full/", true, new JetTestCaseBase.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name) {
                return new FullJetPsiCheckerTest(dataPath, name);
            }
        });
    }
}
