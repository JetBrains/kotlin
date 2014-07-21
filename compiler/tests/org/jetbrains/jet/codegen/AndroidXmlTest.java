package org.jetbrains.jet.codegen;

import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.android.AndroidUIXmlParser;
import org.jetbrains.jet.lang.resolve.android.CliAndroidUIXmlParser;
import org.jetbrains.jet.test.TestCaseWithTmpdir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class AndroidXmlTest extends TestCaseWithTmpdir {

    private final File singleFileDir = new File(getTestDataPath() + "/converter/singleFile/res/");
    private final File fakeActivitySrc = new File(getTestDataPath() + "/fakeHelpers/Activity.kt");
    private final File fakeViewSrc = new File(getTestDataPath() + "/fakeHelpers/View.kt");
    private final File fakeWidgetsSrc = new File(getTestDataPath() + "/fakeHelpers/Widgets.kt");

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @NotNull
    private static String getTestDataPath() {
        return JetTestCaseBuilder.getTestDataPathBase() + "/android";
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "IOResourceOpenedButNotSafelyClosed"})
    protected static String loadOrCreate(File file, String data) throws IOException {
        try {
            return new Scanner(file, "UTF-8" ).useDelimiter("\\A").next();
        } catch (IOException e) {
            file.createNewFile();
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(data);
            fileWriter.close();
            fail("Empty expected data, creating from actual");
            return data;
        }
    }

    public void testCompileResult() throws Exception {
        JetCoreEnvironment jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(getTestRootDisposable(),
                                                                                                            ConfigurationKind.ALL);
        String text = new CliAndroidUIXmlParser(jetCoreEnvironment.getProject(), singleFileDir.getAbsolutePath()).parseToString();

        JetFile psiFile = JetTestUtils.createFile("dummy.kt", text, jetCoreEnvironment.getProject());
        JetFile fakeActivity = JetTestUtils.loadJetFile(jetCoreEnvironment.getProject(), fakeActivitySrc);
        JetFile fakeView = JetTestUtils.loadJetFile(jetCoreEnvironment.getProject(), fakeViewSrc);
        JetFile fakeWidgets = JetTestUtils.loadJetFile(jetCoreEnvironment.getProject(), fakeWidgetsSrc);
        JetFile fakeRClass = JetTestUtils.loadJetFile(jetCoreEnvironment.getProject(),
                                                      new File(getTestDataPath() + "/converter/singleFile/R.kt"));
        List<JetFile> files = new ArrayList<JetFile>();
        files.add(psiFile);
        files.add(fakeActivity);
        files.add(fakeView);
        files.add(fakeRClass);
        files.add(fakeWidgets);
        GenerationUtils.compileManyFilesGetGenerationStateForTest(jetCoreEnvironment.getProject(), files);
        Disposer.dispose(getTestRootDisposable());
    }

    public void testConverterOneFile() throws Exception {
        JetCoreEnvironment jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(getTestRootDisposable(),
                                                                                                            ConfigurationKind.ALL);
        AndroidUIXmlParser parser = new CliAndroidUIXmlParser(jetCoreEnvironment.getProject(), singleFileDir.getAbsolutePath());

        String actual = parser.parseToString();
        String expected = loadOrCreate(new File(getTestDataPath() + "/converter/singleFile/layout.kt"), actual);

        assertEquals(expected, actual);
    }
}
