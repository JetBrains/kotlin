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
import java.util.*;

public class AndroidXmlTest extends TestCaseWithTmpdir {

    private static final String singleFilePrefix = getTestDataPath() + "/converter/singleFile/";
    public static final String singleFileManifestPath = singleFilePrefix + "AndroidManifest.xml";
    private final File singleFileDir = new File(singleFilePrefix + "res/layout/");
    private final File fakeActivitySrc = new File(getTestDataPath() + "/fakeHelpers/Activity.kt");
    private final File fakeViewSrc = new File(getTestDataPath() + "/fakeHelpers/View.kt");
    private final File fakeWidgetsSrc = new File(getTestDataPath() + "/fakeHelpers/Widgets.kt");
    private final File fakeMyActivitySrc = new File(singleFilePrefix + "MyActivity.kt");
    private final String singleFileResPath = singleFilePrefix + "res/layout/";

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
        configuration.put(JVMConfigurationKeys.ANDROID_MANIFEST, singleFileManifestPath);
        return JetCoreEnvironment.createForTests(getTestRootDisposable(),
                                                                                  configuration);
    }

    public void testCompileResult() throws Exception {
        List<File> files = addDefaultFiles();

        compileManyFilesGetGenerationState(files, singleFileResPath);
        Disposer.dispose(getTestRootDisposable());
    }

    private List<File> addDefaultFiles() {
        File fakeRClass = new File(getTestDataPath() + "/converter/singleFile/R.kt");
        List<File> files = new ArrayList<File>();
        files.add(fakeActivitySrc);
        files.add(fakeViewSrc);
        files.add(fakeRClass);
        files.add(fakeWidgetsSrc);
        return files;
    }

    public void testConverterOneFile() throws Exception {
        JetCoreEnvironment jetCoreEnvironment = getEnvironment(singleFileResPath);
        AndroidUIXmlProcessor parser = new CliAndroidUIXmlProcessor(jetCoreEnvironment.getProject(),
                                                                    singleFileDir.getAbsolutePath(),
                                                                    singleFileManifestPath);

        String actual = parser.parseToString();
        String expected = loadOrCreate(new File(getTestDataPath() + "/converter/singleFile/layout.kt"), actual);

        assertEquals(expected, actual);
    }

    public void testGeneratedByteCode() throws Exception {

        String resPath = getTestDataPath() + "/converter/singleFile/res/layout/";
        List<File> files = addDefaultFiles();

        files.add(fakeMyActivitySrc);
        GenerationState state = compileManyFilesGetGenerationState(files, resPath);
        ByteArrayClassLoader classLoader = new ByteArrayClassLoader(new URL[] {}, getClass().getClassLoader(), state.getFactory().asList());
        classLoader.loadFiles();
        Class<?> activity = classLoader.findClass("com.myapp.MyActivity");
        String res =(String) activity.getMethod("test").invoke(activity.newInstance());
        Disposer.dispose(getTestRootDisposable());
        assertEquals("OK", res);
    }
}
