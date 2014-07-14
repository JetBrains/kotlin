package org.jetbrains.jet.codegen;

import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.android.AndroidUIXmlParser;
import org.jetbrains.jet.test.TestCaseWithTmpdir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class AndroidXmlTest extends TestCaseWithTmpdir {

    private final File singleFile = new File(getTestDataPath() + "/converter/singleFile/layout.xml");
    private final File fakeActivity = new File(getTestDataPath() + "/fakeHelpers/Activity.kt");
    private final File fakeView = new File(getTestDataPath() + "/fakeHelpers/View.kt");

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
        ArrayList<File> paths = new ArrayList<File>();
        paths.add(singleFile);
        String text = new AndroidUIXmlParser(null, paths).parse();
        JetCoreEnvironment jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(getTestRootDisposable(),
                                                                                                            ConfigurationKind.ALL);
        JetFile psiFile = JetTestUtils.createFile(singleFile.getName(), text, jetCoreEnvironment.getProject());
        JetFile fakeActivityClass = JetTestUtils.loadJetFile(jetCoreEnvironment.getProject(), fakeActivity);
        JetFile fakeViewClass = JetTestUtils.loadJetFile(jetCoreEnvironment.getProject(), fakeView);
        JetFile fakeRClass = JetTestUtils.loadJetFile(jetCoreEnvironment.getProject(),
                                                      new File(getTestDataPath() + "/converter/singleFile/R.kt"));
        List<JetFile> files = new ArrayList<JetFile>();
        files.add(psiFile);
        files.add(fakeActivityClass);
        files.add(fakeViewClass);
        files.add(fakeRClass);
        GenerationUtils.compileManyFilesGetGenerationStateForTest(jetCoreEnvironment.getProject(), files);
        Disposer.dispose(getTestRootDisposable());
    }

    public void testConverterOneFile() throws Exception {
        ArrayList<File> paths = new ArrayList<File>();
        paths.add(singleFile);
        AndroidUIXmlParser parser = new AndroidUIXmlParser(null, paths);

        String actual = parser.parse();
        String expected = loadOrCreate(new File(getTestDataPath() + "/converter/singleFile/layout.kt"), actual);

        assertEquals(expected, actual);
    }
}
