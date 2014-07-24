package org.jetbrains.jet.lang.resolve.android;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.*;
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.codegen.GenerationUtils;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.test.TestCaseWithTmpdir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class AndroidXmlTest extends TestCaseWithTmpdir {

    private final File singleFileDir = new File(getTestDataPath() + "/converter/singleFile/res/layout/");
    private final File fakeActivitySrc = new File(getTestDataPath() + "/fakeHelpers/Activity.kt");
    private final File fakeViewSrc = new File(getTestDataPath() + "/fakeHelpers/View.kt");
    private final File fakeWidgetsSrc = new File(getTestDataPath() + "/fakeHelpers/Widgets.kt");
    private final File fakeMyActivitySrc = new File(getTestDataPath() + "/converter/singleFile/MyActivity.kt");
    private final String singleFileResPath = getTestDataPath() + "/converter/singleFile/res/layout/";

    public static class ByteClassLoader extends URLClassLoader {

        private Queue<OutputFile> q;
        private final Map<String, Class> extraClassDefs = new HashMap<String, Class>();

        @NotNull
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            final Class aClass = extraClassDefs.get(name);
            if (aClass != null) return aClass;
            else return super.findClass(name);
        }

        public ByteClassLoader(URL[] urls, ClassLoader parent, List<OutputFile> files) {
            super(urls, parent);
            q = new LinkedList<OutputFile>(files);
        }

        private void loadBytes(OutputFile f) {
            try {
                final byte[] b = f.asByteArray();
                final String name = relPathToClassName(f.getRelativePath());
                final Class<?> aClass = defineClass(name, b, 0, b.length);
                extraClassDefs.put(name, aClass);
                q.remove(f);
            } catch (NoClassDefFoundError e) {
                OutputFile found = findByClassName(e.getMessage());
                if (found == null) throw e;
                else {
                    loadBytes(found);
                    loadBytes(f);
                }
            }
        }

        public void loadFiles() {
            OutputFile f = q.peek();
            while (f != null) {
                loadBytes(f);
                f = q.peek();
            }
        }

        private OutputFile findByClassName(String name) {
            String path = classNameToRelPath(name);
            for (OutputFile file: q)
                if (file.getRelativePath().equals(path)) return file;
            return null;
        }

        private String relPathToClassName(String path) {
            return path.replace(".class", "").replace("/", ".");
        }

        private String classNameToRelPath(String name) {
            return name.replace(".", "/").concat(".class");
        }
    }

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

    private GenerationState compileManyFilesGetGenerationState(List<File> files, String resPath) throws IOException {
        JetCoreEnvironment jetCoreEnvironment = getEnvironment(resPath);
        Project project = jetCoreEnvironment.getProject();

        List<JetFile> jetFiles = new ArrayList<JetFile>(files.size());
        for (File file: files) {
            jetFiles.add(JetTestUtils.loadJetFile(project, file));
        }

        return GenerationUtils.compileManyFilesGetGenerationStateForTest(project, jetFiles);
    }

    private JetCoreEnvironment getEnvironment(String resPath) {
        CompilerConfiguration configuration = JetTestUtils.compilerConfigurationForTests(ConfigurationKind.ALL,
                                                                                         TestJdkKind.MOCK_JDK);
        configuration.put(JVMConfigurationKeys.ANDROID_RES_PATH, resPath);
        return JetCoreEnvironment.createForTests(getTestRootDisposable(),
                                                                                  configuration);
    }

    public void testCompileResult() throws Exception {
        List<File> files = addDefaultFiles();

        compileManyFilesGetGenerationState(files, singleFileResPath);
        Disposer.dispose(getTestRootDisposable());
    }

    private List<File> addDefaultFiles() {
        final File fakeRClass = new File(getTestDataPath() + "/converter/singleFile/R.kt");
        List<File> files = new ArrayList<File>();
        files.add(fakeActivitySrc);
        files.add(fakeViewSrc);
        files.add(fakeRClass);
        files.add(fakeWidgetsSrc);
        return files;
    }

    public void testConverterOneFile() throws Exception {
        JetCoreEnvironment jetCoreEnvironment = getEnvironment(singleFileResPath);
        AndroidUIXmlParser parser = new CliAndroidUIXmlParser(jetCoreEnvironment.getProject(), singleFileDir.getAbsolutePath());

        String actual = parser.parseToString();
        String expected = loadOrCreate(new File(getTestDataPath() + "/converter/singleFile/layout.kt"), actual);

        assertEquals(expected, actual);
    }

    public void testGeneratedByteCode() throws Exception {

        String resPath = getTestDataPath() + "/converter/singleFile/res/layout/";
        List<File> files = addDefaultFiles();

        files.add(fakeMyActivitySrc);
        GenerationState state = compileManyFilesGetGenerationState(files, resPath);
        ByteClassLoader classLoader = new ByteClassLoader(new URL[] {}, getClass().getClassLoader(), state.getFactory().asList());
        classLoader.loadFiles();
        final Class<?> activity = classLoader.findClass("com.myapp.MyActivity");
        String res =(String) activity.getMethod("test").invoke(activity.newInstance());
        Disposer.dispose(getTestRootDisposable());
        assertEquals("OK", res);
    }
}
